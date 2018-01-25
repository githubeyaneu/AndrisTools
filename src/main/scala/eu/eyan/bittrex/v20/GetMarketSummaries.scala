package eu.eyan.bittrex.v20

import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

//val j = """src\main\resources\GetMarketSummaries""".linesFromFile.mkString("\r\n")
//  val j = """https://bittrex.com/api/v1.1/public/getmarketsummaries""".asUrlGet

object GetMarketSummaries extends AbstractBittrexApi {
  def getApiUrl = """https://bittrex.com/api/v2.0/pub/markets/GetMarketSummaries"""
  def apply(input: String) = stringToJson(input).extract[MarketSummaries]
  def apply() = getJson.extract[MarketSummaries]

  def tableQuery = TableQuery[MarketSummaryDb]

  def marketSummariesToDbInsert(input: MarketSummaries) = input.result.map { marketSummaryToDb(_) }

  private def marketSummaryToDb(input: MarketSummary) = {
    val market = input.Market
    val summary = input.Summary
    (market.MarketCurrency, market.BaseCurrency, market.MinTradeSize, market.IsActive, market.MarketName, summary.Last, summary.TimeStamp, summary.Bid, summary.Ask)
  }
}

case class Market(MarketCurrency: String, BaseCurrency: String, MarketCurrencyLong: String, BaseCurrencyLong: String, MinTradeSize: Double, MarketName: String, IsActive: Boolean, Created: String, Notice: String, IsSponsored: Option[String], LogoUrl: String)
case class Summary(MarketName: String, High: Double, Low: Double, Volume: Double, Last: Double, BaseVolume: Double, TimeStamp: String, Bid: Double, Ask: Double, OpenBuyOrders: Double, OpenSellOrders: Double, PrevDay: Double, Created: String)
case class MarketSummary(Market: Market, Summary: Summary, IsVerified: Boolean)
case class MarketSummaries(success: Boolean, message: String, result: List[MarketSummary])

class MarketSummaryDb(tag: Tag) extends Table[(String, String, Double, Boolean, String, Double, String, Double, Double)](tag, "MARKETSUMMARY") {
  def MarketCurrency = column[String]("MarketCurrency", O.Length(10))
  def BaseCurrency = column[String]("BaseCurrency", O.Length(10))
  def MinTradeSize = column[Double]("MinTradeSize")
  def IsActive = column[Boolean]("IsActive")
  def MarketName = column[String]("MarketName", O.Length(10))
  def Last = column[Double]("Last")
  def TimeStamp = column[String]("TimeStamp", O.Length(30))
  def Bid = column[Double]("Bid")
  def Ask = column[Double]("Ask")

  def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
  def name = column[String]("NAME")
  def * = (MarketCurrency, BaseCurrency, MinTradeSize, IsActive, MarketName, Last, TimeStamp, Bid, Ask)

}

