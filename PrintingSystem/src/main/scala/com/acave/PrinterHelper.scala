package com.acave

import akka.actor.{Props, Actor}
import scala.util.Random
import java.util.concurrent.atomic.AtomicInteger

object PrinterHelper {
  case class Page(pageNumber:Int=1, totalPages:Int=1)
  case class PrintedPage(pageNumber:Int=1)
  class PaperJamException extends Exception("Paper jam failure")
  class UndergoMaintenanceException extends Exception("Undergoing maintenance ... aborting all jobs")

  def props: Props = Props(classOf[PrinterHelper], true)
}

class PrinterHelper(flaky: Boolean) extends Actor {
  import PrinterHelper._

  val parent = context.parent

  override def preRestart(reason:Throwable, message: Option[Any]):Unit = {
      super.preRestart(reason, message)
      if (reason.isInstanceOf[PaperJamException]){
        println("PrintHelper going to retry message: "+message)
        self ! message.get
      }
  }


  def receive = {
    case Page(page, total) =>
      if (Random.nextBoolean()) parent ! PrintedPage(page)
      else throw new PaperJamException
  }

}
