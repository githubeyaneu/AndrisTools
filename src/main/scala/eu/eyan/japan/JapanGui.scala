package eu.eyan.japan

import java.awt.Font

import com.jgoodies.forms.factories.CC

import eu.eyan.japan.Japan.Blocks
import eu.eyan.japan.Japan.Fields
import eu.eyan.util.rx.lang.scala.ObservablePlus
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicit
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JLabelPlus.JLabelImplicit
import eu.eyan.util.swing.JPanelWithFrameLayout
import javax.swing.JFrame
import javax.swing.JLabel
import rx.lang.scala.subjects.BehaviorSubject
import eu.eyan.util.rx.lang.scala.ObservablePlus.ObservableImplicit
import eu.eyan.util.swing.SwingPlus
import eu.eyan.util.java.lang.ThreadPlus

object JapanGui extends App {

  val file = "panda.txt"
  val dir = """src\main\scala\eu\eyan\japan\puzzles\"""
  val table = createTableFromFile(dir + file)

  def createTableFromFile(file: String) = {
    val lines = file.linesFromFile.map(_.trim).toList
    val sides = lines.map(_.split("[\t ]").toList.map(_.toInt)).span(!_.contains(888))
    val lefts = sides._1
    val ups = sides._2.tail
    println((lefts.flatten.sum, ups.flatten.sum))
    assert(lefts.flatten.sum == ups.flatten.sum)

    new JapanGuiTable(lefts, ups)
  }

  case class JapanGuiTable(lefts: List[Blocks], ups: List[Blocks]) {
    val width = ups.size
    val height = lefts.size
    lazy val upsMax = ups.map(_.size).max
    lazy val leftsMax = lefts.map(_.size).max

    def newValue(col: Col, row: Row, newVal: FieldType) = fieldMap(ColRow(col, row)).onNext(newVal)
    def field$(cr: ColRow) = fieldMap(cr).distinctUntilChanged
    def complexity$(rowOrCol: RowOrCol) = rowOrCol$(rowOrCol).map(fieldsComplexity(rowOrCol))
    def reset =  for (col <- cols; row <- rows) fieldMap(ColRow(col, row)).onNext(Unknown)

    private def fieldsComplexity(rowOrCol: RowOrCol)(list: Fields) = {

      val items = list.size - blocks(rowOrCol).sum - blocks(rowOrCol).size + 1
      val places = blocks(rowOrCol).size + 1
      ("" + Combinations.combinationsWithRepetitionBi(items, places)).length
    }

    def table = {
      val array = Array.ofDim[FieldType](cols.size, rows.size)
      for (col <- cols; row <- rows) array(col.x)(row.y) = fieldMap(ColRow(col, row)).get
      array
    }

    private val cols = (0 until ups.size).map(Col(_))
    private val rows = (0 until lefts.size).map(Row(_))

    private def blocks(rowOrCol: RowOrCol): Blocks = rowOrCol match {
      case Col(idx) => ups(idx)
      case Row(idx) => lefts(idx)
    }

    private def fields$(rowOrCol: RowOrCol) = rowOrCol match {
      case Col(x) => for (row <- rows) yield fieldMap(ColRow(Col(x), row))
      case Row(y) => for (col <- cols) yield fieldMap(ColRow(col, Row(y)))
    }

    private def rowOrCol$(rowOrCol: RowOrCol) = ObservablePlus.toSeq(fields$(rowOrCol))

    private val fieldMap = (for (col <- cols; row <- rows) yield (ColRow(col, row), BehaviorSubject[FieldType](Unknown))).toMap

  }

  val width = table.width
  val height = table.height
  lazy val upsMax = table.upsMax
  lazy val leftsMax = table.leftsMax

  val defFont = new JLabel().getFont()
  val headerFont = new Font(defFont.getName, defFont.getStyle, defFont.getSize - 2)

  val panel = new JPanelWithFrameLayout()
  val cols = table.width + table.leftsMax
  val rows = height + upsMax
  for (x <- 0 to cols - 1) panel.newColumn("15px")
  for (x <- 0 to rows - 1) panel.newRow("15px")

  panel.newColumn("40px:g")
  panel.newRow("40px:g")

  //UPS
  for (x <- 0 until width; nums = table.ups(x); idx <- 0 until nums.size) {
    val col = leftsMax + x + 1
    val row = upsMax - nums.size + idx + 1
    val label = new JLabel("" + nums(idx))
    label.setFont(headerFont)
    label.onClicked(reduceCol(x))
    panel.add(label, CC.xy(col, row))
  }

  // LEFTS
  for (y <- 0 until height; nums = table.lefts(y); idx <- 0 until nums.size) {
    val row = upsMax + y + 1
    val col = leftsMax - nums.size + idx + 1
    val label = new JLabel("" + nums(idx))
    label.setFont(headerFont)
    label.onClicked(reduceRow(y))
    panel.add(label, CC.xy(col, row))
  }

  // RIGHT
  for (x <- 0 until width) {
    val col = x + leftsMax + 1
    val row = height + upsMax + 1
    val colHint$ = table.complexity$(Col(x))

    val label = new JLabel(".")
    label.text(colHint$.map(_.toString))
    label.onClicked(reduceCol(x))
    label.setFont(headerFont)
    panel.add(label, CC.xy(col, row))
  }

  //BOTTOM
  for (y <- 0 until height) {
    val row = y + upsMax + 1
    val col = width + leftsMax + 1
    val rowHint$ = table.complexity$(Row(y))

    val label = new JLabel(".")
    label.text(rowHint$.map(_.toString))
    label.onClicked(reduceRow(y))
    label.setFont(headerFont)
    panel.add(label, CC.xy(col, row))
  }
  
  // SOLVE
  val labelSolve = new JLabel("s")
  labelSolve.onClicked(startToSolve)
  panel.add(labelSolve, CC.xy(1, 1))
  
  // RESET
  val resetLabel = new JLabel("r")
  resetLabel.onClicked(reset)
  panel.add(resetLabel, CC.xy(3, 1))
  

  // FIELDS
  for (x <- 0 until width; y <- 0 until height) {
    val col = x + leftsMax + 1
    val row = y + upsMax + 1
    val label = new JLabel(".")
    label.text(table.field$(ColRow(Col(x), Row(y))).map(_.toString))
    label.onClicked(changeCell(ColRow(Col(x), Row(y))))
    panel.add(label, CC.xy(col, row))
  }
  
  val algo = new Japan8(table.lefts, table.ups, table.newValue)
  
  def reduceRow(y: Int) = algo.reduceFields(Int.MaxValue)(Row(y))

  def reduceCol(x: Int) = algo.reduceFields(Int.MaxValue)(Col(x))
  
  def changeCell(colrow: ColRow) = {
    val actual = table.field$(colrow).get[FieldType]
    table.newValue(colrow.col, colrow.row, if(actual==Unknown) Full else if(actual==Full) Empty else Unknown)
  }

  new JFrame()
    .withComponent(panel)
    .onCloseExit
    .packAndSetVisible

  def startToSolve = ThreadPlus.run( algo.solve(table.table) )

  def reset = table.reset
    
}