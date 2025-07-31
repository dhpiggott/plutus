package plutus

import cats.effect.Concurrent
import cats.syntax.all.*
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import smithy4s.*
import smithy4s.capability.MonadThrowLike
import smithy4s.client.UnaryClientCodecs
import smithy4s.codecs.*
import smithy4s.http.*
import smithy4s.http4s.SimpleProtocolCodecs
import smithy4s.http4s.kernel.*
import smithy4s.interopcats.*
import smithy4s.json.Json
import smithy4s.json.JsoniterCodecCompiler
import smithy4s.kinds.PolyFunction
import smithy4s.server.UnaryServerCodecs

object TokenExchangeCodecs extends SimpleProtocolCodecs:

  private val jsonCodecs = Json.payloadCodecs.withJsoniterCodecCompiler:
    Json.jsoniter.withHintMask:
      alloy.SimpleRestJson.protocol.hintMask ++ JsoniterCodecCompiler.defaultHintMask

  private val jsonPayloadEncoders = jsonCodecs.encoders

  private val jsonPayloadDecoders = jsonCodecs.decoders

  private val urlFormEncoders = UrlForm
    .Encoder(capitalizeStructAndUnionMemberNames = false)
    .mapK:
      smithy4s.codecs.Encoder.andThenK: (form: UrlForm) =>
        Blob:
          form.render

  private val urlFormDecoders = UrlForm
    .Decoder(
      ignoreUrlFormFlattened = false,
      capitalizeStructAndUnionMemberNames = false
    )
    .mapK:
      new PolyFunction[
        Decoder[Either[UrlFormDecodeError, *], UrlForm, *],
        Decoder[Either[PayloadError, *], Blob, *]
      ]:
        def apply[A](
            fa: Decoder[Either[UrlFormDecodeError, *], UrlForm, A]
        ): Decoder[Either[PayloadError, *], Blob, A] =
          (blob: Blob) =>
            (for
              urlForm <- UrlForm.parse:
                blob.toUTF8String
              a <- fa.decode:
                urlForm
            yield a).left.map: urlFormDecodeError =>
              PayloadError(
                urlFormDecodeError.path,
                "",
                urlFormDecodeError.message
              )

  override def makeServerCodecs[F[_]: Concurrent] =
    TokenExchangeServerCodecs[F, Request[F], Response[F]](
      urlFormDecoders,
      jsonPayloadEncoders,
      toSmithy4sHttpRequest[F],
      fromSmithy4sHttpResponse[F](_).pure[F]
    )

  override def makeClientCodecs[F[_]: Concurrent](baseUri: Uri) =
    TokenExchangeClientCodecs(
      jsonPayloadDecoders,
      urlFormEncoders,
      fromSmithy4sHttpRequest[F](_, encodePathSegments = false).pure[F],
      toSmithy4sHttpResponse[F](_),
      toSmithy4sHttpUri(baseUri, None)
    )

  object TokenExchangeServerCodecs:
    private val errorHeaders = List(
      smithy4s.http.errorTypeHeader,
      // Adding X-Amzn-Errortype as well to facilitate interop with
      // Amazon-issued code-generators.
      smithy4s.http.amazonErrorTypeHeader
    )
    private val baseResponse = HttpResponse(200, Map.empty, Blob.empty)
    def apply[F[_], Req, Resp](
        decoders: BlobDecoder.Compiler,
        encoders: BlobEncoder.Compiler,
        reqTransformation: Req => F[HttpRequest[Blob]],
        respTransformation: HttpResponse[Blob] => F[Resp]
    )(implicit F: MonadThrowLike[F]): UnaryServerCodecs.Make[F, Req, Resp] =
      HttpUnaryServerCodecs.builder
        .withBodyDecoders:
          decoders
        .withSuccessBodyEncoders:
          encoders
        .withErrorBodyEncoders:
          encoders
        .withErrorTypeHeaders(errorHeaders*)
        .withMetadataDecoders:
          Metadata.Decoder
        .withMetadataEncoders:
          Metadata.Encoder
        .withBaseResponse: _ =>
          F.pure:
            baseResponse
        .withResponseMediaType:
          "application/json"
        .withWriteEmptyStructs:
          !_.isUnit
        .withRequestTransformation[Req]:
          reqTransformation
        .withResponseTransformation[Resp]:
          respTransformation
        .build()

  object TokenExchangeClientCodecs:
    private val errorHeaders = List(
      smithy4s.http.errorTypeHeader,
      // Adding X-Amzn-Errortype as well to facilitate interop with
      // Amazon-issued code-generators.
      smithy4s.http.amazonErrorTypeHeader
    )
    def apply[F[_], Req, Resp](
        decoders: BlobDecoder.Compiler,
        encoders: BlobEncoder.Compiler,
        reqTransformation: HttpRequest[Blob] => F[Req],
        respTransformation: Resp => F[HttpResponse[Blob]],
        baseUri: HttpUri
    )(implicit F: MonadThrowLike[F]): UnaryClientCodecs.Make[F, Req, Resp] =
      val baseRequest = HttpRequest(
        HttpMethod.POST,
        baseUri,
        Map.empty,
        Blob.empty
      )
      HttpUnaryClientCodecs.builder
        .withBodyEncoders:
          encoders
        .withSuccessBodyDecoders:
          decoders
        .withErrorBodyDecoders:
          decoders
        .withErrorDiscriminator: x =>
          F.pure:
            HttpDiscriminator.fromResponse(errorHeaders, x)
        .withMetadataDecoders:
          Metadata.Decoder
        .withMetadataEncoders:
          Metadata.Encoder
        .withBaseRequest: _ =>
          F.pure:
            baseRequest
        .withRequestMediaType:
          "application/x-www-form-urlencoded"
        .withRequestTransformation:
          reqTransformation
        .withResponseTransformation:
          respTransformation
        .build()
