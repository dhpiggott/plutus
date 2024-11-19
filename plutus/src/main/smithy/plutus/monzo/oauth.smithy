$version: "2"

namespace plutus.monzo

use alloy#simpleRestJson
use alloy#urlFormName

// TODO: Add doc strings from https://docs.monzo.com/?
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
}

@tokenExchange
service TokenApi {
    operations: [
        CreateAccessToken
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

structure CreateAccessTokenOutput {
    @required
    @jsonName("access_token")
    accessToken: AccessToken

    @required
    @jsonName("client_id")
    clientId: ClientId

    @required
    @jsonName("expires_in")
    expiresIn: ExpiresIn

    @required
    @jsonName("refresh_token")
    refreshToken: RefreshToken

    @required
    @jsonName("token_type")
    tokenType: TokenType

    @required
    @jsonName("user_id")
    userId: UserId
}

enum GrantType {
    AUTHORIZATION_CODE = "authorization_code"
    REFRESH_TOKEN = "refresh_token"
}

string ClientId

string ClientSecret

long ExpiresIn

string RedirectUri

string AuthorizationCode

string State

string AccessToken

string RefreshToken

enum TokenType {
    BEARER = "Bearer"
}

string UserId
