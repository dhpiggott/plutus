package plutus

import cats.effect.*
import porcupine.*
import porcupine.Codec.*

object Slot:

  // KvpValueImpl::Type in GnuCash's libgnucash/engine/kvp-value.hpp. Plutus
  // only writes STRING slots — the shape GnuCash uses for the hidden/placeholder
  // booleans (xaccAccountSetHidden -> set_kvp_boolean_path ->
  // set_kvp_string_path). The other type tags (INT64=1, DOUBLE=2, NUMERIC=3,
  // GUID=5, TIME64=6, GLIST=8, FRAME=9, GDATE=10) are unused.
  val SlotTypeString: Long = 4

  // GnuCash encodes a true boolean flag as a STRING slot whose value is "true";
  // false is the slot's absence (set_kvp_boolean_path(false) removes it). So a
  // false flag has no Slot to insert at all.
  def stringSlot(objGuid: String, name: String, value: String): Slot =
    Slot(
      objGuid,
      name,
      SlotTypeString,
      stringVal = Some(value),
      int64Val = None
    )

  def deleteAll(objGuid: String)(using db: Database[IO]): IO[Unit] =
    db.execute(
      query = sql"""
        delete from slots
        where obj_guid = $text
      """.command,
      args = objGuid
    )

  // True if a transaction carrying this Monzo id as an online_id slot already
  // exists — the idempotency check the importer runs before writing a Posting.
  // Mirrors GnuCash's own OFX-import dedup, which recognises the same slot.
  def hasOnlineId(
      transactionId: monzo.TransactionId
  )(using db: Database[IO]): IO[Boolean] =
    db.option(
      query = sql"""
        select 1 from slots where name = 'online_id' and string_val = $text
      """.query(integer),
      args = transactionId.value
    ).map(_.isDefined)

/** sqlite> .schema slots CREATE TABLE slots( id integer PRIMARY KEY
  * AUTOINCREMENT NOT NULL, obj_guid text(32) NOT NULL, name text(4096) NOT
  * NULL, slot_type integer NOT NULL, int64_val integer, string_val text(4096),
  * double_val real, timespec_val text(14), guid_val text(32), numeric_val_num
  * integer, numeric_val_denom integer, gdate_val text(8) );
  */
// We model only string_val and int64_val of the table's nine value columns:
// those are the two GnuCash's boolean-slot read path consults, and STRING is
// the only type Plutus writes. The other seven value columns are written NULL
// and ignored on read. See Account.scala for the read derivation.
final case class Slot(
    objGuid: String,
    name: String,
    slotType: Long,
    stringVal: Option[String],
    int64Val: Option[Long]
):

  def insert(using db: Database[IO]): IO[Unit] =
    db.execute(
      query = sql"""
        insert into slots (
          obj_guid,
          name,
          slot_type,
          string_val,
          int64_val,
          double_val,
          timespec_val,
          guid_val,
          numeric_val_num,
          numeric_val_denom,
          gdate_val
        )
        values (
          $text,
          $text,
          $integer,
          ${text.opt},
          ${integer.opt},
          null,
          null,
          null,
          null,
          null,
          null
        )
      """.command,
      args = (objGuid, name, slotType, stringVal, int64Val)
    )
