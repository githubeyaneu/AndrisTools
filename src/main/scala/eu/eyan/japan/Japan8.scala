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

object Japan8 extends App {

  var ct = 0
  val st = System.currentTimeMillis
  val kton = new Japan5()
  kton.kton(Array(1, 1), 2, a => { println(a.toList) })
  //  kton.kton(Array(2, 2, 3, 6, 2, 2, 2, 6, 3, 2, 2), 28, a => {
  //    ct = ct + 1
  //    if (ct % (100 * 1000 * 1000) == 0) println(ct + " " + (System.currentTimeMillis - st) + "ms")
  //    if (ct % (2 * 100 * 1000 * 1000 + 1) == 0) kton.cancel
  //  })

  kton.reduce(Array.fill[FieldType](70)(Unknown), Array(2, 2, 3, 6, 2, 2, 2, 6, 3, 2, 2))
  println(ct)
}

case class Table(val table: Array[Array[FieldType]], guiSetField: (Col, Row, FieldType) => Unit) {
  val cols = (0 until table.size).map(Col(_))
  val rows = (0 until table(0).size).map(Row(_))

  def fields(rowOrCol: Line) = rowOrCol match {
    case Col(idx) => col(idx)
    case Row(idx) => row(idx)
  }
  def fieldsAll = rows.map(fields).flatten // FIXME dont use rows
  def fields = for (col <- cols; row <- rows) yield (table(col.x)(row.y), ColRow(col, row))

  def update(col: Col, row: Row, newVal: FieldType): Unit = {
    table(col.x)(row.y) = newVal
    guiSetField(col, row, newVal)
  }

  def refreshGui = for (col <- cols; row <- rows) guiSetField(col, row, table(col.x)(row.y))

  def update(colRow: ColRow, newVal: FieldType): Unit = update(colRow.col, colRow.row, newVal)

  private def row(y: Int) = (for (col <- cols) yield table(col.x)(y)).toArray
  private def col(x: Int) = (for (row <- rows) yield table(x)(row.y)).toArray

  override def clone = {
    val newArray = Array.ofDim[FieldType](cols.size, rows.size)
    for (col <- cols; row <- rows) newArray(col.x)(row.y) = table(col.x)(row.y)
    Table(newArray, guiSetField)
  }

  override def toString = rows.map(row => cols.map(col => table(col.x)(row.y).toString).mkString).mkString("\r\n")

  def unknowns = fieldsAll.count(_ == Unknown)
}

class Japan8(lefts: List[Blocks], ups: List[Blocks], width:Width, height:Height, guiSetField: (Col, Row, FieldType) => Unit) {
  def solve(guiTable: Array[Array[FieldType]]): Unit = {
    val table = new Table(guiTable, guiSetField)
    val timeouts = scala.collection.mutable.Set[Line]((table.rows ++ table.cols): _*)

    val firstReduceResult = reduceLines(toCheckTimeouted(timeouts, table), 10, System.currentTimeMillis, table, timeouts)

    if (firstReduceResult.isEmpty) {
      println("the blocks are bad, the first reduce was not able to run without error")
    } else {
      val unknown = firstReduceResult.get
      if (unknown == 0) println("The table is ready! Enjoy the picture")
      //else candidateReduce(table)
    }
  }

  def candidateReduce(guiTable: Array[Array[FieldType]]): Unit = {
    val originalTable = new Table(guiTable, guiSetField)
    println("candidateReduce" + originalTable.unknowns)
    val unknownFields = originalTable.fields.filter(_._1 == Unknown).map(_._2)
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

        for (candidate <- candidates) newTable.update(candidate._1, candidate._2)
        newTable.refreshGui
        val timeouts = scala.collection.mutable.Set[Line]((newTable.rows ++ newTable.cols): _*)
        val reduceResult = reduceLines(toCheckTimeouted(timeouts, newTable), 10, System.currentTimeMillis, newTable, timeouts)
        if (reduceResult.isEmpty) {
          println("candidate " + candidates + " GOOD the blocks are bad, the reduce was not able to run without error")
          //          println("set candidate inverted: "+candidate)
          done = true
          val nextTable = originalTable.clone
          for (candidate <- candidates) nextTable.update(candidate._1, if (candidate._2 == Full) Empty else Full)
          nextTable.refreshGui
          // this is good! // FIXME: for one field it is good. for two???? think
          val timeouts = scala.collection.mutable.Set[Line]((nextTable.rows ++ nextTable.cols): _*)
          val reduceResult = reduceLines(toCheckTimeouted(timeouts, nextTable), 10, System.currentTimeMillis, nextTable, timeouts)
          val unknown = reduceResult.get
          if (unknown == 0) println("After candidate " + candidates + " the table is ready! Enjoy the picture")
          else candidateReduce(nextTable.table)
        } else {
          val unknown = reduceResult.get
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

  private def toCheckTimeouted(timeouts: scala.collection.mutable.Set[Line], table: Table) = timeouts.toList.sorted(orderLinesByComplexity)

  private def reduceLines(linesToCheck: Lines, actualReduceTimeout: Int, start: Long, table: Table, timeouts: scala.collection.mutable.Set[Line]): Option[Int] = {
    //    println("---")
    //    println(actualReduceTimeout)
    //    println("Lines to check: " + linesToCheck.mkString(" "))

    val reduceResultsOptions = linesToCheck.map(reduceLine(actualReduceTimeout, table, timeouts))
    val error = reduceResultsOptions.exists(_.isEmpty)
    if (error) {
      val errorLines = linesToCheck.zip(reduceResultsOptions).filter(_._2.isEmpty).map(_._1)
      //      println("Error: line(s) could not be reduced: " + errorLines.mkString(", "))
      None
    } else {
      val reduceResults = reduceResultsOptions.flatten
      val changedLines = reduceResults.flatten.distinct
      //      println
      //      println("Changed lines:" + changedLines.mkString(" "))
      val changed = 0 < changedLines.size

      if (changed) {
        reduceLines(changedLines, actualReduceTimeout, start, table, timeouts)
      } else if (0 < toCheckTimeouted(timeouts, table).size) {
        reduceLines(toCheckTimeouted(timeouts, table), actualReduceTimeout * 2, start, table, timeouts)
      } else {
        val unknown = table.fieldsAll.count(_ == Unknown)
        //        println("done: full:" + table.fieldsAll.count(_ == Full) + ", empty:" + table.fieldsAll.count(_ == Empty) + ", unknown: " + unknown)
        //        println("" + (System.currentTimeMillis - start) + "ms")
        Option(unknown)
      }
    }
  }

  def reduceLine(timeoutMs: Int, table: Table, timeouts: scala.collection.mutable.Set[Line])(rowOrCol: Line): Option[Lines] = {
    val olds = table.fields(rowOrCol)
    val cancelled$ = BehaviorSubject(false)
    val reduceResultTimeout = ThreadPlus.runBlockingWithTimeout(timeoutMs, reduce(olds, blocks(rowOrCol).toArray, cancelled$), cancelled$.onNext(true))

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
            for (row <- table.rows) table.update(Col(x), row, reducedFields(row.y))
            changedFields.map(r => Row(r._2))
          }
          case Row(y) => {
            for (col <- table.cols) table.update(col, Row(y), reducedFields(col.x))
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
    case Col(idx) => ups(idx)
    case Row(idx) => lefts(idx)
  }

  private def size(rowOrCol: Line): Int = rowOrCol match {
    case Col(_) => height.h
    case Row(_) => width.w
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

  def reduce(knownFields: Fields, blocks: Array[Int], cancelled$: Observable[Boolean] = BehaviorSubject(false)): Option[Fields] = {
    var cancelled = false
    cancelled$.subscribe(newCancelled => {cancelled = newCancelled}) 
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

class Japan8Test() extends TestPlus {
  val ? = Unknown
  val X = Full
  val E = Empty
  val j = new Japan8(null, null, Width(0), Height(0), null)

  @Test
  def testReduce: Unit = {
    j.reduce(Array(?, ?, ?, ?, ?), Array(2, 1)).map(_.toList) ==> Option(List(?, X, ?, ?, ?))
    j.reduce(Array(?, ?, ?, ?, ?), Array(1, 2)).map(_.toList) ==> Option(List(?, ?, ?, X, ?))
    j.reduce(Array(?, ?, ?, ?), Array(1, 2)).map(_.toList) ==> Option(List(X, E, X, X))
    j.reduce(Array(?, ?, ?, ?), Array(1, 1)).map(_.toList) ==> Option(List(?, ?, ?, ?))
    j.reduce(Array(?, ?, ?), Array(1, 1)).map(_.toList) ==> Option(List(X, E, X))
    j.reduce(Array(?, ?, ?), Array(1)).map(_.toList) ==> Option(List(?, ?, ?))
    j.reduce(Array(X, E), Array(1)).map(_.toList) ==> Option(List(X, E))
    j.reduce(Array(E, X), Array(1)).map(_.toList) ==> Option(List(E, X))
    j.reduce(Array(E, ?), Array(1)).map(_.toList) ==> Option(List(E, X))
    j.reduce(Array(X, ?), Array(1)).map(_.toList) ==> Option(List(X, E))
    j.reduce(Array(?, E), Array(1)).map(_.toList) ==> Option(List(X, E))
    j.reduce(Array(?, X), Array(1)).map(_.toList) ==> Option(List(E, X))
    j.reduce(Array(?, ?), Array(1)).map(_.toList) ==> Option(List(?, ?))
    j.reduce(Array(X, E), Array()).map(_.toList) ==> None
    j.reduce(Array(E, X), Array()).map(_.toList) ==> None
    j.reduce(Array(E, ?), Array()).map(_.toList) ==> Option(List(E, E))
    j.reduce(Array(X, ?), Array()).map(_.toList) ==> None
    j.reduce(Array(?, E), Array()).map(_.toList) ==> Option(List(E, E))
    j.reduce(Array(?, X), Array()).map(_.toList) ==> None
    j.reduce(Array(?, ?), Array()).map(_.toList) ==> Option(List(E, E))
    j.reduce(Array(X, E), Array(2)).map(_.toList) ==> None
    j.reduce(Array(E, X), Array(2)).map(_.toList) ==> None
    j.reduce(Array(E, ?), Array(2)).map(_.toList) ==> None
    j.reduce(Array(X, ?), Array(2)).map(_.toList) ==> Option(List(X, X))
    j.reduce(Array(?, E), Array(2)).map(_.toList) ==> None
    j.reduce(Array(?, X), Array(2)).map(_.toList) ==> Option(List(X, X))
    j.reduce(Array(?, ?), Array(2)).map(_.toList) ==> Option(List(X, X))
    j.reduce(Array(E), Array()).map(_.toList) ==> Option(List(E))
    j.reduce(Array(X), Array()).map(_.toList) ==> None
    j.reduce(Array(?), Array()).map(_.toList) ==> Option(List(E))
    j.reduce(Array(E), Array(1)).map(_.toList) ==> None
    j.reduce(Array(X), Array(1)).map(_.toList) ==> Option(List(X))
    j.reduce(Array(?), Array(1)).map(_.toList) ==> Option(List(X))

    println(j.reduce(Array(E, ?, X, X, X, ?, E, E, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, X, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, E, E, E, E), Array(4, 6, 6)).map(_.toList))
  }
}