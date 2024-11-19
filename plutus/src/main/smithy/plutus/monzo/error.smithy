$version: "2"

namespace plutus.monzo

// TODO: Add doc strings from https://docs.monzo.com/?
@error("client")
@httpError(400)
structure BadRequest {}

@error("client")
@httpError(401)
structure Unauthorized {}

@error("client")
@httpError(403)
structure Forbidden {}

@error("client")
@httpError(405)
structure MethodNotAllowed {}

@error("client")
@httpError(404)
structure PageNotFound {}

@error("client")
@httpError(406)
structure NotAcceptable {}

@error("client")
@retryable
@httpError(429)
structure TooManyRequests {}

@error("server")
@httpError(500)
structure InternalServerError {}

@error("server")
@retryable
@httpError(504)
structure GatewayTimeout {}
