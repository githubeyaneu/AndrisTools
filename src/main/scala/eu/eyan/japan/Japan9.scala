package eu.eyan.japan

import org.junit.Test
import eu.eyan.testutil.TestPlus
import scala.annotation.tailrec
import eu.eyan.util.rx.lang.scala.ObservablePlus
import rx.lang.scala.subjects.BehaviorSubject
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicit
import eu.eyan.japan.JapanGui.JapanGuiTable
import eu.eyan.util.java.lang.ThreadPlus
import eu.eyan.japan.Japan.Fields
import eu.eyan.japan.Japan.Blocks
import eu.eyan.util.time.TimeCounter
import eu.eyan.japan.Japan.Lines
import eu.eyan.japan.Japan.Lines
import rx.lang.scala.Observable
import scala.util.Try
import eu.eyan.japan.Japan.Table

case class TableTopAndLeftBlocks(tops: Array[Blocks], lefts: Array[Blocks])

trait Japan9Algorythm {
  case class ReduceLinesResult(isEmpty: Boolean, table: Table)
  type ReduceLineResult = Try[Int]
  case class ReduceLineTimeoutResult(timeout: Boolean, result: ReduceLineResult)
  case class CandidateReduceResult(success: Boolean, table: Table)
  case class Cell(colRow: ColRow, value: FieldType)
  type Cells = Array[Cell]
  type Candidate
  type Candidates

  //  def solve(table: Table): Table
  def reduce(table: Table): ReduceLinesResult
  //  def candidateReduce(tableTopAndLeftBlocks: TableTopAndLeftBlocks, table: Table): Table
  //  def findLines(table: Table): Lines
  def reduceLines(table: Table, lines: Lines): ReduceLinesResult
  //  def reduceLineTimeout(line: Line, timeout: Long): ReduceLineTimeoutResult
  //  def reduceLine(knownFieldsOfLine: Fields, blocks: Blocks): ReduceLineResult
  //  def candidateReduce(table: Table): CandidateReduceResult
  //  def unknownFields(table: Table): Cells
  //  def sortCellsByComplexity(cells: Cells): Cells
  //  def generateCandidates(cells: Cells): Candidates
  //  def createNewTable(table: Table, candidate: Candidate): Table
}

trait Gui {
  def guiSetField(col: Col, row: Row, value: FieldType)
}
object Japan9 extends App {
  val a = Array(Array(1,2,3), Array(4,5,6))
  println(a.map(_.mkString).mkString("\r\n"))
  println
  println(a.transpose.map(_.mkString).mkString("\r\n"))
}
class Japan9(tableTopAndLeftBlocks: TableTopAndLeftBlocks, gui: Gui) extends Japan9Algorythm {

  private lazy val height = tableTopAndLeftBlocks.lefts.size
  private lazy val width = tableTopAndLeftBlocks.tops.size
  private lazy val cols = (0 until width).map(Col(_))
  private lazy val rows = (0 until height).map(Row(_))

  /////////////////////////////////////////////////////////////////////////////

  implicit class Table9(table: Table) {
    def fields(rowOrCol: Line) = rowOrCol.ifRowOrCol(table.transpose.apply, table.apply)
    
    def cells = for (col <- cols; row <- rows) yield Cell(ColRow(col, row), table(col.x)(row.y) )

    def updateCell(col: Col, row: Row, newVal: FieldType): Unit = table(col.x)(row.y) = newVal

    def updateCell(colRow: ColRow, newVal: FieldType): Unit = updateCell(colRow.col, colRow.row, newVal)

    override def clone = Array.tabulate(cols.size, rows.size)((col, row) => table(col)(row))

    override def toString = rows.map(row => cols.map(col => table(col.x)(row.y).toString).mkString).mkString("\r\n")

    def unknowns = table.flatten.count(_ == Unknown)
  }

  def reduce(table: Table) = {
    val linesToCheck = List[Line]((rows ++ cols): _*)

    val reduceLinesResult = reduceLines(table, linesToCheck)

    if (reduceLinesResult.isEmpty) {
      println("the blocks are bad, the first reduce was not able to run without error")
    } else {
      if (reduceLinesResult.table.unknowns == 0) println("The table is ready! Enjoy the picture")
    }

    reduceLinesResult
  }

  def reduceLines(table: Table, linesToCheck: Lines) = {
    def reduceLinesIn(
      table:               Table,
      linesToCheck:        Lines,
      actualReduceTimeout: Int,
      timeouts:            scala.collection.mutable.Set[Line]): ReduceLinesResult = {
      //    println("---")
      //    println(actualReduceTimeout)
      //    println("Lines to check: " + linesToCheck.mkString(" "))

      val reduceResultsOptions = linesToCheck.map(reduceLineTimeout(actualReduceTimeout, table, timeouts))
      val error = reduceResultsOptions.exists(_.isEmpty)
      if (error) {
        val errorLines = linesToCheck.zip(reduceResultsOptions).filter(_._2.isEmpty).map(_._1)
        //      println("Error: line(s) could not be reduced: " + errorLines.mkString(", "))
        ReduceLinesResult(false, table)
      } else {
        val reduceResults = reduceResultsOptions.flatten
        val changedLines = reduceResults.flatten.distinct
        //      println
        //      println("Changed lines:" + changedLines.mkString(" "))
        val changed = 0 < changedLines.size

        if (changed) {
          reduceLinesIn(table, changedLines, actualReduceTimeout, timeouts)
        } else if (0 < timeouts.size) {
          reduceLinesIn(table, sortLines(timeouts.toList, table), actualReduceTimeout * 2, timeouts)
        } else {
          //        println("done: full:" + table.fieldsAll.count(_ == Full) + ", empty:" + table.fieldsAll.count(_ == Empty) + ", unknown: " + unknown)
          //        println("" + (System.currentTimeMillis - start) + "ms")
          ReduceLinesResult(true, table)
        }
      }
    }

    def sortLines(lines: Lines, table: Table) = lines.sorted(orderLinesByComplexity)

    reduceLinesIn(table, linesToCheck, 10, scala.collection.mutable.Set[Line]((rows ++ cols): _*))
  }

  def candidateReduce(guiTable: Array[Array[FieldType]]): Unit = {
    val originalTable = guiTable
    println("candidateReduce" + originalTable.unknowns)
    val unknownFields = originalTable.cells.filter(_.value == Unknown).map(_.colRow)
    if (unknownFields.size == 0) println("The table is ready! Enjoy the picture")
    else {
      val unknownFieldsSorted = unknownFields.sorted(orderCellsByRowOrColComplexity)
      type Candidate = Tuple2[ColRow, FieldType]
      val candidatess: Seq[Candidate] = unknownFieldsSorted.map(cr => Seq((cr, Full), (cr, Empty))).flatten
      println("candidatess" + candidatess.size)
      val candiSize = (1 to candidatess.size).toList.iterator
      val candidatesSubsets = candiSize.map(candidatess.combinations(_)).flatten
      println("candidatesSubsets" + candidatesSubsets)
      // FIXME: filter same field with Full or empty is not possible!!!
      val candidatesSubsetsIterator = candidatesSubsets

      var done = false
      var candidateIndex = 0
      while (!done && candidatesSubsetsIterator.hasNext) {
        val candidates = candidatesSubsetsIterator.next
        //        println("Candidate:" + candidate)
        val newTable = originalTable.clone

        for (candidate <- candidates) newTable.updateCell(candidate._1, candidate._2)

        for (col <- cols; row <- rows) gui.guiSetField(col, row, newTable(col.x)(row.y))

        val timeouts = scala.collection.mutable.Set[Line]((rows ++ cols): _*)
        val reduceResult = reduce(newTable)
        if (reduceResult.isEmpty) {
          println("candidate " + candidates + " GOOD the blocks are bad, the reduce was not able to run without error")
          //          println("set candidate inverted: "+candidate)
          done = true
          val nextTable = originalTable.clone
          for (candidate <- candidates) nextTable.updateCell(candidate._1, if (candidate._2 == Full) Empty else Full)
          for (col <- cols; row <- rows) gui.guiSetField(col, row, nextTable(col.x)(row.y))
          // this is good! // FIXME: for one field it is good. for two???? think
          val timeouts = scala.collection.mutable.Set[Line]((rows ++ cols): _*)
          val reduceResult = reduce(nextTable)
          val unknown = reduceResult.table.unknowns
          if (unknown == 0) println("After candidate " + candidates + " the table is ready! Enjoy the picture")
          else candidateReduce(nextTable)
        } else {
          val unknown = reduceResult.table.unknowns
          if (unknown == 0) {
            println("  After candidate " + candidates + " the table is ready! Enjoy the picture")
            done = true
          } else {
            //println("  After candidate " + candidate + " no error -> dont know if good or bad. try next.")
            candidateIndex = candidateIndex + 1
            print(".")
            if (candidateIndex % 100 == 0) println
          }
        }
      }
    }
  }

  def reduceLineTimeout(timeoutMs: Int, table: Table, timeouts: scala.collection.mutable.Set[Line])(rowOrCol: Line): Option[Lines] = {
    val olds = table.fields(rowOrCol)
    val cancelled$ = BehaviorSubject(false)
    val reduceResultTimeout = ThreadPlus.runBlockingWithTimeout(timeoutMs, reduceLine(olds, blocks(rowOrCol).toArray, cancelled$), cancelled$.onNext(true))

    val changed = if (reduceResultTimeout.isEmpty) {
      timeouts.add(rowOrCol)
      Option(Seq()) // no changed row or col
    } else {
      timeouts.remove(rowOrCol)
      val reduceResult = reduceResultTimeout.get
      if (reduceResult.isEmpty) None // this indicates that there is an error with the table, cannot reduced properly
      else {
        val reducedFields = reduceResult.get
        val changedFields = olds.zip(reducedFields).zipWithIndex.filter(p => p._1._1 != p._1._2)
        val changedRowsOrCols: Lines = rowOrCol match {
          case Col(x) => {
            for (row <- rows) {
              table.updateCell(Col(x), row, reducedFields(row.y))
              gui.guiSetField(Col(x), row, reducedFields(row.y))
            }
            changedFields.map(r => Row(r._2))
          }
          case Row(y) => {
            for (col <- cols) {
              table.updateCell(col, Row(y), reducedFields(col.x))
              gui.guiSetField(col, Row(y), reducedFields(col.x))
            }
            changedFields.map(c => Col(c._2))
          }
        }
        Option(changedRowsOrCols)
      }
    }

    //print(rowOrCol + " " + (if (changed.size > 0) changed.size else if (reduceResultTimeout.nonEmpty) "." else ""))
    changed
  }

  private def blocks(rowOrCol: Line): Blocks = rowOrCol match {
    case Col(idx) => tableTopAndLeftBlocks.tops(idx)
    case Row(idx) => tableTopAndLeftBlocks.lefts(idx)
  }

  private def size(rowOrCol: Line): Int = rowOrCol match {
    case Col(_) => height
    case Row(_) => width
  }

  private val complexityMap = scala.collection.mutable.Map[Line, BigInt]()
  private def complexity(rowOrCol: Line) = {
    def computeComplexity = {
      val blocks = this.blocks(rowOrCol)
      val blocksSize = blocks.size
      val places = blocksSize + 1

      val fieldsLength = size(rowOrCol)
      val blocksSum = blocks.sum
      val blocksAndKnownEmptySpaces = if (blocksSize == 0) 0 else blocksSum + blocksSize - 1
      val extraEmptySpaces = fieldsLength - blocksAndKnownEmptySpaces

      Combinations.combinationsWithRepetitionBi(places, extraEmptySpaces)
    }
    complexityMap.getOrElseUpdate(rowOrCol, computeComplexity)
  }

  private def orderLinesByComplexity: Ordering[Line] = (x: Line, y: Line) => {
    val xc = complexity(x)
    val yc = complexity(y)
    if (xc == yc) 0
    else if (xc < yc) -1
    else 1
  }

  private def orderCellsByRowOrColComplexity: Ordering[ColRow] = (cr1: ColRow, cr2: ColRow) => {
    val cr1c = complexity(cr1.col)
    val cr1r = complexity(cr1.row)
    val cr2c = complexity(cr2.col)
    val cr2r = complexity(cr2.row)

    val c1 = if (cr1c < cr1r) cr1c else cr1r
    val c2 = if (cr2c < cr2r) cr2c else cr2r
    if (c1 == c2) 0
    else if (c1 < c2) -1
    else 1
  }

  def reduceLine(knownFields: Fields, blocks: Array[Int], cancelled$: Observable[Boolean] = BehaviorSubject(false)): Option[Fields] = {
    var cancelled = false
    cancelled$.subscribe(newCancelled => { cancelled = newCancelled })
    val startTime = System.currentTimeMillis
    val fieldsLength = knownFields.size
    val blocksSize = blocks.size
    val blocksSum = blocks.sum
    val blocksAndKnownEmptySpaces = if (blocksSize == 0) 0 else blocksSum + blocksSize - 1
    val extraEmptySpaces = fieldsLength - blocksAndKnownEmptySpaces
    val places = blocksSize + 1
    val allPossibleCombinationsWithoutReduce = Combinations.combinationsWithRepetitionBi(places, extraEmptySpaces)

    def generatePossibleFields(callback: Fields => Unit) = {
      val generatedFields = Unknown ** fieldsLength

      @tailrec
      def checkIfFieldsApplyToKnown(fromIndex: Int, untilIndex: Int): Boolean = {
        if (fromIndex >= untilIndex) true
        else if (knownFields(fromIndex) != Unknown && knownFields(fromIndex) != generatedFields(fromIndex)) false
        else checkIfFieldsApplyToKnown(fromIndex + 1, untilIndex)
      }

      def generatePossibleFieldsSteps(remainingStep: Int, remainingExtraEmptySpaces: Int, actualIndex: Int): Unit = {
        if (cancelled) {
        } else if (remainingStep == 1) {
          for (i <- actualIndex until actualIndex + remainingExtraEmptySpaces) generatedFields(i) = Empty
          if (checkIfFieldsApplyToKnown(actualIndex, actualIndex + remainingExtraEmptySpaces)) callback(generatedFields)
        } else {
          for (nextSize <- 0 to remainingExtraEmptySpaces) {

            val last = 2 == remainingStep
            val emptyAfterBlock = if (last) 0 else 1
            val actualBlockSize = blocks(blocksSize - remainingStep + 1)

            for (i <- actualIndex until actualIndex + nextSize) generatedFields(i) = Empty
            for (i <- actualIndex + nextSize until actualIndex + nextSize + actualBlockSize) generatedFields(i) = Full
            for (i <- actualIndex + nextSize + actualBlockSize until actualIndex + nextSize + actualBlockSize + emptyAfterBlock) generatedFields(i) = Empty

            if (checkIfFieldsApplyToKnown(actualIndex, actualIndex + nextSize + actualBlockSize + emptyAfterBlock))
              generatePossibleFieldsSteps(remainingStep - 1, remainingExtraEmptySpaces - nextSize, actualIndex + nextSize + actualBlockSize + emptyAfterBlock)
          }
        }
      }

      generatePossibleFieldsSteps(places, extraEmptySpaces, 0)
    }

    var possibleFieldsCounter = 0
    val cumulated = Unknown ** fieldsLength
    val fieldsCount = (0 until fieldsLength).toArray
    def reducePossibleFieldsCallback(next: Fields) = {
      possibleFieldsCounter = possibleFieldsCounter + 1
      //      if (possibleFieldsCounter % (10 * 1000 * 1000) == 0) print("_" + ((allPossibleCombinationsWithoutReduce / possibleFieldsCounter) * (System.currentTimeMillis - startTime)) / 60000 + "min")
      if (possibleFieldsCounter == 1) next.copyToArray(cumulated)
      for (idx <- fieldsCount) if (cumulated(idx) == Unknown || cumulated(idx) != next(idx)) cumulated(idx) = Unknown
    }

    generatePossibleFields(reducePossibleFieldsCallback)

    if (possibleFieldsCounter == 0) None else Option(cumulated)
  }
}

class Japan9Test() extends TestPlus {
  val ? = Unknown
  val X = Full
  val E = Empty
  val j = new Japan9(TableTopAndLeftBlocks(null, null), null)

  @Test
  def testReduce: Unit = {
    j.reduceLine(Array(?, ?, ?, ?, ?), Array(2, 1)).map(_.toList) ==> Option(List(?, X, ?, ?, ?))
    j.reduceLine(Array(?, ?, ?, ?, ?), Array(1, 2)).map(_.toList) ==> Option(List(?, ?, ?, X, ?))
    j.reduceLine(Array(?, ?, ?, ?), Array(1, 2)).map(_.toList) ==> Option(List(X, E, X, X))
    j.reduceLine(Array(?, ?, ?, ?), Array(1, 1)).map(_.toList) ==> Option(List(?, ?, ?, ?))
    j.reduceLine(Array(?, ?, ?), Array(1, 1)).map(_.toList) ==> Option(List(X, E, X))
    j.reduceLine(Array(?, ?, ?), Array(1)).map(_.toList) ==> Option(List(?, ?, ?))
    j.reduceLine(Array(X, E), Array(1)).map(_.toList) ==> Option(List(X, E))
    j.reduceLine(Array(E, X), Array(1)).map(_.toList) ==> Option(List(E, X))
    j.reduceLine(Array(E, ?), Array(1)).map(_.toList) ==> Option(List(E, X))
    j.reduceLine(Array(X, ?), Array(1)).map(_.toList) ==> Option(List(X, E))
    j.reduceLine(Array(?, E), Array(1)).map(_.toList) ==> Option(List(X, E))
    j.reduceLine(Array(?, X), Array(1)).map(_.toList) ==> Option(List(E, X))
    j.reduceLine(Array(?, ?), Array(1)).map(_.toList) ==> Option(List(?, ?))
    j.reduceLine(Array(X, E), Array()).map(_.toList) ==> None
    j.reduceLine(Array(E, X), Array()).map(_.toList) ==> None
    j.reduceLine(Array(E, ?), Array()).map(_.toList) ==> Option(List(E, E))
    j.reduceLine(Array(X, ?), Array()).map(_.toList) ==> None
    j.reduceLine(Array(?, E), Array()).map(_.toList) ==> Option(List(E, E))
    j.reduceLine(Array(?, X), Array()).map(_.toList) ==> None
    j.reduceLine(Array(?, ?), Array()).map(_.toList) ==> Option(List(E, E))
    j.reduceLine(Array(X, E), Array(2)).map(_.toList) ==> None
    j.reduceLine(Array(E, X), Array(2)).map(_.toList) ==> None
    j.reduceLine(Array(E, ?), Array(2)).map(_.toList) ==> None
    j.reduceLine(Array(X, ?), Array(2)).map(_.toList) ==> Option(List(X, X))
    j.reduceLine(Array(?, E), Array(2)).map(_.toList) ==> None
    j.reduceLine(Array(?, X), Array(2)).map(_.toList) ==> Option(List(X, X))
    j.reduceLine(Array(?, ?), Array(2)).map(_.toList) ==> Option(List(X, X))
    j.reduceLine(Array(E), Array()).map(_.toList) ==> Option(List(E))
    j.reduceLine(Array(X), Array()).map(_.toList) ==> None
    j.reduceLine(Array(?), Array()).map(_.toList) ==> Option(List(E))
    j.reduceLine(Array(E), Array(1)).map(_.toList) ==> None
    j.reduceLine(Array(X), Array(1)).map(_.toList) ==> Option(List(X))
    j.reduceLine(Array(?), Array(1)).map(_.toList) ==> Option(List(X))

    println(j.reduceLine(Array(E, ?, X, X, X, ?, E, E, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, X, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, E, E, E, E), Array(4, 6, 6)).map(_.toList))
  }
}