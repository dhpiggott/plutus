package smithy4s
package oauth.query

import plutus.monzo.QueryFlattened
import plutus.monzo.QueryName
import smithy4s.*
import smithy4s.oauth.query.FormData
import smithy4s.schema.*

private[oauth] class OAuthSchemaVisitorOAuthQueryCodec(
    val cache: CompilationCache[OAuthQueryCodec]
) extends SchemaVisitor.Cached[OAuthQueryCodec]:
  compile =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): OAuthQueryCodec[P] = new OAuthQueryCodec[P]:
    override def apply(p: P): FormData =
      Primitive.stringWriter(tag, hints) match
        case Some(writer) => FormData.SimpleValue(writer(p)).widen
        case None         => FormData.Empty.widen

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): OAuthQueryCodec[C[A]] =
    val memberWriter = compile(member)
    val maybeKey =
      if hints.has[QueryFlattened] then None
      else Option(getKey(member.hints, "member"))

    new OAuthQueryCodec[C[A]]:
      override def apply(collection: C[A]): FormData =
        val formData = FormData
          .MultipleValues(
            tag
              .iterator(collection)
              .zipWithIndex
              .map { case (member, index) =>
                memberWriter(member).prepend(index + 1)
              }
              .toVector
          )
          .widen
        maybeKey.fold(formData)(key => formData.prepend(key))

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): OAuthQueryCodec[Map[K, V]] =
    type KV = (K, V)
    val kvSchema: Schema[(K, V)] =
      val kField = key.required[KV]("key", _._1)
      val vField = value.required[KV]("value", _._2)
      Schema.struct(kField, vField)((_, _)).addHints(QueryName("entry"))
    val schema = Schema.vector(kvSchema).addHints(hints)
    val codec = compile(schema)

    new OAuthQueryCodec[Map[K, V]]:
      override def apply(m: Map[K, V]): FormData = codec(m.toVector)

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): OAuthQueryCodec[E] =
    if hints.has(IntEnum) then
      new OAuthQueryCodec[E]:
        def apply(value: E): FormData =
          FormData.SimpleValue(total(value).intValue.toString)
    else
      new OAuthQueryCodec[E]:
        def apply(value: E): FormData =
          FormData.SimpleValue(total(value).stringValue)

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, ?]],
      make: IndexedSeq[Any] => S
  ): OAuthQueryCodec[S] =
    def fieldEncoder[A](field: SchemaField[S, A]): OAuthQueryCodec[S] =
      val fieldKey = getKey(field.hints, field.label)

      val encoder = field.foldK(new Field.FolderK[Schema, S, OAuthQueryCodec]:
        override def onRequired[AA](
            label: String,
            instance: Schema[AA],
            get: S => AA
        ): OAuthQueryCodec[AA] =
          val schema = compile(instance)
          new OAuthQueryCodec[AA]:
            def apply(a: AA): FormData = schema(a)

        override def onOptional[AA](
            label: String,
            instance: Schema[AA],
            get: S => Option[AA]
        ): OAuthQueryCodec[Option[AA]] =
          val schema = compile(instance)
          new OAuthQueryCodec[Option[AA]]:
            override def apply(a: Option[AA]): FormData = a match
              case Some(value) => schema(value)
              case None        => FormData.Empty
      )

      new OAuthQueryCodec[S]:
        def apply(s: S): FormData =
          encoder(field.get(s)).prepend(fieldKey)

    val codecs: Vector[OAuthQueryCodec[S]] =
      fields.map(field => fieldEncoder(field))

    new OAuthQueryCodec[S]:
      def apply(s: S): FormData =
        FormData.MultipleValues(codecs.map(codec => codec(s)))

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, ?]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): OAuthQueryCodec[U] =

    def encode[A](u: U, alt: SchemaAlt[U, A]): FormData =
      val key = getKey(alt.hints, alt.label)
      dispatch
        .projector(alt)(u)
        .fold(FormData.Empty.widen)(a => compile(alt.instance)(a))
        .prepend(key)

    new OAuthQueryCodec[U]:
      def apply(u: U): FormData =
        FormData.MultipleValues(alternatives.map(alt => encode(u, alt)))

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): OAuthQueryCodec[B] = new OAuthQueryCodec[B]:
    def apply(b: B): FormData = compile(schema)(bijection.from(b))

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): OAuthQueryCodec[B] = new OAuthQueryCodec[B]:
    def apply(b: B): FormData = compile(schema)(refinement.from(b))

  override def lazily[A](suspend: Lazy[Schema[A]]): OAuthQueryCodec[A] =
    new OAuthQueryCodec[A]:
      lazy val underlying: OAuthQueryCodec[A] =
        suspend.map(schema => compile(schema)).value
      override def apply(a: A): FormData = underlying(a)

  private def getKey(hints: Hints, default: String): String =
    hints
      .get(QueryName)
      .map(_.value)
      .getOrElse(default)
