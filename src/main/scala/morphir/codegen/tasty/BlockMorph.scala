package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.core.Contexts
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{Name, Value, Type as MorphType}
import morphir.sdk.List as MorphList

import scala.quoted.Quotes
import scala.util.{Failure, Try}

object BlockMorph extends TreeResolver {

  def toValue(block: Trees.Block[?], inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] =
    block match {
      case Trees.Block(stats, expr) =>
        toValue(stats, expr, inferredGenericTypeArgs)
    }

  def toValue(stats: List[Trees.Tree[?]],
              expr: Trees.Tree[?],
              inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    for {
      blockType <- resolveType(expr, inferredGenericTypeArgs)
      body <- expandSubTree(expr, blockType.extractGenericTypeArgs.orElse(inferredGenericTypeArgs))
      blockValue <- stats.foldRight(Try(body)) { (stat, acc) =>
        for {
          inValue <- acc
          letValue <- statToLetDefinition(stat, inValue, blockType, inferredGenericTypeArgs)
        } yield
          letValue
      }
    } yield
      blockValue
  }

  private def statToLetDefinition(stat: Trees.Tree[?],
                                  inValue: Value.Value[Unit, MorphType.Type[Unit]],
                                  blockType: MorphType.Type[Unit],
                                  inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    stat match {
      case valDef: Trees.ValDef[?] if !valDef.rhs.isEmpty =>
        for {
          definitionType <- resolveType(stat, inferredGenericTypeArgs)
          definitionBody <- expandSubTree(valDef.rhs, definitionType.extractGenericTypeArgs.orElse(inferredGenericTypeArgs))
        } yield
          Value.Value.LetDefinition(
            blockType,
            Name.fromString(valDef.name.show),
            Value.Definition(
              inputTypes = MorphList.empty,
              outputType = definitionType,
              body = definitionBody
            ),
            inValue
          )

      case valDef: Trees.ValDef[?] =>
        Failure(Exception(s"Local val without rhs is not supported: ${valDef.name.show}"))

      case x =>
        Failure(Exception(s"Block statement is not supported: ${x.getClass}"))
    }
  }
}
