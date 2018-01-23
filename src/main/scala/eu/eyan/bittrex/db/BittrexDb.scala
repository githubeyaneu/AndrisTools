package eu.eyan.bittrex.db

import slick.jdbc.MySQLProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import eu.eyan.util.string.StringPlus.StringPlusImplicit
import eu.eyan.util.io.FilePlus.FilePlusImplicit
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{ Success, Failure }

object BittrexDb extends App {
  val url = """D:\private\bittrex\mrl""".asFile.linesList.head
  val db = Database.forURL(url, driver = "org.mariadb.jdbc.Driver")

  class Person(tag: Tag) extends Table[(Int, String)](tag, "PEOPLE") {
    def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
    def name = column[String]("NAME")
    def * = (id, name)
  }
  val people = TableQuery[Person]

  println("create")
  val setup = DBIO.seq(
    (people.schema).create,
    people += (101, "Mini Me"),
    people += (102, "You"))

  val setupFuture = db.run(setup)

  setupFuture.onComplete {
    case Success(trace) => s"-- Finished successfully in $trace --".println
    case Failure(t)     => s"-- ERROR: ${t.getMessage}".println; t.printStackTrace
  }
  Await.result(setupFuture, 10 seconds)

  println("People:")
  db.run(people.result).map(_.foreach { case (supID, name) => println("  " + name + "\t" + supID) })
  db.run(people.result).map(_.foreach { case (supID, name) => println("  " + name + "\t" + supID) })

  Await.result(db.run(DBIO.seq((people.schema).drop)), 10 seconds)

  db.close
}