import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.language.postfixOps

object TriangulationSystem {
  case class Point(x: Double, y: Double)

  case class GetLocationTriangulation(device: String, replyTo: ActorRef)
  case class LocationTriangulationResult(user: String, locations: List[Point])

  class LocationTriangulationResultBuilder(user:String){
    private var locations = new ListBuffer[Point]
    var hasreceivedTimeout = false
    def addTowerLocation(point:Point) : ListBuffer[Point] = {
      this.locations += point}
    def sendResponse() : Boolean = {
      locations.size == 3
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
        towers.foreach(tower =>{
          tower ! GetTowerLocation(user,aggregator)
        })

    }
  }

  class TowerAggregator(user: String, replyTo: ActorRef) extends Actor{
    val builder = new LocationTriangulationResultBuilder(user)
    context.setReceiveTimeout(1 seconds)
    def receiveResult : Receive = {
      case TowerLocationResult(user,point,towerId) => {
        //println(s"Aggregator received result from $towerId for device $user ")
        builder.addTowerLocation(point)}
      case ReceiveTimeout =>
        println(s"timeout for device $user")
        builder.timeout()
    }
    def checkComplete:Receive = {
      case msg =>
        if(builder.sendResponse()){
          replyTo ! builder.result
          context.stop(self)
        }
    }

    //this one is called and then it calls the receiveResult
    def receive: Receive = receiveResult andThen checkComplete

  }

}
