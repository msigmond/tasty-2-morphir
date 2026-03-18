package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.core.Contexts
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{FQName, Name, Value, Type as MorphType}
import morphir.sdk.List as MorphList

import scala.quoted.Quotes
import scala.util.{Failure, Success, Try}

object MatchMorph extends TreeResolver {

  def toValue(
    mtch: Match[?],
    inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]]
  )(using Quotes)(using Contexts.Context): Try[Value.Value.PatternMatch[Unit, MorphType.Type[Unit]]] = {
    for {
      returnType <- resolveType(mtch, inferredGenericTypeArgs)
      branchOn <- expandSubTree(mtch.selector, inferredGenericTypeArgs)
      branchOnType <- branchOn.extractType
      maybeGenericTypeArgs = returnType.extractGenericTypeArgs
      cases <- mtch.cases.map(toCase(_, branchOnType, maybeGenericTypeArgs)).toTryList
    } yield
      Value.Value.PatternMatch(
        returnType,
        branchOn,
        cases
      )
  }

  private def toCase(
    caseDef: CaseDef[?],
    branchOnType: MorphType.Type[Unit],
    inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]]
  )(using Quotes)(using Contexts.Context): Try[(Value.Pattern[MorphType.Type[Unit]], Value.Value[Unit, MorphType.Type[Unit]])] = {
    for {
      _ <- if caseDef.guard.isEmpty then Success(())
      else Failure(NotImplementedError(s"Match guards are not supported: ${caseDef.guard.getClass}"))
      pattern <- toPattern(caseDef.pat, branchOnType)
      body <- expandSubTree(caseDef.body, inferredGenericTypeArgs)
    } yield
      (pattern, body)
  }

  private def toPattern(
    patternTree: Tree[?],
    expectedType: MorphType.Type[Unit]
  )(using Quotes)(using Contexts.Context): Try[Value.Pattern[MorphType.Type[Unit]]] =
    patternTree match {
      case Bind(name, body) =>
        for {
          bindType <- resolveType(patternTree, inferredGenericTypeArgs = None)
          innerPattern <- toPattern(body, bindType)
        } yield
          Value.Pattern.AsPattern(
            bindType,
            innerPattern,
            Name.fromString(name.show)
          )

      case Ident(name) if name.show == "_" =>
        Success(Value.Pattern.WildcardPattern(expectedType))

      case Typed(expr, _) =>
        toPattern(expr, expectedType)

      case lit: Literal[?] =>
        LiteralMorph.toPattern(lit)

      case UnApply(fun, _, patterns) =>
        for {
          constructorFQName <- toOptionConstructorFQName(fun)
          constructorArgs <- toConstructorArgs(patterns, expectedType, constructorFQName)
        } yield
          Value.Pattern.ConstructorPattern(
            expectedType,
            constructorFQName,
            constructorArgs
          )

      case Ident(name) if name.show == "None" =>
        Success(
          Value.Pattern.ConstructorPattern(
            expectedType,
            maybeConstructor("nothing"),
            List.empty
          )
        )

      case x =>
        Failure(NotImplementedError(s"Pattern is not supported: ${x.getClass}"))
    }

  private def toConstructorArgs(
    patterns: List[Tree[?]],
    expectedType: MorphType.Type[Unit],
    constructorFQName: FQName.FQName
  )(using Quotes)(using Contexts.Context): Try[List[Value.Pattern[MorphType.Type[Unit]]]] =
    constructorFQName match {
      case (_, _, localName) if localName == Name.fromString("just") =>
        expectedType.extractGenericTypeArgs match {
          case Some(innerType :: Nil) if patterns.size == 1 =>
            patterns.map(toPattern(_, innerType)).toTryList
          case Some(_) =>
            Failure(NotImplementedError(s"Option Some pattern must have exactly one argument: ${patterns.size}"))
          case None =>
            Failure(NotImplementedError(s"Could not resolve Some pattern type from: $expectedType"))
        }

      case (_, _, localName) if localName == Name.fromString("nothing") && patterns.isEmpty =>
        Success(List.empty)

      case (_, _, localName) =>
        Failure(NotImplementedError(s"Constructor pattern is not supported: $localName"))
    }

  private def toOptionConstructorFQName(fun: Tree[?])(using Quotes)(using Contexts.Context): Try[FQName.FQName] =
    unwrapTypeApply(fun) match {
      case Select(qualifier, name) if qualifier.symbol.name.show == "Some" && name.show == "unapply" =>
        Success(maybeConstructor("just"))
      case Select(qualifier, name) if qualifier.symbol.name.show == "None" && name.show == "unapply" =>
        Success(maybeConstructor("nothing"))
      case Select(_, name) if name.show == "unapplySeq" =>
        Failure(NotImplementedError("Sequence extractor patterns are not supported"))
      case x =>
        Failure(NotImplementedError(s"Pattern extractor is not supported: ${x.getClass}"))
    }

  private def unwrapTypeApply(tree: Tree[?]): Tree[?] =
    tree match {
      case TypeApply(fun, _) => unwrapTypeApply(fun)
      case other => other
    }

  private def maybeConstructor(localName: String): FQName.FQName =
    FQName.fqn("morphir.SDK")("maybe")(localName)
}
