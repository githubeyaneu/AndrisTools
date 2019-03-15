package eu.eyan.japan


trait FieldType {
  type Fields = Seq[FieldType]
  
  def apply(length: Int) = Seq.fill[FieldType](length)(this)
  def *(length: Int) = apply(length)

  def applyA(length: Int) = Array.fill[FieldType](length)(this)
  def **(length: Int) = applyA(length)
}

case object Full extends FieldType { override def toString() = "X" }
case object Empty extends FieldType { override def toString() = "_" }
case object Unknown extends FieldType { override def toString() = " " }