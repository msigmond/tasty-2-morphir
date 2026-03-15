package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees.Literal
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts
import morphir.ir.{Value, Literal as MorphLiteral, Type as MorphType}

import scala.quoted.Quotes
import scala.util.{Failure, Success, Try}

object LiteralMorph {
  def toValue(lit: Literal[?])(using Quotes)(using Contexts.Context): Try[Value.Value.Literal[Unit, MorphType.Type[Unit]]] = {
    lit match {
      case Literal(Constant(value: Boolean)) =>
        Success(
          Value.Value.Literal(
            StandardTypes.boolReference,
            MorphLiteral.boolLiteral(value)
          )
        )

      case Literal(Constant(value: Int)) =>
        Success(
          Value.Value.Literal(
            StandardTypes.intReference,
            MorphLiteral.intLiteral(value)
          )
        )

      case Literal(Constant(value: Float)) =>
        Success(
          Value.Value.Literal(
            StandardTypes.floatReference,
            MorphLiteral.floatLiteral(value)
          )
        )

      case Literal(Constant(value: Double)) =>
        Success(
          Value.Value.Literal(
            StandardTypes.floatReference,
            MorphLiteral.floatLiteral(value)
          )
        )

      case Literal(Constant(value: String)) =>
        Success(
          Value.Value.Literal(
            StandardTypes.stringReference,
            MorphLiteral.stringLiteral(value)
          )
        )

      case Literal(Constant(x)) => Failure(Exception(s"Literal not supported: ${x.getClass}"))
    }
  }
}
