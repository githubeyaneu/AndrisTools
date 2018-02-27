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
import com.typesafe.config.ConfigFactory
import eu.eyan.util.config.Config
import eu.eyan.util.scala.TryPlus.TryPlusImplicit
import eu.eyan.bittrex.v20.MarketSummaries
import scala.util.Try

object AllToDb extends App {
  Log.activate

  val config = Config("""C:\DEVELOPING_1\projects\coindatachris\conf.properties""")
  val tarsPath = config.get("tarsPath").getOrElse("")
  Log.info("Tars path: " + tarsPath)

  //  val tarsPath = """C:\DEVELOPING_1\projects\coindatachris\json"""
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

  def importJsonGzArrayToDb(fileNameAndContent: NameAndContent) = {
    val contentOption = ZipPlus.unzipToString(fileNameAndContent.content)
    //    val conentFuture = Future.fromTry(contentOption)

    val marketOption = contentOption.mapWithErrorHandler(GetMarketSummaries.apply, t => Log.error("error creating MarketSummaries: " + fileNameAndContent.filename))
    
    val marketFuture = Future.fromTry(marketOption)
    
    //val insertIntoDbFuture = marketFuture map bittrexReader.toDB

    val r = contentOption.map(content => TryCatchThrowable(
      bittrexReader.toDB(GetMarketSummaries(content)),
      t => Future(Log.error("parsing: " + fileNameAndContent.filename))))

    val content = contentOption.get
    val ret = TryCatchThrowable(
      bittrexReader.toDB(GetMarketSummaries(content)),
      t => Future(Log.error("parsing: " + fileNameAndContent.filename)))

    ret
  }
}