$version: "2"

namespace plutus

use plutus.monzo#ClientId
use plutus.monzo#ClientSecret
use plutus.monzo#RefreshToken

/// Credentials only. Where a run starts comes from the book it is reconciled
/// against, never from here. State saved by earlier versions also carries
/// last_transactions and pot_ids, which decoding skips and the next save drops.
@suppress(["UnreferencedShape"])
structure State {
    @required
    @jsonName("client_id")
    clientId: ClientId

    @required
    @jsonName("client_secret")
    clientSecret: ClientSecret

    @required
    @jsonName("authorized_at")
    authorizedAt: AuthorizedAt

    @required
    @jsonName("refresh_token")
    refreshToken: RefreshToken

    @jsonName("refresh_token_expires_at")
    refreshTokenExpiresAt: RefreshTokenExpiresAt
}

@timestampFormat("epoch-seconds")
timestamp AuthorizedAt

@timestampFormat("epoch-seconds")
timestamp RefreshTokenExpiresAt
