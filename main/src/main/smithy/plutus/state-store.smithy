$version: "2"

namespace plutus

use plutus.monzo#AccountId
use plutus.monzo#ClientId
use plutus.monzo#ClientSecret
use plutus.monzo#Created
use plutus.monzo#PotId
use plutus.monzo#RefreshToken
use plutus.monzo#TransactionId

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

    @required
    @jsonName("last_transactions")
    lastTransactions: LastTransactions

    /// Backing-account ID -> pot ID, recorded whenever pot-transfer metadata
    /// links the two, so import can name a pot's asset account even in a window
    /// holding no transfer for it (a dormant pot earning only interest).
    /// Defaulted so state saved before this field existed still decodes.
    @jsonName("pot_ids")
    potIds: PotIds = {}
}

map PotIds {
    key: AccountId
    value: PotId
}

@timestampFormat("epoch-seconds")
timestamp AuthorizedAt

@timestampFormat("epoch-seconds")
timestamp RefreshTokenExpiresAt

map LastTransactions {
    key: AccountId
    value: LastTransaction
}

structure LastTransaction {
    @required
    id: TransactionId

    @required
    created: Created
}
