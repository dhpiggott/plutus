$version: "2"

namespace plutus.monzo

use alloy#jsonUnknown
use alloy#simpleRestJson
use alloy#urlFormName

@externalDocumentation(url: "https://docs.monzo.com/?shell#acquire-an-access-token")
@simpleRestJson
service AuthorizationCodeReceiver {
    operations: [
        ReceiveAuthorizationCode
    ]
}

@externalDocumentation(url: "https://docs.monzo.com/?shell#acquire-an-access-token")
@readonly
@http(method: "GET", uri: "/oauth/callback")
operation ReceiveAuthorizationCode {
    input := {
        @required
        @httpQuery("code")
        code: AuthorizationCode

        @required
        @httpQuery("state")
        state: State
    }

    output := {
        @required
        // TODO: Make this render as text.
        @httpPayload
        body: Body
    }
}

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

    output: CreateAccessTokenOutput
}

string AuthorizationCode

string State

string Body

enum GrantType {
    AUTHORIZATION_CODE = "authorization_code"
    REFRESH_TOKEN = "refresh_token"
}

string ClientId

string ClientSecret

string RedirectUri

structure CreateAccessTokenOutput {
    @required
    @jsonName("access_token")
    accessToken: AccessToken

    @required
    @jsonName("refresh_token")
    refreshToken: RefreshToken

    @jsonUnknown
    unknown: UnknownProperties
}

string AccessToken

string RefreshToken

map UnknownProperties {
    key: String
    value: Document
}
