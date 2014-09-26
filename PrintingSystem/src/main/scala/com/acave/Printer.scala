package com.acave
import akka.actor._
import akka.actor.Terminated
import akka.actor.SupervisorStrategy.Stop
import akka.actor.SupervisorStrategy.Restart
import scala.util.Random
import scala.concurrent.duration._
import Person._

object Printer {
 
    case class Page(pageNumber:Int=1, totalPages:Int=1)
    case class PrintedPage(pageNumber:Int=1)
    case object GetRemainingJobs
    case object PrintNextJob
    case class RemainingJobs(actorToDocumentTuple:List[(ActorRef, Int)])
    class UndergoMaintenanceException extends Exception("Undergoing maintenance")
    class PaperJamException extends Exception("paper jam")

    def props(props: Props) = Props(new Printer(props))
}

class Printer(props:Props) extends Actor {
    import Printer._

    val printerHelper = context.actorOf(props)
    var recipientList = List[ActorRef]()
    var docTupList = List[(ActorRef, Int)]()
    var totalPages = 0

    override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries=15, withinTimeRange=1 minute){
        case _:PaperJamException =>             Restart
        case _:UndergoMaintenanceException =>   groupAndDistributeRemainingJobsByRecipientsThenBecomeIdle 
                                                Stop
    }

    private def groupAndDistributeRemainingJobsByRecipientsThenBecomeIdle = {
        val remainingJobs = docTupList.groupBy((k) => k._1).mapValues( listOfRefToJob => listOfRefToJob.map( (refToJob) => Document(refToJob._2)))
        remainingJobs.foreach( (jobs) => jobs._1 ! RemainingDocuments(jobs._2))
        docTupList = Nil
        context.unbecome
    }

    private def nextPrintJob(pages:Int) = {
        totalPages = pages
        printerHelper ! Page(1, pages)
    }

    override def preStart = {
        context watch printerHelper
    }

    def printing:Receive = {
        case Document(y) => docTupList = docTupList :+ (sender, y)
        case PrintedPage(x) => if (totalPages == x){
                                  val (actorRef, doc) = docTupList.head
                                  docTupList = docTupList.tail
                                  actorRef ! PrintedDoc
                                  if (!docTupList.isEmpty){
                                     self ! PrintNextJob
                                  } else {
                                    //if docTupList is empty, become idle
                                    context.unbecome
                                  }
                               } else {
                                  printerHelper ! Page(x + 1, totalPages)
                               }
        case PrintNextJob => val (actorRef, pages) = docTupList.head
                             nextPrintJob(pages)
        case GetRemainingJobs => sender ! RemainingJobs(docTupList)
        case Terminated(child) => context unwatch child
                                  groupAndDistributeRemainingJobsByRecipientsThenBecomeIdle                           
                                  context.stop(self) //shut down
        case _ =>
    }

    def idle:Receive = {
        case Document(y) => context.become(printing)
                            docTupList = docTupList :+ (sender, y)
                            nextPrintJob(y)
        case GetRemainingJobs => sender ! RemainingJobs(Nil)
        case _ => println("Do not recognize request from idle state")
    }

    def receive = idle
    
}
