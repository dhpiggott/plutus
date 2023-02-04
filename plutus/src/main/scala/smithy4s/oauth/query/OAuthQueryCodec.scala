package smithy4s
package oauth.query

private[oauth] trait OAuthQueryCodec[-A] extends (A => FormData):
  def apply(a: A): FormData
