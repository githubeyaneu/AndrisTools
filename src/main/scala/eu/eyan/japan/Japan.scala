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

  trait FieldType { def value: Int }
  case object Full extends FieldType { override def toString() = "X"; def value = 1 }
  case object Empty extends FieldType { override def toString() = " "; def value = 0 }
  case object Unknown extends FieldType { override def toString() = "?"; def value = ??? }

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
  val up = """C:\DEVELOPING_1\projects\AndrisTools\src\main\scala\eu\eyan\japan\fent.txt"""
  val left = """C:\DEVELOPING_1\projects\AndrisTools\src\main\scala\eu\eyan\japan\bal.txt"""
  val ups = up.linesFromFile.toList.map(_.split("\t").toList.map(_.toInt))
  val lefts = left.linesFromFile.toList.map(_.split("\t").toList.map(_.toInt))

  println(ups.mkString("\r\n"))
  println(lefts.mkString("\r\n"))

  case class JapanTable(lefts: List[List[Int]], ups: List[List[Int]]) {
    lazy val width = ups.size
    lazy val height = lefts.size
    lazy val upsMax = ups.map(_.size).max
    lazy val leftsMax = lefts.map(_.size).max
  }

  val japan = JapanTable(lefts, ups)

  val panel = new JPanelWithFrameLayout()
  val cols = japan.width + japan.leftsMax
  val rows = japan.height + japan.upsMax
  for (x <- 0 to cols) panel.newColumn("15px")
  for (x <- 0 to rows) panel.newRow("15px")

  for (x <- 0 until japan.width; nums = japan.ups(x); idx <- 0 until nums.size) {
    val col = japan.leftsMax + x + 1
    val row = japan.upsMax - nums.size + idx + 1
    println((col, row))
    panel.add(new JLabel("" + nums(idx)), CC.xy(col, row))
  }
  
  for (y <- 0 until japan.height; nums = japan.lefts(y); idx <- 0 until nums.size) {
	  val row = japan.upsMax + y + 1
			  val col = japan.leftsMax - nums.size + idx + 1
			  println((col, row))
			  panel.add(new JLabel("" + nums(idx)), CC.xy(col, row))
  }

  for (col <- japan.leftsMax + 1 to cols; row <- japan.upsMax + 1 to rows)
    panel.add(new JLabel("?"), CC.xy(col, row))

  new JFrame().withComponent(panel).onCloseExit.packAndSetVisible

}