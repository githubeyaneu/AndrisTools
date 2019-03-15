package eu.eyan.japan

import org.junit.Test
import eu.eyan.testutil.TestPlus
import eu.eyan.log.Log
import org.junit.Ignore
import scala.annotation.tailrec

class Japan2 extends TestPlus {

  def reduce(knownFields: Seq[FieldType], blocks: Seq[Int]): Option[Seq[FieldType]] = {
    val stream = generateFields(knownFields, blocks)
    if (stream.isEmpty) None
    else Option(stream.reduce(reduceFields))
  }

  def reduceFields(cumulated: Seq[FieldType], next: Seq[FieldType]) = {
    for (idx <- 0 until cumulated.size) yield {
      //assert(knownFields(idx) == Unknown || knownFields(idx) == next(idx))
      val cumIdx = cumulated(idx)
      val nextIdx = next(idx)
      if (cumIdx == Unknown) Unknown
      else if (cumIdx == nextIdx) cumIdx
      else Unknown
    }
  }

  def applies(known: FieldType, field: FieldType): Boolean = known == Unknown || known == field

  def applies(knownFields: Seq[FieldType], fields: Seq[FieldType]): Boolean = {
    //assert(knownFields.size == fields.size)
    val oneFieldNotApplyable = knownFields.zip(fields).exists { case (known, field) => !applies(known, field) }
    !oneFieldNotApplyable
  }

  implicit class IntToFieldType(int: Int) {
    def *(fieldType: FieldType) = fieldType(int)
  }

  def generateFields(knownFields: Seq[FieldType], blocks: Seq[Int]): Stream[Seq[FieldType]] = {
    //    assert(knownFields.size <= lt)
    //    assert(blocks.sum + blocks.size - 1 <= lt)

    val length = knownFields.size
    val remainingEmptySpace = length - (blocks.sum + blocks.size - 1)

    if (blocks.size == 0) {
      val empties = length * Empty
      if (applies(knownFields, empties)) Stream(empties)
      else Stream.Empty
    } else {
      val nextss = (0 to remainingEmptySpace).map(remainingEmptySpace => {
        val pre = remainingEmptySpace * Empty ++ blocks.head * Full ++ (if (blocks.size < 2) Seq() else 1 * Empty)
        val nextKnown = knownFields.slice(pre.size, knownFields.size)
        val posts =
          if (applies(knownFields.slice(0, pre.size), pre)) generateFields(nextKnown, blocks.tail)
          else Stream.Empty
        posts.map(post => pre ++ post)
      })

      nextss.toStream.flatten
    }
  }

  val ? = Unknown
  val X = Full
  val E = Empty

  @Test
  def generateFields_Known: Unit = {
    //    generateFields(5, List(?, ?, ?, ?, ?), List(2, 1)).toList ==> List(List(X, X, E, X, E), List(X, X, E, E, X), List(E, X, X, E, X))
    //    generateFields(5, List(?, ?, ?, ?, ?), List(1, 2)).toList ==> List(List(X, E, X, X, E), List(X, E, E, X, X), List(E, X, E, X, X))
    //    generateFields(4, List(?, ?, ?, ?), List(1, 2)) ==> Stream(List(X, E, X, X))
    //    generateFields(4, List(?, ?, ?, ?), List(1, 1)).toList ==> List(List(X, E, X, E), List(X, E, E, X), List(E, X, E, X))
    //    generateFields(3, List(?, ?, ?), List(1, 1)) ==> Stream(List(X, E, X))
    //    generateFields(3, List(?, ?, ?), List(1)) ==> Stream(List(X, E, E), List(E, X, E), List(E, E, X))

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