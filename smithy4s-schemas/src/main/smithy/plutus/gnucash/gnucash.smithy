$version: "2"

namespace plutus.gnucash

@externalDocumentation(url: "https://wiki.gnucash.org/wiki/GnuCash_XML_format")
@suppress(["UnreferencedShape"])
@xmlName("gnc-v2")
@xmlNamespace(
    prefix: "gnc",
    uri: "http://www.gnucash.org/XML/gnc"
)
// @xmlNamespace(
//     prefix: "act",
//     uri: "http://www.gnucash.org/XML/act"
// )
// @xmlNamespace(
//     prefix: "cd",
//     uri: "http://www.gnucash.org/XML/cd"
// )
// @xmlNamespace(
//     prefix: "cmdty",
//     uri: "http://www.gnucash.org/XML/cmdty"
// )
// @xmlNamespace(
//     prefix: "price",
//     uri: "http://www.gnucash.org/XML/price"
// )
// @xmlNamespace(
//     prefix: "slot",
//     uri: "http://www.gnucash.org/XML/slot"
// )
// @xmlNamespace(
//     prefix: "split",
//     uri: "http://www.gnucash.org/XML/split"
// )
// @xmlNamespace(
//     prefix: "sx",
//     uri: "http://www.gnucash.org/XML/sx"
// )
// @xmlNamespace(
//     prefix: "trn",
//     uri: "http://www.gnucash.org/XML/trn"
// )
// @xmlNamespace(
//     prefix: "ts",
//     uri: "http://www.gnucash.org/XML/ts"
// )
// @xmlNamespace(
//     prefix: "fs",
//     uri: "http://www.gnucash.org/XML/fs"
// )
// @xmlNamespace(
//     prefix: "bgt",
//     uri: "http://www.gnucash.org/XML/bgt"
// )
// @xmlNamespace(
//     prefix: "recurrence",
//     uri: "http://www.gnucash.org/XML/recurrence"
// )
// @xmlNamespace(
//     prefix: "lot",
//     uri: "http://www.gnucash.org/XML/lot"
// )
// @xmlNamespace(
//     prefix: "addr",
//     uri: "http://www.gnucash.org/XML/addr"
// )
// @xmlNamespace(
//     prefix: "billterm",
//     uri: "http://www.gnucash.org/XML/billterm"
// )
// @xmlNamespace(
//     prefix: "bt-days",
//     uri: "http://www.gnucash.org/XML/bt-days"
// )
// @xmlNamespace(
//     prefix: "bt-prox",
//     uri: "http://www.gnucash.org/XML/bt-prox"
// )
// @xmlNamespace(
//     prefix: "cust",
//     uri: "http://www.gnucash.org/XML/cust"
// )
// @xmlNamespace(
//     prefix: "employee",
//     uri: "http://www.gnucash.org/XML/employee"
// )
// @xmlNamespace(
//     prefix: "entry",
//     uri: "http://www.gnucash.org/XML/entry"
// )
// @xmlNamespace(
//     prefix: "invoice",
//     uri: "http://www.gnucash.org/XML/invoice"
// )
// @xmlNamespace(
//     prefix: "job",
//     uri: "http://www.gnucash.org/XML/job"
// )
// @xmlNamespace(
//     prefix: "order",
//     uri: "http://www.gnucash.org/XML/order"
// )
// @xmlNamespace(
//     prefix: "owner",
//     uri: "http://www.gnucash.org/XML/owner"
// )
// @xmlNamespace(
//     prefix: "tte",
//     uri: "http://www.gnucash.org/XML/tte"
// )
// @xmlNamespace(
//     prefix: "vendor",
//     uri: "http://www.gnucash.org/XML/vendor"
// )
structure GnuCash {
    @required
    @xmlName("gnc:count-data")
    countData: CountData

    @required
    @xmlName("gnc:count-data")
    count: Integer

    @required
    @xmlName("gnc:book")
    @xmlNamespace(
        prefix: "book",
        uri: "http://www.gnucash.org/XML/book"
    )
    book: Book
}

structure CountData {
    @xmlAttribute
    @xmlName("cd:type")
    type: String
}

structure Book {
    @xmlAttribute
    version: String

    @required
    @xmlName("book:id")
    idData: IdData

    @required
    @xmlName("book:id")
    id: String
}

structure IdData {
    @xmlAttribute
    @xmlName("type")
    type: String
}
