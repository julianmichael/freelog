package freelog

// import org.scalatest._
// import org.scalatest.prop._

import cats._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._

// import munit.FunSuite
import munit.CatsEffectSuite

class FreelogTests extends CatsEffectSuite {

  def showLogTree(tree: LogTree[String]) = tree.cata[String](
    labeled = (label, children) => label + children.map(s =>
      "\n  " + s.replaceAll("\n", "\n  ") // indent whole string and add indented newline
    ).mkString,
    unlabeled = _.mkString("\n")
  )

  implicit val logLevel = LogLevel.Info

  def runTest1[F[_]: Monad](logger: TreeLogger[F, String]) = for {
    _ <- logger.infoBranch("Beginning...") {
      (1 to 5).toList.traverse(i =>
        // List('a', 'b', 'c').traverse
        logger.info(s"Counting $i...")
      )
    }
    _ <- logger.info("Done.")
  } yield ()

  val test1Res = """
    |Beginning...
    |  Counting 1...
    |  Counting 2...
    |  Counting 3...
    |  Counting 4...
    |  Counting 5...
    |Done.
  """.stripMargin.trim

  test("IndentingWriterLogger") {
    val tree = runTest1(freelog.loggers.IndentingWriterLogger((x, l) => x)).run._1.trim
    assertEquals(tree, test1Res)
  }

  test("TreeWriterLogger") {
    val tree = showLogTree(
      runTest1(freelog.loggers.TreeWriterLogger[String]((x, l) => x)).run._1
    ).trim
    assertEquals(tree, test1Res)
  }

  val ephLoggerIO = freelog.loggers.RewindingConsoleLineLogger.create(x => IO.unit)

  test("ephemeral logging checkpoint state unchanged after save+restore+flush") {
    for {
      logger <- ephLoggerIO
      initState <- logger.checkpointState.get
      _ <- logger.save >> logger.restore >> logger.flush
      endState <- logger.checkpointState.get
    } yield assertEquals(initState, endState)
  }

  test("ephemeral logging restore properly rewinds checkpoint state") {
    for {
      logger <- ephLoggerIO
      initState <- logger.checkpointState.get
      _ <- logger.save >> logger.info("Test log message.") >> logger.restore >> logger.flush
      endState <- logger.checkpointState.get
    } yield assertEquals(initState, endState)
  }

}
