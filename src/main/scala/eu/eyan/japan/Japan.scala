package eu.eyan.japan

object Japan {
  type Fields = Array[FieldType]
  type Blocks = List[Int]
  type Lines = Seq[Line]
  type Table = Array[Array[FieldType]]
}


trait Line{
  def ifRowOrCol[T](ifRow: Int => T, ifCol: Int => T) = this match {
      case Col(x) => ifCol(x)
      case Row(y) => ifRow(y)
    }
}
case class Col(x: Int) extends Line { override def toString = "c" + x }
case class Row(y: Int) extends Line { override def toString = "r" + y }
case class ColRow(col: Col, row: Row)

case class Width(w:Int)
case class Height(h:Int)

trait FieldType {
  def apply(length: Int) = Seq.fill[FieldType](length)(this)
  def *(length: Int) = apply(length)

  def applyA(length: Int) = Array.fill[FieldType](length)(this)
  def **(length: Int) = applyA(length)
}

case object Full extends FieldType { override def toString() = "◘" }
case object Empty extends FieldType { override def toString() = " " }
//case object Full extends FieldType { override def toString() = "X" }
//case object Empty extends FieldType { override def toString() = "E" }
case object Unknown extends FieldType { override def toString() = "?" }