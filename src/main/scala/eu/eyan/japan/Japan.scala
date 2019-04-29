package eu.eyan.japan

object Japan {
  case class TableBlocks(colBlocks: ColBlocks, rowBlocks: RowBlocks)
  case class ColBlocks(colBlocks: Array[Blocks])
  case class RowBlocks(rowBlocks: Array[Blocks])
  case class Table(fields: Array[Array[Field]])
  type Field = FieldType // TODO rename FieldType to Field
  trait Gui {
    def setCellAsKnown(cell: Cell)(value: Field)
    def setCellAsCandidate(cell: Cell)(value: Field)
  }
  case class Cell(col: Col, row: Row)
  
  
  
  
  type Fields = Seq[FieldType]
  type Blocks = List[Int]
  type Lines = Seq[RowOrCol]
}
trait RowOrCol
case class Col(x: Int) extends RowOrCol { override def toString = "c" + x }
case class Row(y: Int) extends RowOrCol { override def toString = "r" + y }
case class ColRow(col: Col, row: Row)

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


