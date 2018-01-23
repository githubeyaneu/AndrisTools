package eu.eyan.bittrex.v20

import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.native.JsonMethods._

trait AbstractBittrexApi {
  implicit val formats = DefaultFormats
  def getApiUrl: String
  val getApiContents = getApiUrl.asUrlGet
  def getJson = parse(getApiContents)
}