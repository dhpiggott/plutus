$version: "2"

namespace plutus

use plutus.monzo#RefreshToken

@suppress(["UnreferencedShape"])
structure State {
    @required
    @jsonName("authorized_at")
    authorizedAt: AuthorizedAt

    @required
    @jsonName("refresh_token")
    refreshToken: RefreshToken
}

@timestampFormat("epoch-seconds")
timestamp AuthorizedAt
