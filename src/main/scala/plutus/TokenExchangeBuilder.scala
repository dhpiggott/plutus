package plutus

import smithy4s.http4s.SimpleProtocolBuilder

object TokenExchangeBuilder
    extends SimpleProtocolBuilder[monzo.TokenExchange](TokenExchangeCodecs)
