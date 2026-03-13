package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees.Ident
import dotty.tools.dotc.core.{Contexts, Symbols, Types}
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{FQName, Name, Path, Value, Type as MorphType}
import morphir.sdk.List as MorphList

import scala.quoted.*
import scala.util.{Failure, Success, Try}

object IdentMorph extends TreeResolver {

  def toPath(id: Ident[?])(using Quotes)(using Contexts.Context): Try[Path.Path] = {
    val path = resolveNamespace(id.symbol).map(Name.fromString).reverse
    Success(Path.fromList(path))
  }

  def toFQN(id: Ident[?])(using Quotes)(using Contexts.Context): Try[FQName.FQName] = {
    resolveNamespace(id.symbol) match {
      case localName :: moduleName :: packageName =>
        Success(FQName.fqn(packageName.reverse.mkString("."))(moduleName)(localName))
      case x =>
        Failure(Exception(s"Could not resolve FQName from: $x"))
    }
  }

  def toValue(id: Ident[?], inferredGenericTypeArgs: Option[MorphList.List[MorphType.Type[Unit]]] = None)(using Quotes)(using Contexts.Context): Try[Value.Value[Unit, MorphType.Type[Unit]]] = {
    id.typeOpt match {
      case Types.TermRef(prefix, sbl: Symbols.Symbol) =>
        for {
          typeRef <- sbl.localReturnType.toType(inferredGenericTypeArgs)
        } yield
          Value.Value.Variable(
            typeRef,
            Name.fromString(id.symbol.name.show)
          )

      case typeOpt: Types.Type =>
        for {
          typ <- typeOpt.toType(inferredGenericTypeArgs)
          fun <- StandardFunctions.get(id.symbol, typ)
        } yield 
          fun
    }
  }
}
