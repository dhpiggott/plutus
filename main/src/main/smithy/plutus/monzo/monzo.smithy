$version: "2"

namespace plutus.monzo

use alloy#jsonUnknown
use alloy#simpleRestJson
use alloy#urlFormName

string ClientId

string RedirectUri

string State

string AuthorizationCode

@tokenExchange
service TokenApi {
    operations: [
        CreateAccessToken
    ]
}

@protocolDefinition(
    traits: [
        smithy.api#default
        smithy.api#error
        smithy.api#required
        smithy.api#pattern
        smithy.api#range
        smithy.api#length
        smithy.api#http
        smithy.api#httpError
        smithy.api#httpHeader
        smithy.api#httpLabel
        smithy.api#httpPayload
        smithy.api#httpPrefixHeaders
        smithy.api#httpQuery
        smithy.api#httpQueryParams
        smithy.api#jsonName
        smithy.api#timestampFormat
        alloy#uncheckedExamples
        alloy#untagged
        alloy#urlFormFlattened
        alloy#urlFormName
        alloy#uuidFormat
        alloy#discriminated
    ]
)
@trait(selector: "service [trait]")
structure tokenExchange {}

@externalDocumentation(url: "https://docs.monzo.com/?shell#acquire-an-access-token")
@http(method: "POST", uri: "/oauth2/token")
operation CreateAccessToken {
    input := {
        @required
        @urlFormName("grant_type")
        grantType: GrantType

        @required
        @urlFormName("client_id")
        clientId: ClientId

        @required
        @urlFormName("client_secret")
        clientSecret: ClientSecret

        @urlFormName("redirect_uri")
        redirectUri: RedirectUri

        code: AuthorizationCode

        @urlFormName("refresh_token")
        refreshToken: RefreshToken
    }

    output := {
        @required
        @jsonName("access_token")
        accessToken: AccessToken

        @required
        @jsonName("refresh_token")
        refreshToken: RefreshToken

        @jsonUnknown
        unknown: UnknownProperties
    }
}

string RefreshToken

enum GrantType {
    AUTHORIZATION_CODE = "authorization_code"
    REFRESH_TOKEN = "refresh_token"
}

string ClientSecret

string AccessToken

@externalDocumentation(url: "https://docs.monzo.com/?shell#introduction")
@httpBearerAuth
@simpleRestJson
service Api {
    operations: [
        ListAccounts
        ListPots
        ListTransactions
    ]
}

@externalDocumentation(url: "https://docs.monzo.com/?shell#list-accounts")
@readonly
@http(method: "GET", uri: "/accounts")
operation ListAccounts {
    output := {
        @required
        accounts: Accounts

        @jsonUnknown
        unknown: UnknownProperties
    }
}

@externalDocumentation(url: "https://docs.monzo.com/?shell#list-pots")
@readonly
@http(method: "GET", uri: "/pots")
operation ListPots {
    input := {
        @required
        @httpQuery("current_account_id")
        currentAccountId: AccountId
    }

    output := {
        @required
        pots: Pots

        @jsonUnknown
        unknown: UnknownProperties
    }
}

@externalDocumentation(url: "https://docs.monzo.com/?shell#list-transactions")
@readonly
@http(method: "GET", uri: "/transactions?expand[]=merchant")
operation ListTransactions {
    input := {
        @required
        @httpQuery("account_id")
        accountId: AccountId

        @httpQuery("since")
        since: Since

        @httpQuery("before")
        before: Before

        @httpQuery("limit")
        limit: Limit
    }

    output := {
        @required
        transactions: Transactions

        @jsonUnknown
        unknown: UnknownProperties
    }
}

list Accounts {
    member: Account
}

list Pots {
    member: Pot
}

string AccountId

string PotId

string Currency

boolean Deleted

/// The archive trigger for main accounts, as Deleted is for pots. Optional
/// because pot backing accounts are constructed from transfer metadata rather
/// than decoded from /accounts, so no closed flag is ever seen for them.
boolean Closed

/// uk_retail, uk_retail_joint, … Modelled as an open string (like Category) so
/// a type Monzo adds later can't fail a decode. Optional because pot backing
/// accounts are discovered from transaction metadata rather than /accounts, so
/// no type is ever seen for them — AssetAccounts treats its absence as "pot".
string AccountType

/// This can be a date-time string or a transaction ID. Ideally we'd model it as
/// a union, but because it's a query parameter, we can't.
string Since

timestamp Before

integer Limit

list Transactions {
    member: Transaction
}

structure Account {
    @required
    id: AccountId

    closed: Closed

    @jsonName("type")
    accountType: AccountType

    @jsonUnknown
    unknown: UnknownProperties
}

/// The fields import consumes: name names the pot's asset account, currency
/// backs the fail-fast check that a pot is denominated in the book's currency,
/// and deleted triggers automatic archiving of the pot's asset account.
/// Deleted pots are listed too (with their names intact), which is exactly
/// right: their historical transactions may still fall in an import window.
structure Pot {
    @required
    id: PotId

    @required
    name: Name

    @required
    currency: Currency

    @required
    deleted: Deleted

    @jsonUnknown
    unknown: UnknownProperties
}

structure Transaction {
    @required
    id: TransactionId

    @required
    created: Created

    @required
    amount: Amount

    @jsonName("decline_reason")
    declineReason: DeclineReason

    category: Category

    merchant: Merchant

    @required
    counterparty: Counterparty

    @required
    description: Description

    @required
    notes: Notes

    metadata: Metadata

    @jsonUnknown
    unknown: UnknownProperties
}

/// The two keys Plutus consumes are modelled as members; every other key lands
/// in unknown, with document values so an unexpected shape there can't fail the
/// decode of a whole transaction page. Both members appear on pot-transfer
/// legs: pot_account_id (main-account side) is the pot's backing account —
/// a real account object whose transactions (interest, in particular) are only
/// reachable by passing that ID to /transactions — and pot_id names which pot,
/// linking the two. See https://community.monzo.com/t/-/193089/11 —
/// undocumented, may change.
structure Metadata {
    @jsonName("pot_account_id")
    potAccountId: AccountId

    @jsonName("pot_id")
    potId: PotId

    @jsonUnknown
    unknown: UnknownProperties
}

string TransactionId

@timestampFormat("date-time")
timestamp Created

bigInteger Amount

string DeclineReason

// Monzo's own categorisation (general, eating_out, groceries, transport, …).
// Modelled as an open string rather than an enum so a category Monzo adds later
// can't fail the decode of a whole transaction page; import files by it,
// creating the category's expense account on first sight.
string Category

string Description

string Notes

structure Counterparty {
    name: Name

    @jsonUnknown
    unknown: UnknownProperties
}

structure Merchant {
    @required
    name: Name

    @jsonUnknown
    unknown: UnknownProperties
}

string Name

map UnknownProperties {
    key: String
    value: Document
}
