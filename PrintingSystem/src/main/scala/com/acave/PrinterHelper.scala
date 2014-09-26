package com.acave

import akka.actor.{Props, Actor}
import scala.util.Random
import java.util.concurrent.atomic.AtomicInteger

object PrinterHelper {
  case class Page(pageNumber:Int=1, totalPages:Int=1)
  case class PrintedPage(pageNumber:Int=1)
  class PaperJamException extends Exception("Paper jam failure")
  class UndergoMaintenanceException extends Exception("Undergoing maintenance ... aborting all jobs")

  def props: Props = Props(classOf[PrinterHelper])
}

class PrinterHelper(flaky: Boolean) extends Actor {
  import PrinterHelper._

  def receive = {
    case Page(page, total) =>
      if (Random.nextBoolean()) sender ! PrintedPage(page)
      else throw new PaperJamException
  }

}
