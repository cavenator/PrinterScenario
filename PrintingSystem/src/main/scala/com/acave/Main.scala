package com.acave

import akka.actor._
import Person._
import Printer._
import PrinterHelper._
import scala.concurrent._
import scala.concurrent.duration._

object Main extends App {
    val system = ActorSystem("PrinterExercise")

    val printer = system.actorOf(Printer.props(PrinterHelper.props), "UniversalPrinter")

    val alice = system.actorOf(Person.props(printer), "Alice")

    val bill  = system.actorOf(Person.props(printer), "Bill")

    val carolyne = system.actorOf(Person.props(printer), "Carolyne")

    alice ! Documents(Document(1), Document(2))

    bill ! Documents(Document(9))

    carolyne ! Documents(Document(5), Document(15), Document(1))
    

    import system.dispatcher

    alice ! TimedMessage(1 second)

    system.scheduler.scheduleOnce(5 second, bill, "Print")

    carolyne ! TimedMessage(3 second)

}
