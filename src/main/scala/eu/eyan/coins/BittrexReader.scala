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

import scala.util.{ Success, Failure }
import eu.eyan.log.Log
import eu.eyan.bittrex.v20.MarketSummaries

class BittrexReader  {

  // https://github.com/thebotguys/golang-bittrex-api/wiki/Bittrex-API-Reference-(Unofficial)
  // https://json2caseclass.cleverapps.io/

  //GetMarkets https://bittrex.com/api/v2.0/pub/markets/GetMarkets

  // https://bittrex.com/api/v2.0/pub/markets/GetMarketSummaries

  Log.activateInfoLevel
 
  val url = """c:\private\bittrex\mrl""".asFile.linesList.head
  val db = Database.forURL(url, driver = "org.mariadb.jdbc.Driver")
  
  def toDB(summaries: MarketSummaries) = {

    //  val dbInsert = GetMarketSummaries.marketSummariesToDbInsert(summaries)

//    val create = GetMarketSummaries.tableQuery
//    val setup = DBIO.seq((create.schema).create)
    //  val setupFuture = db.run(setup)
    //  Await.result(setupFuture, 10 seconds)

//    GetMarketSummaries.marketSummariesToDbInsert(summaries).foreach{summary => Await.result(db.run(DBIO.seq(GetMarketSummaries.tableQuery +=summary )), 10 seconds)}
    
        db.run(
            DBIO.seq(
                GetMarketSummaries.tableQuery ++= GetMarketSummaries.marketSummariesToDbInsert(summaries)))

  }
  def close = db.close

  val summariesOnline = GetMarketSummaries()
}