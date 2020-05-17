package eu.eyan.sudoku

import eu.eyan.util.time.Timer

object Sudoku extends App{
  case class Col(col:Int){
    def block = ((col-1)/3)*3
  }
  case class Row(row:Int){
    def block = ((row-1)/3)*3
  }
  case class Pos(col: Col, row: Row)
  def cols = (1 to 9).map(Col(_)).toStream
  def rows = (1 to 9).map(Row(_)).toStream
  
  case class Grid(private val grid: Array[Array[Int]]){
    override def toString = grid.map(_.mkString(" ")).mkString("\r\n")+"\r\n"
    def get(pos:Pos) = grid(pos.row.row-1)(pos.col.col-1)
    def set(pos:Pos, n: Int) = grid(pos.row.row-1)(pos.col.col-1) = n
    def firstEmptyRow:Option[Row]={
        val rowIndex = grid.indexWhere(_.contains(0))
        if(rowIndex > -1) Option(Row(rowIndex+1))
        else Option.empty
    }
    def firstEmptyPos:Option[Pos] = firstEmptyRow.flatMap(row=> {
        val colIndex = grid(row.row-1).indexWhere(_ == 0)
        if(colIndex > -1) Option(Pos(Col(colIndex+1), row))
        else Option.empty
      } )
  }
  
  val grid = Grid(Array(
    Array(5,3,0,0,7,0,0,0,0),
    Array(6,0,0,1,9,5,0,0,0),
    Array(0,9,8,0,0,0,0,6,0),
    
    Array(8,0,0,0,6,0,0,0,3),
    Array(4,0,0,8,0,3,0,0,1),
    Array(7,0,0,0,2,0,0,0,6),
    
    Array(0,6,0,0,0,0,2,8,0),
    Array(0,0,0,4,1,9,0,0,5),
    Array(0,0,0,0,8,0,0,7,9),
  ))
  
  val grid2 = Grid(Array(
    Array(5,3,0,0,7,0,0,0,0),
    Array(6,0,0,1,9,5,0,0,0),
    Array(0,9,8,0,0,0,0,6,0),
    
    Array(8,0,0,0,6,0,0,0,3),
    Array(4,0,0,8,0,3,0,0,1),
    Array(7,0,0,0,2,0,0,0,6),
    
    Array(0,6,0,0,0,0,2,8,0),
    Array(0,0,0,4,1,9,0,0,5),
    Array(0,0,0,0,8,0,0,0,0),
  ))
  
  
  def possible(grid: Grid, pos:Pos, n:Int):Boolean={
    def colContainsN = cols.map(col => grid.get(Pos(col, pos.row))==n)
    def rowContainsN = rows.map(row => grid.get(Pos(pos.col, row))==n)
    def blockContainsN = (for(i<- 1 to 3; j<- 1 to 3) yield grid.get(Pos(Col(pos.col.block+j),Row(pos.row.block+i)))==n).toStream 
    !(colContainsN #::: rowContainsN #::: blockContainsN).contains(true)
  }
  
  def solve(grid:Grid):Unit = 
    grid.firstEmptyPos.map{emptyPos =>
      for(n <- 1 to 9;if (possible(grid, emptyPos,n))){
        grid.set(emptyPos, n)
        solve(grid) 
        grid.set(emptyPos, 0)
      }}.orElse({println(grid); None})
  
  Timer.time(solve(grid))
  Timer.time(solve(grid2))
}
