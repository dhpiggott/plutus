package plutus.scalafix

import scala.collection.mutable
import scala.meta._

import metaconfig.ConfDecoder
import metaconfig.Configured
import metaconfig.generic.Surface
import scalafix.lint.Diagnostic
import scalafix.v1._

// The part of CLAUDE.md's definition-order convention that is a property of
// pairs, checked one scope at a time. A scope is a file's top level, or the
// body of one object, class, trait, enum or extension block.
//
// A term (def, val, object, extension block) sits below every definition in its
// scope that uses it. A type (class, trait, enum or type alias, with any
// companion) sits immediately above the first definition in its scope that
// names it, with nothing in between but other types placed for that same
// definition.
//
// Definitions that reach each other through a cycle are left unordered, and so
// are definitions with no use between them: the depth-first tie-break that
// settles those is not checked.
class DefinitionOrder(config: DefinitionOrderConfig)
    extends SemanticRule("DefinitionOrder") {

  def this() = this(DefinitionOrderConfig.default)

  override def withConfiguration(
      configuration: Configuration
  ): Configured[Rule] =
    configuration.conf
      .getOrElse("DefinitionOrder")(config)
      .map(new DefinitionOrder(_))

  override def fix(implicit doc: SemanticDocument): Patch = {
    val path = DefinitionOrder.pathOf(doc)
    if (config.exclude.exists(_.r.findFirstIn(path).isDefined)) Patch.empty
    else new DefinitionOrder.FileCheck().patch
  }
}

object DefinitionOrder {

  private def pathOf(doc: SemanticDocument): String = {
    val path = doc.input match {
      case Input.File(file, _)        => file.toString
      case Input.VirtualFile(file, _) => file
      case other                      => other.syntax
    }
    path.replace('\\', '/')
  }

  private final case class Member(stat: Stat, index: Int, names: List[Name])

  private final case class Definition(
      index: Int,
      name: String,
      position: Position,
      isType: Boolean,
      stats: List[Stat],
      symbols: Set[Symbol]
  )

  private final class FileCheck(implicit doc: SemanticDocument) {

    def patch: Patch = scopes(doc.tree).map(checkScope).asPatch

    private def scopes(tree: Tree): List[List[Stat]] = tree match {
      case source: Source => scopesIn(source.stats)
      case pkg: Pkg       => scopesIn(pkg.body.stats)
      case _              => Nil
    }

    private def scopesIn(stats: List[Stat]): List[List[Stat]] = {
      val (packages, members) = stats.partition(_.isInstanceOf[Pkg])
      members :: packages.flatMap(scopes) ++ members.flatMap(nested)
    }

    private def nested(stat: Stat): List[List[Stat]] = {
      val body = stat match {
        case defn: Defn.Object         => defn.templ.body.stats
        case defn: Defn.Class          => defn.templ.body.stats
        case defn: Defn.Trait          => defn.templ.body.stats
        case defn: Defn.Enum           => defn.templ.body.stats
        case defn: Defn.ExtensionGroup => extensionMembers(defn)
        case _                         => Nil
      }
      if (body.isEmpty) Nil else body :: body.flatMap(nested)
    }

    private def checkScope(stats: List[Stat]): Patch = {
      val definitions = definitionsIn(stats)
      val owners = definitions.flatMap(d => d.symbols.map(_ -> d)).toMap
      val uses = definitions.map(d => d -> usedBy(d, owners)).toMap
      val component = stronglyConnected(definitions, uses)
      val edges = for {
        user <- definitions
        used <- uses(user).toList
        if component(user) != component(used)
      } yield (user, used)
      val usersOf = edges
        .groupBy { case (_, used) => used }
        .map { case (used, pairs) => used -> pairs.map(_._1).sortBy(_.index) }
      val firstUserOf = usersOf.map { case (used, users) => used -> users.head }
      definitions.flatMap { definition =>
        usersOf.get(definition).flatMap { users =>
          if (definition.isType)
            checkType(definition, users.head, definitions, firstUserOf)
          else checkTerm(definition, users)
        }
      }.asPatch
    }

    private def usedBy(
        definition: Definition,
        owners: Map[Symbol, Definition]
    ): Set[Definition] =
      definition.stats
        .flatMap(_.collect { case name: Name => name.symbol })
        .filterNot(_.isNone)
        .flatMap(ownerIn(owners, _))
        .filter(_ != definition)
        .toSet

    private def ownerIn(
        owners: Map[Symbol, Definition],
        symbol: Symbol
    ): Option[Definition] =
      Iterator
        .iterate(symbol)(_.owner)
        .takeWhile(s => !s.isNone && !s.isRootPackage && !s.isEmptyPackage)
        .flatMap(owners.get)
        .nextOption()

    // A class and its companion are one definition: the row-type convention
    // already orders them against each other, with the class reading the
    // companion and the companion constructing the class.
    private def definitionsIn(stats: List[Stat]): List[Definition] =
      stats.zipWithIndex
        .flatMap { case (stat, index) =>
          definedNames(stat).map(Member(stat, index, _))
        }
        .groupBy(companionKey)
        .values
        .toList
        .map { members =>
          val ordered = members.sortBy(_.index)
          Definition(
            index = ordered.head.index,
            name = ordered.head.names.head.value,
            position = ordered.head.names.head.pos,
            isType = ordered.exists(member => isType(member.stat)),
            stats = ordered.map(_.stat),
            symbols = ordered.flatMap(_.names.map(_.symbol)).toSet
          )
        }
        .sortBy(_.index)

    private def companionKey(member: Member): Either[String, Int] =
      member.stat match {
        case _: Defn.Object | _: Defn.Class | _: Defn.Trait | _: Defn.Enum =>
          Left(member.names.head.value)
        case _ => Right(member.index)
      }

    private def isType(stat: Stat): Boolean = stat match {
      case _: Defn.Class | _: Defn.Trait | _: Defn.Enum => true
      case _: Defn.Type | _: Decl.Type                  => true
      case _                                            => false
    }

    private def definedNames(stat: Stat): Option[List[Name]] = {
      val names = stat match {
        case defn: Defn.Def            => List(defn.name)
        case defn: Defn.Val            => defn.pats.flatMap(patternNames)
        case defn: Defn.Var            => defn.pats.flatMap(patternNames)
        case defn: Defn.Type           => List(defn.name)
        case defn: Defn.Class          => List(defn.name)
        case defn: Defn.Trait          => List(defn.name)
        case defn: Defn.Object         => List(defn.name)
        case defn: Defn.Enum           => List(defn.name)
        case defn: Defn.Given          => List(defn.name)
        case defn: Defn.GivenAlias     => List(defn.name)
        case defn: Defn.ExtensionGroup => extensionNames(defn)
        case decl: Decl.Def            => List(decl.name)
        case decl: Decl.Val            => decl.pats.flatMap(patternNames)
        case decl: Decl.Type           => List(decl.name)
        case _                         => Nil
      }
      Some(names.filter(_.value.nonEmpty)).filter(_.nonEmpty)
    }

    private def extensionNames(defn: Defn.ExtensionGroup): List[Name] =
      extensionMembers(defn).flatMap {
        case member: Defn.Def => List(member.name)
        case member: Decl.Def => List(member.name)
        case _                => Nil
      }

    private def patternNames(pat: Pat): List[Name] =
      pat.collect { case Pat.Var(name) => name }
  }

  private def extensionMembers(defn: Defn.ExtensionGroup): List[Stat] =
    defn.body match {
      case block: Term.Block => block.stats
      case stat              => List(stat)
    }

  private def checkTerm(
      term: Definition,
      users: List[Definition]
  ): Option[Patch] = {
    val below = users.filter(_.index > term.index)
    if (below.isEmpty) None
    else {
      val verb = if (below.size == 1) "uses" else "use"
      val message =
        s"`${term.name}` is above ${names(below)}, which $verb it. $TermRule"
      Some(Patch.lint(Misplaced(term.position, message)))
    }
  }

  private def checkType(
      tpe: Definition,
      firstUser: Definition,
      definitions: List[Definition],
      firstUserOf: Map[Definition, Definition]
  ): Option[Patch] = {
    val between = definitions.filter { definition =>
      definition.index > tpe.index &&
      definition.index < firstUser.index &&
      !(definition.isType && firstUserOf.get(definition).contains(firstUser))
    }
    val relation =
      if (tpe.index > firstUser.index) Some("is below")
      else if (between.nonEmpty) Some(s"is separated by ${names(between)} from")
      else None
    relation.map { relation =>
      val message =
        s"`${tpe.name}` $relation `${firstUser.name}`, the first definition " +
          s"that names it. $TypeRule"
      Patch.lint(Misplaced(tpe.position, message))
    }
  }

  private val TermRule: String =
    "A term goes below every definition in its scope that uses it."

  private val TypeRule: String =
    "A type goes immediately above the first definition that names it."

  private def names(definitions: List[Definition]): String =
    definitions.map(definition => s"`${definition.name}`") match {
      case init :+ last if init.nonEmpty => s"${init.mkString(", ")} and $last"
      case all                           => all.mkString
    }

  // Tarjan's algorithm. Definitions in one component reach each other, so no
  // order puts every user above what it uses, and none is asked for.
  private def stronglyConnected(
      definitions: List[Definition],
      uses: Map[Definition, Set[Definition]]
  ): Map[Definition, Int] = {
    val index = mutable.Map.empty[Definition, Int]
    val low = mutable.Map.empty[Definition, Int]
    val onStack = mutable.Map.empty[Definition, Boolean]
    val component = mutable.Map.empty[Definition, Int]
    var stack = List.empty[Definition]
    def visit(definition: Definition): Unit = {
      index(definition) = index.size
      low(definition) = index(definition)
      stack = definition :: stack
      onStack(definition) = true
      uses(definition).foreach { used =>
        if (!index.contains(used)) {
          visit(used)
          low(definition) = low(definition).min(low(used))
        } else if (onStack.getOrElse(used, false))
          low(definition) = low(definition).min(index(used))
      }
      if (low(definition) == index(definition)) {
        val id = component.size
        var member = stack.head
        while (member != definition) {
          component(member) = id
          onStack(member) = false
          stack = stack.tail
          member = stack.head
        }
        component(definition) = id
        onStack(definition) = false
        stack = stack.tail
      }
    }
    definitions.foreach { definition =>
      if (!index.contains(definition)) visit(definition)
    }
    component.toMap
  }
}

final case class DefinitionOrderConfig(exclude: List[String] = Nil)

object DefinitionOrderConfig {
  val default: DefinitionOrderConfig = DefinitionOrderConfig()
  implicit val surface: Surface[DefinitionOrderConfig] =
    metaconfig.generic.deriveSurface[DefinitionOrderConfig]
  implicit val decoder: ConfDecoder[DefinitionOrderConfig] =
    metaconfig.generic.deriveDecoder(default)
}

final case class Misplaced(position: Position, message: String)
    extends Diagnostic
