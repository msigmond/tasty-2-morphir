package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.core.{Contexts, Flags}
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{AccessControlled, Documented, Module, Name, Path, Value, Type as MorphType}
import morphir.sdk.{Dict, List as MorphList}

import scala.quoted.*
import scala.collection.immutable.ListMap
import scala.util.{Failure, Try}

object TypeDefMorph extends TreeResolver {

  def shouldIgnore(td: Trees.TypeDef[?])(using Quotes)(using Contexts.Context): Boolean =
    td.symbol.flags.is(Flags.Module) && td.symbol.companionClass.flags.is(Flags.Case)

  def toModule(td: Trees.TypeDef[?])(using Quotes)(using Contexts.Context): Try[(Module.ModuleName, AccessControlled.AccessControlled[Module.Definition[Unit, MorphType.Type[Unit]]])] = {
    td match {
      case Trees.TypeDef(name, Trees.Template(constructor, parentsOrDerived, self, preBody: List[?])) if td.symbol.flags.is(Flags.Case) =>
        toCaseClassModule(td, name.show, preBody)

      case Trees.TypeDef(name, Trees.Template(constructor, parentsOrDerived, self, preBody: List[?])) =>
        toObjectModule(name.show, preBody)

      case _ => Failure(Exception("TypeDef could not be processed"))
    }
  }

  private def toObjectModule(name: String, preBody: List[?])(using Quotes)(using Contexts.Context): Try[(Module.ModuleName, AccessControlled.AccessControlled[Module.Definition[Unit, MorphType.Type[Unit]]])] = {
    for {
      values <- getValues(preBody)
    } yield {
      val moduleName = Path.fromList(List(Name.fromString(name)))
      val moduleDef = AccessControlled.AccessControlled(
        access = AccessControlled.Access.Public,
        value = Module.Definition(
          types = Dict.empty,
          values = values,
          doc = None
        )
      )
      (moduleName, moduleDef)
    }
  }

  private def toCaseClassModule(td: Trees.TypeDef[?], name: String, preBody: List[?])(using Quotes)(using Contexts.Context): Try[(Module.ModuleName, AccessControlled.AccessControlled[Module.Definition[Unit, MorphType.Type[Unit]]])] = {
    for {
      typeAlias <- getTypeAlias(preBody)
    } yield {
      val typeName = Name.fromString(name)
      val moduleName = Path.fromList(List(typeName))
      val moduleDef = AccessControlled.AccessControlled(
        access = AccessControlled.Access.Public,
        value = Module.Definition(
          types = Dict.empty.updated(typeName, typeAlias),
          values = Dict.empty[Name.Name, AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]]],
          doc = None
        )
      )
      (moduleName, moduleDef)
    }
  }

  private def getTypeAlias(preBody: List[?])(using Quotes)(using Contexts.Context): Try[AccessControlled.AccessControlled[Documented.Documented[morphir.ir.Type.Definition[Unit]]]] = {
    val listOfTries: List[Try[morphir.ir.Type.Field[Unit]]] =
      preBody.collect {
        case vd: Trees.ValDef[?] if vd.symbol.flags.is(Flags.CaseAccessor) =>
          for {
            morphType <- vd.tpt.tpe.toType(inferredGenericTypeArgs = None)
          } yield morphir.ir.Type.Field(Name.fromString(vd.name.show), morphType)
      }

    listOfTries.toTryList.map { fields =>
      val typeParams =
        fields
          .flatMap(field => collectTypeVariables(field.tpe))
          .foldLeft(List.empty[Name.Name]) { (acc, typeParam) =>
            if acc.contains(typeParam) then acc else acc :+ typeParam
          }

      val typeDef = morphir.ir.Type.TypeAliasDefinition(
        typeParams,
        morphir.ir.Type.Record((), fields)
      )

      AccessControlled.AccessControlled(
        access = AccessControlled.Access.Public,
        value = Documented.Documented(
          doc = "",
          value = typeDef
        )
      )
    }
  }

  private def collectTypeVariables(tpe: MorphType.Type[Unit]): List[Name.Name] =
    tpe match {
      case MorphType.ExtensibleRecord(_, _, fields) =>
        fields.flatMap(field => collectTypeVariables(field.tpe))
      case MorphType.Function(_, argumentType, returnType) =>
        collectTypeVariables(argumentType) ++ collectTypeVariables(returnType)
      case MorphType.Record(_, fields) =>
        fields.flatMap(field => collectTypeVariables(field.tpe))
      case MorphType.Reference(_, _, typeArgs) =>
        typeArgs.flatMap(collectTypeVariables)
      case MorphType.Tuple(_, elements) =>
        elements.flatMap(collectTypeVariables)
      case MorphType.Variable(_, name) =>
        List(name)
      case MorphType.Unit(_) =>
        List.empty
    }

  private def getValues(preBody: List[?])(using Quotes)(using Contexts.Context): Try[Dict.Dict[Name.Name, AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]]]] = {
    val listOfTries: List[Try[(Name.Name, AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]])]] =
      preBody collect {
        case dd: Trees.DefDef[?] if !dd.name.startsWith("writeReplace") => dd.toValue
      }

    listOfTries.toTryList.map(
      _.zipWithIndex
        .map { case ((valueName, accessControlled), valueIndex) =>
          (valueName, normalizeLocalLetTypes(accessControlled, valueIndex))
        }
        .sortBy { case (valueName, _) => valueName.mkString(".") }
        .foldLeft(ListMap.empty[Name.Name, AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]]]) {
          case (acc, (valueName, valueDefinition)) => acc.updated(valueName, valueDefinition)
        }
    )
  }

  private def normalizeLocalLetTypes(
    accessControlled: AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]],
    valueIndex: Int
  ): AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]] = {
    val letOutputType = MorphType.Variable((), Name.fromList(List("t", (4 + valueIndex * 2).toString)))
    accessControlled.copy(
      value = accessControlled.value.copy(
        value = accessControlled.value.value.copy(
          body = normalizeLocalLetTypes(accessControlled.value.value.body, letOutputType)
        )
      )
    )
  }

  private def normalizeLocalLetTypes(
    value: Value.Value[Unit, MorphType.Type[Unit]],
    letOutputType: MorphType.Type[Unit]
  ): Value.Value[Unit, MorphType.Type[Unit]] =
    value match {
      case Value.Value.Apply(attributes, function, argument) =>
        Value.Value.Apply(attributes, normalizeLocalLetTypes(function, letOutputType), normalizeLocalLetTypes(argument, letOutputType))

      case Value.Value.Constructor(_, _) | Value.Value.FieldFunction(_, _) | Value.Value.Literal(_, _) |
          Value.Value.Reference(_, _) | Value.Value.Unit(_) | Value.Value.Variable(_, _) =>
        value

      case Value.Value.Destructure(attributes, pattern, valueToDestruct, inValue) =>
        Value.Value.Destructure(attributes, pattern, normalizeLocalLetTypes(valueToDestruct, letOutputType), normalizeLocalLetTypes(inValue, letOutputType))

      case Value.Value.Field(attributes, subjectValue, fieldName) =>
        Value.Value.Field(attributes, normalizeLocalLetTypes(subjectValue, letOutputType), fieldName)

      case Value.Value.IfThenElse(attributes, condition, thenBranch, elseBranch) =>
        Value.Value.IfThenElse(
          attributes,
          normalizeLocalLetTypes(condition, letOutputType),
          normalizeLocalLetTypes(thenBranch, letOutputType),
          normalizeLocalLetTypes(elseBranch, letOutputType)
        )

      case Value.Value.Lambda(attributes, argumentPattern, body) =>
        Value.Value.Lambda(attributes, argumentPattern, normalizeLocalLetTypes(body, letOutputType))

      case Value.Value.LetDefinition(attributes, valueName, definition, inValue) =>
        Value.Value.LetDefinition(
          attributes,
          valueName,
          definition.copy(
            outputType = letOutputType,
            body = normalizeLocalLetTypes(definition.body, letOutputType)
          ),
          normalizeLocalLetTypes(inValue, letOutputType)
        )

      case Value.Value.LetRecursion(attributes, definitions, inValue) =>
        Value.Value.LetRecursion(
          attributes,
          definitions.map { case (name, definition) =>
            (name, definition.copy(
              outputType = letOutputType,
              body = normalizeLocalLetTypes(definition.body, letOutputType)
            ))
          },
          normalizeLocalLetTypes(inValue, letOutputType)
        )

      case Value.Value.List(attributes, items) =>
        Value.Value.List(attributes, items.map(normalizeLocalLetTypes(_, letOutputType)))

      case Value.Value.PatternMatch(attributes, branchOn, cases) =>
        Value.Value.PatternMatch(
          attributes,
          normalizeLocalLetTypes(branchOn, letOutputType),
          cases.map { case (pattern, body) => (pattern, normalizeLocalLetTypes(body, letOutputType)) }
        )

      case Value.Value.Record(attributes, fields) =>
        Value.Value.Record(attributes, fields.map { case (fieldName, fieldValue) => (fieldName, normalizeLocalLetTypes(fieldValue, letOutputType)) })

      case Value.Value.Tuple(attributes, elements) =>
        Value.Value.Tuple(attributes, elements.map(normalizeLocalLetTypes(_, letOutputType)))

      case Value.Value.UpdateRecord(attributes, valueToUpdate, fieldsToUpdate) =>
        Value.Value.UpdateRecord(
          attributes,
          normalizeLocalLetTypes(valueToUpdate, letOutputType),
          fieldsToUpdate.map { case (fieldName, fieldValue) => (fieldName, normalizeLocalLetTypes(fieldValue, letOutputType)) }
        )
    }
}
