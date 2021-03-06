package org.http4s.blaze.pipeline.stages

import java.nio.ByteBuffer
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.websocket.WebsocketBits.WebSocketFrame

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.charset.Charset

import scala.xml.Node

package object http {

  type Headers = Seq[(String, String)]

  sealed trait Response

  case class SimpleHttpResponse(status: String, code: Int, headers: Headers, body: ByteBuffer) extends Response {
    def stringBody(charset: Charset = UTF_8): String = {
      // In principle we should get the charset from the headers
      charset.decode(body.asReadOnlyBuffer()).toString
    }
  }

  object SimpleHttpResponse {
    def Ok(body: Array[Byte], headers: Headers = Nil): SimpleHttpResponse =
      SimpleHttpResponse("OK", 200, headers, ByteBuffer.wrap(body))

    def Ok(body: String, headers: Headers): SimpleHttpResponse =
      Ok(body.getBytes(UTF_8), ("Content-Type", "text/plain; charset=UTF-8")+:headers)

    def Ok(body: String): SimpleHttpResponse = Ok(body, Nil)

    def Ok(body: Node, headers: Headers): SimpleHttpResponse =
      Ok(body.toString(), headers)

    def Ok(body: Node): SimpleHttpResponse = Ok(body, Nil)
  }

  case class WSResponse(stage: LeafBuilder[WebSocketFrame]) extends Response
}
