$version: "2"

namespace plutus.ofx

@externalDocumentation(url: "https://financialdataexchange.org/common/Uploaded%20files/OFX%20files/OFX%20Banking%20Specification%20v2.3.pdf")
@suppress(["UnreferencedShape"])
@xmlName("OFX")
structure Ofx {
    @required
    @xmlName("SIGNONMSGSRSV1")
    signonMessageSetResponse: SignonMessageSetResponse

    @required
    @xmlName("BANKMSGSRSV1")
    bankMessageSetResponse: BankMessageSetResponse

    @required
    @xmlName("CREDITCARDMSGSRSV1")
    creditCardMessageSetResponse: CreditCardMessageSetResponse
}

structure SignonMessageSetResponse {
    @required
    @xmlName("SONRS")
    signonResponse: SignonResponse
}

structure SignonResponse {
    @required
    @xmlName("STATUS")
    status: Status

    @required
    @xmlName("DTSERVER")
    dateServer: Datetime

    @required
    @xmlName("LANGUAGE")
    language: Language
}

structure BankMessageSetResponse {
    @required
    @xmlFlattened
    @xmlName("STMTTRNRS")
    statementTransactionsResponses: StatementTransactionsResponses
}

structure CreditCardMessageSetResponse {
    @required
    @xmlFlattened
    @xmlName("CCSTMTTRNRS")
    creditCardStatementTransactionsResponses: CreditCardStatementTransactionsResponses
}

list StatementTransactionsResponses {
    member: StatementTransactionsResponse
}

list CreditCardStatementTransactionsResponses {
    member: CreditCardStatementTransactionsResponse
}

structure StatementTransactionsResponse {
    @required
    @xmlName("TRNUID")
    transactionUniqueId: TransactionUniqueId

    @required
    @xmlName("STATUS")
    status: Status

    @required
    @xmlName("STMTRS")
    statementResponse: StatementResponse
}

structure CreditCardStatementTransactionsResponse {
    @required
    @xmlName("TRNUID")
    transactionUniqueId: TransactionUniqueId

    @required
    @xmlName("STATUS")
    status: Status

    @required
    @xmlName("CCSTMTRS")
    creditCardStatementResponse: CreditCardStatementResponse
}

structure Status {
    @required
    @xmlName("CODE")
    code: Code

    @required
    @xmlName("SEVERITY")
    severity: Severity
}

structure StatementResponse {
    @required
    @xmlName("CURDEF")
    currencyDefault: DefaultCurrency

    @required
    @xmlName("BANKACCTFROM")
    bankAccountFrom: BankAccountFrom

    @required
    @xmlName("BANKTRANLIST")
    bankTransactionList: BankTransactionList
}

structure CreditCardStatementResponse {
    @required
    @xmlName("CURDEF")
    currencyDefault: DefaultCurrency

    @required
    @xmlName("CCACCTFROM")
    creditCardAccountFrom: CreditCardAccountFrom

    @required
    @xmlName("BANKTRANLIST")
    bankTransactionList: BankTransactionList
}

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

structure CreditCardAccountFrom {
    @required
    @xmlName("ACCTID")
    accountId: AccountId
}

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
