package eu.eyan.yate

import java.io.File
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import scala.annotation.tailrec
import eu.eyan.util.snmp.SnmpClient

object YateTools extends App {

  def logs = """src\test\resources\yate.log""".splitLinesFromFile(isLogStart)



  val timeR = """\d{14}.\d{6}.*"""

  def isLogStart(line: String) = {
    line.containsAny(List("Initializing", "Supervisor", "is starting", "shutting down", "is stopping", "Loaded module", "Unloading module", "Unloaded module", "Unload module", "Initialization complete")) ||
      line.matchesAny(timeR)
  }

  logs.foreach(log => println("--->" + log /*.lines.toList.lift(0).getOrElse("")*/ + "<---"))
  //Supervisor (64285) is starting
  //Yate (64286) is starting Thu Jul 12 06:49:41 2018
  //20180712044943.940442 <sip:INFO> 'udp:0.0.0.0:5060' received 490 bytes SIP message from 91.82.50.5:5060 [0x556fbc17e590]
  //Yate engine is shutting down
  //Yate (64286) is stopping
}