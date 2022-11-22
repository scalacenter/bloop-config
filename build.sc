import $ivy.`com.goyeau::mill-scalafix::0.2.11`

import mill._
import mill.scalalib._
import mill.scalajslib._
import mill.scalajslib.api.ModuleKind
import mill.scalalib.scalafmt.ScalafmtModule

import com.goyeau.mill.scalafix.ScalafixModule

val scala211 = "2.11.12"
val scala212 = "2.12.17"
val scala213 = "2.13.10"

val scalaVersions = List(scala211, scala212, scala213)
val scalaJSVersions = scalaVersions.map((_, "1.11.0"))
val scalaNativeVersions = scalaVersions.map((_, "0.4.7"))

val configName = "config"

trait Common extends CrossScalaModule with ScalafmtModule with ScalafixModule {
  def platform: String

  override def millSourcePath = build.millSourcePath / configName

  override def sources = T.sources(
    millSourcePath / "src",
    millSourcePath / s"src-$platform"
  )

  override def scalafixIvyDeps = Agg(
    ivy"com.github.liancheng::organize-imports:0.6.0"
  )

  val jsoniterVersion = "2.4.0"

  override def ivyDeps = Agg(
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::$jsoniterVersion"
  )

  override def compileIvyDeps = Agg(
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros::$jsoniterVersion"
  )

  override def scalacOptions = Seq("-Ywarn-unused", "-deprecation")
}

trait CommonTest extends ScalaModule with TestModule.Munit {
  def platform: String

  def ivyDeps = Agg(ivy"org.scalameta::munit::1.0.0-M7")

  def sources = T.sources(
    millSourcePath / "src",
    millSourcePath / s"src-$platform"
  )
}

object config extends Module {
  object jvm extends Cross[ConfigJvmModule](scalaVersions: _*)
  class ConfigJvmModule(val crossScalaVersion: String) extends Common {
    override def platform = "jvm"

    object test extends Tests with CommonTest {
      override def platform = "jvm"
    }
  }

  object js extends Cross[ConfigJsModule](scalaJSVersions: _*)
  class ConfigJsModule(val crossScalaVersion: String, crossJSVersion: String)
      extends Common
      with ScalaJSModule {
    override def platform = "js"
    override def scalaJSVersion = crossJSVersion

    object test extends Tests with CommonTest {
      override def platform = "js"
      override def moduleKind = T { ModuleKind.CommonJSModule }
    }
  }
}
