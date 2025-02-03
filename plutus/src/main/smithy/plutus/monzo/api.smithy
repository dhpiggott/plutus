$version: "2"

namespace plutus.monzo

use alloy#discriminated
use alloy#jsonUnknown
use alloy#simpleRestJson
use alloy#urlFormName
use smithy4s.meta#adt

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

string AccountId

/// This can be a date-time string or a transaction ID. Ideally we'd model it as
/// a union, but because it's a query parameter, we can't.
string Since

timestamp Before

integer Limit

list Transactions {
    member: Transaction
}

@adt
@discriminated("type")
union Account {
    @jsonName("uk_retail")
    ukRetail: UkRetailAccount

    @jsonName("uk_retail_joint")
    ukRetailJoint: UkRetailJointAccount

    @jsonName("uk_monzo_flex")
    ukMonzoFlex: UkMonzoFlexAccount
}

@adt
@discriminated("scheme")
union Transaction {
    @jsonName("bacs")
    bacsTransaction: BacsTransaction

    @jsonName("payport_faster_payments")
    payportFasterPaymentsTransaction: PayportFasterPaymentsTransaction

    @jsonName("international-payments")
    internationalPaymentsTransaction: InternationalPaymentsTransaction

    @jsonName("mastercard")
    mastercardTransaction: MastercardTransaction

    @jsonName("3dsecure")
    d3secureTransaction: D3secureTransaction

    @jsonName("monzo_paid")
    monzoPaidTransaction: MonzoPaidTransaction

    @jsonName("monzo_flex")
    monzoFlexTransaction: MonzoFlexTransaction
}

structure UkRetailAccount with [AccountMixin] {}

structure UkRetailJointAccount with [AccountMixin] {}

structure UkMonzoFlexAccount with [AccountMixin] {}

@mixin
structure AccountMixin {
    @required
    id: AccountId

    @jsonUnknown
    unknown: UnknownProperties
}

structure BacsTransaction with [TransactionMixin, TransactionWithCounterpartyMixin] {}

structure PayportFasterPaymentsTransaction with [TransactionMixin, TransactionWithCounterpartyMixin] {}

structure InternationalPaymentsTransaction with [TransactionMixin, TransactionWithCounterpartyMixin] {}

structure MastercardTransaction with [TransactionMixin, TransactionWithMerchantMixin] {}

structure D3secureTransaction with [TransactionMixin, TransactionWithMerchantMixin] {}

structure MonzoPaidTransaction with [TransactionMixin] {}

structure MonzoFlexTransaction with [TransactionMixin] {}

@mixin
structure TransactionMixin {
    @required
    id: TransactionId

    @required
    created: Created

    @required
    amount: Amount

    @jsonName("decline_reason")
    declineReason: DeclineReason

    @required
    description: Description

    @required
    notes: Notes

    @jsonUnknown
    unknown: UnknownProperties
}

@mixin
structure TransactionWithCounterpartyMixin {
    @required
    counterparty: Counterparty
}

@mixin
structure TransactionWithMerchantMixin {
    @required
    merchant: Merchant
}

string TransactionId

@timestampFormat("date-time")
timestamp Created

bigInteger Amount

string DeclineReason

string Description

string Notes

structure Counterparty {
    @required
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
