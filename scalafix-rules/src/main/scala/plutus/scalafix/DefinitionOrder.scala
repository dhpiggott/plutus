package plutus.scalafix

import scala.annotation.tailrec
import scala.meta._

import metaconfig.ConfDecoder
import metaconfig.Configured
import metaconfig.generic.Surface
import scalafix.lint.Diagnostic
import scalafix.v1._

// Puts the definitions of each scope in one canonical order, worked out from
// the code alone, so the order they start in never matters. A scope is a
// file's top level, or the body of one object, class, trait, enum or extension
// block. CLAUDE.md describes the order; this is the one implementation of it.
//
// A class and its companion are one item, class first. Within a scope:
//
//   - A term goes below everything that uses it, and is placed as soon as
//     everything that uses it has been: depth first from each root, visiting
//     what an item uses in the order its body first mentions it.
//   - A type, and a strict val another strict val's initialiser reads, goes
//     immediately above the first item that needs it.
//   - Roots, which nothing in the scope uses, come public before private and
//     then by name; so does anything a cycle leaves undecided.
//   - A class, trait or enum body puts its fields (strict vals and vars)
//     first, then everything else.
//
// The rule rewrites a scope that is out of order, so `scalafixAll` fixes it
// and `scalafixAll --check` fails on it. A comment directly above a definition
// moves with it; a scope it can't rewrite safely, such as one with a comment
// standing apart from any definition, is reported instead.
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
    else new DefinitionOrder.FileOrder().patch
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

  private final case class Scope(stats: List[Stat], fieldsFirst: Boolean)

  private final case class Member(stat: Stat, names: List[Name])

  private final case class Edit(start: Int, end: Int, text: String)

  private final case class Item(
      stats: List[Stat],
      key: (Boolean, String, String),
      isType: Boolean,
      isStrict: Boolean,
      symbols: Set[Symbol]
  )

  private final class FileOrder(implicit doc: SemanticDocument) {

    private val text: String = doc.input.text

    private var problems = List.empty[Patch]

    def patch: Patch = {
      val edits = scopes(doc.tree).flatMap(editsFor)
      (edits.map(toPatch) ++ problems).asPatch
    }

    private def scopes(tree: Tree): List[Scope] = tree match {
      case source: Source => scopesIn(source.stats)
      case pkg: Pkg       => scopesIn(pkg.body.stats)
      case _              => Nil
    }

    private def scopesIn(stats: List[Stat]): List[Scope] = {
      val (packages, members) = stats.partition(_.isInstanceOf[Pkg])
      Scope(members, fieldsFirst = false) :: packages.flatMap(scopes)
    }

    private def editsFor(scope: Scope): List[Edit] = {
      val members = scope.stats.flatMap { stat =>
        definedNames(stat).map(Member(stat, _))
      }
      lazy val nestedEdits = members.flatMap(member => editsWithin(member.stat))
      val order = canonicalOrder(scope, members)
      if (order.map(_.pos.start) == members.map(_.stat.pos.start)) nestedEdits
      else reordered(members, order).fold(nestedEdits)(List(_))
    }

    private def editsWithin(stat: Stat): List[Edit] =
      nested(stat).toList.flatMap(editsFor)

    private def nested(stat: Stat): Option[Scope] = {
      val scope = stat match {
        case defn: Defn.Object         => Scope(defn.templ.body.stats, false)
        case defn: Defn.Class          => Scope(defn.templ.body.stats, true)
        case defn: Defn.Trait          => Scope(defn.templ.body.stats, true)
        case defn: Defn.Enum           => Scope(defn.templ.body.stats, true)
        case defn: Defn.ExtensionGroup => Scope(extensionMembers(defn), false)
        case _                         => Scope(Nil, false)
      }
      Some(scope).filter(_.stats.nonEmpty)
    }

    // The whole run of definitions, rewritten in the canonical order with one
    // blank line between them. Only whitespace may separate them: anything
    // else between two definitions has nowhere to go once they move.
    private def reordered(
        members: List[Member],
        order: List[Stat]
    ): Option[Edit] = {
      val blocks = members.map(member => blockOf(member.stat))
      val gaps = blocks.zip(blocks.drop(1)).map { case ((_, end), (start, _)) =>
        (end, start)
      }
      val (start, end) = (blocks.head._1, blocks.last._2)
      val indent = text.substring(lineStart(start), start)
      val blocked = gaps.find { case (from, to) =>
        from > to || text.substring(from, to).trim.nonEmpty
      }
      blocked match {
        case Some((from, to)) =>
          report(
            Position.Range(doc.input, from, to.max(from)),
            "These definitions are out of order, but something other than " +
              "whitespace separates two of them, so they can't be moved " +
              "automatically. A comment moves with the definition directly " +
              "below it; one separated from it by a blank line doesn't."
          )
          None
        case None if indent.trim.nonEmpty =>
          report(
            Position.Range(doc.input, start, end),
            "These definitions are out of order, but they don't start on a " +
              "line of their own, so they can't be moved automatically."
          )
          None
        case None =>
          val rendered = order.map(render).mkString("\n\n" + indent)
          Some(Edit(start, end, rendered))
      }
    }

    private def report(position: Position, message: String): Unit =
      problems = Patch.lint(Misplaced(position, message)) :: problems

    private def render(stat: Stat): String = {
      val (start, end) = blockOf(stat)
      editsWithin(stat)
        .sortBy(-_.start)
        .foldLeft(text.substring(start, end)) { (rendered, edit) =>
          rendered.patch(edit.start - start, edit.text, edit.end - edit.start)
        }
    }

    // A definition together with the comments directly above it, and one
    // trailing it on its last line.
    private def blockOf(stat: Stat): (Int, Int) = {
      val lineEnd = text.indexOf('\n', stat.pos.end) match {
        case -1  => text.length
        case end => end
      }
      val trailing = text.substring(stat.pos.end, lineEnd).trim
      val end = if (trailing.startsWith("//")) lineEnd else stat.pos.end
      (leadingStart(stat.pos.start), end)
    }

    @tailrec private def leadingStart(start: Int): Int = {
      val currentLine = lineStart(start)
      if (currentLine == 0 || text.substring(currentLine, start).trim.nonEmpty)
        start
      else {
        val previousEnd = currentLine - 1
        val previousStart = lineStart(previousEnd)
        val previous = text.substring(previousStart, previousEnd).trim
        val open = text.lastIndexOf("/*", previousEnd)
        val opensLine =
          open >= 0 && text.substring(lineStart(open), open).trim.isEmpty
        if (previous.startsWith("//"))
          leadingStart(text.indexOf("//", previousStart))
        else if (previous.endsWith("*/") && opensLine) leadingStart(open)
        else start
      }
    }

    private def lineStart(offset: Int): Int =
      text.lastIndexOf('\n', offset - 1) + 1

    private def toPatch(edit: Edit): Patch = {
      val tokens = doc.tokens.filter { token =>
        token.start >= edit.start && token.end <= edit.end
      }
      tokens.find(_.start == edit.start) match {
        case Some(first) =>
          Patch.removeTokens(tokens.filter(_ != first)) +
            Patch.replaceToken(first, edit.text)
        case None => Patch.empty
      }
    }

    private def canonicalOrder(
        scope: Scope,
        members: List[Member]
    ): List[Stat] = {
      val items = itemsOf(members)
      val owners = items.indices
        .flatMap(id => items(id).symbols.map(_ -> id))
        .toMap
      val mentions = items.indices.toVector.map(mentionsBy(_, items, owners))
      val isField = items.map(item => scope.fieldsFirst && item.isStrict)
      val uses = items.indices.toVector.map { user =>
        mentions(user).filterNot(used => isField(used) && !isField(user))
      }
      val pulls = items.indices.toVector.map { user =>
        val reads =
          if (items(user).isStrict) strictReads(user, items, mentions) else Nil
        val candidates = (uses(user) ++ reads).distinct
        candidates.filter(used => items(used).isType || reads.contains(used))
      }
      val children = items.indices.toVector.map { user =>
        uses(user).filterNot(pulls(user).contains)
      }
      val waits = items.indices.toVector.map { used =>
        items.indices.filter(children(_).contains(used)).toSet
      }
      val component = stronglyConnected(items.size, pulls, children)
      val walk = new Walk(items, pulls, children, waits, component)
      val phases =
        if (!scope.fieldsFirst) List(items.indices)
        else {
          val (fields, others) = items.indices.partition(isField)
          List(fields, others)
        }
      phases.foreach { phase =>
        val ids = phase.toSet
        phase
          .filter(id => walk.isReady(id) && !walk.isPulled(id))
          .sortBy(items(_).key)
          .foreach(walk.visit(_, ids))
        walk.finish(ids)
      }
      walk.order.flatMap(items(_).stats)
    }

    // The strict vals a strict val's initialiser reads, directly or through
    // the defs it calls, all of which have to be initialised before it.
    private def strictReads(
        start: Int,
        items: Vector[Item],
        mentions: Vector[List[Int]]
    ): List[Int] = {
      @tailrec def loop(
          todo: List[Int],
          seen: Set[Int],
          found: List[Int]
      ): List[Int] = todo match {
        case Nil        => found.reverse
        case id :: rest =>
          if (seen(id) || id == start) loop(rest, seen, found)
          else if (items(id).isStrict) loop(rest, seen + id, id :: found)
          else if (items(id).isType) loop(rest, seen + id, found)
          else loop(mentions(id) ++ rest, seen + id, found)
      }
      loop(mentions(start), Set.empty, Nil)
    }

    // A class and its companion are one item, class first.
    private def itemsOf(members: List[Member]): Vector[Item] =
      members.zipWithIndex
        .groupBy { case (member, index) => companionKey(member, index) }
        .values
        .toVector
        .sortBy(_.map(_._2).min)
        .map { group =>
          val stats = group
            .sortBy(_._2)
            .map { case (member, _) => member.stat }
            .sortBy(stat => if (stat.isInstanceOf[Defn.Object]) 1 else 0)
          val names = group.flatMap { case (member, _) => member.names }
          val lead = stats.head
          Item(
            stats = stats,
            key = (
              isPrivate(lead),
              names.head.value,
              text.substring(lead.pos.start, lead.pos.end)
            ),
            isType = stats.exists(isType),
            isStrict = stats.exists(isStrict),
            symbols = names.flatMap(name => withCompanion(name.symbol)).toSet
          )
        }

    // The other items an item uses, in the order its body first mentions them.
    private def mentionsBy(
        id: Int,
        items: Vector[Item],
        owners: Map[Symbol, Int]
    ): List[Int] =
      items(id).stats
        .flatMap(_.collect { case name: Name => name })
        .sortBy(_.pos.start)
        .flatMap(name => ownerIn(owners, name.symbol))
        .filter(_ != id)
        .distinct

    private def ownerIn(owners: Map[Symbol, Int], symbol: Symbol): Option[Int] =
      Iterator
        .iterate(symbol)(_.owner)
        .takeWhile(s => !s.isNone && !s.isRootPackage && !s.isEmptyPackage)
        .flatMap(owners.get)
        .nextOption()

    // A class, trait or enum with no companion written out still has one in
    // SemanticDB, and it is what `Type(...)` and `Type.Case` resolve to.
    private def withCompanion(symbol: Symbol): List[Symbol] =
      if (!symbol.value.endsWith("#")) List(symbol)
      else List(symbol, Symbol(symbol.value.stripSuffix("#") + "."))

    private def companionKey(member: Member, index: Int): Either[String, Int] =
      member.stat match {
        case _: Defn.Object | _: Defn.Class | _: Defn.Trait | _: Defn.Enum =>
          Left(member.names.head.value)
        case _ => Right(index)
      }

    private def isType(stat: Stat): Boolean = stat match {
      case _: Defn.Class | _: Defn.Trait | _: Defn.Enum => true
      case _: Defn.Type | _: Decl.Type                  => true
      case _                                            => false
    }

    private def isStrict(stat: Stat): Boolean = stat match {
      case defn: Defn.Val => !defn.mods.exists(_.isInstanceOf[Mod.Lazy])
      case _: Defn.Var    => true
      case _              => false
    }

    private def isPrivate(stat: Stat): Boolean =
      modsOf(stat).exists {
        case _: Mod.Private | _: Mod.Protected => true
        case _                                 => false
      }

    private def modsOf(stat: Stat): List[Mod] = stat match {
      case defn: Defn.Def        => defn.mods
      case defn: Defn.Val        => defn.mods
      case defn: Defn.Var        => defn.mods
      case defn: Defn.Type       => defn.mods
      case defn: Defn.Class      => defn.mods
      case defn: Defn.Trait      => defn.mods
      case defn: Defn.Object     => defn.mods
      case defn: Defn.Enum       => defn.mods
      case defn: Defn.Given      => defn.mods
      case defn: Defn.GivenAlias => defn.mods
      case decl: Decl.Def        => decl.mods
      case decl: Decl.Val        => decl.mods
      case decl: Decl.Type       => decl.mods
      case _                     => Nil
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

  // The depth-first walk. `pulls` are what an item needs directly above it
  // (types it names, strict vals its initialiser reads), placed just before
  // it; `children` are what it uses, each placed once everything that uses it
  // outside its own cycle has been.
  private final class Walk(
      items: Vector[Item],
      pulls: Vector[List[Int]],
      children: Vector[List[Int]],
      waits: Vector[Set[Int]],
      component: Vector[Int]
  ) {

    private val placed = Array.fill(items.size)(false)

    private val pulled = pulls.flatten.toSet

    private var reversed = List.empty[Int]

    def order: List[Int] = reversed.reverse

    def isPulled(id: Int): Boolean = pulled(id)

    def isReady(id: Int): Boolean =
      waits(id).forall(user => placed(user) || component(user) == component(id))

    def visit(id: Int, phase: Set[Int]): Unit =
      if (!placed(id)) {
        val newlyPlaced = place(id)
        val next = (id :: newlyPlaced.filter(_ != id)).flatMap(children(_))
        next.distinct.foreach { child =>
          if (phase(child) && isReady(child)) visit(child, phase)
        }
      }

    // Anything the walk didn't reach from a root, such as the members of a
    // cycle no root leads into: ready ones first, then in key order, leaving
    // what something pulls for that to place.
    @tailrec def finish(phase: Set[Int]): Unit = {
      val remaining = phase.filterNot(placed(_)).toList.sortBy(items(_).key)
      val candidates = remaining.filterNot(pulled)
      val next = candidates
        .find(isReady)
        .orElse(candidates.headOption)
        .orElse(remaining.headOption)
      next match {
        case Some(id) =>
          visit(id, phase)
          finish(phase)
        case None => ()
      }
    }

    private def place(id: Int): List[Int] =
      if (placed(id)) Nil
      else {
        placed(id) = true
        val before = pulls(id).flatMap(place)
        reversed = id :: reversed
        before :+ id
      }
  }

  // Tarjan's algorithm over both kinds of edge, each pointing from the item
  // that goes above to the one that goes below.
  private def stronglyConnected(
      size: Int,
      pulls: Vector[List[Int]],
      children: Vector[List[Int]]
  ): Vector[Int] = {
    val below = Vector.tabulate(size) { id =>
      children(id) ++ (0 until size).filter(pulls(_).contains(id))
    }
    val index = Array.fill(size)(-1)
    val low = Array.fill(size)(0)
    val onStack = Array.fill(size)(false)
    val component = Array.fill(size)(0)
    var stack = List.empty[Int]
    var visited = 0
    var components = 0
    def visit(id: Int): Unit = {
      index(id) = visited
      low(id) = visited
      visited += 1
      stack = id :: stack
      onStack(id) = true
      below(id).foreach { next =>
        if (index(next) < 0) {
          visit(next)
          low(id) = low(id).min(low(next))
        } else if (onStack(next)) low(id) = low(id).min(index(next))
      }
      if (low(id) == index(id)) {
        var member = -1
        while (member != id) {
          member = stack.head
          stack = stack.tail
          onStack(member) = false
          component(member) = components
        }
        components += 1
      }
    }
    (0 until size).foreach(id => if (index(id) < 0) visit(id))
    component.toVector
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
