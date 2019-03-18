package eu.eyan.japan

import org.junit.Test
import eu.eyan.testutil.TestPlus
import scala.annotation.tailrec
import eu.eyan.util.rx.lang.scala.ObservablePlus
import rx.lang.scala.subjects.BehaviorSubject
import eu.eyan.util.rx.lang.scala.subjects.BehaviorSubjectPlus.BehaviorSubjectImplicit
import eu.eyan.japan.JapanGui.JapanGuiTable
import eu.eyan.japan.JapanGui.RowOrCol
import eu.eyan.japan.JapanGui.Col
import eu.eyan.japan.JapanGui.Row
import eu.eyan.util.java.lang.ThreadPlus
import eu.eyan.japan.Japan.Fields
import eu.eyan.japan.Japan.Blocks
import eu.eyan.util.time.TimeCounter

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

case class Table(private val table: Array[Array[FieldType]], guiSetField: (Col, Row, FieldType) => Unit) {
  val cols = (0 until table.size).map(Col(_))
  val rows = (0 until table(0).size).map(Row(_))

  def fields(rowOrCol: RowOrCol) = rowOrCol match {
    case Col(idx) => col(idx)
    case Row(idx) => row(idx)
  }
  def fieldsAll = rows.map(fields).flatten // FIXME dont use rows

  def update(col: Col, row: Row, newVal: FieldType) = {
    table(col.x)(row.y) = newVal

    guiSetField(col, row, newVal)
  }

  private def row(y: Int) = (for (col <- cols) yield table(col.x)(y))
  private def col(x: Int) = (for (row <- rows) yield table(x)(row.y))
}

class Japan8() extends TestPlus {
  def solve(lefts: List[Blocks], ups: List[Blocks], table: Array[Array[FieldType]], guiSetField: (Col, Row, FieldType) => Unit): Unit = {
    this.lefts = lefts
    this.ups = ups
    this.table = new Table(table, guiSetField)
    timeouts = scala.collection.mutable.Set[RowOrCol]((this.table.rows ++ this.table.cols): _*)
    actualReduceTimeout = 10
    start = System.currentTimeMillis
    startToSolve(toCheckTimeouted)
  }

  private var lefts: List[Blocks] = _
  private var ups: List[Blocks] = _
  private var table: Table = _
  private var timeouts: scala.collection.mutable.Set[RowOrCol] = scala.collection.mutable.Set[RowOrCol]()
  private def toCheckTimeouted = timeouts.toList.sorted(sorter)
  private var actualReduceTimeout = 1000
  private var start = System.currentTimeMillis

  def startToSolve(linesToCheck: Seq[RowOrCol]): Unit = {
    println("---")
    println(actualReduceTimeout)
    println("Lines to check: " + linesToCheck.mkString(" "))

    val changedLines = linesToCheck.map(reduceFields(actualReduceTimeout)).flatten.flatten.distinct
    println
    println("Changed lines:" + changedLines.mkString(" "))
    val changed = 0 < changedLines.size
    if (changed) startToSolve(changedLines)
    else if (0 < toCheckTimeouted.size) {
      actualReduceTimeout = actualReduceTimeout * 2
      startToSolve(toCheckTimeouted)
    } else {
      val unknown = table.fieldsAll.count(_ == Unknown)
      val full = table.fieldsAll.count(_ == Full)
      val empty = table.fieldsAll.count(_ == Empty)
      println("done: full:" + full + ", empty:" + empty + ", unknown: " + unknown)
      println(" " + (System.currentTimeMillis - start) + "ms")
    }
  }

  def reduceFields(timeoutMs: Int)(rowOrCol: RowOrCol): Option[Seq[RowOrCol]] = {
    val olds = table.fields(rowOrCol)
    //TODO gives RowOrCol back instead of int
    val reduceResultTimeout = ThreadPlus.runBlockingWithTimeout(timeoutMs, reduce(olds, blocks(rowOrCol).toArray), cancel)

    if (reduceResultTimeout.isEmpty) timeouts.add(rowOrCol)
    else timeouts.remove(rowOrCol)

    val changed = reduceResultTimeout.map(reduceResult => {

      //TODO: make it easier
      rowOrCol match {
        case Col(x) => {
          reduceResult.foreach(reduced => for (row <- table.rows) table.update(Col(x), row, reduced(row.y)))
          reduceResult.map(news => olds.zip(news).zipWithIndex.filter(p => p._1._1 != p._1._2).map(r => Row(r._2))).getOrElse(List())
        }
        case Row(y) => {
          reduceResult.foreach(reduced => for (col <- table.cols) table.update(col, Row(y), reduced(col.x)))
          reduceResult.map(news => olds.zip(news).zipWithIndex.filter(p => p._1._1 != p._1._2).map(c => Col(c._2))).getOrElse(List())
        }
      }
    })

    print(rowOrCol + " " + (if (changed.size > 0) changed.size else if (reduceResultTimeout.nonEmpty) "." else ""))
    changed
  }

  //TODO duplicate
  def complexity(rowOrCol: RowOrCol) = {
    val blocks = this.blocks(rowOrCol)
    val blocksSize = blocks.size
    val places = blocksSize + 1

    val fieldsLength = table.fields(rowOrCol).size
    val blocksSum = blocks.sum
    val blocksAndKnownEmptySpaces = if (blocksSize == 0) 0 else blocksSum + blocksSize - 1
    val extraEmptySpaces = fieldsLength - blocksAndKnownEmptySpaces

    Combinations.combinationsWithRepetitionBi(places, extraEmptySpaces)
  }

  def sorter: Ordering[RowOrCol] = (x: RowOrCol, y: RowOrCol) => {
    val xc = complexity(x)
    val yc = complexity(y)
    if (xc == yc) 0
    else if (xc < yc) -1
    else 1
  }

  def cancel = cancelled = true
  private var cancelled = false

  def reduce(knownFields: Fields, blocks: Array[Int]): Option[Fields] = {
    cancelled = false
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
      def checkIfFieldsApplyToKnown(fromIndex: Int, untilIndex: Int): Boolean =
        if (fromIndex >= untilIndex) true
        else if (knownFields(fromIndex) != Unknown && knownFields(fromIndex) != generatedFields(fromIndex)) false
        else checkIfFieldsApplyToKnown(fromIndex + 1, untilIndex)

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
    def reducePossibleFieldsCallback(next: Fields) = {
      possibleFieldsCounter = possibleFieldsCounter + 1
      if (possibleFieldsCounter % (10 * 1000 * 1000) == 0) print("_" + ((allPossibleCombinationsWithoutReduce / possibleFieldsCounter) * (System.currentTimeMillis - startTime)) / 60000 + "min")
      if (possibleFieldsCounter == 1) next.copyToArray(cumulated)
      for (idx <- 0 until fieldsLength) if (cumulated(idx) == Unknown || cumulated(idx) != next(idx)) cumulated(idx) = Unknown
    }

    generatePossibleFields(reducePossibleFieldsCallback)

    if (possibleFieldsCounter == 0) None else Option(cumulated)
  }

  def blocks(rowOrCol: RowOrCol): Blocks = rowOrCol match {
    case Col(idx) => ups(idx)
    case Row(idx) => lefts(idx)
  }

  val ? = Unknown
  val X = Full
  val E = Empty

  @Test
  def testReduce: Unit = {
    reduce(Array(?, ?, ?, ?, ?), Array(2, 1)).map(_.toList) ==> Option(List(?, X, ?, ?, ?))
    reduce(Array(?, ?, ?, ?, ?), Array(1, 2)).map(_.toList) ==> Option(List(?, ?, ?, X, ?))
    reduce(Array(?, ?, ?, ?), Array(1, 2)).map(_.toList) ==> Option(List(X, E, X, X))
    reduce(Array(?, ?, ?, ?), Array(1, 1)).map(_.toList) ==> Option(List(?, ?, ?, ?))
    reduce(Array(?, ?, ?), Array(1, 1)).map(_.toList) ==> Option(List(X, E, X))
    reduce(Array(?, ?, ?), Array(1)).map(_.toList) ==> Option(List(?, ?, ?))
    reduce(Array(X, E), Array(1)).map(_.toList) ==> Option(List(X, E))
    reduce(Array(E, X), Array(1)).map(_.toList) ==> Option(List(E, X))
    reduce(Array(E, ?), Array(1)).map(_.toList) ==> Option(List(E, X))
    reduce(Array(X, ?), Array(1)).map(_.toList) ==> Option(List(X, E))
    reduce(Array(?, E), Array(1)).map(_.toList) ==> Option(List(X, E))
    reduce(Array(?, X), Array(1)).map(_.toList) ==> Option(List(E, X))
    reduce(Array(?, ?), Array(1)).map(_.toList) ==> Option(List(?, ?))
    reduce(Array(X, E), Array()).map(_.toList) ==> None
    reduce(Array(E, X), Array()).map(_.toList) ==> None
    reduce(Array(E, ?), Array()).map(_.toList) ==> Option(List(E, E))
    reduce(Array(X, ?), Array()).map(_.toList) ==> None
    reduce(Array(?, E), Array()).map(_.toList) ==> Option(List(E, E))
    reduce(Array(?, X), Array()).map(_.toList) ==> None
    reduce(Array(?, ?), Array()).map(_.toList) ==> Option(List(E, E))
    reduce(Array(X, E), Array(2)).map(_.toList) ==> None
    reduce(Array(E, X), Array(2)).map(_.toList) ==> None
    reduce(Array(E, ?), Array(2)).map(_.toList) ==> None
    reduce(Array(X, ?), Array(2)).map(_.toList) ==> Option(List(X, X))
    reduce(Array(?, E), Array(2)).map(_.toList) ==> None
    reduce(Array(?, X), Array(2)).map(_.toList) ==> Option(List(X, X))
    reduce(Array(?, ?), Array(2)).map(_.toList) ==> Option(List(X, X))
    reduce(Array(E), Array()).map(_.toList) ==> Option(List(E))
    reduce(Array(X), Array()).map(_.toList) ==> None
    reduce(Array(?), Array()).map(_.toList) ==> Option(List(E))
    reduce(Array(E), Array(1)).map(_.toList) ==> None
    reduce(Array(X), Array(1)).map(_.toList) ==> Option(List(X))
    reduce(Array(?), Array(1)).map(_.toList) ==> Option(List(X))

    println(reduce(Array(E, ?, X, X, X, ?, E, E, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, X, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, E, E, E, E), Array(4, 6, 6)).map(_.toList))
  }
}