package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees.Literal
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts
import morphir.ir.{Value, Literal as MorphLiteral, Type as MorphType}

import scala.quoted.Quotes
import scala.util.{Failure, Success, Try}

object LiteralMorph {
  def toLiteral(lit: Literal[?])(using Quotes)(using Contexts.Context): Try[(MorphType.Type[Unit], MorphLiteral.Literal)] =
    lit match {
      case Literal(Constant(value: Boolean)) =>
        Success((StandardTypes.boolReference, MorphLiteral.boolLiteral(value)))

      case Literal(Constant(value: Int)) =>
        Success((StandardTypes.intReference, MorphLiteral.intLiteral(value)))

      case Literal(Constant(value: Float)) =>
        Success((StandardTypes.floatReference, MorphLiteral.floatLiteral(value)))

      case Literal(Constant(value: Double)) =>
        Success((StandardTypes.floatReference, MorphLiteral.floatLiteral(value)))

      case Literal(Constant(value: String)) =>
        Success((StandardTypes.stringReference, MorphLiteral.stringLiteral(value)))

      case Literal(Constant(x)) => Failure(Exception(s"Literal not supported: ${x.getClass}"))
    }

  def toValue(lit: Literal[?])(using Quotes)(using Contexts.Context): Try[Value.Value.Literal[Unit, MorphType.Type[Unit]]] = {
    for {
      (morphType, morphLiteral) <- toLiteral(lit)
    } yield
      Value.Value.Literal(
        morphType,
        morphLiteral
      )
  }

  def toPattern(lit: Literal[?])(using Quotes)(using Contexts.Context): Try[Value.Pattern.LiteralPattern[MorphType.Type[Unit]]] = {
    for {
      (morphType, morphLiteral) <- toLiteral(lit)
    } yield
      Value.Pattern.LiteralPattern(
        morphType,
        morphLiteral
      )
    }
}
