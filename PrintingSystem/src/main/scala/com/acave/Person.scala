package com.acave
import akka.actor._
import scala.util.Random

object Person {
 
    case class Document(numOfPages:Int=1)
    case class Documents(docs:Document*)
    case class RemainingDocuments(docs: List[Document])
    case class JobStatus(inProgress:List[Document], remainingJobs:List[Document])
    case object PrintedDoc
    case object GetJobStatus
    def props(ref: ActorRef) = Props(new Person(ref))
}

//TODO:  Make "In-Progress" and "remaining" into Enums
class Person(printer: ActorRef) extends Actor {
    import Person._

    var jobsMaps = Map[String, List[Document]]().empty

    def receive:Receive = {
        case Documents(docs @ _*) => jobsMaps = jobsMaps + ("remaining" -> docs.toList)
        case "Print" => val docs = jobsMaps.getOrElse("remaining", Nil)
                        if (!docs.isEmpty){
                            val doc = docs.head
                            jobsMaps = jobsMaps.updated("In-Progress", jobsMaps.getOrElse("In-Progress", Nil) :+ doc)
                            jobsMaps = jobsMaps.updated("remaining", docs.tail)
                            printer ! doc
                        }
        case GetJobStatus => sender ! JobStatus(jobsMaps.getOrElse("In-Progress", Nil), jobsMaps.getOrElse("remaining", Nil))
        case PrintedDoc => jobsMaps = jobsMaps.updated("In-Progress", jobsMaps("In-Progress").tail)
        case RemainingDocuments(list) => println("list of documents that were unable to be printed: "+list)
                                         jobsMaps = jobsMaps.mapValues((vals) => Nil)
        case _ => println(" Person retrieved a message they don't know how to process!")
    }
    
}
