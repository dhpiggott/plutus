package plutus

import smithy4s.http4s.SimpleProtocolBuilder

object TokenExchangeBuilder extends TokenExchangeBuilder(1024, false)
class TokenExchangeBuilder(tokenExchangeCodecs: TokenExchangeCodecs)
    extends SimpleProtocolBuilder[monzo.TokenExchange](
      tokenExchangeCodecs
    ):

  def this(maxArity: Int, explicitDefaultsEncoding: Boolean) =
    this(new TokenExchangeCodecs(maxArity, explicitDefaultsEncoding))

  def withMaxArity(maxArity: Int): TokenExchangeBuilder =
    new TokenExchangeBuilder(
      maxArity,
      tokenExchangeCodecs.explicitDefaultsEncoding
    )

  def withExplicitDefaultsEncoding(
      explicitDefaultsEncoding: Boolean
  ): TokenExchangeBuilder =
    new TokenExchangeBuilder(
      tokenExchangeCodecs.maxArity,
      explicitDefaultsEncoding
    )
