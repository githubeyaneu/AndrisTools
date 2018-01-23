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
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{ Success, Failure }


object BittrexReader extends App {

  // https://github.com/thebotguys/golang-bittrex-api/wiki/Bittrex-API-Reference-(Unofficial)
  // https://json2caseclass.cleverapps.io/
  
	//GetMarkets https://bittrex.com/api/v2.0/pub/markets/GetMarkets
  
  // https://bittrex.com/api/v2.0/pub/markets/GetMarketSummaries
  
  val summaries = GetMarketSummaries()
  println(summaries)
  
  
  val url = """c:\private\bittrex\mrl""".asFile.linesList.head
  val db = Database.forURL(url, driver = "org.mariadb.jdbc.Driver")

  val dbInsert = GetMarketSummaries.marketSummariesToDbInsert(summaries)

//  val setup = DBIO.seq( (dbInsert.schema).create )
//  val setupFuture = db.run(setup)

//    val setup = DBIO.seq( dbInsert.insertStatement )
//  val setupFuture = db.run(setup)

//  setupFuture.onComplete {
//    case Success(trace) => s"-- Finished successfully in $trace --".println
//    case Failure(t)     => s"-- ERROR: ${t.getMessage}".println; t.printStackTrace
//  }
//  Await.result(setupFuture, 10 seconds)

//  println("People:")
//  db.run(people.result).map(_.foreach { case (supID, name) => println("  " + name + "\t" + supID) })
//  db.run(people.result).map(_.foreach { case (supID, name) => println("  " + name + "\t" + supID) })
//  Await.result(db.run(DBIO.seq((people.schema).drop)), 10 seconds)

  db.close


}