import $ivy.`com.github.lolgab::mill-mima::0.0.17`
import $ivy.`com.github.lolgab::mill-crossplatform::0.1.4`
import $ivy.`com.goyeau::mill-scalafix::0.2.11`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.5`

import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalajslib._
import mill.scalajslib.api.ModuleKind
import mill.scalalib.scalafmt.ScalafmtModule

import com.github.lolgab.mill.mima._
import com.github.lolgab.mill.crossplatform._
import com.goyeau.mill.scalafix.ScalafixModule
import io.kipp.mill.ci.release.CiReleaseModule

val scala211 = "2.11.12"
val scala212 = "2.12.17"
val scala213 = "2.13.10"

val scalaJS1 = "1.11.0"

val scalaVersions = List(scala211, scala212, scala213)

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
  def ivyDeps = Agg(ivy"org.scalameta::munit::1.0.0-M7")
}

object config extends Cross[ConfigModule](scalaVersions: _*)
class ConfigModule(val crossScalaVersion: String) extends CrossPlatform {
  trait Shared
      extends CrossPlatformCrossScalaModule
      with Common
      with CommonPublish
  object jvm extends Shared {
    object test extends CrossPlatformSources with Tests with CommonTest
  }
  object js extends Shared with ScalaJSModule {
    override def scalaJSVersion = scalaJS1
    object test extends CrossPlatformSources with Tests with CommonTest {
      override def moduleKind = T { ModuleKind.CommonJSModule }
    }
  }
}
