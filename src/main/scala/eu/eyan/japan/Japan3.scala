package eu.eyan.japan

import org.junit.Test
import eu.eyan.testutil.TestPlus
import eu.eyan.japan.Japan.FieldType
import eu.eyan.japan.Japan.Unknown
import eu.eyan.japan.Japan.Full
import eu.eyan.japan.Japan.Empty
import eu.eyan.log.Log
import org.junit.Ignore
import scala.annotation.tailrec
import eu.eyan.japan.Japan.FieldType
import eu.eyan.japan.Japan.FieldType

class Japan3 extends TestPlus{

  private var cancelled = false
  def cancel = cancelled = true
  
  type Fields = Seq[FieldType]
  
  def reduce(knownFields: Fields, blocks: Seq[Int]): Option[Fields] = {
    val stream = generateFields(knownFields, blocks)
    if (stream.isEmpty) None
    else Option(stream.reduce(reduceFields))
  }

  def reduceFields(cumulated: Fields, next: Fields) = {
    def reduceFiledsSub(cumAndNext: Tuple2[FieldType, FieldType])={
      if (cumAndNext._1 == Unknown) Unknown
      else if (cumAndNext._1 == cumAndNext._2) cumAndNext._1
      else Unknown
    }
    cumulated.zip(next).map(reduceFiledsSub)
  }

  def applies(known: FieldType, field: FieldType): Boolean = known == Unknown || known == field

  def applies(knownFields: Fields)(fields: Fields): Boolean = {
    def appliesSub(tuple: Tuple2[FieldType, FieldType])=  !applies(tuple._1, tuple._2)
    val oneFieldNotApplyable = knownFields.zip(fields).exists(appliesSub)
    !oneFieldNotApplyable
  }

  implicit class IntToFieldType(int: Int) {
    def *(fieldType: FieldType) = fieldType(int)
  }

  def generateFields(knownFields: Fields, blocks: Seq[Int]): Stream[Fields] = {
    generateFieldsAcc(knownFields, blocks, Seq())
  }
  
  def empties(nr: Int) = Empty(nr)

//  @tailrec
  def generateFieldsAcc(knownFields: Fields, blocks: Seq[Int], acc: Fields): Stream[Fields] = {
    val length = knownFields.size
    val remainingEmptySpace = length - (blocks.sum + blocks.size - 1)
    if(cancelled) Stream.Empty
    else if (blocks.size == 0) {
      val empties = Empty(length)
      if (applies(knownFields)(empties)) Stream(acc ++ empties)
      else Stream.Empty
    } else {
      val preEmptySpaces = Stream.range(0, remainingEmptySpace + 1, 1).map(empties)
      val blockFollowingEmpty = blocks.head * Full ++ (if (blocks.size < 2) Seq() else 1 * Empty)
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
    generateFields(List(?, ?, ?, ?, ?), List(2, 1)).toList ==> List(List(X, X, E, X, E), List(X, X, E, E, X), List(E, X, X, E, X))
    generateFields(List(?, ?, ?, ?, ?), List(1, 2)).toList ==> List(List(X, E, X, X, E), List(X, E, E, X, X), List(E, X, E, X, X))
    generateFields(List(?, ?, ?, ?), List(1, 2)) ==> Stream(List(X, E, X, X))
    generateFields(List(?, ?, ?, ?), List(1, 1)).toList ==> List(List(X, E, X, E), List(X, E, E, X), List(E, X, E, X))
    generateFields(List(?, ?, ?), List(1, 1)) ==> Stream(List(X, E, X))
    generateFields(List(?, ?, ?), List(1)) ==> Stream(List(X, E, E), List(E, X, E), List(E, E, X))
    generateFields(List(?, ?), List(1)) ==> Stream(List(X, E), List(E, X))
    generateFields(List(?, ?), List()) ==> Stream(List(E, E))
    generateFields(List(?, ?), List(2)) ==> Stream(List(X, X))
    generateFields(List(?), List()) ==> Stream(List(E))
    generateFields(List(?), List(1)) ==> Stream(List(X))
  }

  @Test
  def generateFields_Known: Unit = {
    generateFields(List(?, ?, ?, ?, ?), List(2, 1)).toList ==> List(List(X, X, E, X, E), List(X, X, E, E, X), List(E, X, X, E, X))
    generateFields(List(?, ?, ?, ?, ?), List(1, 2)).toList ==> List(List(X, E, X, X, E), List(X, E, E, X, X), List(E, X, E, X, X))
    generateFields(List(?, ?, ?, ?), List(1, 2)) ==> Stream(List(X, E, X, X))
    generateFields(List(?, ?, ?, ?), List(1, 1)).toList ==> List(List(X, E, X, E), List(X, E, E, X), List(E, X, E, X))
    generateFields(List(?, ?, ?), List(1, 1)) ==> Stream(List(X, E, X))
    generateFields(List(?, ?, ?), List(1)) ==> Stream(List(X, E, E), List(E, X, E), List(E, E, X))

    generateFields(List(X, E), List(1)) ==> Stream(List(X, E))
    generateFields(List(E, X), List(1)) ==> Stream(List(E, X))
    generateFields(List(E, ?), List(1)) ==> Stream(List(E, X))
    generateFields(List(X, ?), List(1)) ==> Stream(List(X, E))
    generateFields(List(?, E), List(1)) ==> Stream(List(X, E))
    generateFields(List(?, X), List(1)) ==> Stream(List(E, X))

    generateFields(List(X, E), List()) ==> Stream()
    generateFields(List(E, X), List()) ==> Stream()
    generateFields(List(E, ?), List()) ==> Stream(List(E, E))
    generateFields(List(X, ?), List()) ==> Stream()
    generateFields(List(?, E), List()) ==> Stream(List(E, E))
    generateFields(List(?, X), List()) ==> Stream()

    generateFields(List(X, E), List(2)) ==> Stream()
    generateFields(List(E, X), List(2)) ==> Stream()
    generateFields(List(E, ?), List(2)) ==> Stream()
    generateFields(List(X, ?), List(2)) ==> Stream(List(X, X))
    generateFields(List(?, E), List(2)) ==> Stream()
    generateFields(List(?, X), List(2)) ==> Stream(List(X, X))

    generateFields(List(E), List()) ==> Stream(List(E))
    generateFields(List(X), List()) ==> Stream()

    generateFields(List(E), List(1)) ==> Stream()
    generateFields(List(X), List(1)) ==> Stream(List(X))
  }

  @Test
  def testBig: Unit = {
    generateFields(List.fill(45)(?), List(32)).size ==> 14
    reduce(List.fill(45)(?), List(32)) ==> Option(List.fill(13)(?) ++ List.fill(19)(X) ++ List.fill(13)(?))
    reduce(List(X) ++ List.fill(44)(?), List(32)) ==> Option(List.fill(32)(X) ++ List.fill(13)(E))
    reduce(List(?) ++ List(X) ++ List.fill(43)(?), List(32)) ==> Option(List.fill(1)(?) ++ List.fill(31)(X) ++ List.fill(1)(?) ++ List.fill(12)(E))
  }

  @Test
  def testReduce: Unit = {
    reduce(List(?, ?, ?, ?, ?), List(2, 1)) ==> Option(List(?, X, ?, ?, ?))
    reduce(List(?, ?, ?, ?, ?), List(1, 2)) ==> Option(List(?, ?, ?, X, ?))
    reduce(List(?, ?, ?, ?), List(1, 2)) ==> Option(List(X, E, X, X))
    reduce(List(?, ?, ?, ?), List(1, 1)) ==> Option(List(?, ?, ?, ?))
    reduce(List(?, ?, ?), List(1, 1)) ==> Option(List(X, E, X))
    reduce(List(?, ?, ?), List(1)) ==> Option(List(?, ?, ?))

    reduce(List(X, E), List(1)) ==> Option(List(X, E))
    reduce(List(E, X), List(1)) ==> Option(List(E, X))
    reduce(List(E, ?), List(1)) ==> Option(List(E, X))
    reduce(List(X, ?), List(1)) ==> Option(List(X, E))
    reduce(List(?, E), List(1)) ==> Option(List(X, E))
    reduce(List(?, X), List(1)) ==> Option(List(E, X))
    reduce(List(?, ?), List(1)) ==> Option(List(?, ?))

    reduce(List(X, E), List()) ==> None
    reduce(List(E, X), List()) ==> None
    reduce(List(E, ?), List()) ==> Option(List(E, E))
    reduce(List(X, ?), List()) ==> None
    reduce(List(?, E), List()) ==> Option(List(E, E))
    reduce(List(?, X), List()) ==> None
    reduce(List(?, ?), List()) ==> Option(List(E, E))

    reduce(List(X, E), List(2)) ==> None
    reduce(List(E, X), List(2)) ==> None
    reduce(List(E, ?), List(2)) ==> None
    reduce(List(X, ?), List(2)) ==> Option(List(X, X))
    reduce(List(?, E), List(2)) ==> None
    reduce(List(?, X), List(2)) ==> Option(List(X, X))
    reduce(List(?, ?), List(2)) ==> Option(List(X, X))

    reduce(List(E), List()) ==> Option(List(E))
    reduce(List(X), List()) ==> None
    reduce(List(?), List()) ==> Option(List(E))

    reduce(List(E), List(1)) ==> None
    reduce(List(X), List(1)) ==> Option(List(X))
    reduce(List(?), List(1)) ==> Option(List(X))
  }
}