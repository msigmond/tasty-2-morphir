package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.core.{Contexts, Flags, Names}
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{Value, Type as MorphType}
import morphir.sdk.List as MorphList

import scala.quoted.Quotes
import scala.util.{Failure, Try}

object ApplyMorph extends TreeResolver {

  def toValue(apl: Trees.Apply[?], inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    apl match {
      case Trees.Apply(sel: Trees.Select[?], args) =>
        if isEnumConstructorApply(sel) then
          for {
            returnType <- resolveType(apl, inferredGenericTypeArgs)
            function <- toEnumConstructorValue(sel, args, returnType, inferredGenericTypeArgs)
            applied <- applyArguments(function, args, returnType.extractGenericTypeArgs)
          } yield
            applied
        else
          for {
            returnType <- resolveType(apl, inferredGenericTypeArgs)
            function <- sel.toValue(returnType.extractGenericTypeArgs)
            applied <- applyArguments(function, args, returnType.extractGenericTypeArgs)
          } yield
            applied

      case Trees.Apply(functionId: Trees.Ident[?], args) =>
        for {
          returnType <- resolveType(apl, inferredGenericTypeArgs)
          maybeGenericTypeArgs = returnType.extractGenericTypeArgs
          theApply <- toValue(functionId, returnType, args.reverse, maybeGenericTypeArgs)
        } yield
          theApply

      case Trees.Apply(fun: Trees.TypeApply[?], args) if isTupleApply(fun, args.size) =>
        for {
          returnType <- resolveType(apl, inferredGenericTypeArgs)
          elements <- args.map(expandSubTree(_, inferredGenericTypeArgs)).toTryList
        } yield
          Value.Value.Tuple(
            returnType,
            elements
          )

      case Trees.Apply(fun: Trees.TypeApply[?], args) if isListApply(fun) && hasListElements(args) =>
        for {
          returnType <- resolveType(apl, inferredGenericTypeArgs).orElse {
            inferredGenericTypeArgs match {
              case Some(typeArgs) if typeArgs.size == 1 => Try(StandardTypes.listReference(typeArgs))
              case _ => Failure(Exception("Could not resolve list type for List()"))
            }
          }
          elements <- extractListElements(args)
            .map(_.map(expandSubTree(_, returnType.extractGenericTypeArgs.orElse(inferredGenericTypeArgs))).toTryList)
            .getOrElse(Failure(Exception("Could not extract list elements from List()")))
        } yield
          Value.Value.List(
            returnType,
            elements
          )

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

      case Trees.Apply(fun, args) =>
        for {
          function <- expandSubTree(fun, inferredGenericTypeArgs)
          applied <- applyArguments(function, args, inferredGenericTypeArgs)
        } yield
          applied
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

  private def applyArguments(function: Value.Value[Unit, MorphType.Type[Unit]],
                             args: List[Trees.Tree[?]],
                             inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    args match {
      case Nil => Failure(Exception("Function application requires at least one argument"))
      case head :: tail =>
        for {
          argument <- expandSubTree(head, inferredGenericTypeArgs)
          nextReturnType <- function.extractType.flatMap {
            case MorphType.Function(_, _, returnType) => Try(returnType)
            case functionType => Failure(Exception(s"Cannot apply argument to non-function type: $functionType"))
          }
          applied = Value.Value.Apply(
            nextReturnType,
            function,
            argument
          )
          fullyApplied <- if (tail.isEmpty) {
            Try(applied)
          } else {
            applyArguments(applied, tail, nextReturnType.extractGenericTypeArgs.orElse(inferredGenericTypeArgs))
          }
        } yield
          fullyApplied
    }
  }

  private def isListApply(fun: Trees.TypeApply[?])(using Quotes)(using Contexts.Context): Boolean =
    fun match {
      case Trees.TypeApply(Trees.Select(id: Trees.Ident[?], methodName), _) if methodName.show == "apply" =>
        resolveNamespace(id.symbol) match {
          case "List" :: "scala" :: Nil => true
          case "List" :: "package" :: "scala" :: Nil => true
          case "List" :: "immutable" :: "collection" :: "scala" :: Nil => true
          case _ => false
        }
      case _ =>
        false
    }

  private def hasListElements(args: List[Trees.Tree[?]]): Boolean =
    extractListElements(args).nonEmpty

  private def extractListElements(args: List[Trees.Tree[?]]): Option[List[Trees.Tree[?]]] =
    args match {
      case Nil => Some(List.empty)
      case oneArg :: Nil => extractRepeatedArgElements(oneArg)
      case _ => None
    }

  private def extractRepeatedArgElements(arg: Trees.Tree[?]): Option[List[Trees.Tree[?]]] =
    arg match {
      case Trees.SeqLiteral(elements, _) => Some(elements)
      case Trees.Typed(expr, _) => extractRepeatedArgElements(expr)
      case Trees.Inlined(_, bindings, expansion) if bindings.isEmpty => extractRepeatedArgElements(expansion)
      case _ => None
    }

  private def isTupleApply(fun: Trees.TypeApply[?], arity: Int)(using Quotes)(using Contexts.Context): Boolean =
    fun match {
      case Trees.TypeApply(Trees.Select(id: Trees.Ident[?], methodName), _) if methodName.show == "apply" =>
        resolveNamespace(id.symbol) match {
          case tupleName :: "scala" :: Nil =>
            tupleName.stripPrefix("Tuple").toIntOption.contains(arity)
          case _ =>
            false
        }
      case _ =>
        false
    }

  private def isEnumConstructorApply(sel: Trees.Select[?])(using Quotes)(using Contexts.Context): Boolean =
    sel match {
      case Trees.Select(qualifier: Trees.Select[?], methodName) if methodName.show == "apply" =>
        isEnumConstructorReference(qualifier)
      case _ =>
        false
    }

  private def isEnumConstructorReference(sel: Trees.Select[?])(using Quotes)(using Contexts.Context): Boolean =
    (sel.symbol.flags.is(Flags.Case) && !sel.symbol.flags.is(Flags.CaseAccessor)) ||
      sel.symbol.companionClass.flags.is(Flags.Case)

  private def toEnumConstructorValue(
    sel: Trees.Select[?],
    args: List[Trees.Tree[?]],
    returnType: MorphType.Type[Unit],
    inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]]
  )(using Quotes)(using Contexts.Context): Try[Value.Value.Constructor[Unit, MorphType.Type[Unit]]] =
    sel match {
      case Trees.Select(qualifier: Trees.Select[?], _) =>
        for {
          argTypes <- args.map(expandSubTree(_, inferredGenericTypeArgs).flatMap(_.extractType)).toTryList
          constructor <- toConstructorFQName(qualifier.symbol)
          enumReturnType <- qualifier.symbol.owner.typeRef.toType(inferredGenericTypeArgs = None).orElse(Try(returnType))
        } yield
          Value.Value.Constructor(
            argTypes.foldRight(enumReturnType) { (argType, accType) =>
              MorphType.Function((), argType, accType)
            },
            constructor
          )

      case x =>
        Failure(Exception(s"Enum constructor application could not be resolved from: ${x.getClass}"))
    }
}
