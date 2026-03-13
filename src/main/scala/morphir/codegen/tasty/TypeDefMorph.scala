package morphir.codegen.tasty

import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.core.Contexts
import morphir.codegen.tasty.MorphUtils.*
import morphir.ir.{AccessControlled, Documented, Module, Name, Path, Value, Type as MorphType}
import morphir.sdk.Dict

import scala.quoted.*
import scala.util.{Failure, Try}

object TypeDefMorph {

  def toModule(td: Trees.TypeDef[?])(using Quotes)(using Contexts.Context): Try[(Module.ModuleName, AccessControlled.AccessControlled[Module.Definition[Unit, MorphType.Type[Unit]]])] = {
    td match {
      case Trees.TypeDef(name, Trees.Template(constructor, parentsOrDerived, self, preBody: List[?])) =>
        for {
          values <- getValues(preBody)
        } yield {
          val moduleName = Path.fromList(List(Name.fromString(name.show)))
          val moduleDef = AccessControlled.AccessControlled(
            access = AccessControlled.Access.Public,
            value = Module.Definition(
              types = Dict.empty,
              values = values,
              doc = None
            )
          )
          (moduleName, moduleDef)
        }

      case _ => Failure(Exception("TypeDef could not be processed"))
    }
  }

  private def getValues(preBody: List[?])(using Quotes)(using Contexts.Context): Try[Dict.Dict[Name.Name, AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]]]] = {
    val listOfTries: List[Try[(Name.Name, AccessControlled.AccessControlled[Documented.Documented[Value.Definition[Unit, MorphType.Type[Unit]]]])]] =
      preBody collect {
        case dd: Trees.DefDef[?] if !dd.name.startsWith("writeReplace") => dd.toValue
      }

    listOfTries.toTryList.map(_.toMap)
  }
}
