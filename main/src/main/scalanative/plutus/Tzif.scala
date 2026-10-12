package plutus

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.ZoneOffset
import java.time.zone.ZoneOffsetTransition
import java.time.zone.ZoneOffsetTransitionRule
import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition
import java.time.zone.ZoneRules
import scala.util.Try

// The machine's own zone rules, read from the compiled TZif files zic writes
// (RFC 8536), for java.time.zone.TzdbZoneRulesProvider on Scala Native. Only
// what java.time can represent is read: the UTC offset each transition moves
// to, and the footer's POSIX TZ rule for the years after the last one, which
// is all a slim file has for any zone still observing DST.
object Tzif:

  // On macOS a link to /var/db/timezone/zoneinfo, which the OS updates.
  val zoneinfo: String = "/usr/share/zoneinfo"

  lazy val version: String =
    Try(
      String(
        Files.readAllBytes(Paths.get(zoneinfo, "+VERSION")),
        StandardCharsets.US_ASCII
      ).trim
    ).toOption
      .filter(_.nonEmpty)
      .getOrElse("system")

  // The directory also holds tables (zone.tab, tzdata.zi, …) that aren't zones.
  def isTzif(file: java.io.File): Boolean =
    Try:
      val in = java.io.FileInputStream(file)
      try
        val found = Array.ofDim[Byte](magic.length)
        in.read(found) == magic.length && found.sameElements(magic)
      finally in.close()
    .getOrElse(false)

  def read(regionId: String): ZoneRules =
    parse(Files.readAllBytes(Paths.get(zoneinfo, regionId)))

  // Version 1 data uses 32-bit times, which stop in 2038; version 2 and later
  // repeat everything with 64-bit times after it, and add the footer.
  def parse(bytes: Array[Byte]): ZoneRules =
    val reader = Reader(bytes)
    val v1 = header(reader)
    if v1.version < '2' then rules(block(reader, v1, timeSize = 4), None)
    else
      reader.skip(v1.blockLength(timeSize = 4))
      val v2 = header(reader)
      val data = block(reader, v2, timeSize = 8)
      rules(data, footer(reader))

  private val magic = "TZif".getBytes(StandardCharsets.US_ASCII)

  // Big-endian throughout, as ByteBuffer's absolute reads are by default.
  private class Reader(bytes: Array[Byte]):
    private val buffer = ByteBuffer.wrap(bytes)
    private var at = 0

    def skip(count: Int): Unit = at += count

    def byte(): Int =
      val value = buffer.get(at) & 0xff
      at += 1
      value

    def int(): Int =
      val value = buffer.getInt(at)
      at += 4
      value

    def long(): Long =
      val value = buffer.getLong(at)
      at += 8
      value

    def rest(): Array[Byte] =
      val remaining = bytes.drop(at)
      at = bytes.length
      remaining

  private case class Header(
      version: Char,
      isUtCount: Int,
      isStdCount: Int,
      leapCount: Int,
      timeCount: Int,
      typeCount: Int,
      charCount: Int
  ):
    def blockLength(timeSize: Int): Int =
      timeCount * timeSize + timeCount + typeCount * 6 + charCount +
        leapCount * (timeSize + 4) + isStdCount + isUtCount

  private def header(reader: Reader): Header =
    val found = Array.fill(magic.length)(reader.byte().toByte)
    if !found.sameElements(magic) then
      throw IllegalArgumentException("Not a TZif file")
    val version = reader.byte().toChar
    reader.skip(15)
    Header(
      version = version,
      isUtCount = reader.int(),
      isStdCount = reader.int(),
      leapCount = reader.int(),
      timeCount = reader.int(),
      typeCount = reader.int(),
      charCount = reader.int()
    )

  private case class LocalTimeType(utOffset: Int, isDst: Boolean)

  private case class Block(
      times: Vector[Long],
      typeIndices: Vector[Int],
      types: Vector[LocalTimeType]
  )

  private def block(reader: Reader, header: Header, timeSize: Int): Block =
    val times = Vector.fill(header.timeCount):
      if timeSize == 8 then reader.long() else reader.int().toLong
    val typeIndices = Vector.fill(header.timeCount)(reader.byte())
    val types = Vector.fill(header.typeCount):
      val utOffset = reader.int()
      val isDst = reader.byte() != 0
      // The abbreviation's index, which java.time has no use for.
      reader.skip(1)
      LocalTimeType(utOffset, isDst)
    reader.skip(
      header.charCount + header.leapCount * (timeSize + 4) +
        header.isStdCount + header.isUtCount
    )
    Block(times, typeIndices, types)

  private def footer(reader: Reader): Option[String] =
    val text = String(reader.rest(), StandardCharsets.US_ASCII)
    Option
      .when(text.startsWith("\n")):
        text.drop(1).takeWhile(_ != '\n')
      .filter(_.nonEmpty)

  // java.time keeps two histories where TZif keeps one: the wall offset, and
  // the standard offset beneath it. TZif only marks a type as DST or not, so a
  // DST type leaves the standard offset where the last standard type put it.
  // A transition that changes neither (a new abbreviation, say) isn't one to
  // java.time, and one before LocalDateTime's range — zic writes a "big bang"
  // sentinel at -2^59 — only sets the offsets in force from the start.
  private def rules(block: Block, footer: Option[String]): ZoneRules =
    val first = block.types(0)
    val initial = History(
      wall = first.utOffset,
      standard = block.types.find(!_.isDst).getOrElse(first).utOffset
    )
    val history = block.times
      .zip(block.typeIndices)
      .foldLeft(initial): (history, transition) =>
        val (time, index) = transition
        history.next(time, block.types(index))
    val lastRules = footer.flatMap(posixRules).getOrElse(Nil)
    val bridge = block.times.lastOption
      .filter(_ >= Instant.MIN.getEpochSecond)
      .fold(Nil)(bridgeTo(_, history.wall, lastRules))
    val wallTransitions = history.wallTransitions.reverse ++ bridge
    ZoneRules.of(
      offset(history.baseStandard),
      offset(history.baseWall),
      javaList(history.standardTransitions.reverse),
      javaList(wallTransitions),
      // StandardZoneRules only consults its rules after the last transition,
      // and assumes there is one.
      javaList(if wallTransitions.isEmpty then Nil else lastRules)
    )

  // StandardZoneRules switches to its rules after the last transition that
  // changes the offset, but the footer only takes over after the file's last
  // transition of any kind. A slim file can end on one that changes only the
  // DST flag — Lisbon's 1996 move from CET to WEST, both +01:00, or Ojinaga's
  // 2022 one from MDT to CST, both -06:00 — and the rules would then wrongly
  // apply from the change before it. So the rules' own transitions for the
  // rest of that year and the next are spelled out, which moves the switch
  // past the file's end. One that would leave the offset as it is (CST to
  // CST, in Ojinaga's case) is skipped; one that starts from an offset the
  // file doesn't end on means the two disagree, and nothing is spelled out.
  private def bridgeTo(
      last: Long,
      wall: Int,
      rules: List[ZoneOffsetTransitionRule]
  ): List[ZoneOffsetTransition] =
    val year = LocalDateTime.ofEpochSecond(last, 0, offset(wall)).getYear
    List(year, year + 1)
      .flatMap(year => rules.map(_.createTransition(year)))
      .filter(_.toEpochSecond > last)
      .foldLeft(Option((offset(wall), List.empty[ZoneOffsetTransition]))):
        case (Some((current, bridged)), transition) =>
          if transition.getOffsetAfter == current then Some((current, bridged))
          else
            Option.when(transition.getOffsetBefore == current):
              (transition.getOffsetAfter, transition :: bridged)
        case (None, _) => None
      .fold(Nil)((_, bridged) => bridged.reverse)

  private case class History(
      wall: Int,
      standard: Int,
      wallTransitions: List[ZoneOffsetTransition] = Nil,
      standardTransitions: List[ZoneOffsetTransition] = Nil
  ):
    val baseWall: Int =
      wallTransitions.lastOption.fold(wall)(_.getOffsetBefore.getTotalSeconds)

    val baseStandard: Int =
      standardTransitions.lastOption.fold(standard)(
        _.getOffsetBefore.getTotalSeconds
      )

    def next(time: Long, to: LocalTimeType): History =
      val nextStandard = if to.isDst then standard else to.utOffset
      if time < Instant.MIN.getEpochSecond then
        copy(wall = to.utOffset, standard = nextStandard)
      else
        History(
          wall = to.utOffset,
          standard = nextStandard,
          wallTransitions =
            if to.utOffset == wall then wallTransitions
            else transition(time, wall, to.utOffset) :: wallTransitions,
          standardTransitions =
            if nextStandard == standard then standardTransitions
            else transition(time, standard, nextStandard) :: standardTransitions
        )

  private def javaList[A](list: List[A]): java.util.List[A] =
    val result = java.util.ArrayList[A]()
    list.foreach(result.add)
    result

  private def transition(
      epochSecond: Long,
      before: Int,
      after: Int
  ): ZoneOffsetTransition =
    ZoneOffsetTransition.of(
      LocalDateTime.ofEpochSecond(epochSecond, 0, offset(before)),
      offset(before),
      offset(after)
    )

  private def offset(seconds: Int): ZoneOffset =
    ZoneOffset.ofTotalSeconds(seconds)

  // A POSIX TZ rule, such as GMT0BST,M3.5.0/1,M10.5.0: a standard name and
  // offset, then optionally a DST name, offset and the two dates it starts and
  // ends on. Offsets are hours west of UTC, the opposite sign to java.time's.
  // Some(Nil) is a zone with no DST, and None a rule java.time can't express —
  // a Julian-day date, say — in which case the last explicit transition's
  // offset stands, which for a file zic wrote "fat" is correct until 2037.
  private def posixRules(
      tz: String
  ): Option[List[ZoneOffsetTransitionRule]] =
    val rule = PosixRule(tz)
    for
      _ <- rule.name()
      standardWest <- rule.duration()
      result <-
        if rule.atEnd then Some(Nil)
        else
          for
            _ <- rule.name()
            dstWest <-
              if rule.atEnd || rule.peek == ',' then Some(standardWest - 3600)
              else rule.duration()
            _ <- rule.expect(',')
            start <- rule.date()
            _ <- rule.expect(',')
            end <- rule.date()
            if rule.atEnd
            standard = offset(-standardWest)
            dst = offset(-dstWest)
            starts <- start.rule(standard, before = standard, after = dst)
            ends <- end.rule(standard, before = dst, after = standard)
          // StandardZoneRules checks a year's transitions in list order.
          yield List(starts, ends).sortBy: rule =>
            val day = rule.getDayOfMonthIndicator
            (rule.getMonth.getValue, if day < 0 then 32 else day)
    yield result

  private case class PosixDate(month: Int, week: Int, day: Int, time: Int):
    // RFC 8536 lets the time run from -167h to +167h, so 26:00 on the 4th
    // Thursday (Jerusalem) is 02:00 on the Friday after, and -1:00 on the last
    // Sunday (Nuuk) 23:00 on the Saturday before. java.time spells the first
    // as Friday on or after the 23rd and the second as Saturday on or before
    // the month's second-to-last day; a shift that would leave the month has
    // no spelling and gives no rule.
    def rule(
        standard: ZoneOffset,
        before: ZoneOffset,
        after: ZoneOffset
    ): Option[ZoneOffsetTransitionRule] =
      val endOfDay = time == 86400
      val days = if endOfDay then 0 else Math.floorDiv(time, 86400)
      val dayOfMonthIndicator =
        // Week 5 is the last such day of the month, which java.time spells as
        // the weekday on or before the month's last day.
        if week == 5 then -1 + days else 1 + (week - 1) * 7 + days
      Option.when(
        if week == 5 then dayOfMonthIndicator >= -28 && dayOfMonthIndicator < 0
        else dayOfMonthIndicator >= 1 && dayOfMonthIndicator <= 28
      ):
        ZoneOffsetTransitionRule.of(
          Month.of(month),
          dayOfMonthIndicator,
          // POSIX numbers Sunday 0, java.time 7.
          DayOfWeek.of(Math.floorMod(day + days - 1, 7) + 1),
          if endOfDay then LocalTime.MIDNIGHT
          else LocalTime.ofSecondOfDay(Math.floorMod(time, 86400).toLong),
          endOfDay,
          TimeDefinition.WALL,
          standard,
          before,
          after
        )

  private class PosixRule(text: String):
    private var at = 0

    def atEnd: Boolean = at >= text.length

    def peek: Char = text(at)

    def expect(c: Char): Option[Unit] =
      Option.when(!atEnd && peek == c)(at += 1)

    def name(): Option[String] =
      if !atEnd && peek == '<' then
        val close = text.indexOf('>', at)
        Option.when(close > at):
          val quoted = text.substring(at + 1, close)
          at = close + 1
          quoted
      else
        val start = at
        while !atEnd && peek.isLetter do at += 1
        Option.when(at - start >= 3)(text.substring(start, at))

    // [+-]h[h][:mm[:ss]], in seconds.
    def duration(): Option[Int] =
      val sign = if expect('-').isDefined then -1 else 1
      if sign == 1 && !atEnd && peek == '+' then at += 1
      for
        hours <- number()
        minutes <- if expect(':').isDefined then number() else Some(0)
        seconds <- if expect(':').isDefined then number() else Some(0)
      yield sign * (hours * 3600 + minutes * 60 + seconds)

    // Mm.w.d[/time]; the Jn and n forms aren't representable as java.time
    // rules, so they end the parse.
    def date(): Option[PosixDate] =
      for
        _ <- expect('M')
        month <- number()
        _ <- expect('.')
        week <- number()
        _ <- expect('.')
        day <- number()
        time <-
          if expect('/').isDefined then duration() else Some(2 * 3600)
        if month >= 1 && month <= 12 && week >= 1 && week <= 5 &&
          day >= 0 && day <= 6
      yield PosixDate(month, week, day, time)

    private def number(): Option[Int] =
      val start = at
      while !atEnd && peek.isDigit do at += 1
      Option.when(at > start)(text.substring(start, at).toInt)
