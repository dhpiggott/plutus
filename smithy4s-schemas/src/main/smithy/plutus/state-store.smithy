$version: "2"

namespace plutus

use plutus.monzo#AccountId
use plutus.monzo#ClientId
use plutus.monzo#ClientSecret
use plutus.monzo#Created
use plutus.monzo#RefreshToken
use plutus.monzo#TransactionId

service StateStore {
    operations: [
        LoadState,
        SaveState
    ]
}

operation LoadState {
    output := {
        state: State
    }
}

operation SaveState {
    input := {
        @required
        state: State

        @required
        mode: SaveStateMode
    }
}

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

    @required
    @jsonName("last_transactions")
    lastTransactions: LastTransactions
}

@timestampFormat("epoch-seconds")
timestamp AuthorizedAt

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

enum SaveStateMode {
  CREATE
  UPDATE
}
