/**
 * Adapted from Roland Kuhn's "Reactive Design Patterns" book.
 */
package messaging

import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.AbstractActor.Receive
import akka.actor.Props
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.util.Timeout
import scala.concurrent.duration._
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.ActorSystem
import scala.language.postfixOps

object Aggregator extends App {
  case class GetTheme(user: String, replyTo: ActorRef)
  case class ThemeResult(css: String)

  case class GetPersonalNews(user: String, replyTo: ActorRef)
  case class PersonalNewsResult(news: List[String])

  case class GetTopNews(replyTo: ActorRef)
  case class TopNewsResult(news: List[String])

  case class GetFrontPage(user: String, replyTo: ActorRef)
  case class FrontPageResult(user: String, css: String, news: List[String])

  case class GetOverride(replyTo: ActorRef)
  sealed trait OverrideResult
  case object NoOverride extends OverrideResult
  case class Override(css: String, news: List[String]) extends OverrideResult

  
  class FrontPageResultBuilder(user: String) {
    private var css: Option[String] = None
    private var personalNews: Option[List[String]] = None
    private var topNews: Option[List[String]] = None

    def addCSS(css: String): Unit = 
      this.css = Option(css)
    def addPersonalNews(news: List[String]): Unit = 
      this.personalNews = Option(news)
    def addTopNews(news: List[String]): Unit = 
      this.topNews = Option(news)

    def timeout() : Unit = {
      if (css.isEmpty) css = Some("default.css")
      if (personalNews.isEmpty) personalNews = Some(Nil)
      if (topNews.isEmpty) topNews = Some(Nil)
    }

    def isComplete() : Boolean = 
      css.isDefined && personalNews.isDefined && topNews.isDefined

    def result() : FrontPageResult = {
      val topSet = topNews.get.toSet
      val allNews = topNews.get ::: personalNews.get.filterNot(topSet.contains)
      FrontPageResult(user, css.get, allNews)
    }
  }
  
  class Themes extends Actor {
    println("Themes Instantiated")
    def receive = {
      case GetTheme(user, replyTo) =>{
        println("Theme received")
        replyTo ! ThemeResult("css")
      }
    }
  }
  class PersonalNews extends Actor {
    def receive = {
      case GetPersonalNews(user, replyTo) =>
        replyTo ! PersonalNewsResult(List(s"$user news 1", s"$user news 2"))
    }
  }
  class TopNews extends Actor {
    def receive = {
      case GetTopNews(replyTo) =>
        Thread.sleep(200)
        replyTo ! TopNewsResult(List("top news 1", " top news 2"))
    }
  }
  
  class FrontPage(themes : ActorRef, personalNews : ActorRef, topNews : ActorRef)
  extends Actor {
    println("frontapage init")
    def receive : Receive = {
      case GetFrontPage(user, replyTo) =>
        println("FrontPage received GetFrontPage")
        val aggregator = context.actorOf(Props(new FrontPageNewsAggregator(user, replyTo)))
        themes ! GetTheme(user, aggregator)
        personalNews ! GetPersonalNews(user, aggregator)
        topNews ! GetTopNews(aggregator)
    }
  }

  class FrontPageNewsAggregator(user : String, replyTo : ActorRef) 
  extends Actor with ActorLogging {
    println("Aggregator Init")
    val builder = new FrontPageResultBuilder(user)
    context.setReceiveTimeout(1 seconds)
    def receiveResult : Receive = {
      case ThemeResult(css) =>
        println("Aggregator receive ThemeResult")
        builder.addCSS(css)
      case PersonalNewsResult(news) =>
        println("Aggregator receive PersonalNewsResult")
        builder.addPersonalNews(news)
      case TopNewsResult(news) =>
        println("Aggregator receive TopNewsResult")
        builder.addTopNews(news)
      case ReceiveTimeout =>
        println("Builder is using defaults for timed out result.")
        builder.timeout()
    }
    def checkComplete : Receive = {
      case msg => 
        log.debug("Aggregator received result: {}", msg) 
        if(builder.isComplete()) {
          log.debug("Aggregated result is now complete") 
          replyTo ! builder.result
          context.stop(self)
      }
    }
    
    def receive : Receive = receiveResult andThen checkComplete
  }
  
  class Runner(frontPage : ActorRef) extends Actor with ActorLogging {
    println("runner")
    frontPage ! GetFrontPage("cderoove", self)
    def receive : Receive = {
      case fpr : FrontPageResult =>
        println(("Displaying front page for user {}: {}"), fpr.user, fpr)
        context.system.terminate()
    }
  }
  
  val sys = ActorSystem("AggregatorPattern")
  val themes =  sys.actorOf(Props[Themes], "themes")
  val personalNews =  sys.actorOf(Props[PersonalNews], "personalNews")
  val topNews =  sys.actorOf(Props[TopNews], "topNews")  
  val frontpage = sys.actorOf(Props(new FrontPage(themes, personalNews, topNews)), "frontpage")
  val runner = sys.actorOf(Props(new Runner(frontpage)), "runner")
}