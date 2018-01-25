package eu.eyan.coins

import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import org.json4s._
import org.json4s.native.JsonMethods._

import eu.eyan.bittrex.v20.GetMarketSummaries
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{ Success, Failure }
import eu.eyan.log.Log
import eu.eyan.bittrex.v20.MarketSummaries
import scala.concurrent.Await
import java.time.format.DateTimeFormatter

object Check extends App {
  
//  val summ = GetMarketSummaries("""C:\private\2017_12_18\summaries_minute_2017_12_18-01_30_55.json""".asFile.linesList.mkString)
//  println(summ.result.size)
  val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS")
  
  
  val date = "2017-12-18T00:30:47.18".asDateTime(inputFormat)
  println("2017-12-18T00:30:47.18".asDateTime(inputFormat))
  
}