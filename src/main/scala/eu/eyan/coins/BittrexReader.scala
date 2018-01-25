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

class BittrexReader {

  // https://github.com/thebotguys/golang-bittrex-api/wiki/Bittrex-API-Reference-(Unofficial)
  // https://json2caseclass.cleverapps.io/

  //GetMarkets https://bittrex.com/api/v2.0/pub/markets/GetMarkets

  // https://bittrex.com/api/v2.0/pub/markets/GetMarketSummaries

  Log.activateInfoLevel

  val url = """c:\private\bittrex\mrl""".asFile.linesList.head
  //  val db = Database.forURL(url, driver = "org.mariadb.jdbc.Driver")
  val db = Database.forURL(url, driver = "com.mysql.cj.jdbc.Driver")

  //  val create = GetMarketSummaries.tableQuery
  //  val setup = DBIO.seq((create.schema).create)
  //  val setupFuture = db.run(setup)
  //  Await.result(setupFuture, 10 seconds)

    def toDB(summaries: MarketSummaries) = db.run(DBIO.seq(GetMarketSummaries.tableQuery ++= GetMarketSummaries.marketSummariesToDbInsert(summaries)))
//  def toDB(summaries: MarketSummaries) = {
//    val chunkFutures = for (chunk <- GetMarketSummaries.marketSummariesToDbInsert(summaries).sliding(100, 100))
//      yield db.run(DBIO.seq(GetMarketSummaries.tableQuery ++= chunk))
//    Future.sequence(chunkFutures)
//  }

  def close = db.close
}