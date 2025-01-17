$version: "2"

namespace plutus.monzo

use alloy#discriminated
use alloy#jsonUnknown
use alloy#simpleRestJson
use smithy4s.meta#adt

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

@mixin
structure AccountMixin {
    @required
    id: AccountId

    @jsonUnknown
    unknown: UnknownProperties
}

@mixin
structure UkRetailAccountMixin with [AccountMixin] {
    @required
    @jsonName("sort_code")
    sortCode: SortCode

    @required
    @jsonName("account_number")
    accountNumber: AccountNumber
}

structure UkRetailAccount with [UkRetailAccountMixin] {
}

structure UkRetailJointAccount with [UkRetailAccountMixin] {
}

structure UkMonzoFlexAccount with [AccountMixin] {
}

structure Transaction {
    @required
    counterparty: Counterparty

    @required
    amount: Amount

    @jsonName("decline_reason")
    declineReason: DeclineReason

    @required
    created: Created

    @required
    description: Description

    @required
    id: TransactionId

    merchant: Merchant

    @required
    notes: Notes

    @jsonUnknown
    unknown: UnknownProperties
}

string AccountId

string SortCode

string AccountNumber

timestamp Since

timestamp Before

integer Limit

structure Counterparty {
    name: Name

    @jsonUnknown
    unknown: UnknownProperties
}

bigInteger Amount

string DeclineReason

@timestampFormat("date-time")
timestamp Created

string Description

string TransactionId

structure Merchant {
    @required
    name: Name

    @jsonUnknown
    unknown: UnknownProperties
}

string Notes

string Name

map UnknownProperties {
    key: String
    value: Document
}
