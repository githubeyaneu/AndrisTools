package eu.eyan.yate

import eu.eyan.util.snmp.SnmpClient
import eu.eyan.util.scala.TryCatchFinallyClose
import eu.eyan.log.Log
import scala.util.Try
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.scala.collection.immutable.ListPlus.ListImplicit
import scala.util.Success
import scala.util.Failure
import eu.eyan.log.LogWindow

object YateSnmpClient extends App {
  
  Log.activateInfoLevel
//  Log.redirectSystemOutAndErrToLogWindow
//  LogWindow.show(null)
  
  val OID_VERSION = ".1.3.6.1.4.1.34501.1.2.0"
  
  val oids = Map (
		  "nodeName" -> ".1.3.6.1.4.1.34501.1.3.2.1.0"
		  ,"nodeState" -> ".1.3.6.1.4.1.34501.1.3.2.2.0"
		  ,"failedAuths" -> ".1.3.6.1.4.1.34501.1.8.4.2.0"
		  ,"x" -> ".1.3.6.1.4.1.34501.1.8.4.2.0.2345"
		  
		  
//		  ,"upTime" -> ".1.3.6.1.4.1.34501.1.3.2.4.0"
//    ,"version" -> ".1.3.6.1.4.1.34501.1.2.0"
//    ,"threads" -> ".1.3.6.1.4.1.34501.1.3.1.4.0"
//    ,"semaphores" -> ".1.3.6.1.4.1.34501.1.3.1.7.1.0"
//    ,"accept" -> ".1.3.6.1.4.1.34501.1.3.1.8.0"
//    ,"systemLoad" -> ".1.3.6.1.4.1.34501.1.3.1.9.4.0"
//    ,"activeCallsCount" -> ".1.3.6.1.4.1.34501.1.5.1.0"
//    ,"incomingCounter" -> ".1.3.6.1.4.1.34501.1.7.1.1.1.0"
//    ,"outgoingCounter" -> ".1.3.6.1.4.1.34501.1.7.1.1.2.0"
//    ,"authenticationRequests" -> ".1.3.6.1.4.1.34501.1.7.4.1.0"
//    ,"registerRequests" -> ".1.3.6.1.4.1.34501.1.7.4.2.0"
//    ,"transactionsTimedOut" -> ".1.3.6.1.4.1.34501.1.8.4.1.0"
//    ,"byesTimedOut" -> ".1.3.6.1.4.1.34501.1.8.4.3.0"
//    ,"unexpectedRestart" -> ".1.3.6.1.4.1.34501.1.8.6.1.0"
  )
  
  def processOids(queryOidValues: List[String] => List[Try[String]]) ={
    //TODO make it asynchron...
    val lists = oids.toList.unzip
    
    val names = lists._1
    val values = queryOidValues(lists._2) map errorMapper 
    
    names.zip(values).printlnNL
  }
  
  def errorMapper(t: Try[String]) = {
    t match {
      case Success(s) =>s 
      case Failure(t) =>t.getMessage.toString 
    }
  }
  
  TryCatchFinallyClose(
      {SnmpClient("voip.eyan.eu",16001, "testsnmp")},
      {snmpClient:SnmpClient => processOids(oids => snmpClient.getOids(oids))}
      ,e=>Log.error(e))
  
}