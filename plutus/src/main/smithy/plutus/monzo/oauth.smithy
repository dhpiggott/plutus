$version: "2"

namespace plutus.monzo

use alloy#simpleRestJson

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

@externalDocumentation(url: "https://docs.monzo.com/?shell#acquire-an-access-token")
@http(method: "POST", uri: "/oauth2/token")
operation CreateAccessToken {
    input := {
        @required
        @queryName("grant_type")
        grantType: GrantType
        @required
        @queryName("client_id")
        clientId: ClientId
        @required
        @queryName("client_secret")
        clientSecret: ClientSecret
        @queryName("redirect_uri")
        redirectUri: RedirectUri
        code: AuthorizationCode
        @queryName("refresh_token")
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

@protocolDefinition
@trait(selector: "service")
structure tokenExchange {}

/// Unwraps the values of a list, set, or map into the containing
/// structure/union.
@trait(
    selector: ":is(structure, union) > :test(member > :test(list, map))",
    breakingChanges: [{change: "any"}]
)
structure queryFlattened {}

/// The queryName trait allows a serialized form key to differ from a structure
/// member name used in the model.
@trait(
    selector: ":is(structure, union) > member",
    breakingChanges: [{change: "any"}]
)
string queryName

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
