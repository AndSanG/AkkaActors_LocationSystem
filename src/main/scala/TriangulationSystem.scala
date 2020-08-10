import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.language.postfixOps

object TriangulationSystem extends App{
  case class Point(x: Double, y: Double)

  case class GetTowerLocation(user:String, replyTo:ActorRef)
  case class TowerLocationResult(user:String, point: Point)

  case class GetLocationTriangulation(user: String, replyTo: ActorRef)
  case class LocationTriangulationResult(user: String, locations: List[Point])

  class LocationTriangulationResultBuilder(user:String){
    private var locations = new ListBuffer[Point]
    private var hasreceivedTimeout = false
    def addTowerLocation(point:Point) : ListBuffer[Point] = {
      this.locations += point}
    def sendResponse() : Boolean = {
      locations.size == 3 || hasreceivedTimeout
    }
    def timeout() ={
      hasreceivedTimeout = true
    }
    def result() :LocationTriangulationResult = {
      LocationTriangulationResult(user,locations.toList)
    }
  }

  class TowerTriangulation(towers:List[ActorRef]) extends Actor {
    def receive : Receive = {
      case GetLocationTriangulation(user,replyTo)=>
        val aggregator = context.actorOf(Props(new TowerAggregator(user,replyTo)))
        //more instances ?
        towers.foreach(tower =>{
          tower ! GetTowerLocation(user,aggregator)
        })

    }
  }

  class TowerAggregator(user: String, replyTo: ActorRef) extends Actor{
    val builder = new LocationTriangulationResultBuilder(user)
    context.setReceiveTimeout(1 seconds)
    def receiveResult : Receive = {
      case TowerLocationResult(user,point) => {
        println("Aggregator received result")
        builder.addTowerLocation(point)}
      case ReceiveTimeout =>
        println("timeout")
        builder.timeout()
    }
    def checkComplete:Receive = {
      case msg =>
        if(builder.sendResponse()){
          println("Result Complete")
          replyTo ! builder.result
          context.stop(self)
        }
    }

    //this one is called and then it calls the receiveResult
    def receive: Receive = receiveResult andThen checkComplete

  }

}
