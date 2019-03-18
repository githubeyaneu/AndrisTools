package eu.eyan.japan

import org.junit.Test
import eu.eyan.testutil.TestPlus

object Japan6 extends App {

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

class Japan6 extends TestPlus {
  type Fields = Array[FieldType]
  def cancel = stopped = true
  private var stopped = false

  def reduce(knownFields: Fields, blocks: Array[Int]): Option[Fields] = {
    val st = System.currentTimeMillis
    val all = Combinations.combinationsWithRepetitionBi(blocks.size + 1, knownFields.size - (if (blocks.size == 0) 0 else (blocks.sum + blocks.size - 1)))

    val cumulated = Array.fill[FieldType](knownFields.size)(null)

    var ct = 0
    kton(blocks, knownFields.size - (if (blocks.size == 0) 0 else (blocks.sum + blocks.size - 1)), next => {
      ct = ct + 1
      if (ct % (10 * 1000 * 1000) == 0) print("_" + /*ct + " " + (System.currentTimeMillis - st) + "ms" + " " +*/ ((all / ct) * (System.currentTimeMillis - st)) / 60000 + "min")
      for (idx <- 0 until cumulated.size) {
        val cumField = cumulated(idx)
        val nextField = next(idx)
        cumulated(idx) = if (cumField == null) nextField else if (cumField == Unknown) Unknown
        else if (cumField == nextField) cumField
        else Unknown
      }
    }, knownFields)

    if (ct == 0) None else Option(cumulated)
  }

  def applies(knownFields: Fields)(fields: Fields): Boolean = {
    val length = knownFields.length
    def appliesSub(i: Int): Boolean = {
      if (i >= length) true
      else if (!fieldApplies(knownFields(i))(fields(i))) false
      else appliesSub(i + 1)
    }
    appliesSub(0)
  }

  def fieldsApply(knownFields: Fields, fields: Fields, fromIndex: Int, untilIndex: Int): Boolean = {
    if (fromIndex >= untilIndex) true
    else if (!fieldApplies(knownFields(fromIndex))(fields(fromIndex))) false
    else fieldsApply(knownFields, fields, fromIndex + 1, untilIndex)
  }
  def fieldApplies(knownField: FieldType)(field: FieldType) = (knownField == Unknown || knownField == field)

  def kton(blocks: Array[Int], extraEmptySpaces: Int, callback: Fields => Unit, knownFields: Fields) = {
    val blocksSize = blocks.size
    def ktonSub(callback: Fields => Unit, array: Fields, remainingStep: Int, remainingItems: Int, actualIndex: Int): Unit = {
      if (stopped) {
      } else if (remainingStep == 1) {
        for (i <- actualIndex until actualIndex + remainingItems) array(i) = Empty
        if (fieldsApply(knownFields, array, actualIndex, actualIndex + remainingItems))
          callback(array)
      } else {
        for (nextSize <- 0 to remainingItems) {

          val last = 2 == remainingStep
          val emptyAfterBlock = if (last) 0 else 1
          val actualBlockSize = blocks(blocksSize - remainingStep + 1)

          for (i <- actualIndex until actualIndex + nextSize) array(i) = Empty
          for (i <- actualIndex + nextSize until actualIndex + nextSize + actualBlockSize) array(i) = Full
          for (i <- actualIndex + nextSize + actualBlockSize until actualIndex + nextSize + actualBlockSize + emptyAfterBlock) array(i) = Empty

          if (fieldsApply(knownFields, array, actualIndex, actualIndex + nextSize + actualBlockSize + emptyAfterBlock))
            ktonSub(callback, array, remainingStep - 1, remainingItems - nextSize, actualIndex + nextSize + actualBlockSize + emptyAfterBlock)
        }
      }
    }

    ktonSub(callback, Array.fill((if (blocksSize == 0) 0 else blocks.sum + blocksSize - 1) + extraEmptySpaces)(Unknown), blocksSize + 1, extraEmptySpaces, 0)
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