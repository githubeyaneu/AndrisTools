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
import java.sql.DriverManager
import eu.eyan.util.scala.TryCatchThrowable

class BittrexReader {

  // https://github.com/thebotguys/golang-bittrex-api/wiki/Bittrex-API-Reference-(Unofficial)
  // https://json2caseclass.cleverapps.io/

  //GetMarkets https://bittrex.com/api/v2.0/pub/markets/GetMarkets

  // https://bittrex.com/api/v2.0/pub/markets/GetMarketSummaries

  Log.activateInfoLevel

  val url = """c:\private\bittrex\mrl""".asFile.linesList.head
  val db = Database.forURL(url, driver = "com.mysql.cj.jdbc.Driver")
  //  val db = Database.forURL(url, driver = "org.mariadb.jdbc.Driver")

  // CREATE SCHEMA
  //  val setup = DBIO.seq((GetMarketSummaries.tableQuery.schema).create)
  //    Await.result(db.run(setup), 10 seconds)

  // to DB SLICK
  def toDBS(summaries: MarketSummaries) = db.run(DBIO.seq(GetMarketSummaries.tableQuery ++= GetMarketSummaries.marketSummariesToDbInsertSlick(summaries)))
  //  def toDB(summaries: MarketSummaries) = {
  //    val chunkFutures = for (chunk <- GetMarketSummaries.marketSummariesToDbInsert(summaries).sliding(100, 100))
  //      yield db.run(DBIO.seq(GetMarketSummaries.tableQuery ++= chunk))
  //    Future.sequence(chunkFutures)
  //  }

  //to db batch insert
  Class.forName("com.mysql.cj.jdbc.Driver")
  val con = DriverManager.getConnection(url)
  con.setAutoCommit(false)

  def toDB(summaries: MarketSummaries) = Future(
    TryCatchThrowable({
      val st = con.createStatement
      //      GetMarketSummaries.marketSummariesToDbInsert(summaries).foreach(st.addBatch)

      st.executeUpdate("INSERT INTO MARKETSUMMARY VALUES " + GetMarketSummaries.marketSummariesToDbInsertValues(summaries).mkString(","))

      //      val sql = "select * from person"
      //      val rs = st.executeQuery(sql)
      //  System.out.println("No  \tName")
      //  while (rs.next()) {
      //  System.out.print(rs.getString(1) + " \t")
      //  System.out.println(rs.getString(2))
      //  }
      //      rs.close

      st.close
    }, t => Log.error(t)))

  def close = { /*db.close*/ con.close }
}