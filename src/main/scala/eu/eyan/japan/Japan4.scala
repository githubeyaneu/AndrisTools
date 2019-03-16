package eu.eyan.japan

import org.junit.Test
import eu.eyan.testutil.TestPlus
import eu.eyan.log.Log
import org.junit.Ignore
import scala.annotation.tailrec

class Japan4 extends TestPlus {

  private var cancelled = false
  def cancel = cancelled = true

  type Fields = Array[FieldType]

  def reduce(knownFields: Fields, blocks: Array[Int]): Option[Fields] = {
    val stream = generateFields(knownFields, blocks)
    if (stream.isEmpty) None
    else Option(reduceStream(stream))
  }

  def reduceStream(stream: Stream[Fields]): Fields = {
    def reduceFields(cumulated: Fields, next: Fields): Fields = {
      for (idx <- 0 until cumulated.size) {
        val cumIdx = cumulated(idx)
        val nextIdx = next(idx)
        cumulated(idx) = if (cumIdx == Unknown) Unknown
        else if (cumIdx == nextIdx) cumIdx
        else Unknown
      }
      cumulated
    }
    stream.reduce(reduceFields)
  }

  def applies(knownFields: Fields)(fields: Fields): Boolean = {
    def applies(known: FieldType, field: FieldType): Boolean = known == Unknown || known == field
    def appliesSub(tuple: Tuple2[FieldType, FieldType]) = !applies(tuple._1, tuple._2)
    val oneFieldNotApplyable = knownFields.zip(fields).exists(appliesSub)
    !oneFieldNotApplyable
  }

  implicit class IntToFieldType(int: Int) {
    def **(fieldType: FieldType) = fieldType.applyA(int)
  }

  def generateFields(knownFields: Fields, blocks: Array[Int]): Stream[Fields] = {
    generateFieldsAcc(knownFields, blocks, Array())
  }

  def empties(nr: Int): Fields = Empty.applyA(nr)

  //  @tailrec
  def generateFieldsAcc(knownFields: Fields, blocks: Array[Int], acc: Fields): Stream[Fields] = {
    val length = knownFields.size
    val remainingEmptySpace = length - (blocks.sum + blocks.size - 1)
    if (cancelled) Stream.Empty
    else if (blocks.size == 0) {
      val empties = Empty.applyA(length)
      if (applies(knownFields)(empties)) Stream(acc ++ empties)
      else Stream.Empty
    } else {
      val preEmptySpaces = Stream.range(0, remainingEmptySpace + 1, 1).map(empties)
      val blockFollowingEmpty = blocks.head ** Full ++ (if (blocks.size < 2) Array[FieldType]() else 1 ** Empty)
      def prepend(l1: Fields)(l2: Fields) = l2 ++ l1
      val pres = preEmptySpaces.map(prepend(blockFollowingEmpty))
      def filter(pre: Fields) = applies(knownFields.slice(0, pre.size))(pre)
      val filteredPres = pres.filter(filter)
      def generator(pre: Fields) = generateFieldsAcc(knownFields.slice(pre.size, knownFields.size), blocks.tail, acc ++ pre)
      val nextss = filteredPres.map(generator)
      nextss.flatten
    }
  }

  val ? = Unknown
  val X = Full
  val E = Empty

  @Test
  def generateFields_allUnknown: Unit = {
    generateFields(Array(?, ?, ?, ?, ?), Array(2, 1)).toList.map(_.toList) ==> List(List(X, X, E, X, E), List(X, X, E, E, X), List(E, X, X, E, X))
    generateFields(Array(?, ?, ?, ?, ?), Array(1, 2)).toList.map(_.toList) ==> List(List(X, E, X, X, E), List(X, E, E, X, X), List(E, X, E, X, X))
    generateFields(Array(?, ?, ?, ?), Array(1, 2)).map(_.toList) ==> Stream(List(X, E, X, X))
    generateFields(Array(?, ?, ?, ?), Array(1, 1)).toList.map(_.toList) ==> List(List(X, E, X, E), List(X, E, E, X), List(E, X, E, X))
    generateFields(Array(?, ?, ?), Array(1, 1)).map(_.toList) ==> Stream(List(X, E, X))
    generateFields(Array(?, ?, ?), Array(1)).map(_.toList) ==> Stream(List(X, E, E), List(E, X, E), List(E, E, X))
    generateFields(Array(?, ?), Array(1)).map(_.toList) ==> Stream(List(X, E), List(E, X))
    generateFields(Array(?, ?), Array()).map(_.toList) ==> Stream(List(E, E))
    generateFields(Array(?, ?), Array(2)).map(_.toList) ==> Stream(List(X, X))
    generateFields(Array(?), Array()).map(_.toList) ==> Stream(List(E))
    generateFields(Array(?), Array(1)).map(_.toList) ==> Stream(List(X))
  }

  @Test
  def generateFields_Known: Unit = {
    generateFields(Array(?, ?, ?, ?, ?), Array(2, 1)).toList.map(_.toList) ==> List(List(X, X, E, X, E), List(X, X, E, E, X), List(E, X, X, E, X))
    generateFields(Array(?, ?, ?, ?, ?), Array(1, 2)).toList.map(_.toList) ==> List(List(X, E, X, X, E), List(X, E, E, X, X), List(E, X, E, X, X))
    generateFields(Array(?, ?, ?, ?), Array(1, 2)).map(_.toList) ==> Stream(List(X, E, X, X))
    generateFields(Array(?, ?, ?, ?), Array(1, 1)).toList.map(_.toList) ==> List(List(X, E, X, E), List(X, E, E, X), List(E, X, E, X))
    generateFields(Array(?, ?, ?), Array(1, 1)).map(_.toList) ==> Stream(List(X, E, X))
    generateFields(Array(?, ?, ?), Array(1)).map(_.toList) ==> Stream(List(X, E, E), List(E, X, E), List(E, E, X))

    generateFields(Array(X, E), Array(1)).map(_.toList) ==> Stream(List(X, E))
    generateFields(Array(E, X), Array(1)).map(_.toList) ==> Stream(List(E, X))
    generateFields(Array(E, ?), Array(1)).map(_.toList) ==> Stream(List(E, X))
    generateFields(Array(X, ?), Array(1)).map(_.toList) ==> Stream(List(X, E))
    generateFields(Array(?, E), Array(1)).map(_.toList) ==> Stream(List(X, E))
    generateFields(Array(?, X), Array(1)).map(_.toList) ==> Stream(List(E, X))
    generateFields(Array(X, E), Array()).map(_.toList) ==> Stream()
    generateFields(Array(E, X), Array()).map(_.toList) ==> Stream()
    generateFields(Array(E, ?), Array()).map(_.toList) ==> Stream(List(E, E))
    generateFields(Array(X, ?), Array()).map(_.toList) ==> Stream()
    generateFields(Array(?, E), Array()).map(_.toList) ==> Stream(List(E, E))
    generateFields(Array(?, X), Array()).map(_.toList) ==> Stream()
    generateFields(Array(X, E), Array(2)).map(_.toList) ==> Stream()
    generateFields(Array(E, X), Array(2)).map(_.toList) ==> Stream()
    generateFields(Array(E, ?), Array(2)).map(_.toList) ==> Stream()
    generateFields(Array(X, ?), Array(2)).map(_.toList) ==> Stream(List(X, X))
    generateFields(Array(?, E), Array(2)).map(_.toList) ==> Stream()
    generateFields(Array(?, X), Array(2)).map(_.toList) ==> Stream(List(X, X))
    generateFields(Array(E), Array()).map(_.toList) ==> Stream(List(E))
    generateFields(Array(X), Array()).map(_.toList) ==> Stream()
    generateFields(Array(E), Array(1)).map(_.toList) ==> Stream()
    generateFields(Array(X), Array(1)).map(_.toList) ==> Stream(List(X))
  }

  @Test
  def testBig: Unit = {
    generateFields(Array.fill(45)(?), Array(32)).size ==> 14
    reduce(Array.fill(45)(?), Array(32)).map(_.toList) ==> Option(List.fill(13)(?) ++ List.fill(19)(X) ++ List.fill(13)(?))
    reduce(Array(X) ++ Array.fill(44)(?), Array(32)).map(_.toList) ==> Option(List.fill(32)(X) ++ List.fill(13)(E))
    reduce(Array(?) ++ Array(X) ++ Array.fill(43)(?), Array(32)).map(_.toList) ==> Option(List.fill(1)(?) ++ List.fill(31)(X) ++ List.fill(1)(?) ++ List.fill(12)(E))
  }

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