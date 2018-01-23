package eu.eyan.bittrex.v20

//val j = """src\main\resources\GetMarketSummaries""".linesFromFile.mkString("\r\n")
//  val j = """https://bittrex.com/api/v1.1/public/getmarketsummaries""".asUrlGet

object GetMarketSummaries extends AbstractBittrexApi {
  def getApiUrl = """https://bittrex.com/api/v2.0/pub/markets/GetMarketSummaries"""
  def get = getJson.extract[MarketSummaries]
}

case class Market(MarketCurrency: String, BaseCurrency: String, MarketCurrencyLong: String, BaseCurrencyLong: String, MinTradeSize: Double, MarketName: String, IsActive: Boolean, Created: String, Notice: String, IsSponsored: Option[String], LogoUrl: String)
case class Summary(MarketName: String, High: Double, Low: Double, Volume: Double, Last: Double, BaseVolume: Double, TimeStamp: String, Bid: Double, Ask: Double, OpenBuyOrders: Double, OpenSellOrders: Double, PrevDay: Double, Created: String)
case class MarketSummary(Market: Market, Summary: Summary, IsVerified: Boolean)
case class MarketSummaries(success: Boolean, message: String, result: List[MarketSummary])