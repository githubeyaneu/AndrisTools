package eu.eyan.japan

import org.junit.Test
import eu.eyan.testutil.TestPlus
import scala.annotation.tailrec

object Japan7 extends App {

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

class Japan7 extends TestPlus {
  type Fields = Array[FieldType]
  def cancel = cancelled = true
  private var cancelled = false

  def reduce(knownFields: Fields, blocks: Array[Int]): Option[Fields] = {
    val startTime = System.currentTimeMillis
    val fieldsLength = knownFields.size
    val blocksSize = blocks.size
    val blocksSum = blocks.sum
    val blocksAndKnownEmptySpaces = if (blocksSize == 0) 0 else blocksSum + blocksSize - 1
    val extraEmptySpaces = fieldsLength - blocksAndKnownEmptySpaces
    val places = blocksSize + 1
    val allPossibleCombinationsWithoutReduce = JapanGui.combinationsWithRepetitionBi(places, extraEmptySpaces)

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
  }
}