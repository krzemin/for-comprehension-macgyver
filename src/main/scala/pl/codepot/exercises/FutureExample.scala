package pl.codepot.exercises

import akka.actor.{ Props, ActorSystem, Actor }
import akka.util.Timeout
import pl.codepot.common._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object FutureExample extends App {

  /**
   * T5.0
   * Convert transaction to be in EUR
   * Assume that given transaction id is for transaction in different currency than EUR
   *
   * Experiment with deconstructors and alias
   * Hint:
   * use
   *     TransactionService.get
   *     CurrencyService.convertToEUR
   */
  def convertToEur(id: TransactionId): Future[Transaction] = for {
    transaction <- TransactionService.get(id)
    amountInEUR <- CurrencyService.convertToEUR(transaction.amount, transaction.currency)
  } yield transaction.copy(
      amount = amountInEUR,
      currency = Currency("EUR")
    )

  print(Await.result(convertToEur(TransactionId("123")), 5.seconds))

  /**
   * T5.1
   * Print given amount(PLN) in USD and EUR
   *
   * Hint:
   * Is following execution really done in parallel?
   */
  def makeItFaster(amount: Double): Future[Unit] = {
    val usd = Currency("USD")
    val eur = Currency("EUR")

    val f1 = CurrencyService.convertPlnTo(amount, usd)
    val f2 = CurrencyService.convertPlnTo(amount, eur)

    for {
      inUSD <- f1
      inEUR <- f2
      _ = println(s"$amount PLN is $inUSD USD or $inEUR EUR")
    } yield ()
  }
  makeItFaster(10.0).foreach(println)

  /**
   * T5.2
   * How to handle guard?
   *
   * Hint:
   * Is `result`filled with Success or Failure?
   * How to recover() from failure? Especialy from NoSuchElementException?
   */
  def futureGuardProblem = {
    val result = for {
      number <- Future.successful(1)
      if number < 1
    } yield number
    print(Await.result(result.recover{ case _: NoSuchElementException => 0}, 5.seconds))
  }

  futureGuardProblem

  /**
   * T5.3
   * Examine the execution time of the example with and without guard
   * @return
   */
  def breakChain = {
    def sleepAndGet[T](seconds: Int, value: T) = Future {
      Thread.sleep(seconds * 1000)
      value
    }
    val result = for {
      a <- sleepAndGet(1, "a")
      if a.startsWith("prefix")
      b <- sleepAndGet(5, "b")
      c <- sleepAndGet(10, "c")
    } yield a + b + c
    Await.ready(result, 30.seconds)
  }

  val t0 = System.currentTimeMillis()
  breakChain
  val t1 = System.currentTimeMillis()
  println(s"breakChain took ${t1 - t0} ms")

  /**
   * T5.4
   * How to compose future[Option[T] in one for-comprehension
   *
   * Return projects of author of given commit id
   * Hint:
   *  RemoteGitClient.get(sha)
   *  Use deconstruction on Some
   */
  def futureOption(): Future[List[String]] = {
    val sha = SHA("1213123213123")
    def projects(author: Commit.Author) =
      Future.successful(List("scala/scala", "akka/akka", "EnterpriseQualityCoding/FizzBuzzEnterpriseEdition"))

    for {
      Some(commit) <- RemoteGitClient.get(sha)
      projectz <- projects(commit.author)
    } yield projectz
  }


  /**
   * T5.5
   * Use optionT from scalaz.OptionT._ to compose two Future[Option[
   *
   * Hint:
   *  RemoteGitClient.get(sha)
   *  I recommend just google stackoverflow
   * @return
   */
  def futureOptionScalaZ: Future[Option[Commit]] = {
    import scalaz._
    import Scalaz._
    import scalaz.OptionT._
    val shaF = Future.successful(Option(SHA("1213123213123")))

    (for {
      sha <- optionT(shaF)
      commit <- optionT(RemoteGitClient.get(sha))
    } yield commit).run
  }

  /**
   * T5.6
   * If world is beautiful place then ask your boss for money and print what you received.
   * Use akka.pattern.ask to combine aktor responses together
   */
  def akkaFun = {
    import akka.pattern.ask
    import scala.concurrent.duration._
    val system = ActorSystem()
    implicit val timeout = Timeout(1.second)
    val omniscient = system.actorOf(Props(new Actor {
      def receive = {
        case "Is the world beautiful place?" => sender() ! true
      }
    }))
    val boss = system.actorOf(Props(new Actor {
      def receive = {
        case "Money!" => sender() ! ":)"
      }
    }))


    for {
      isBeautiful <- ask(omniscient, "Is the world beautiful place?").mapTo[Boolean]
      if isBeautiful
      money <- ask(boss, "Money!").mapTo[String]
    } println(money)

    ///
    system.shutdown()
  }

}

object FutureAnswers {

  /**
   * A5.0
   */
  def convertToEur(id: TransactionId): Future[Transaction] = for {
    t @ Transaction(_, _, amount, currency) <- TransactionService.get(id)
    converted <- CurrencyService.convertToEUR(amount, currency)
  } yield t.copy(currency = Currency("EUR"), amount = converted)

  /**
   * A5.1
   */
  def makeItFaster(amount: Double): Future[Unit] = {
    val usd = Currency("USD")
    val eur = Currency("EUR")
    val inUSDF = CurrencyService.convertPlnTo(amount, usd)
    val inEURF = CurrencyService.convertPlnTo(amount, eur)
    for {
      inUSD <- inUSDF
      inEUR <- inEURF
      _ = println(s"$amount PLN is $inUSD USD or $inEUR EUR")
    } yield ()
  }

  /**
   * A5.2
   */

  /**
   * A5.3
   */

  /**
   * A5.4
   */
  def futureOption(): Future[List[String]] = {
    val sha = SHA("1213123213123")
    def projects(author: Commit.Author) = Future.successful(List("scala/scala", "akka/akka", "EnterpriseQualityCoding/FizzBuzzEnterpriseEdition"))

    for {
      Some(remoteCommit) <- RemoteGitClient.get(sha)
      author = remoteCommit.author
      j <- projects(author)
    } yield j
  }

  /**
   * A5.5
   */
  def futureOptionScalaZ: Future[Option[Commit]] = {
    import scalaz._
    import Scalaz._
    import scalaz.OptionT._
    val shaF = Future.successful(Option(SHA("1213123213123")))

    (for {
      i <- optionT(shaF)
      j <- optionT(RemoteGitClient.get(i))
    } yield j).run
  }

  /**
   * A5.6
   */
  def akkaFun = {
    import akka.pattern.ask
    import scala.concurrent.duration._
    val system = ActorSystem()
    implicit val timeout = Timeout(1.second)
    val omniscient = system.actorOf(Props(new Actor {
      def receive = {
        case "Is the world beautiful place?" => sender() ! true
      }
    }))
    val boss = system.actorOf(Props(new Actor {
      def receive = {
        case "Money!" => sender() ! ":)"
      }
    }))

    for {
      answer <- ask(omniscient, "Is the world beautiful place?").mapTo[Boolean]
      if answer
      money <- ask(boss, "Money!").mapTo[String]
    } println(s"My boss gave me $money because world is beautiful place")

    ///
    system.shutdown()
  }
}
