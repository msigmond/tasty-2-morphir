package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.core.{Contexts, Names, Types}
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{AccessControlled, Documented, Name, Value, Type as MorphType}
import morphir.sdk.List as MorphList

import scala.quoted.Quotes
import scala.util.{Failure, Try}

object DefDefMorph extends TreeResolver {

  def toValue(dd: Trees.DefDef[?])(using Quotes)(using Contexts.Context): Try[(Name.Name, AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]])] = {
    dd match {
      case Trees.DefDef(methodName, valDefs: List[List[?]] @unchecked, outputTypeId: Trees.Ident[?], preRhs: Trees.Tree[?]) =>
        toMethodDefinition(methodName, valDefs, outputTypeId.typeOpt, preRhs)

      case Trees.DefDef(methodName, valDefs: List[List[?]] @unchecked, outputType, preRhs: Trees.Tree[?]) =>
        toMethodDefinition(methodName, valDefs, outputType.typeOpt, preRhs)

      case x => Failure(Exception(s"DefDef type not supported: ${x.getClass}"))
    }
  }

  private def toMethodDefinition(methodName: Names.TermName, valDefs: List[List[?]], outputTypeOpt: Types.Type, preRhs: Trees.Tree[?])(using Quotes)(using Contexts.Context): Try[(Name.Name, AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]])] = {
    for {
      inputTypes <- getInputTypes(valDefs)
      outputType <- outputTypeOpt.toType(inferredGenericTypeArgs = None) // Generic arguments or return type is expected to be defined on a method
      maybeGenericTypeArgs = outputType.extractGenericTypeArgs
      body <- expandSubTree(preRhs, maybeGenericTypeArgs)
    } yield {
      val valueDef = Value.Definition(
        inputTypes = inputTypes,
        outputType = outputType,
        body = body
      )

      val valueDoc = Documented.Documented(
        doc = "",
        value = valueDef
      )

      val valueAccessControlled = AccessControlled.AccessControlled(
        access = AccessControlled.Access.Public,
        value = valueDoc
      )

      (Name.fromString(methodName.show), valueAccessControlled)
    }
  }

  private def getInputTypes(valDefs: List[List[?]])(using Quotes)(using Contexts.Context): Try[MorphList.List[(Name.Name, MorphType.Type[Unit], MorphType.Type[Unit])]] = {
    val listOfTries: MorphList.List[Try[(Name.Name, MorphType.Type[Unit], MorphType.Type[Unit])]] =
      valDefs.flatten.map { case Trees.ValDef(paramName, tree: Trees.Tree[?], preRhs) =>
        for {
          morphType <- resolveType(tree, inferredGenericTypeArgs = None)
        } yield (Name.fromString(paramName.show), morphType, morphType)
      }

    listOfTries.toTryList
  }
}
