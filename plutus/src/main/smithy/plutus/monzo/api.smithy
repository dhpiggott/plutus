$version: "2"

namespace plutus.monzo

use alloy#simpleRestJson

// TODO: Add doc strings from https://docs.monzo.com/?

@httpBearerAuth
@simpleRestJson
service Api {
    operations: [
        WhoAmI
        ListAccounts
        GetBalance
        ListTransactions
    ]
    errors: [
        BadRequest
        Unauthorized
        Forbidden
        MethodNotAllowed
        PageNotFound
        NotAcceptable
        TooManyRequests
        InternalServerError
        GatewayTimeout
    ]
}

@externalDocumentation(url: "https://docs.monzo.com/?shell#authenticating-requests")
@readonly
@http(method: "GET", uri: "/ping/whoami")
operation WhoAmI {
    output := {
        @required
        authenticated: Authenticated
        @required
        @jsonName("client_id")
        clientId: ClientId
        @required
        @jsonName("user_id")
        userId: UserId
    }
}

@externalDocumentation(url: "https://docs.monzo.com/?shell#list-accounts")
@readonly
@http(method: "GET", uri: "/accounts")
operation ListAccounts {
    output := {
        @required
        accounts: Accounts
    }
}

@externalDocumentation(url: "https://docs.monzo.com/#balance")
@readonly
@http(method: "GET", uri: "/balance")
operation GetBalance {
    input := {
        @required
        @httpQuery("account_id")
        accountId: AccountId
    }
    output := {
        @required
        balance: Amount
        @required
        @jsonName("total_balance")
        totalBalance: Amount
        @required
        currency: Currency
        @required
        @jsonName("spend_today")
        spendToday: Amount
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
    }
}

boolean Authenticated

list Accounts {
    member: Account
}

list Transactions {
    member: Transaction
}

// TODO: Refine this (named types, required hints).
structure Account {
    @required
    id: AccountId
    @required
    description: Description
    @required
    created: Created
    closed: Boolean,
    owners: Document,
    @required
    @jsonName("sort_code")
    sortCode: String
    currency: String,
    type: String,
    @jsonName("payment_details")
    paymentDetails: Document,
    @required
    @jsonName("account_number")
    accountNumber: String,
    @jsonName("country_code")
    countryCode: String
}

structure Transaction {
    counterparty: Counterparty
    @required
    amount: Amount
    @jsonName("decline_reason")
    declineReason: DeclineReason
    @required
    created: Created
    @required
    currency: Currency
    @required
    description: Description
    @required
    id: TransactionId
    merchant: Merchant
    @required
    metadata: Metadata
    @required
    notes: Notes
    @required
    @jsonName("is_load")
    isLoad: IsLoad
    settled: Settled
    @required
    category: Category
}

string AccountId

timestamp Since

timestamp Before

integer Limit

string Description

// TODO: Refine this (named types, required hints).
structure Counterparty {
    @jsonName("account_number")
    accountNumber: String
    @jsonName("account_id")
    accountId: String
    name: String
    @jsonName("preferred_name")
    preferredName: String
    @jsonName("sort_code")
    sortCode: String
    @jsonName("user_id")
    userId: String
}

bigInteger Amount

enum DeclineReason {
    INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS"
    CARD_INACTIVE = "CARD_INACTIVE"
    CARD_BLOCKED = "CARD_BLOCKED"
    INVALID_CVC = "INVALID_CVC"
    PIN_REQUIRED = "PIN_REQUIRED"
    SCA_NOT_AUTHENTICATED_CARD_NOT_PRESENT = "SCA_NOT_AUTHENTICATED_CARD_NOT_PRESENT"
    OTHER = "OTHER"
}

timestamp Created

string Currency

string TransactionId

// TODO: Refine this (named types, required hints).
structure Merchant {
    groupId: String
    name: String
    suggestedTags: String
    emoji: String
    atm: Boolean
    disableFeedback: Boolean
    online: Boolean
    logo: String
    id: String
    address: Document
    category: String
    metadata: Document
}

document Metadata

string Notes

boolean IsLoad

/// This should be a timestamp, but there are cases when we get empty
/// string... which of course fails to parse as one.
string Settled

string Category
