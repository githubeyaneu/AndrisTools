package eu.eyan.coins

import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import org.json4s._
import org.json4s.native.JsonMethods._

object BittrexReader extends App {
  val j = """C:\DEVELOPING_1\projects\AndrisTools\src\main\resources\GetMarketSummaries""".linesFromFile.mkString("\r\n")
  val json = parse(j)

  // https://json2caseclass.cleverapps.io/
  case class Market(
    MarketCurrency: String,
    BaseCurrency: String,
    MarketCurrencyLong: String,
    BaseCurrencyLong: String,
    MinTradeSize: Double,
    MarketName: String,
    IsActive: Boolean,
    Created: String,
    Notice: String,
    IsSponsored: Option[String],
    LogoUrl: String)
  case class Summary(
    MarketName: String,
    High: Double,
    Low: Double,
    Volume: Double,
    Last: Double,
    BaseVolume: Double,
    TimeStamp: String,
    Bid: Double,
    Ask: Double,
    OpenBuyOrders: Double,
    OpenSellOrders: Double,
    PrevDay: Double,
    Created: String)
  case class MarketSummary(
    Market: Market,
    Summary: Summary,
    IsVerified: Boolean)
  case class MarketSummaries(
    success: Boolean,
    message: String,
    result: List[MarketSummary])

  implicit val formats = DefaultFormats

  val markets = json.extract[MarketSummaries]
  
  println(markets)
  

}