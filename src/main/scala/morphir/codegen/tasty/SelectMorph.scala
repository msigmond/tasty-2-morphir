package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees.Select
import dotty.tools.dotc.core.{Contexts, Names}
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{Value, Type as MorphType}
import morphir.sdk.List as MorphList

import scala.quoted.Quotes
import scala.util.Try

object SelectMorph extends TreeResolver {

  def toValue(sel: Select[?], inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    sel match {
      case Select(qualifier, _) =>
        for {
          returnType <- resolveType(sel, inferredGenericTypeArgs)
          maybeGenericTypeArgs = returnType.extractGenericTypeArgs
          argument <- expandSubTree(qualifier, maybeGenericTypeArgs)
          argumentType <- argument.extractType
          function <- StandardFunctions.get(sel.symbol, returnType, argumentType)
        } yield
          Value.Value.Apply(
            MorphType.Function(
              (),
              argumentType,
              returnType
            ),
            function,
            argument
          )
    }
  }
}
