package eu.eyan.bittrex.v20

import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import eu.eyan.util.time.DateTimePlus.InstantPlusImplicit
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.Date
import eu.eyan.util.scala.TryCatch

//val j = """src\main\resources\GetMarketSummaries""".linesFromFile.mkString("\r\n")
//  val j = """https://bittrex.com/api/v1.1/public/getmarketsummaries""".asUrlGet

object GetMarketSummaries extends AbstractBittrexApi {
  def getApiUrl = """https://bittrex.com/api/v2.0/pub/markets/GetMarketSummaries"""
  def apply(input: String) = stringToJson(input).extract[MarketSummaries]
  def apply() = getJson.extract[MarketSummaries]

  def tableQuery = TableQuery[MarketSummaryDb]

  def marketSummariesToDbInsert(input: MarketSummaries) = input.result.map { marketSummaryToDb(_) }

  val inputFormat1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.S")
  val inputFormat2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS")
  val inputFormat3 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
  val inputFormat4 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

  private def marketSummaryToDb(input: MarketSummary) = {
    val market = input.Market
    val summary = input.Summary
    // 2017-12-18T00:31:21.637
    // dateTimeString.asDateTime(inputFormat).inZone(DE).format(outputFormat)
    // "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd '->' HH:mm:ss '<-'"
    //val dt = "2017-12-18T00:31:21.637".asDateTime(format).inZone(ZoneId.of("US")).format(formatter)
    val instant =
      TryCatch(
        summary.TimeStamp.asDateTime(inputFormat3),
        TryCatch(
          summary.TimeStamp.asDateTime(inputFormat2),
          TryCatch(
            summary.TimeStamp.asDateTime(inputFormat1),
            summary.TimeStamp.asDateTime(inputFormat4))))
            
    val ts = java.sql.Timestamp.from(instant)
    (market.MarketCurrency, market.BaseCurrency, market.MinTradeSize, market.IsActive, market.MarketName, summary.Last, ts, summary.Bid, summary.Ask)
  }
}

case class Market(MarketCurrency: String, BaseCurrency: String, MarketCurrencyLong: String, BaseCurrencyLong: String, MinTradeSize: Double, MarketName: String, IsActive: Boolean, Created: String, Notice: String, IsSponsored: Option[String], LogoUrl: String)
case class Summary(MarketName: String, High: Double, Low: Double, Volume: Double, Last: Double, BaseVolume: Double, TimeStamp: String, Bid: Double, Ask: Double, OpenBuyOrders: Double, OpenSellOrders: Double, PrevDay: Double, Created: String)
case class MarketSummary(Market: Market, Summary: Summary, IsVerified: Boolean)
case class MarketSummaries(success: Boolean, message: String, result: List[MarketSummary])

class MarketSummaryDb(tag: Tag) extends Table[(String, String, Double, Boolean, String, Double, java.sql.Timestamp, Double, Double)](tag, "MARKETSUMMARY") {
  def MarketCurrency = column[String]("MarketCurrency", O.Length(5)) // max 5 chars...
  def BaseCurrency = column[String]("BaseCurrency", O.Length(4)) // max 4
  def MinTradeSize = column[Double]("MinTradeSize")
  def IsActive = column[Boolean]("IsActive")
  def MarketName = column[String]("MarketName", O.Length(9)) // max 9
  def Last = column[Double]("Last")
  def TimeStamp = column[java.sql.Timestamp]("TimeStamp")
  def Bid = column[Double]("Bid")
  def Ask = column[Double]("Ask")

  def idx_marketCurrency = index("idx_m", (MarketCurrency), unique = false)
  def idx_baseCurrency = index("idx_b", (BaseCurrency), unique = false)
  def idx_timestamp = index("idx_t", (TimeStamp), unique = false)

  def * = (MarketCurrency, BaseCurrency, MinTradeSize, IsActive, MarketName, Last, TimeStamp, Bid, Ask)
}

//datetime:String
//class MarketSummaryDb(tag: Tag) extends Table[(String, String, Double, Boolean, String, Double, String, Double, Double)](tag, "MARKETSUMMARY") {
//	def MarketCurrency = column[String]("MarketCurrency", O.Length(10)) // max 5 chars...
//			def BaseCurrency = column[String]("BaseCurrency", O.Length(10)) // max 4 
//			def MinTradeSize = column[Double]("MinTradeSize")
//			def IsActive = column[Boolean]("IsActive")
//			def MarketName = column[String]("MarketName", O.Length(10)) // max 9
//			def Last = column[Double]("Last")
//			def TimeStamp = column[String]("TimeStamp", O.Length(30)) // -> TODO index!
////  def TimeStamp = column[Date]("TimeStamp")(DateMapper.utilDate2SqlDate) // ! TODO index
//			def Bid = column[Double]("Bid")
//			def Ask = column[Double]("Ask")
//			
//			def idx = index("idx_mbt", (MarketCurrency, BaseCurrency, TimeStamp), unique = false)
//			def * = (MarketCurrency, BaseCurrency, MinTradeSize, IsActive, MarketName, Last, TimeStamp, Bid, Ask)
//}

