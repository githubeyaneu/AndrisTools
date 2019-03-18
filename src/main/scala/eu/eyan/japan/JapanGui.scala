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
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicit
import rx.lang.scala.Observable
import eu.eyan.util.rx.lang.scala.ObservablePlus
import scala.concurrent.Await
import scala.concurrent.Future
import java.util.concurrent.TimeoutException
import eu.eyan.util.java.lang.ThreadPlus
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicit
import scala.collection.mutable.Map
import scala.collection.mutable.Set
import scala.annotation.tailrec

object JapanGui extends App {
  val lines = """src\main\scala\eu\eyan\japan\puzzles\panda.txt""".linesFromFile.map(_.trim).toList
  val sides = lines.map(_.split("[\t ]").toList.map(_.toInt)).span(!_.contains(888))
  val lefts = sides._1
  val ups = sides._2.tail
  println((lefts.flatten.sum, ups.flatten.sum))
  assert(lefts.flatten.sum == ups.flatten.sum)

  val width = ups.size
  val height = lefts.size
  lazy val upsMax = ups.map(_.size).max
  lazy val leftsMax = lefts.map(_.size).max

  val japanAlgo = new Japan8(lefts, ups)
  val table = japanAlgo.table

  val defFont = new JLabel().getFont()
  val headerFont = new Font(defFont.getName, defFont.getStyle, defFont.getSize - 2)

  val panel = new JPanelWithFrameLayout()
  val cols = width + leftsMax
  val rows = height + upsMax
  for (x <- 0 to cols - 1) panel.newColumn("15px")
  for (x <- 0 to rows - 1) panel.newRow("15px")

  panel.newColumn("40px:g")
  panel.newRow("40px:g")

  for (x <- 0 until width; nums = ups(x); idx <- 0 until nums.size) {
    val col = leftsMax + x + 1
    val row = upsMax - nums.size + idx + 1
    val label = new JLabel("" + nums(idx))
    label.setFont(headerFont)
    label.onClicked(colClick(x))
    panel.add(label, CC.xy(col, row))
  }

  for (y <- 0 until height; nums = lefts(y); idx <- 0 until nums.size) {
    val row = upsMax + y + 1
    val col = leftsMax - nums.size + idx + 1
    val label = new JLabel("" + nums(idx))
    label.setFont(headerFont)
    label.onClicked(rowClick(y))
    panel.add(label, CC.xy(col, row))
  }

  for (x <- 0 until width; y <- 0 until height) {
    val col = x + leftsMax + 1
    val row = y + upsMax + 1
    val label = new JLabel(".")
    label.text(table.field$(ColRow(Col(x), Row(y))).map(_.toString))
    panel.add(label, CC.xy(col, row))
  }

  for (x <- 0 until width) {
    val col = x + leftsMax + 1
    val row = height + upsMax + 1
    val colHint$ = japanAlgo.complexity$(Col(x))

    val label = new JLabel(".")
    label.text(colHint$.map(_.toString))
    label.onClicked(colClick(x))
    label.setFont(headerFont)
    panel.add(label, CC.xy(col, row))
  }

  for (y <- 0 until height) {
    val row = y + upsMax + 1
    val col = width + leftsMax + 1
    val rowHint$ = japanAlgo.complexity$(Row(y))

    val label = new JLabel(".")
    label.text(rowHint$.map(_.toString))
    label.onClicked(rowClick(y))
    label.setFont(headerFont)
    panel.add(label, CC.xy(col, row))
  }

  new JFrame().withComponent(panel).onCloseExit.packAndSetVisible

  var actualReduceTimeout = 1000

  def rowClick(rowIdx: Int) = {
    //    print("row" + rowIdx + " ")
    //    println
    //    println(japan.row(rowIdx))
    val olds = table.row(rowIdx)
    val news = ThreadPlus.runBlockingWithTimeout(actualReduceTimeout, japanAlgo.reduce(olds.toArray, table.blocks(Row(rowIdx)).toArray), japanAlgo.cancel).flatten

    if (news.isEmpty) rowsTimeout.add(rowIdx)
    else rowsTimeout.remove(rowIdx)

    news.foreach(reduced => for (x <- 0 until width) table.update(x, rowIdx, reduced(x)))
    val changed = news.map(news => olds.zip(news).zipWithIndex.filter(p => p._1._1 != p._1._2).map(_._2)).getOrElse(List())
    print("r" + (if (changed.size > 0) changed.size else if (news.nonEmpty) "." else ""))
    changed
  }

  def colClick(colIdx: Int) = {
    //    print("col" + colIdx + " ")
    //    println
    //    println(japan.col(colIdx))
    reduceCol(colIdx)
  }

  def reduceCol(x: Int) = {
    val olds = table.col(x)
    val news = ThreadPlus.runBlockingWithTimeout(actualReduceTimeout, japanAlgo.reduce(olds.toArray, table.blocks(Col(x)).toArray), japanAlgo.cancel).flatten

    if (news.isEmpty) colsTimeout.add(x)
    else colsTimeout.remove(x)

    news.foreach(reduced => for (y <- 0 until height) table.update(x, y, reduced(y)))
    val changed = news.map(news => olds.zip(news).zipWithIndex.filter(p => p._1._1 != p._1._2).map(_._2)).getOrElse(List())
    print("c" + (if (changed.size > 0) changed.size else if (news.nonEmpty) "." else ""))
    changed
  }

  val rowsTimeout = Set[Int]()
  (0 until height).foreach(rowsTimeout.add(_))
  def toRowsToCheck = rowsTimeout.toList.sorted

  val colsTimeout = Set[Int]()
  (0 until width).foreach(colsTimeout.add(_))
  def toColsToCheck = colsTimeout.toList.sorted

  startToSolve(toColsToCheck, toRowsToCheck)

  def startToSolve(colsToCheck: Seq[Int], rowsToCheck: Seq[Int]): Unit = {
    println(actualReduceTimeout)
    println(("c" * colsToCheck.size) + ("r" * rowsToCheck.size))

    val checkColChangedRows = colsToCheck.map(colClick).flatten.distinct.sorted
    val checkRowChangedCols = rowsToCheck.map(rowClick).flatten.distinct.sorted
    println
    println((checkColChangedRows, checkRowChangedCols))
    val changed = 0 < checkColChangedRows.size + checkRowChangedCols.size
    if (changed) startToSolve(checkRowChangedCols, checkColChangedRows)
    else if (0 < toColsToCheck.size + toRowsToCheck.size) {
      actualReduceTimeout = actualReduceTimeout * 2
      startToSolve(toColsToCheck, toRowsToCheck)
    } else {
      val unknown = (0 until height).map(table.row(_)).flatten.count(_ == Unknown)
      val full = (0 until height).map(table.row(_)).flatten.count(_ == Full)
      val empty = (0 until height).map(table.row(_)).flatten.count(_ == Empty)
      println("done: full:" + full + ", empty:" + empty + ", unknown: " + unknown)
    }
  }
}