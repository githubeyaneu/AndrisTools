package eu.eyan.japan

object KtoN extends App {

  println(kton(2, 3, a => { println(a.toList) }))
  println(kton(3, 3, a => { println(a.toList) }))

  def kton(k: Int, n: Int, callback: Array[Int] => Unit) = {
    if(n==3) kto3(k,callback)
  }

  def kto3(k: Int, callback: Array[Int] => Unit, array: Array[Int] = Array.ofDim(3)) =
    for (i0 <- 0 to k; i1 <- 0 to k - i0; i2 = k - i0 - i1) {
      array(0) = i0
      array(1) = i1
      array(2) = i2
      callback(array)
    }

  // n: 1 - 14
}