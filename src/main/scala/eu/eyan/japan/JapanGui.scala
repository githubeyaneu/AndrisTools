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
  val cols = japan.width + japan.leftsMax + 1
  val rows = japan.height + japan.upsMax + 1
  for (x <- 0 to cols) panel.newColumn("15px")
  for (x <- 0 to rows) panel.newRow("15px")

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

  for (x <- 0 until japan.width) {
    val col = x + japan.leftsMax + 1
    val row = japan.height + japan.upsMax + 1
    val colHint$ = japan.col$(x).map(list => {
      val colBlocks = japan.colBlocks(x)
      val fulls = list.filter(_ == Full).size
      val empties = list.filter(_ == Empty).size
      val unknowns = list.filter(_ == Unknown).size
      val hint = japan.height + fulls - colBlocks.sum - colBlocks.size + 1
      hint
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
      val hint = japan.height + fulls - rowBlocks.sum - rowBlocks.size + 1
      hint
    })

    val label = new JLabel(".")
    label.text(rowHint$.map(_.toString))
    label.onClicked(rowClick(y))
    label.setFont(headerFont)
    panel.add(label, CC.xy(col, row))
  }

  new JFrame().withComponent(panel).onCloseExit.packAndSetVisible

  val japanAlgo = new Japan2()

  def rowClick(rowIdx: Int) = {
    println(rowIdx)
    println(japan.row(rowIdx))
    val reduced = japanAlgo.reduce(japan.row(rowIdx), japan.rowBlocks(rowIdx))
    reduced.foreach(reduced => for (x <- 0 until japan.width) japan.update(x, rowIdx, reduced(x)))
    println("done")
  }

  def colClick(colIdx: Int) = {
    println(colIdx)
    println(japan.col(colIdx))
    val reduced = japanAlgo.reduce(japan.col(colIdx), japan.colBlocks(colIdx))
    reduced.foreach(reduced => for (y <- 0 until japan.height) japan.update(colIdx, y, reduced(y)))
    println("done")
  }
  
  for (x <- 0 until japan.width) colClick(x)
  for (y <- 0 until japan.height) rowClick(y)
}