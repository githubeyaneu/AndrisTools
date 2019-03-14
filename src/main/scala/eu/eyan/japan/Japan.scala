package eu.eyan.japan

import eu.eyan.util.string.StringPlus.StringPlusImplicit
import javax.swing.JFrame
import eu.eyan.util.swing.JFramePlus.JFramePlusImplicit
import eu.eyan.util.swing.JPanelWithFrameLayout
import javax.swing.JLabel
import com.jgoodies.forms.factories.CC

object Japan extends App {

  def extraEmptySpaceKombinations(size: Int, tiles: List[Int]) = generate(size - tiles.sum - tiles.size + 1, tiles.size + 1)
  def generate(items: Int, remainingPlaces: Int): Seq[List[Int]] = {
    if (remainingPlaces == 1) Seq(List(items))
    else {
      val x = (0 to items).map(i => generate(items - i, remainingPlaces - 1))
      val fx = (0 to items).zip(x)
      val lists = fx.map(tuple => {
        val i = tuple._1
        val lis = tuple._2
        lis.map(i :: _)
      })
      val y = lists.flatten
      y
    }
  }

  def toFields(size: Int, tiles: List[Int])(emptySpaces: List[Int]): List[FieldType] = {
    val fulls = tiles.map(i => List.fill(i)(Full))
    val empties = emptySpaces.map(i => List.fill(i)(Empty))
    val inds = (0 until tiles.size + emptySpaces.size).toList
    val ll = inds.map(i => if (i % 2 == 0) empties(i / 2) else fulls(i / 2) ::: (if (i / 2 < fulls.size - 1) List[FieldType](Empty) else List[FieldType]()))
    val ret = ll.flatten
    assert(size == ret.size)
    ret
  }

  //val eesk = extraEmptySpaceKombinations(7, List(1, 2))
  //println(eesk.mkString("\r\n"))
  //val eesk2 = extraEmptySpaceKombinations(70, List(2,2,3,6,2,2,2,6,3,2,2))
  //println(eesk2.size)

  trait FieldType {
    def apply(length: Int) = Seq.fill[FieldType](length)(this)
    def *(length:Int) = apply(length)
    
    def applyA(length: Int) = Array.fill[FieldType](length)(this)
    def **(length:Int) = applyA(length)
  }
  case object Full extends FieldType { override def toString() = "X" }
  case object Empty extends FieldType { override def toString() = "_" }
  case object Unknown extends FieldType { override def toString() = " " }

  //  val size = 45
  ////  val fulls = List(4, 3, 9, 10, 3)
  //  val fulls = List(20,20)
  //  val ees = extraEmptySpaceKombinations(size, fulls)
  //  val fieldss = ees.map(toFields(size, fulls))
  //  val countss = fieldss.foldLeft(List.fill(size)(0))((counts, fields) => counts.zip(fields.map(_.value)).map(t=>t._1+t._2))
  //
  //  println(fieldss.mkString("\r\n"))
  //  println(countss)
  //  println(countss.map(ct => if(ct==fieldss.size) Full else if(ct==0) Empty else Unknown))
  //
  //  val known = List(Unknown ,Full  ,Unknown  ,Unknown  , Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown,Unknown  , Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Unknown, Full, Unknown)
  //  assert(size==known.size)
  //  println
  //  println(known)
  //  println
  //  val filtered = fieldss.filter(fields =>{
  //    val bools = (0 until fields.size).toList.map(index =>{
  //      known(index) match{
  //        case Full => fields(index) == Full
  //        case Empty => fields(index) == Empty
  //        case _ => true
  //      }
  //    })
  //    !bools.exists( b => !b)
  //  })
  //  println(filtered.mkString("\r\n"))
  //  val countssFilteres = filtered.foldLeft(List.fill(size)(0))((counts, fields) => counts.zip(fields.map(_.value)).map(t=>t._1+t._2))
  //  println(countssFilteres)
  //  println(countssFilteres.map(ct => if(ct==filtered.size) Full else if(ct==0) Empty else Unknown))
}