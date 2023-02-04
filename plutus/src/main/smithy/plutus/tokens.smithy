$version: "2"

namespace plutus

use plutus.monzo#CreateAccessTokenOutput

@suppress(["UnreferencedShape"])
structure StoredTokens {
    @required
    @jsonName("create_access_token_output")
    createAccessTokenOutput: CreateAccessTokenOutput
    @required
    @jsonName("created_at")
    createdAt: CreatedAt
}

@timestampFormat("epoch-seconds")
timestamp CreatedAt
