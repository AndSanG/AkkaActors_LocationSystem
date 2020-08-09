package messaging;
/**
 * Adapted from Roland Kuhn's "Reactive Design Patterns" book.
 */
import java.util.UUID
import akka.actor.ActorRef
import akka.actor.Actor
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import akka.pattern.AskTimeoutException
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorLogging
import akka.actor.ReceiveTimeout
import akka.actor.TypedActor.PostStop
import scala.language.postfixOps

object AskPattern extends App {
  //request to business process
  sealed trait MyCommands
  case class StartVerification(replyTo: ActorRef) extends MyCommands
  
  //response from business process
  sealed trait VerificationResponse
  case object VerificationCompleted extends VerificationResponse
  case object VerificationFailed extends VerificationResponse
  
  //request to EmailGateway
  case class SendEmail(correlationID: UUID, replyTo: ActorRef)
  //response from EmailGateway
  sealed trait Result
  case class SendEmailResult(correlationID: UUID, status: StatusCode,
                             explanation: String) extends Result

  sealed trait StatusCode
  object StatusCode {
    case object OK extends StatusCode
    case object Failed extends StatusCode
  }
  
  class EmailGateway extends Actor with ActorLogging { 
    def receive : Receive = {
      case SendEmail(correlationID: UUID, replyTo : ActorRef) => 
         log.info("sent verification e-mail; waiting for user to click on link")
        //change to 6000 to see difference in behavior
         Thread.sleep(3000)
         replyTo ! SendEmailResult(correlationID, StatusCode.OK, "")
    }
  }
  
 class Runner(asker : ActorRef) extends Actor with ActorLogging {
    asker ! StartVerification(self)
    def receive : Receive = { 
      case x : VerificationResponse => 
        log.info("shutting down after reception of {}", x)
        context.system.terminate()
    }
  }

  
  class AskPatternWithFuture(emailGateway : ActorRef) extends Actor with ActorLogging {
    //exposes the actor's thread pool to the future's recover method
    implicit val executionContext = context.dispatcher

    def receive : Receive = {
      case StartVerification(replyTo) =>
        val corrID = UUID.randomUUID()
        val request = SendEmail(corrID, self)
        implicit val timeout = Timeout(5 seconds)
        val responseFuture : Future[Any] = emailGateway ? request
        responseFuture.map {
          case SendEmailResult(`corrID`, StatusCode.OK, _) =>
              log.debug("successfully started the verification for {}", corrID)
              VerificationCompleted
          case SendEmailResult(`corrID`, StatusCode.Failed, explanation) =>
              log.info("failed to start the verification for {}: {}", corrID, explanation)
              VerificationFailed
          case SendEmailResult(wrongID, _, _) =>
              log.error("received wrong SendEmailResult for {}", corrID)
              VerificationFailed
        }.recover {
          case _: AskTimeoutException =>
              log.warning("verification initiation timed out for {}", corrID)
              VerificationFailed
        }.foreach(result => replyTo ! result)
    }
  }
  
  class ResponseHandlerChild(corrID : UUID, replyTo : ActorRef) 
  extends Actor with ActorLogging {
    context.setReceiveTimeout(5 seconds)
    override def postStop = {
      log.info("shutting down response handler child for {}", corrID)
    }
    def receive: Receive = {
      case ReceiveTimeout => 
        log.warning("verification initiation timed out for {}", corrID)
        replyTo ! VerificationFailed
        context.stop(self)
      case SendEmailResult(`corrID`, StatusCode.OK, _) => 
        log.debug("successfully started the verification for {}", corrID)
        replyTo ! VerificationCompleted
        context.stop(self)
      case SendEmailResult(`corrID`, StatusCode.Failed, explanation) => 
        log.info("failed to start the verification for {}: {}", corrID, explanation)
        replyTo ! VerificationFailed
        context.stop(self)
      case SendEmailResult(wrongID, _, _) => 
        log.error("received wrong SendEmailResult for corrID {}", corrID)
        replyTo ! VerificationFailed
        context.stop(self)
    }
  }
  
  class AskPatternWithChild(emailGateway : ActorRef) extends Actor with ActorLogging {
    def receive : Receive = {
      case StartVerification(replyTo) =>
        val corrID = UUID.randomUUID()
        val childActor = context.actorOf(Props(new ResponseHandlerChild(corrID, replyTo)))
        val request = SendEmail(corrID, childActor)
        emailGateway ! request
    }
  } 
  
  

  class WithoutAskPattern(emailGateway : ActorRef) extends Actor with ActorLogging {
    //exposes the actor's thread pool to the scheduler's scheduleOnce method
    implicit val executionContext = context.dispatcher

    var statusMap = scala.collection.mutable.Map.empty[UUID, ActorRef]
    def receive : Receive = {
      case StartVerification(replyTo) =>
        val corrID = UUID.randomUUID()
        val request = SendEmail(corrID, self)
        emailGateway ! request
        statusMap += corrID -> replyTo
        context.system.scheduler.scheduleOnce(5 seconds, self, SendEmailResult(corrID, StatusCode.Failed, "timeout"))
      case SendEmailResult(corrID, status, expl) =>
        statusMap.get(corrID) match {
          case None =>
            log.error("received SendEmailResult for unknown correlation ID {}", corrID)
          case Some(replyTo) =>
            status match {
              case StatusCode.OK =>
                log.debug("successfully started the verification for {}", corrID)
                replyTo ! VerificationCompleted
              case StatusCode.Failed =>
                log.info("failed to start the verification for {} ({})", corrID, expl)
                replyTo ! VerificationFailed
            }
            statusMap -= corrID
            }
    }
  }
    
  val sys = ActorSystem("AskPattern")
  val gateway = sys.actorOf(Props[EmailGateway], "gateway")
  //can be inter-changed freely
  //val asker = sys.actorOf(Props(new WithoutAskPattern(gateway)), "asker")
  //val asker = sys.actorOf(Props(new AskPatternWithChild(gateway)), "asker")
  val asker = sys.actorOf(Props(new AskPatternWithFuture(gateway)), "asker")

  val runner = sys.actorOf(Props(new Runner(asker)), "runner")
  
    
}