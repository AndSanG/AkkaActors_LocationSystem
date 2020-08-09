/**
 * Copyright (C) 2015 Roland Kuhn <http://rolandkuhn.com>
 */
package messaging

import akka.actor._

object RequestResponseActors extends App {
  
  case class Request(msg: String)
  case class Response(msg: String)
  
  class Requester(responder: ActorRef) extends Actor {
    responder ! Request("hello")
    
    def receive = {
      case Response(msg) =>
        println(s"got response: $msg")
        context.system.terminate()
    }
  }
  
  class Responder extends Actor {
    def receive = {
      case Request(msg) =>
        println(s"got request: $msg")
        sender() ! Response("got it!")
    }
  }
  
  val sys = ActorSystem("ReqRes")
  val responder = sys.actorOf(Props[Responder], "responder")
  val requester = sys.actorOf(Props(new Requester(responder)), "requester")
  
}