package morphir.codegen.tasty

import dotty.tools.dotc.core.{Contexts, Symbols}
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{Type as MorphType, *}

import scala.quoted.Quotes
import scala.util.{Failure, Success, Try}

object StandardFunctions {

  private def isDecimalType(morphType: MorphType.Type[Unit]): Boolean =
    morphType == StandardTypes.decimalReference

  def get(symbol: Symbols.Symbol, returnType: MorphType.Type[Unit], argumentType: MorphType.Type[Unit])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    get(symbol, returnType, Some(argumentType))
  }
  
  def get(symbol: Symbols.Symbol, returnType: MorphType.Type[Unit])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    get(symbol, returnType, None)
  }

  private def get(symbol: Symbols.Symbol, returnType: MorphType.Type[Unit], maybeArgumentType: Option[MorphType.Type[Unit]])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    val symbolNamespace = resolveNamespace(symbol)
    maybeArgumentType match {
      case Some(argumentType) =>
        val operator = symbol.name.show

        if (isDecimalType(argumentType)) {
          operator match {
            case "+"  => Success(toFunctionReference(FQName.fqn("morphir.SDK")("decimal")("add"), returnType, argumentType))
            case "-"  => Success(toFunctionReference(FQName.fqn("morphir.SDK")("decimal")("sub"), returnType, argumentType))
            case "*"  => Success(toFunctionReference(FQName.fqn("morphir.SDK")("decimal")("mul"), returnType, argumentType))
            case "/"  => Success(toFunctionReference(FQName.fqn("morphir.SDK")("decimal")("div.unsafe"), returnType, argumentType))
            case "<"  => Success(toFunctionReference(FQName.fqn("morphir.SDK")("decimal")("lt"), returnType, argumentType))
            case "<=" => Success(toFunctionReference(FQName.fqn("morphir.SDK")("decimal")("lte"), returnType, argumentType))
            case ">"  => Success(toFunctionReference(FQName.fqn("morphir.SDK")("decimal")("gt"), returnType, argumentType))
            case ">=" => Success(toFunctionReference(FQName.fqn("morphir.SDK")("decimal")("gte"), returnType, argumentType))
            case _    => bySymbolNamespace(symbolNamespace, returnType, argumentType)
          }
        } else {
          bySymbolNamespace(symbolNamespace, returnType, argumentType)
        }

      case _ =>
        symbolNamespace match {
          // Constructors
          case "None" :: "scala" :: Nil => Success(toConstructor(FQName.fqn("morphir.SDK")("maybe")("nothing"), returnType))
          case "Some" :: "scala" :: Nil => Success(toConstructor(FQName.fqn("morphir.SDK")("maybe")("just"), returnType))
          // If not implemented, return a failure
          case x => Failure(Exception(s"Constructor for symbol ${symbolNamespace.mkString(",")} not found."))
        }
    }
  }

  private def bySymbolNamespace(symbolNamespace: List[String], returnType: MorphType.Type[Unit], argumentType: MorphType.Type[Unit])(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] =
    symbolNamespace match {
      case "/" :: "Int" :: "scala" :: Nil => Success(toFunctionReference(FQName.fqn("morphir.SDK")("basics")("integerDivide"), returnType, argumentType))
      case "+" :: _ => Success(toFunctionReference(FQName.fqn("morphir.SDK")("basics")("add"), returnType, argumentType))
      case "-" :: _ => Success(toFunctionReference(FQName.fqn("morphir.SDK")("basics")("subtract"), returnType, argumentType))
      case "*" :: _ => Success(toFunctionReference(FQName.fqn("morphir.SDK")("basics")("multiply"), returnType, argumentType))
      case "/" :: _ => Success(toFunctionReference(FQName.fqn("morphir.SDK")("basics")("divide"), returnType, argumentType))
      case "<" :: _  => Success(toFunctionReference(FQName.fqn("morphir.SDK")("basics")("less.than"), returnType, argumentType))
      case "<=" :: _ => Success(toFunctionReference(FQName.fqn("morphir.SDK")("basics")("less.than.or.equal"), returnType, argumentType))
      case ">" :: _  => Success(toFunctionReference(FQName.fqn("morphir.SDK")("basics")("greater.than"), returnType, argumentType))
      case ">=" :: _ => Success(toFunctionReference(FQName.fqn("morphir.SDK")("basics")("greater.than.or.equal"), returnType, argumentType))
      case x => Failure(Exception(s"Standard function for symbol ${symbolNamespace.mkString(",")} not found."))
    }

  private def toFunctionReference(fQName: FQName.FQName, returnType: MorphType.Type[Unit], argumentType: MorphType.Type[Unit])(using Quotes)(using Contexts.Context): Value.Value.Reference[Unit, MorphType.Type[Unit]] = {
    Value.Value.Reference(
      MorphType.Function(
        (),
        argumentType,
        MorphType.Function(
          (),
          argumentType,
          returnType
        )
      ),
      fQName
    )
  }

  private def toConstructor(fQName: FQName.FQName, returnType: MorphType.Type[Unit])(using Quotes)(using Contexts.Context): Value.Value.Constructor[Unit, MorphType.Type[Unit]] = {
    Value.Value.Constructor(
      returnType,
      fQName
    )
  }
}
