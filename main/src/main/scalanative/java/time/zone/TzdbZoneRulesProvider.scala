package java.time.zone

import org.portablescala.reflect.annotation.EnableReflectiveInstantiation
import plutus.Tzif

// scala-java-time's zone rules on Scala Native come from whichever class it
// finds by this exact name, java.time.zone.TzdbZoneRulesProvider, looked up
// reflectively when java.time first needs a zone; failing that it knows only
// UTC and GMT. scala-java-time-tzdb is the usual answer, but it added 4.4 MB to
// the release binary and its data is frozen at the release's snapshot (2019c
// as of 2.6.0). This answers from the machine's own zoneinfo files instead,
// which macOS keeps up to date and which TZ and /etc/localtime name zones by
// in the first place. See Tzif for the format, and Host.zone for the default.
@EnableReflectiveInstantiation
final class TzdbZoneRulesProvider extends ZoneRulesProvider:

  // Every ID has to be named up front, because registration is what makes
  // ZoneId.of accept one. posix/ and right/ hold copies of the same zones under
  // a prefix; right/'s also count leap seconds, which java.time doesn't.
  override protected def provideZoneIds: java.util.Set[String] =
    val ids = java.util.HashSet[String]()
    zoneIds(java.io.File(Tzif.zoneinfo), "").foreach(ids.add)
    ids

  private def zoneIds(dir: java.io.File, prefix: String): List[String] =
    Option(dir.listFiles).toList.flatten.flatMap: file =>
      val id = prefix + file.getName
      if file.isDirectory then
        if id == "posix" || id == "right" then Nil
        else zoneIds(file, id + "/")
      else if Tzif.isTzif(file) then List(id)
      else Nil

  override protected def provideRules(
      regionId: String,
      forCaching: Boolean
  ): ZoneRules =
    try Tzif.read(regionId)
    catch
      case e: Exception =>
        throw ZoneRulesException(s"Can't read the rules for $regionId: $e")

  override protected def provideVersions(
      zoneId: String
  ): java.util.NavigableMap[String, ZoneRules] =
    val versions = java.util.TreeMap[String, ZoneRules]()
    versions.put(Tzif.version, provideRules(zoneId, forCaching = false))
    versions
