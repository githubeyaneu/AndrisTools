package eu.eyan.japan

import eu.eyan.util.swing.JPanelWithFrameLayout
import javax.swing.JFrame
import javax.swing.JLabel
import com.jgoodies.forms.factories.CC
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import java.awt.Font
import eu.eyan.util.swing.JLabelPlus.JLabelImplicit
import rx.lang.scala.subjects.BehaviorSubject
import eu.eyan.japan.Japan.Unknown
import eu.eyan.japan.Japan.FieldType
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicit
import eu.eyan.japan.Japan.FieldType
import rx.lang.scala.Observable
import eu.eyan.util.rx.lang.scala.ObservablePlus
import eu.eyan.japan.Japan.Full
import eu.eyan.japan.Japan.Empty
import scala.concurrent.Await
import scala.concurrent.Future
import java.util.concurrent.TimeoutException
import eu.eyan.util.java.lang.ThreadPlus
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicit
import scala.collection.mutable.Map
import scala.collection.mutable.Set
import scala.annotation.tailrec

object JapanGui extends App {
  val up = """src\main\scala\eu\eyan\japan\fent.txt"""
  val left = """src\main\scala\eu\eyan\japan\bal.txt"""
  val ups = up.linesFromFile.toList.map(_.split("\t").toList.map(_.toInt))
  val lefts = left.linesFromFile.toList.map(_.split("\t").toList.map(_.toInt))

  case class JapanTable(lefts: List[List[Int]], ups: List[List[Int]]) {
    lazy val width = ups.size
    lazy val height = lefts.size
    lazy val upsMax = ups.map(_.size).max
    lazy val leftsMax = lefts.map(_.size).max

    def field$(x: Int, y: Int) = fieldMap((x, y)).map(_.toString).distinctUntilChanged

    def row(y: Int) = (for (x <- 0 until width) yield fieldMap((x, y))).map(_.get[FieldType]).toList
    def col(x: Int) = (for (y <- 0 until height) yield fieldMap((x, y))).map(_.get[FieldType]).toList

    def col$(x: Int) = {
      val $list = (for (y <- 0 until height) yield fieldMap((x, y)))
      val list$ = ObservablePlus.toList($list: _*)
      list$
    }

    def row$(y: Int) = {
      val $list = (for (x <- 0 until width) yield fieldMap((x, y)))
      val list$ = ObservablePlus.toList($list: _*)
      list$
    }

    def rowBlocks(rowIdx: Int) = lefts(rowIdx)
    def colBlocks(colIdx: Int) = ups(colIdx)

    def update(x: Int, y: Int, newVal: FieldType) = fieldMap((x, y)).onNext(newVal)

    private val fieldMap = (for (x <- 0 to width; y <- 0 to height) yield ((x, y), BehaviorSubject[FieldType](Unknown))).toMap
  }

  val japan = JapanTable(lefts, ups)
  val defFont = new JLabel().getFont()
  val headerFont = new Font(defFont.getName, defFont.getStyle, defFont.getSize - 2)

  val panel = new JPanelWithFrameLayout()
  val cols = japan.width + japan.leftsMax
  val rows = japan.height + japan.upsMax
  for (x <- 0 to cols - 1) panel.newColumn("15px")
  for (x <- 0 to rows - 1) panel.newRow("15px")

  panel.newColumn("40px:g")
  panel.newRow("40px:g")

  for (x <- 0 until japan.width; nums = japan.ups(x); idx <- 0 until nums.size) {
    val col = japan.leftsMax + x + 1
    val row = japan.upsMax - nums.size + idx + 1
    val label = new JLabel("" + nums(idx))
    label.setFont(headerFont)
    label.onClicked(colClick(x))
    panel.add(label, CC.xy(col, row))
  }

  for (y <- 0 until japan.height; nums = japan.lefts(y); idx <- 0 until nums.size) {
    val row = japan.upsMax + y + 1
    val col = japan.leftsMax - nums.size + idx + 1
    val label = new JLabel("" + nums(idx))
    label.setFont(headerFont)
    label.onClicked(rowClick(y))
    panel.add(label, CC.xy(col, row))
  }

  for (x <- 0 until japan.width; y <- 0 until japan.height) {
    val col = x + japan.leftsMax + 1
    val row = y + japan.upsMax + 1
    val label = new JLabel(".")
    label.text(japan.field$(x, y))
    panel.add(label, CC.xy(col, row))
  }

  //TODO move to somwhere else
  implicit class LongToFieldType(long: Long) {
    @tailrec
    def factorial(x: Long, acc: Long = 1): Long = if (x <= 1) acc else factorial(x - 1, x * acc)
    def ! = factorial(long)
  }

  def binom(n: Long, k: Long): Long = (n.!) / ((k.!) * (n - k).!)

  def combinationsWithRepetition(places: Long, items: Long) = binom(places + items - 1, items)

  implicit class BigIntToFieldType(bigInt: BigInt) {
    @tailrec
    def factorial(x: BigInt, acc: BigInt = 1): BigInt = if (x <= 1) acc else factorial(x - 1, x * acc)
    def ! = factorial(bigInt)
  }
  def binomBi(n: BigInt, k: BigInt): BigInt = (n.!) / ((k.!) * (n - k).!)
  // binom: n * n-1 * n-2 * ... * n-k+1 / (k * k-1 * k-2 * ... * 1)
  def combinationsWithRepetitionBi(places: Long, items: Long) = binomBi(places + items - 1, items)
  //TODO move to somwhere else

  for (x <- 0 until japan.width) {
    val col = x + japan.leftsMax + 1
    val row = japan.height + japan.upsMax + 1
    val colHint$ = japan.col$(x).map(list => {
      val colBlocks = japan.colBlocks(x)
      val fulls = list.filter(_ == Full).size
      val empties = list.filter(_ == Empty).size
      val unknowns = list.filter(_ == Unknown).size
      val items = japan.height - colBlocks.sum - colBlocks.size + 1
      val places = colBlocks.size + 1
      ("" + combinationsWithRepetitionBi(items, places)).length
    })

    val label = new JLabel(".")
    label.text(colHint$.map(_.toString))
    label.onClicked(colClick(x))
    label.setFont(headerFont)
    panel.add(label, CC.xy(col, row))
  }

  for (y <- 0 until japan.height) {
    val row = y + japan.upsMax + 1
    val col = japan.width + japan.leftsMax + 1
    val rowHint$ = japan.row$(y).map(list => {
      val rowBlocks = japan.rowBlocks(y)
      val fulls = list.filter(_ == Full).size
      val empties = list.filter(_ == Empty).size
      val unknowns = list.filter(_ == Unknown).size
      //      japan.height + fulls - rowBlocks.sum - rowBlocks.size + 1
      val items = japan.width - rowBlocks.sum - rowBlocks.size + 1
      val places = rowBlocks.size + 1
      //(combinationsWithRepetition(places, items) + "").length
      ("" + combinationsWithRepetitionBi(items, places)).length + ", " + (places, items)
    })

    val label = new JLabel(".")
    label.text(rowHint$.map(_.toString))
    label.onClicked(rowClick(y))
    label.setFont(headerFont)
    panel.add(label, CC.xy(col, row))
  }

  new JFrame().withComponent(panel).onCloseExit.packAndSetVisible

  import scala.concurrent._
  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global
  def timeout[T](ms: Int, action: => T, cancelAction: => Unit) = {
    def now = System.currentTimeMillis
    val threadAction = ThreadPlus.run(action)
    val start = now
    while (!threadAction.done && now < start + ms) Thread.sleep(10)
    if (threadAction.done) {
      //      print((now-start)+"ms    ")
      threadAction.result
    } else {
      cancelAction
      while (!threadAction.done) Thread.sleep(10)
      None
    }
  }

  def rowClick(rowIdx: Int) = {
    //    print("row" + rowIdx + " ")
    val olds = japan.row(rowIdx)
    val japanAlgo = new Japan5()
    val news = timeout(tms, japanAlgo.reduce(olds.toArray, japan.rowBlocks(rowIdx).toArray), japanAlgo.cancel).flatten

    if (news.isEmpty) rowsTimeout.add(rowIdx)
    else rowsTimeout.remove(rowIdx)

    news.foreach(reduced => for (x <- 0 until japan.width) japan.update(x, rowIdx, reduced(x)))
    val changed = news.map(news => olds.zip(news).zipWithIndex.filter(p => p._1._1 != p._1._2).map(_._2)).getOrElse(List())
    print("r" + (if (changed.size > 0) changed.size else ""))
    changed
  }

  def colClick(colIdx: Int) = {
    //    print("col" + colIdx + " ")
    val olds = japan.col(colIdx)
    val japanAlgo = new Japan5()
    val news = timeout(tms, japanAlgo.reduce(olds.toArray, japan.colBlocks(colIdx).toArray), japanAlgo.cancel).flatten

    if (news.isEmpty) colsTimeout.add(colIdx)
    else colsTimeout.remove(colIdx)

    news.foreach(reduced => for (y <- 0 until japan.height) japan.update(colIdx, y, reduced(y)))
    val changed = news.map(news => olds.zip(news).zipWithIndex.filter(p => p._1._1 != p._1._2).map(_._2)).getOrElse(List())
    print("c" + (if (changed.size > 0) changed.size else ""))
    changed
  }

  var tms = 1000

  val rowsTimeout = Set[Int]()
  (0 until japan.height).foreach(rowsTimeout.add(_))
  def toRowsToCheck = rowsTimeout.toList.sorted

  val colsTimeout = Set[Int]()
  (0 until japan.width).foreach(colsTimeout.add(_))
  def toColsToCheck = colsTimeout.toList.sorted

  startToSolve(toColsToCheck, toRowsToCheck)

  def startToSolve(colsToCheck: Seq[Int], rowsToCheck: Seq[Int]): Unit = {
    println(tms)
    println(("c" * colsToCheck.size) + ("r" * rowsToCheck.size))
    val checkColChangedRows = colsToCheck.map(colClick).flatten.distinct.sorted
    val checkRowChangedCols = rowsToCheck.map(rowClick).flatten.distinct.sorted
    println
    println((checkColChangedRows, checkRowChangedCols))
    val changed = 0 < checkColChangedRows.size + checkRowChangedCols.size
    if (changed) startToSolve(checkRowChangedCols, checkColChangedRows)
    else {
      tms = tms * 2
      startToSolve(toColsToCheck, toRowsToCheck)
    }
  }
}