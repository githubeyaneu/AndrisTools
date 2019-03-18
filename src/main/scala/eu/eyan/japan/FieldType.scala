package eu.eyan.japan


object Japan {
  type Fields = Seq[FieldType]
  type Blocks = List[Int]
}
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