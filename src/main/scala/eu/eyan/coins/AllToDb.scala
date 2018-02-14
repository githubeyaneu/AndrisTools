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
import eu.eyan.log.Log
import eu.eyan.util.scala.TryCatch
import eu.eyan.util.time.TimeCounter
import eu.eyan.util.scala.TryCatchThrowable
import eu.eyan.util.compress.ZipPlus.NameAndContent

object AllToDb extends App {
  Log.activate

  val tarsPath = """C:\DEVELOPING_1\projects\coindatachris\json"""
  //  val tarPath = """C:\DEVELOPING_1\projects\coindatachris\json\2017_12_18.tar"""
  //  val jsonsPath = """C:\DEVELOPING_1\projects\coindatachris\2017_12_18"""
  //  val jsonPath = """C:\DEVELOPING_1\projects\coindatachris\2017_12_18\summaries_minute_2017_12_18-01_30_55.json"""
  //  val jsonGzPath = """C:\DEVELOPING_1\projects\coindatachris\2017_12_18\summaries_minute_2017_12_18-01_30_55.json.gz"""

  private val bittrexReader = new BittrexReader()
  TimeCounter.countAndPrint(s"All To DB") { tarsPath.asDir.subFiles.foreach(importTarToDb) }
  bittrexReader.close

  def importTarToDb(tar: File) = {
    Log.info(s"Processing $tar")
    TimeCounter.countAndPrint("processing tar:" + tar) {
      val gzips = ZipPlus.unTarAllFilesToMemory(tar)
      val gzipChunks = gzips.sliding(600, 600).zipWithIndex
      gzipChunks.foreach {
        case (gzips, index) =>
          TimeCounter.countAndPrint(s"chunk $index done") {
            Await.result(Future.sequence(gzips map importJsonGzArrayToDb), 1 hours)
          }
      }
    }
  }

  def importJsonGzArrayToDb(file: NameAndContent) = {
    val contentOption = ZipPlus.unzipToString(file.content)
//    val conentFuture = Future.fromTry(contentOption)
    
    val marketOption = contentOption.map(GetMarketSummaries.apply)
    
    
    val r = contentOption.map(content => TryCatchThrowable(
      bittrexReader.toDB(GetMarketSummaries(content)),
      t => Future(Log.error("parsing: " + file.filename))))
    
		val content = contentOption.get
    TryCatchThrowable(
      bittrexReader.toDB(GetMarketSummaries(content)),
      t => Future(Log.error("parsing: " + file.filename)))
  }
}