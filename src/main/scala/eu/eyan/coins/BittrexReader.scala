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
import eu.eyan.log.Log

object BittrexReader extends App {

  // https://github.com/thebotguys/golang-bittrex-api/wiki/Bittrex-API-Reference-(Unofficial)
  // https://json2caseclass.cleverapps.io/

  //GetMarkets https://bittrex.com/api/v2.0/pub/markets/GetMarkets

  // https://bittrex.com/api/v2.0/pub/markets/GetMarketSummaries

  Log.activateInfoLevel
  val summaries = GetMarketSummaries()

  val url = """c:\private\bittrex\mrl""".asFile.linesList.head
  val db = Database.forURL(url, driver = "org.mariadb.jdbc.Driver")

  //  val dbInsert = GetMarketSummaries.marketSummariesToDbInsert(summaries)

  val create = GetMarketSummaries.tableQuery
  val setup = DBIO.seq((create.schema).create)
  val setupFuture = db.run(setup)
  Await.result(setupFuture, 10 seconds)
  println("created")

  val insertQuery = GetMarketSummaries.tableQuery
  val insertAction = DBIO.seq(insertQuery ++= GetMarketSummaries.marketSummariesToDbInsert(summaries))
  println("instering")
  val insertFuture = db.run(insertAction)
  println("run insert")

  insertFuture.onComplete {
    case Success(_) => s"-- Insert successful --".println
    case Failure(t) => s"-- ERROR: ${t.getMessage}".println; t.printStackTrace
  }
  Await.result(insertFuture, 10 seconds)
  println("inserted")

  db.close
}