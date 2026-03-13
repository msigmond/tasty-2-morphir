package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.core.{Contexts, Names}
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{Value, Type as MorphType}
import morphir.sdk.List as MorphList

import scala.quoted.Quotes
import scala.util.{Failure, Try}

object ApplyMorph extends TreeResolver {

  def toValue(apl: Trees.Apply[?], inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    apl match {
      case Trees.Apply(sel: Trees.Select[?], args) =>
        for {
          returnType <- resolveType(apl, inferredGenericTypeArgs)
          maybeGenericTypeArgs = returnType.extractGenericTypeArgs
          argument <- getFunctionArgument(args, maybeGenericTypeArgs)
          function <- sel.toValue(maybeGenericTypeArgs)
        } yield
          Value.Value.Apply(
            returnType,
            function,
            argument
          )

      case Trees.Apply(functionId: Trees.Ident[?], args) =>
        for {
          returnType <- resolveType(apl, inferredGenericTypeArgs)
          maybeGenericTypeArgs = returnType.extractGenericTypeArgs
          theApply <- toValue(functionId, returnType, args.reverse, maybeGenericTypeArgs)
        } yield
          theApply

      case Trees.Apply(fun: Trees.TypeApply[?], args) =>
        for {
          returnType <- resolveType(apl, inferredGenericTypeArgs)
          maybeGenericTypeArgs = returnType.extractGenericTypeArgs
          argument <- getFunctionArgument(args, maybeGenericTypeArgs)
          function <- toValue(fun, inferredGenericTypeArgs)
        } yield
          Value.Value.Apply(
            returnType,
            function,
            argument
          )

      case Trees.Apply(fun, args) => Failure(Exception(s"Apply could not be processed: Apply(${fun.getClass},[${args.map(_.getClass).mkString(",")}])"))
    }
  }

  def toValue(apl: Trees.TypeApply[?], inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    apl match {
      case Trees.TypeApply(Trees.Select(id: Trees.Ident[?],methodName), args) if methodName.show == "apply" =>
        for {
          returnType <- resolveType(apl, inferredGenericTypeArgs)
          constructor <- StandardFunctions.get(id.symbol, returnType)
        } yield
          constructor

      case Trees.TypeApply(fun, args) => Failure(Exception(s"Apply could not be processed: TypeApply(${fun.getClass},[${args.map(_.getClass).mkString(",")}])"))
    }
  }

  def toValue(functionId: Trees.Ident[?],
              returnType: MorphType.Type[Unit],
              argsReversed: List[Trees.Tree[?]],
              inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]])(using Quotes)(using Contexts.Context): Try[Value.Value.Apply[Unit, MorphType.Type[Unit]]] = {
    val maybeGenericTypeArgs = returnType.extractGenericTypeArgs
    argsReversed match {
      case head :: Nil =>
        for {
          argument <- getFunctionArgument(List(head), maybeGenericTypeArgs)
          argumentType <- argument.extractType
          functionFQN <- functionId.toFQN
        } yield
          Value.Value.Apply(
            returnType,
            Value.Value.Reference(
              MorphType.Function(
                (),
                argumentType,
                returnType
              ),
              functionFQN
            ),
            argument
          )

      case head :: tail =>
        for {
          argument <- getFunctionArgument(List(head), maybeGenericTypeArgs)
          argumentType <- argument.extractType
          nestedApplyReturnType = MorphType.Function(
            (),
            argumentType,
            returnType
          )
          nestedApply <- toValue(functionId, nestedApplyReturnType, tail, maybeGenericTypeArgs)
        } yield
          Value.Value.Apply(
            returnType,
            nestedApply,
            argument
          )

      case x => Failure(Exception(s"Apply arguments is unexpected: $x"))
    }
  }

  private def getFunctionArgument(args: List[Trees.Tree[?]], inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    Try(args)
      .map {
        case oneElem :: Nil => oneElem
        case moreElem => throw Exception(s"Number of args: ${moreElem.size}, but only one is supported")
      }
      .map(oneElem => expandSubTree(oneElem, inferredGenericTypeArgs))
      .flatten
  }
}
