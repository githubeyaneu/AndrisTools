package eu.eyan.japan

import eu.eyan.japan.Japan.FieldType
import eu.eyan.japan.Japan.Full
import eu.eyan.japan.Japan.Empty
import org.junit.Test
import eu.eyan.testutil.TestPlus

  case object Unknown extends FieldType { override def toString() = "?" } 

object Japan5 extends App {

  var ct = 0
  val st = System.currentTimeMillis
  val kton = new Japan5()
  kton.kton(Array(1, 1), 2, a => { println(a.toList) })
  kton.kton(Array(2, 2, 3, 6, 2, 2, 2, 6, 3, 2, 2), 28, a => {
    ct = ct + 1
    if (ct % 100000000 == 0) println(ct + " " + (System.currentTimeMillis - st) + "ms")
    if (ct % 200000001 == 0) kton.cancel
  })
  println(ct)
}

class Japan5 extends TestPlus {
  type Fields = Array[FieldType]
  def cancel = stopped = true
  private var stopped = false

  def reduce(knownFields: Fields, blocks: Array[Int]): Option[Fields] = {
    println("reduce: "+(knownFields.toList, blocks.toList))
    val cumulated = Array.fill[FieldType](knownFields.size)(null)

    var ct = 0
    kton(blocks, knownFields.size - (if(blocks.size==0) 0 else (blocks.sum + blocks.size - 1)), next => if(applies(knownFields)( next)){
      ct = ct + 1
      println("kton "+ct+": "+next.toList)
      for (idx <- 0 until cumulated.size) {
        val cumField = cumulated(idx)
        val nextField = next(idx)
        cumulated(idx) = if (cumField == null) nextField else if (cumField == Unknown) Unknown
        else if (cumField == nextField) cumField
        else Unknown
      }
    })

    if (ct == 0) None else Option(cumulated)
  }

  def applies(knownFields: Fields)(fields: Fields): Boolean = {
    def applies(known: FieldType, field: FieldType): Boolean = known == Unknown || known == field
    def appliesSub(tuple: Tuple2[FieldType, FieldType]) = !applies(tuple._1, tuple._2)
    val oneFieldNotApplyable = knownFields.zip(fields).exists(appliesSub)
    !oneFieldNotApplyable
  }
    
  def kton(blocks: Array[Int], extraEmptySpaces: Int, callback: Fields => Unit) = {
    println("kton: "+(blocks.toList, extraEmptySpaces))
    val blocksSize = blocks.size
    def ktonSub(callback: Fields => Unit, array: Fields, remainingStep: Int, remainingItems: Int, actualIndex: Int): Unit = {
      println((blocks.toList, extraEmptySpaces, array.toList, remainingStep, remainingItems, actualIndex))
      if (stopped) {
      } else if (remainingStep == 1) {
        for (i <- actualIndex until actualIndex + remainingItems) array(i) = Empty
        callback(array)
      } else {
        for (nextSize <- 0 to remainingItems) {

          val last = 2 == remainingStep
          val emptyAfterBlock = if (last) 0 else 1
          val actualBlockSize = blocks(blocksSize - remainingStep + 1)

          for (i <- actualIndex until actualIndex + nextSize) array(i) = Empty
          for (i <- actualIndex + nextSize until actualIndex + nextSize + actualBlockSize) array(i) = Full
          for (i <- actualIndex + nextSize + actualBlockSize until actualIndex + nextSize + actualBlockSize + emptyAfterBlock) array(i) = Empty

          ktonSub(callback, array, remainingStep - 1, remainingItems - nextSize, actualIndex + nextSize + actualBlockSize + emptyAfterBlock)
        }
      }
    }

    ktonSub(callback, Array.fill((if(blocksSize==0) 0 else blocks.sum + blocksSize - 1) + extraEmptySpaces)(Unknown), blocksSize + 1, extraEmptySpaces, 0)
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