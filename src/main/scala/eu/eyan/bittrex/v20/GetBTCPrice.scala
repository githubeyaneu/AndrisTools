package eu.eyan.bittrex.v20

object GetBTCPrice extends AbstractBittrexApi {
  def getApiUrl = """https://bittrex.com/api/v2.0/pub/currencies/GetBTCPrice"""
  def get = getJson.extract[MarketSummaries]
}

case class Time(updated: String, updatedISO: String, updateduk: String)
case class USD(code: String, rate: String, description: String, rate_float: Double)
case class Bpi(USD: USD)
case class BTCPrice(time: Time, disclaimer: String, bpi: Bpi)
case class BTCPrices(success: Boolean, message: String, result: BTCPrice)
