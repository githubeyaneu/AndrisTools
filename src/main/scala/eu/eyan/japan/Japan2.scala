package eu.eyan.japan

import org.junit.Test
import eu.eyan.testutil.TestPlus
import eu.eyan.japan.Japan.FieldType
import eu.eyan.japan.Japan.Unknown
import eu.eyan.japan.Japan.Full
import eu.eyan.japan.Japan.Empty
import eu.eyan.log.Log
import org.junit.Ignore

class Japan2 extends TestPlus {
  def reduce(knownFields: List[FieldType], blocks: List[Int]): Option[List[FieldType]] = {
//    println("-------------------------")
//    println("known=" + knownFields)
//    println("blocks=" + blocks)
    val length = knownFields.size
    val stream = generateFields(length, knownFields, blocks)
//    println("generated=" + stream.toList)
    if (stream.isEmpty) None
    else {
      val reducedList = stream.reduce((cumulated, next) => {
        val nextCumulated = for (idx <- 0 until length) yield {
          assert(knownFields(idx) == Unknown || knownFields(idx) == next(idx))
          val cumIdx = cumulated(idx)
          val nextIdx = next(idx)
          if (cumIdx == Unknown) Unknown
          else if (cumIdx == nextIdx) cumIdx
          else Unknown
        }
        nextCumulated.toList
      })
      Option(reducedList)
    }
  }

  def generateFields(lt: Int, knownFields: List[FieldType], blocks: List[Int]): Stream[List[FieldType]] = {
    def log(m: String): Unit = {} //println((" " * (lt - knownFields.size)) + m)

    def applies(knownFields: List[FieldType], fields: List[FieldType]) = {
      log("knownFields=" + knownFields + ", fields=" + fields)
      assert(knownFields.size == fields.size)
      def applies(known: FieldType, field: FieldType) = known == Unknown || known == field
      val oneFieldNotApplyable = knownFields.zip(fields).exists { case (known, field) => !applies(known, field) }
      !oneFieldNotApplyable
    }
    assert(knownFields.size <= lt)
    assert(blocks.sum + blocks.size - 1 <= lt)

    log("***************************************")
    log(knownFields + " " + blocks)
    val length = knownFields.size
    val remainingEmptySpace = length - (blocks.sum + blocks.size - 1)
    log("length=" + length + " remainingEmptySpace=" + remainingEmptySpace)

    val ret = if (blocks.size == 0) {
      log("-> blocks.size == 0")
      val empties = List.fill(length)(Empty)
      if (applies(knownFields, empties)) Stream(empties)
      else Stream()
    } else {
      log("-> generate")
      val nextss = (0 to remainingEmptySpace).map(remainingEmptySpace => {
        log("gennext remainingEmptySpace=" + remainingEmptySpace)
        val pre = List.fill(remainingEmptySpace)(Empty) ++ List.fill(blocks.head)(Full) ++ (if (blocks.size < 2) List() else List(Empty))
        log("pre=" + pre)
        val nextKnown = knownFields.slice(pre.size, knownFields.size)
        log("nextKnown=" + nextKnown)
        val posts =
          if (applies(knownFields.slice(0, pre.size), pre)) generateFields(length, nextKnown, blocks.tail)
          else Stream()
        posts.map(post => pre ++ post)
      })

      nextss.toStream.flatten
    }
    log("***************************************")

    ret
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

    generateFields(2, List(X, E), List(1)) ==> Stream(List(X, E))
    generateFields(2, List(E, X), List(1)) ==> Stream(List(E, X))
    generateFields(2, List(E, ?), List(1)) ==> Stream(List(E, X))
    generateFields(2, List(X, ?), List(1)) ==> Stream(List(X, E))
    generateFields(2, List(?, E), List(1)) ==> Stream(List(X, E))
    generateFields(2, List(?, X), List(1)) ==> Stream(List(E, X))

    generateFields(2, List(X, E), List()) ==> Stream()
    generateFields(2, List(E, X), List()) ==> Stream()
    generateFields(2, List(E, ?), List()) ==> Stream(List(E, E))
    generateFields(2, List(X, ?), List()) ==> Stream()
    generateFields(2, List(?, E), List()) ==> Stream(List(E, E))
    generateFields(2, List(?, X), List()) ==> Stream()

    generateFields(2, List(X, E), List(2)) ==> Stream()
    generateFields(2, List(E, X), List(2)) ==> Stream()
    generateFields(2, List(E, ?), List(2)) ==> Stream()
    generateFields(2, List(X, ?), List(2)) ==> Stream(List(X, X))
    generateFields(2, List(?, E), List(2)) ==> Stream()
    generateFields(2, List(?, X), List(2)) ==> Stream(List(X, X))

    generateFields(1, List(E), List()) ==> Stream(List(E))
    generateFields(1, List(X), List()) ==> Stream()

    generateFields(1, List(E), List(1)) ==> Stream()
    generateFields(1, List(X), List(1)) ==> Stream(List(X))
  }

  @Test
  def generateFields_allUnknown: Unit = {
    generateFields(5, List(?, ?, ?, ?, ?), List(2, 1)).toList ==> List(List(X, X, E, X, E), List(X, X, E, E, X), List(E, X, X, E, X))
    generateFields(5, List(?, ?, ?, ?, ?), List(1, 2)).toList ==> List(List(X, E, X, X, E), List(X, E, E, X, X), List(E, X, E, X, X))
    generateFields(4, List(?, ?, ?, ?), List(1, 2)) ==> Stream(List(X, E, X, X))
    generateFields(4, List(?, ?, ?, ?), List(1, 1)).toList ==> List(List(X, E, X, E), List(X, E, E, X), List(E, X, E, X))
    generateFields(3, List(?, ?, ?), List(1, 1)) ==> Stream(List(X, E, X))
    generateFields(3, List(?, ?, ?), List(1)) ==> Stream(List(X, E, E), List(E, X, E), List(E, E, X))
    generateFields(2, List(?, ?), List(1)) ==> Stream(List(X, E), List(E, X))
    generateFields(2, List(?, ?), List()) ==> Stream(List(E, E))
    generateFields(2, List(?, ?), List(2)) ==> Stream(List(X, X))
    generateFields(1, List(?), List()) ==> Stream(List(E))
    generateFields(1, List(?), List(1)) ==> Stream(List(X))
  }

  @Test
  def testBig: Unit = {
    generateFields(45, List.fill(45)(?), List(32)).size ==> 14
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