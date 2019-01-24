package graphql

import play.api.mvc.{Cookie, Cookies, Headers}

import scala.collection.mutable.ListBuffer

case class Context(requestHeaders: Headers,
                   requestCookies: Cookies,
                   newHeaders: ListBuffer[(String, String)] = ListBuffer.empty,
                   newCookies: ListBuffer[Cookie] = ListBuffer.empty)