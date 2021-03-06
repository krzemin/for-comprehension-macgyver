package pl.codepot.exercises

import scala.io.Source
import scala.util.{Success, Try, Failure}

object TryExample extends App {

  /**
   * T4.0
   * Using String.toInt wrapped by Try implement division
   * division(a,b) = a/b
   * Use guard to not divide by 0
   * What is returned if b == 0 ?
   */
  def division(dividend: String, divisor: String): Try[Int] = for {
    a <- Try(dividend.toInt)
    b <- Try(divisor.toInt)
    if b != 0
  } yield a / b

  println(division("5", "0"))

  /**
   * T4.1
   * What to do with exception in Try?
   *
   * Using previus function division(dividend: String, divisor: String) exercise handling failures with
   *  pattern matching
   *  recover
   */

  def division2(dividend: String, divisor: String): Try[Int] =  {
    Try(dividend.toInt) match {
      case Success(a) =>
        Try(divisor.toInt) match {
          case Success(b) => b match {
            case 0 => Failure(new NoSuchElementException("Predicate does not hold for 0"))
            case _ => Success(a / b)
          }
          case f => f
        }
      case f => f
    }
  }

  println(division2("0","0"))

  /**
   * T4.2
   * prove that Try invocation chain is stopped with first failure.
   *
   * Hint: use some visible side effect like print() to prove that other Try in not executed
   */
  def chain = for {
    v1 <- Try(throw new RuntimeException("It was my fault"))
    v2 <- Try {println("Hello!")}
  } yield v2

  println(chain)
  /**
   * T4.3
   * Enable options to work with Try.
   *
   * What if under Try object there is hidden exception?
   */
  def tryWithOption: Option[Int] = for {
    i <- Some(1)
    j <- Try(1).toOption
  } yield i + j

  println(tryWithOption)
}

object TryAnswers {

  /**
   * A4.2
   */
  def chain = for {
    v1 <- Try(throw new RuntimeException("It was my fault"))
    v2 <- Try {
      print("(Un)Expected side effect")
      Thread.sleep(10 * 1000)
      Source.fromURL("www.google.pl").mkString
    }
  } yield v2

  /**
   * A4.3
   */
  def tryWithOption: Option[Int] = for {
    i <- Some(1)
    j <- Try(1).toOption
  } yield i + j
}
