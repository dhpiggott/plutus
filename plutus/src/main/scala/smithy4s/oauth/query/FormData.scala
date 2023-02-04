package smithy4s
package oauth.query

import smithy4s.PayloadPath
import smithy4s.PayloadPath.Segment
import smithy4s.http.internals.URIEncoderDecoder

import scala.collection.mutable

sealed trait FormData extends Product with Serializable:
  def render: String
  def prepend(key: String): FormData
  def prepend(index: Int): FormData
  def widen: FormData = this

object FormData:
  case object Empty extends FormData:
    override def render: String = ""

    override def prepend(key: String): FormData = this

    override def prepend(index: Int): FormData = this

  final case class SimpleValue(str: String) extends FormData:
    override def render: String = URIEncoderDecoder.encode(str)

    override def prepend(key: String): FormData =
      PathedValue(PayloadPath(key), str)

    override def prepend(index: Int): FormData =
      PathedValue(PayloadPath(index), str)
  final case class PathedValue(path: PayloadPath, value: String)
      extends FormData:

    // TODO: Understand the root reason and have a better solution for a
    // workaround removing the '.' prefix.
    override def render: String =
      val lastIndex = path.segments.size - 1
      val key = path.segments.zipWithIndex
        .foldLeft(new mutable.StringBuilder) {

          case (builder, (Segment.Label(label), i)) if i < lastIndex =>
            builder.append(URIEncoderDecoder.encode(label))
            builder.append('.')

          case (builder, (Segment.Index(index), i)) if i < lastIndex =>
            builder.append(index)
            builder.append('.')

          case (builder, (Segment.Label(label), _)) =>
            builder.append(URIEncoderDecoder.encode(label))

          case (builder, (Segment.Index(index), _)) =>
            builder.append(index)
        }
        .toString()

      key + "=" + URIEncoderDecoder.encode(value)

    override def prepend(key: String): FormData =
      copy(
        path.copy(segments = Segment(key) :: path.segments),
        value
      )

    override def prepend(index: Int): FormData =
      copy(
        path.copy(segments = Segment(index) :: path.segments),
        value
      )

  final case class MultipleValues(values: Vector[FormData]) extends FormData:
    override def render: String =
      values.map(_.render).filter(str => str.nonEmpty).mkString("&")

    override def prepend(key: String): FormData =
      copy(values.map(_.prepend(key)))

    override def prepend(index: Int): FormData =
      copy(values.map(_.prepend(index)))
