$version: "2"

namespace plutus

use plutus.monzo#CreateAccessTokenOutput

@suppress(["UnreferencedShape"])
structure StoredTokens {
    @required
    @jsonName("authorized_at")
    authorizedAt: AuthorizedAt

    @required
    @jsonName("create_access_token_output")
    createAccessTokenOutput: CreateAccessTokenOutput
}

@timestampFormat("epoch-seconds")
timestamp AuthorizedAt
