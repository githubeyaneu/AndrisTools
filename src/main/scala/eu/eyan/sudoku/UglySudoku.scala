package eu.eyan.sudoku

import eu.eyan.util.time.Timer

object UglySudoku extends App {
  val grid = Array(
    Array(5, 3, 0, 0, 7, 0, 0, 0, 0),
    Array(6, 0, 0, 1, 9, 5, 0, 0, 0),
    Array(0, 9, 8, 0, 0, 0, 0, 6, 0),

    Array(8, 0, 0, 0, 6, 0, 0, 0, 3),
    Array(4, 0, 0, 8, 0, 3, 0, 0, 1),
    Array(7, 0, 0, 0, 2, 0, 0, 0, 6),

    Array(0, 6, 0, 0, 0, 0, 2, 8, 0),
    Array(0, 0, 0, 4, 1, 9, 0, 0, 5),
    Array(0, 0, 0, 0, 8, 0, 0, 7, 9)) //00

  def possible(row: Int, col: Int, n: Int): Boolean = {
    for (i <- 1 to 9) if (grid(row - 1)(i - 1) == n) return false
    for (i <- 1 to 9) if (grid(i - 1)(col - 1) == n) return false
    val x0 = ((row - 1) / 3) * 3
    val y0 = ((col - 1) / 3) * 3
    for (i <- 1 to 3)
      for (j <- 1 to 3)
        if (grid(x0 + i - 1)(y0 + j - 1) == n) return false
    return true
  }

  def solve: Unit = {
    for (row <- 1 to 9)
      for (col <- 1 to 9)
        if (grid(row - 1)(col - 1) == 0) {
          for (n <- 1 to 9)
            if (possible(row, col, n)) {
              grid(row - 1)(col - 1) = n
              solve
              grid(row - 1)(col - 1) = 0
            }
          return
        }
    println(grid.map(_.mkString(" ")).mkString("\r\n") + "\r\n")
  }

  val st = System.currentTimeMillis
  solve
  println(System.currentTimeMillis - st + "ms")
}