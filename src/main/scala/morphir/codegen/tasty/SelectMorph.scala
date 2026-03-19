package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.ast.Trees.Select
import dotty.tools.dotc.core.{Contexts, Flags}
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{Name, Value, Type as MorphType}
import morphir.sdk.List as MorphList

import scala.quoted.Quotes
import scala.util.Try

object SelectMorph extends TreeResolver {
  private val selfParamName = Name.fromString("this")

  def toValue(sel: Select[?], inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    sel match {
      case Select(_, _) if isEnumConstructor(sel) =>
        for {
          returnType <- resolveType(sel, inferredGenericTypeArgs)
          constructor <- toConstructorFQName(sel.symbol)
        } yield
          Value.Value.Constructor(
            returnType,
            constructor
          )

      case Select(qualifier: Trees.This[?], fieldName) if sel.symbol.flags.is(Flags.CaseAccessor) =>
        for {
          subjectType <- resolveType(qualifier, inferredGenericTypeArgs = None)
          returnType <- sel.symbol.denot.info.resultType.toType(subjectType.extractGenericTypeArgs.orElse(inferredGenericTypeArgs))
        } yield
          Value.Value.Field(
            returnType,
            Value.Value.Variable(subjectType, selfParamName),
            morphir.ir.Name.fromString(fieldName.show)
          )

      case Select(qualifier, fieldName) if sel.symbol.flags.is(Flags.CaseAccessor) =>
        for {
          subject <- expandSubTree(qualifier, inferredGenericTypeArgs = None)
          maybeGenericTypeArgs = subject.extractType.toOption.flatMap(_.extractGenericTypeArgs)
          returnType <- sel.symbol.denot.info.resultType.toType(maybeGenericTypeArgs.orElse(inferredGenericTypeArgs))
        } yield
          Value.Value.Field(
            returnType,
            subject,
            morphir.ir.Name.fromString(fieldName.show)
          )

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

  private def isEnumConstructor(sel: Select[?])(using Quotes)(using Contexts.Context): Boolean =
    sel.symbol.flags.is(Flags.Case) && !sel.symbol.flags.is(Flags.CaseAccessor)
}
