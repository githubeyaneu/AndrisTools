package eu.eyan.japan

object KtoN extends App {

  // n: 1 - 14
  def kto3(k: Int, callback: Array[Int] => Unit, array: Array[Int] = Array.ofDim(3)) =
    for (i0 <- 0 to k; i1 <- 0 to k - i0; i2 = k - i0 - i1) {
      array(0) = i0
      array(1) = i1
      array(2) = i2
      callback(array)
    }

  def kton(k: Int, n: Int, callback: Array[Int] => Unit) = {
    def ktonSub(array: Array[Int], remainingStep: Int, remainingItems: Int): Unit = {
      //println((k,n,array.toList, remainingStep, remainingItems))
      if (remainingStep == 1) {
        array(n - 1) = remainingItems
        callback(array)
      } else {
        for (i <- 0 to remainingItems) {
          array(n - remainingStep) = i
          ktonSub(array, remainingStep - 1, remainingItems - i)
        }
      }
    }
    ktonSub(Array.ofDim(n), n, k)
  }

  //  kto3(2, a => { println(a.toList) })
  println
  var ct = 0
  val st = System.currentTimeMillis
  kton(3, 3, a => { println(a.toList) })
  kton(12, 28, a => { ct = ct + 1; if (ct % 100000000 == 0) println(ct+" "+(System.currentTimeMillis -st)+"ms") })
  println(ct)
}