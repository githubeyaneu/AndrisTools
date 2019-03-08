package eu.eyan.japan

object Japan extends App {

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

  def extraEmptySpaceKombinations(size: Int, tiles: List[Int]) = generate(size - tiles.sum - tiles.size + 1, tiles.size + 1)
  val eesk = extraEmptySpaceKombinations(7, List(1, 2))
  println(eesk.mkString("\r\n"))
  //val eesk2 = extraEmptySpaceKombinations(70, List(2,2,3,6,2,2,2,6,3,2,2))
  //println(eesk2.size)
  
  println(Math.pow((70-32-10),12))
}