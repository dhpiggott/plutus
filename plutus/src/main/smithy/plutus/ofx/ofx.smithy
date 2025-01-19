$version: "2"

namespace plutus.ofx

@externalDocumentation(url: "https://financialdataexchange.org/common/Uploaded%20files/OFX%20files/OFX%20Banking%20Specification%20v2.3.pdf")
@suppress(["UnreferencedShape"])
@xmlName("OFX")
structure Ofx {
    @required
    @xmlName("BANKMSGSRSV1")
    bankMessageSetResponse: BankMessageSetResponse
}

structure BankMessageSetResponse {
    @required
    @xmlFlattened
    @xmlName("STMTTRNRS")
    statementTransactionsResponses: StatementTransactionsResponses
}

list StatementTransactionsResponses {
    member: StatementTransactionsResponse
}

structure StatementTransactionsResponse {
    @required
    @xmlName("STMTRS")
    statementResponse: StatementResponse
}

structure StatementResponse {
    @required
    @xmlName("BANKACCTFROM")
    bankAccountFrom: BankAccountFrom

    @required
    @xmlName("BANKTRANLIST")
    bankTransactionList: BankTransactionList
}

structure BankAccountFrom {
    @required
    @xmlName("ACCTID")
    accountId: AccountId
}

structure BankTransactionList {
    @required
    @xmlFlattened
    @xmlName("STMTTRN")
    statementTransactions: StatementTransactions
}

list StatementTransactions {
    member: StatementTransaction
}

structure StatementTransaction {
    @required
    @xmlName("DTPOSTED")
    datePosted: Datetime

    @required
    @xmlName("TRNAMT")
    transactionAmount: TransactionAmount

    @required
    @xmlName("FITID")
    financialInstitutionId: FinancialInstitutionId

    @required
    @xmlName("NAME")
    name: Name

    @xmlName("MEMO")
    memo: Memo
}

string Datetime

string AccountId

bigDecimal TransactionAmount

string FinancialInstitutionId

string Name

string Memo
