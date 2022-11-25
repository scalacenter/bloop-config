import $ivy.`com.github.lolgab::mill-mima::0.0.13`
import $ivy.`com.goyeau::mill-scalafix::0.2.11`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.3`

import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalajslib._
import mill.scalajslib.api.ModuleKind
import mill.scalalib.scalafmt.ScalafmtModule

import com.github.lolgab.mill.mima._
import com.goyeau.mill.scalafix.ScalafixModule
import io.kipp.mill.ci.release.CiReleaseModule

val scala211 = "2.11.12"
val scala212 = "2.12.17"
val scala213 = "2.13.10"

val scalaVersions = List(scala211, scala212, scala213)
val scalaJSVersions = scalaVersions.map((_, "1.11.0"))
val scalaNativeVersions = scalaVersions.map((_, "0.4.7"))

object Platforms {
  val jvm = "jvm"
  val js = "js"
}

trait CommonPublish extends CiReleaseModule with Mima {

  override def artifactName = "bloop-config"

  override def mimaPreviousVersions = Seq("1.5.4")

  override def pomSettings = PomSettings(
    description = "Bloop configuration library.",
    organization = "ch.epfl.scala",
    url = "https://github.com/scalacenter/bloop-config",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("scalacenter", "bloop-config"),
    developers = Seq(
      Developer(
        "jvican",
        "Jorge Vicente Cantero",
        "https://github.com/jvican"
      ),
      Developer("Duhem", "Martin Duhem", "https://github.com/Duhemm")
    )
  )
}

trait Common extends CrossScalaModule with ScalafmtModule with ScalafixModule {

  def platform: String

  def moduleDir: String

  override def millSourcePath = build.millSourcePath / moduleDir

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
  class ConfigJvmModule(val crossScalaVersion: String)
      extends Common
      with CommonPublish {
    override def platform = Platforms.jvm
    override def moduleDir: String = "config"

    object test extends Tests with CommonTest {
      override def platform = Platforms.jvm
      override def moduleDeps = Seq(configTestUtil.jvm())
    }
  }

  object js extends Cross[ConfigJsModule](scalaJSVersions: _*)
  class ConfigJsModule(val crossScalaVersion: String, crossJSVersion: String)
      extends Common
      with ScalaJSModule
      with CommonPublish {
    override def platform = Platforms.js
    override def moduleDir: String = "config"
    override def scalaJSVersion = crossJSVersion

    object test extends Tests with CommonTest {
      override def platform = Platforms.js
      override def moduleDeps = Seq(configTestUtil.js())
      override def moduleKind = T { ModuleKind.CommonJSModule }
    }
  }
}

object configTestUtil extends Module {
  object jvm extends Cross[ConfigJvmModule](scalaVersions: _*)
  class ConfigJvmModule(val crossScalaVersion: String)
      extends Common
      with CommonPublish {
    override def platform = Platforms.jvm
    override def moduleDir: String = "configTestUtil"
    override def moduleDeps = Seq(config.jvm())
    override def artifactName = super.artifactName() ++ "-test-util"
  }

  object js extends Cross[ConfigJsModule](scalaJSVersions: _*)
  class ConfigJsModule(val crossScalaVersion: String, crossJSVersion: String)
      extends Common
      with ScalaJSModule
      with CommonPublish {
    override def platform = Platforms.js
    override def moduleDir: String = "configTestUtil"
    override def scalaJSVersion = crossJSVersion
    override def moduleDeps = Seq(config.js())
    override def artifactName = super.artifactName() ++ "-test-util"
  }
}
