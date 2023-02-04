$version: "2"

namespace plutus.ofx

// TODO: Add doc strings from https://financialdataexchange.org/common/Uploaded%20files/OFX%20files/OFX%20Banking%20Specification%20v2.3.pdf?

// TODO: Header?
@suppress(["UnreferencedShape"])
@xmlName("OFX")
structure Ofx {
    @required
    signonMessageSetResponse: SignonMessageSetResponse
    @required
    bankMessageSetResponse: BankMessageSetResponse
}

@xmlName("SIGNONMSGSRSV1")
structure SignonMessageSetResponse {
    @required
    signonResponse: SignonResponse
}

@xmlName("SONRS")
structure SignonResponse {
    @required
    status: Status
    @required
    @xmlName("DTSERVER")
    dateServer: Datetime
    @required
    @xmlName("LANGUAGE")
    language: Language
}

@xmlName("BANKMSGSRSV1")
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
    @xmlName("TRNUID")
    transactionUniqueId: TransactionUniqueId
    @required
    status: Status
    @required
    statementResponse: StatementResponse
}

@xmlName("STATUS")
structure Status {
    @required
    @xmlName("CODE")
    code: Code
    @required
    @xmlName("SEVERITY")
    severity: Severity
}

@xmlName("STMTRS")
structure StatementResponse {
    @required
    @xmlName("CURDEF")
    currencyDefault: DefaultCurrency
    @required
    bankAccountFrom: BankAccountFrom
    @required
    bankTransactionList: BankTransactionList
}

@xmlName("BANKACCTFROM")
structure BankAccountFrom {
    @required
    @xmlName("BANKID")
    bankId: BankId
    @required
    @xmlName("ACCTID")
    accountId: AccountId
    @required
    @xmlName("ACCTTYPE")
    accountType: AccountType
}

@xmlName("BANKTRANLIST")
structure BankTransactionList {
    @required
    @xmlName("DTSTART")
    dateStart: Datetime
    @required
    @xmlName("DTEND")
    dateEnd: Datetime
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
    @xmlName("TRNTYPE")
    transactionType: TransactionType
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

integer Code

enum Severity {
    INFO = "INFO"
}

string Datetime

string Language

integer TransactionUniqueId

string DefaultCurrency

string BankId

string AccountId

enum AccountType {
    CHECKING = "CHECKING"
    SAVINGS = "SAVINGS"
}

enum TransactionType {
    CREDIT = "CREDIT"
    DEBIT = "DEBIT"
    INT = "INT"
    DIV = "DIV"
    FEE = "FEE"
    SRVCHG = "SRVCHG"
    DEP = "DEP"
    ATM = "ATM"
    POS = "POS"
    XFER = "XFER"
    CHECK = "CHECK"
    PAYMENT = "PAYMENT"
    CASH = "CASH"
    DIRECTDEP = "DIRECTDEP"
    DIRECTDEBIT = "DIRECTDEBIT"
    REPEATPMT = "REPEATPMT"
    HOLD = "HOLD"
    OTHER = "OTHER"
}

bigDecimal TransactionAmount

string FinancialInstitutionId

string Name

string Memo
