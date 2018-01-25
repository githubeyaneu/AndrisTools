package eu.eyan.coins

import eu.eyan.bittrex.v20.GetMarketSummaries
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import eu.eyan.util.compress.ZipPlus
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import scala.io.Source
import eu.eyan.util.time.TimeCounter._
import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import eu.eyan.util.scala.Try

object AllToDb extends App {
  val tarsPath = """C:\DEVELOPING_1\projects\coindatachris\json"""
  val tarPath = """C:\DEVELOPING_1\projects\coindatachris\json\2017_12_18.tar"""
  val jsonsPath = """C:\DEVELOPING_1\projects\coindatachris\2017_12_18"""
  val jsonPath = """C:\DEVELOPING_1\projects\coindatachris\2017_12_18\summaries_minute_2017_12_18-01_30_55.json"""
  val jsonGzPath = """C:\DEVELOPING_1\projects\coindatachris\2017_12_18\summaries_minute_2017_12_18-01_30_55.json.gz"""

  val bittrexReader = new BittrexReader()
  tarsPath.asDir.subFiles.foreach(importTarsToDb)
  def importTarsToDb(tar: File) = {
    val gzips = ZipPlus.unTarAllFilesToMemory(tar)
    val inserts = gzips.map(t => importJsonGzArrayToDb(t._1,t._2))
    //inserts.foreach(t => countAndPrint(t._1) { Await.result(t._2, 10 hours) })
//    val futures = inserts.unzip._2
    Future.sequence(inserts)
  }
  def importJsonGzArrayToDb(name:String, gz: Array[Byte]) ={println(name); Try( bittrexReader.toDB(GetMarketSummaries(ZipPlus.gzArrayToString(gz)))).get }
  bittrexReader.close

  //  
  //  

  //  def importJsonGzToDb(file: File) = countAndPrint(file.toString) { BittrexReader.toDB(GetMarketSummaries(ZipPlus.gzToString(file))) }
  //  jsonsPath.asFile.subFiles.filter(_.getName.endsWith("gz")).foreach(importJsonGzToDb)
}

