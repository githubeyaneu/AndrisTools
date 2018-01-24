package eu.eyan.bittrex.v20

import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.native.JsonMethods._
import eu.eyan.log.Log

trait AbstractBittrexApi {
  implicit val formats = DefaultFormats
  def getApiUrl: String

  def getJson = {
    Log.info("get Json: " + getApiUrl)
    val contents = getApiUrl.asUrlGet
    Log.info("parse Json text length=" + contents.length)
    parse(contents)
  }
}