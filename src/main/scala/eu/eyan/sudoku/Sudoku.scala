package eu.eyan.sudoku

object Sudoku extends App{
  case class Col(col:Int){
    def block = ((col-1)/3)*3
  }
  case class Row(row:Int){
    def block = ((row-1)/3)*3
  }
  val cols = (1 to 9).map(Col(_)).toList
  val rows = (1 to 9).map(Row(_)).toList
  
  case class Grid(private val grid: Array[Array[Int]]){
	  override def toString = grid.map(_.mkString(" ")).mkString("\r\n")+"\r\n"
	  def get(row: Row, col: Col) = grid(row.row-1)(col.col-1)
	  def set(row: Row, col: Col, n: Int) = grid(row.row-1)(col.col-1) = n
  }
  
  val grid = Grid(Array(
  Array(5,3,0,  0,7,0,  0,0,0),    
  Array(6,0,0,  1,9,5,  0,0,0),    
  Array(0,9,8,  0,0,0,  0,6,0),
  
  Array(8,0,0,  0,6,0,  0,0,3),    
  Array(4,0,0,  8,0,3,  0,0,1),    
  Array(7,0,0,  0,2,0,  0,0,6),
  
  Array(0,6,0,  0,0,0,  2,8,0),
  Array(0,0,0,  4,1,9,  0,0,5),    
  Array(0,0,0,  0,8,0,  0,7,9),    
  ))

  val grid2 = Grid(Array(
				  Array(5,3,0,  0,7,0,  0,0,0),    
				  Array(6,0,0,  1,9,5,  0,0,0),    
				  Array(0,9,8,  0,0,0,  0,6,0),
				  
				  Array(8,0,0,  0,6,0,  0,0,3),    
				  Array(4,0,0,  8,0,3,  0,0,1),    
				  Array(7,0,0,  0,2,0,  0,0,6),
				  
				  Array(0,6,0,  0,0,0,  2,8,0),
				  Array(0,0,0,  4,1,9,  0,0,5),    
				  Array(0,0,0,  0,8,0,  0,0,0),    
				  ))
  
  
  def possible(grid: Grid, row:Row, col:Col, n:Int):Boolean={
    val cb = cols.map(grid.get(row, _)==n)
    val rb = rows.map(grid.get(_, col)==n)
    val bb = (for(i<- 1 to 3; j<- 1 to 3) yield grid.get(Row(row.block+i), Col(col.block+j))==n).toList 
    !(cb++rb++bb).contains(true)
  }
  
  def solve(grid:Grid):Unit = {
    for(row<-rows;col <- cols;if(grid.get(row,col)==0)){
      for(n <- 1 to 9;if (possible(grid, row, col,n))){
        grid.set(row, col, n)
        solve(grid) 
        grid.set(row, col, 0)
      }
      return
    }
    println(grid)
  }
  
  solve(grid)
  solve(grid2)
}
  