package eu.eyan.amoba

object HaromHarom extends App {
  class Field(t: String)
  case object X extends Field("X")
  case object Y extends Field("Y")
  case object E extends Field(" ")
  case class Table(fields: List[Field]) {
    //    def row(idx: Int) = Lis
    override def toString = fields.sliding(3,3).map(_.mkString).mkString("\r\n")
  }

  val indices = List(0, 1, 2, 3, 4, 5, 6, 7, 8)

  val tercs = List(
    List(0, 1, 2), List(3, 4, 5), List(6, 7, 8),
    List(0, 3, 6), List(1, 4, 7), List(2, 5, 8),
    List(0, 4, 8), List(2, 4, 6))

  val tables = nextStep(X, Set(Table(List(E, E, E, E, E, E, E, E, E))))

  def nextStep(step: Field, tables: Set[Table]) = {
    println(tables.mkString("\r\n\r\n"))
//    val nexts = for(table <- tables) yield nextStep(step, table)
  }
  
  def nextStep(step: Field, table: Table):Set[Table] = {
    Set()//FIXME
  }

}