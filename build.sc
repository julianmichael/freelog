import mill._, mill.scalalib._, mill.scalalib.publish._, mill.scalajslib._
import mill.scalalib.scalafmt._
import coursier.maven.MavenRepository

import $ivy.`com.lihaoyi::mill-contrib-bloop:$MILL_VERSION`

import ammonite.ops._

val thisPublishVersion = "0.1.1-SNAPSHOT"
val scalaVersions = List(
  "2.12.13",
  "2.13.4"
  // "3.0.0-M3"
)
val thisScalaJSVersion = "1.4.0"

// val macroParadiseVersion = "2.1.1"
val kindProjectorVersion = "0.11.3"
val splainVersion = "0.3.4"
val betterMonadicForVersion = "0.3.1"

val catsVersion = "2.3.1"
// val kittensVersion = "1.1.1"

val catsEffectVersion = "2.3.1"
val fs2Version = "2.5.0"
// ivy"co.fs2::fs2-io::$fs2Version",
// val http4sVersion = "0.20.11"
// ivy"org.http4s::http4s-blaze-client::$http4sVersion",
// ivy"org.http4s::http4s-blaze-server::$http4sVersion",

val fansiVersion = "0.2.10"
val jlineVersion = "3.19.0"

val munitVersion = "0.7.21"
val munitCatsEffectVersion = "0.11.0"

trait CommonModule extends ScalaModule with ScalafmtModule {

  def platformSegment: String

  override def sources = T.sources(
    millSourcePath / "src",
    millSourcePath / s"src-$platformSegment"
  )

  override def scalacOptions = T {
    Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:higherKinds"
    ) ++ (
      if(scalaVersion().startsWith("2.12")) Seq("-Ypartial-unification")
      else Seq()
    )
  }

  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(
    // ivy"io.tryp:::splain:$splainVersion",
    // ivy"org.scalamacros:::paradise:$macroParadiseVersion",
    ivy"org.typelevel:::kind-projector:$kindProjectorVersion",
    ivy"com.olegpy::better-monadic-for:$betterMonadicForVersion"
  )

  // add back in when necessary
  // def repositories = super.repositories ++ Seq(
  //   MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
  // )

  override def ivyDeps = Agg(
    ivy"org.typelevel::cats-core::$catsVersion",
    ivy"org.typelevel::cats-effect::$catsEffectVersion",
    ivy"co.fs2::fs2-core::$fs2Version",
    ivy"com.lihaoyi::fansi::$fansiVersion"
    // ivy"org.typelevel::alleycats-core::$catsVersion",
    // ivy"org.typelevel::kittens::$kittensVersion",
  )

}

trait CommonPublishModule extends CommonModule with PublishModule with CrossScalaModule {
  def millSourcePath = super.millSourcePath / RelPath.up
  def artifactName = "freelog"
  def publishVersion = thisPublishVersion
  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "org.julianmichael",
    url = "https://github.com/julianmichael/freelog",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("julianmichael", "freelog"),
    developers = Seq(
      Developer("julianmichael", "Julian Michael","https://github.com/julianmichael")
    )
  )
  trait CommonTestModule extends CommonModule with TestModule {
    override def ivyDeps = Agg(
      ivy"org.scalameta::munit::$munitVersion",
      ivy"org.typelevel::munit-cats-effect-2::$munitCatsEffectVersion",
    )
    def testFrameworks = Seq("munit.Framework")
  }
}

trait JvmModule extends CommonPublishModule {
  def platformSegment = "jvm"
    override def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.jline:jline-terminal:$jlineVersion"
    )
  trait Tests extends super.Tests with CommonTestModule {
    def platformSegment = "jvm"
  }
}

trait JsModule extends CommonPublishModule with ScalaJSModule {
  def scalaJSVersion = T(thisScalaJSVersion)
  def platformSegment = "js"
  trait Tests extends super.Tests with CommonTestModule {
    def scalaJSVersion = T(thisScalaJSVersion)
    def platformSegment = "js"
    def moduleKind = T(mill.scalajslib.api.ModuleKind.CommonJSModule)
  }
}

object freelog extends Module {
  object jvm extends Cross[Jvm](scalaVersions: _*)
  class Jvm(val crossScalaVersion: String) extends JvmModule {
    object test extends Tests
  }

  object js extends Cross[Js](scalaVersions: _*)
  class Js(val crossScalaVersion: String) extends JsModule {
    object test extends Tests
  }
}
