package eu.eyan.bittrex.v20

object GetBTCPrice extends AbstractBittrexApi {
	def getApiUrl = """https://bittrex.com/api/v2.0/pub/currencies/GetBTCPrice"""
  def get = getJson.extract[MarketSummaries]
}