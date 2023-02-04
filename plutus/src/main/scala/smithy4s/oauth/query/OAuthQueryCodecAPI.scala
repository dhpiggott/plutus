package smithy4s
package oauth.query

import com.github.plokhotnyuk.jsoniter_scala
import smithy4s.http.BodyPartial
import smithy4s.http.CodecAPI
import smithy4s.http.HttpMediaType
import smithy4s.http.PayloadError
import smithy4s.http.json.JCodec
import smithy4s.internals.InputOutput
import smithy4s.schema.CompilationCache

import java.nio.ByteBuffer

object OAuthQueryCodecAPI extends CodecAPI:

  val jsonCodecAPI = smithy4s.http.json.codecs(
    alloy.SimpleRestJson.protocol.hintMask ++ HintMask(
      InputOutput,
      IntEnum
    )
  )

  override type Codec[A] = Either[OAuthQueryCodec[A], JCodec[A]]
  override type Cache = CompilationCache[OAuthQueryCodec]

  override def createCache(): Cache = CompilationCache.make[OAuthQueryCodec]

  override def compileCodec[A](
      schema: Schema[A],
      cache: Cache
  ): Codec[A] =
    schema.hints.get(InputOutput) match
      case Some(InputOutput.Input) =>
        val visitor = new OAuthSchemaVisitorOAuthQueryCodec(cache)
        val awsQueryEncoder = schema.compile(visitor)
        Left(awsQueryEncoder)
      case Some(InputOutput.Output) | None =>
        val jsonDecoder = jsonCodecAPI.compileCodec(schema)
        Right(jsonDecoder)

  // TODO
  override def mediaType[A](codec: Codec[A]): HttpMediaType =
    HttpMediaType("application/x-www-form-urlencoded")

  override def decodeFromByteArrayPartial[A](
      codec: Codec[A],
      bytes: Array[Byte]
  ): Either[PayloadError, BodyPartial[A]] = codec match
    case Left(_) =>
      Left(
        PayloadError(
          PayloadPath.root,
          "",
          "Invalid codec: got OAuth Query encoder, expected JSON decoder"
        )
      )
    case Right(jsonCodec) =>
      try
        Right {
          BodyPartial(
            jsoniter_scala.core.readFromArray(bytes)(jsonCodec.messageCodec)
          )
        }
      catch case e: PayloadError => Left(e)

  override def decodeFromByteBufferPartial[A](
      codec: Codec[A],
      bytes: ByteBuffer
  ): Either[PayloadError, BodyPartial[A]] =
    throw new IllegalStateException("Must have not been called")

  override def writeToArray[A](codec: Codec[A], value: A): Array[Byte] =
    codec match
      case Left(encoder) =>
        encoder(value).render
          .getBytes("UTF-8")

      case Right(_) =>
        throw new IllegalStateException(
          "Invalid codec: got JSON decoder, must be OAuth query encoder"
        )
