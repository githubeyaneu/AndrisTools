package eu.eyan.coins

import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import org.json4s._
import org.json4s.native.JsonMethods._

import eu.eyan.bittrex.v20.GetMarketSummaries

object BittrexReader extends App {

  // https://github.com/thebotguys/golang-bittrex-api/wiki/Bittrex-API-Reference-(Unofficial)
  // https://json2caseclass.cleverapps.io/
  
  // https://bittrex.com/api/v2.0/pub/markets/GetMarketSummaries



  
  val markets = GetMarketSummaries.get
  
  println(markets)
}