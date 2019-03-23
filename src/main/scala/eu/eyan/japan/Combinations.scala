package eu.eyan.japan

object Combinations {
//TODO check it
//  def multi[A](as: List[A], k: Int): List[List[A]] = (List.fill(k)(as)).flatten.combinations(k).toList

//  implicit class LongToFieldType(long: Long) {
//    @tailrec//TODO
//    def factorial(x: Long, acc: Long = 1): Long = if (x <= 1) acc else factorial(x - 1, x * acc)
//    def ! = factorial(long)
//  }

//  def binom(n: Long, k: Long): Long = (n.!) / ((k.!) * (n - k).!)

//  def combinationsWithRepetition(places: Long, items: Long) = binom(places + items - 1, items)
  
  implicit class BigIntToFieldType(bigInt: BigInt) {
    //    @tailrec//TODO
    def factorial(x: BigInt, acc: BigInt = 1): BigInt = if (x <= 1) acc else factorial(x - 1, x * acc)
    def ! = factorial(bigInt)
  }
  
  def binomBi(n: BigInt, k: BigInt): BigInt = (n.!) / ((k.!) * (n - k).!)
  // binom: n * n-1 * n-2 * ... * n-k+1 / (k * k-1 * k-2 * ... * 1)
  
  def combinationsWithRepetitionBi(places: Long, items: Long) = binomBi(places + items - 1, items)
}