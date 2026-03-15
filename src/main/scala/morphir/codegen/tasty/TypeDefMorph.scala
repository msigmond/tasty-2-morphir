package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.core.{Contexts, Flags}
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{AccessControlled, Documented, Module, Name, Path, Value, Type as MorphType}
import morphir.sdk.{Dict, List as MorphList}

import scala.quoted.*
import scala.util.{Failure, Try}

object TypeDefMorph extends TreeResolver {

  def shouldIgnore(td: Trees.TypeDef[?])(using Quotes)(using Contexts.Context): Boolean =
    td.symbol.flags.is(Flags.Module) && td.symbol.companionClass.flags.is(Flags.Case)

  def toModule(td: Trees.TypeDef[?])(using Quotes)(using Contexts.Context): Try[(Module.ModuleName, AccessControlled.AccessControlled[Module.Definition[Unit, MorphType.Type[Unit]]])] = {
    td match {
      case Trees.TypeDef(name, Trees.Template(constructor, parentsOrDerived, self, preBody: List[?])) if td.symbol.flags.is(Flags.Case) =>
        toCaseClassModule(name.show, preBody)

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

  private def toCaseClassModule(name: String, preBody: List[?])(using Quotes)(using Contexts.Context): Try[(Module.ModuleName, AccessControlled.AccessControlled[Module.Definition[Unit, MorphType.Type[Unit]]])] = {
    for {
      typeAlias <- getTypeAlias(name, preBody)
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

  private def getTypeAlias(name: String, preBody: List[?])(using Quotes)(using Contexts.Context): Try[AccessControlled.AccessControlled[Documented.Documented[morphir.ir.Type.Definition[Unit]]]] = {
    val listOfTries: List[Try[morphir.ir.Type.Field[Unit]]] =
      preBody.collect {
        case vd: Trees.ValDef[?] if vd.symbol.flags.is(Flags.CaseAccessor) =>
          for {
            morphType <- resolveType(vd, inferredGenericTypeArgs = None)
          } yield morphir.ir.Type.Field(Name.fromString(vd.name.show), morphType)
      }

    listOfTries.toTryList.map { fields =>
      val typeDef = morphir.ir.Type.TypeAliasDefinition(
        MorphList.empty[Name.Name],
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

  private def getValues(preBody: List[?])(using Quotes)(using Contexts.Context): Try[Dict.Dict[Name.Name, AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]]]] = {
    val listOfTries: List[Try[(Name.Name, AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]])]] =
      preBody collect {
        case dd: Trees.DefDef[?] if !dd.name.startsWith("writeReplace") => dd.toValue
      }

    listOfTries.toTryList.map(_.toMap)
  }
}
