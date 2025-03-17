import os.Path
import $ivy.`com.github.lolgab::mill-mima::0.1.1`
import $ivy.`com.github.lolgab::mill-crossplatform::0.2.4`
import $ivy.`com.goyeau::mill-scalafix::0.5.1`
import $ivy.`io.chris-kipp::mill-ci-release::0.3.0`

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

val scala212 = "2.12.20"
val scala213 = "2.13.16"
val scala3 = "3.3.5"

val scalaJS1 = "1.17.0"

val scalaVersions = List(scala212, scala213, scala3)

trait CommonPublish extends CiReleaseModule with Mima {

  override def artifactName = "bloop-config"

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

  val jsoniterVersion = "2.13.5.2"
  val unrollVersion = "0.1.12"

  override def scalafixConfig: T[Option[Path]] = T {
    if (scalaVersion() == scala3) Some(os.pwd / ".scalafix3.conf")
    else Some(os.pwd / ".scalafix.conf")
  }

  override def ivyDeps = Agg(
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::$jsoniterVersion",
    ivy"com.lihaoyi::unroll-annotation:$unrollVersion"
  )

  override def compileIvyDeps = Agg(
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros::$jsoniterVersion"
  )

  override def scalacOptions =
    Seq("-Ywarn-unused", "-deprecation", "-release", "8")

  override def scalacPluginIvyDeps = T {
    super.scalacPluginIvyDeps() ++
      Agg(ivy"com.lihaoyi::unroll-plugin:$unrollVersion")
  }
}

trait CommonTest extends ScalaModule with TestModule.Munit {
  def ivyDeps = Agg(ivy"org.scalameta::munit::1.0.4")
}

object config extends Cross[ConfigModule](scalaVersions)

trait ConfigModule extends CrossPlatform {
  trait Shared
      extends CrossPlatformCrossScalaModule
      with Common
      with CommonPublish {

    // note: somehow, this doesn't work (causes "Please override mimaPreviousVersions or mimaPreviousArtifacts")
    // override def mimaPreviousVersions: Target[Seq[String]] =
    //   T {
    //     if (scalaVersion() == scala3) Seq.empty[String] else Seq("2.0.2")
    //   }
    // so we have to override mimaPreviousArtifacts fully:
    // at least until the Scala 3 release is published.

    override def mimaPreviousArtifacts: Target[Agg[Dep]] = T {
      if (scalaVersion() == scala3) Agg.empty[Dep]
      else Agg(ivy"ch.epfl.scala::bloop-config::2.0.2")
    }
  }

  object jvm extends Shared {
    object test extends CrossPlatformSources with ScalaTests with CommonTest
  }

  object js extends Shared with ScalaJSModule {
    override def scalaJSVersion = scalaJS1
    override def moduleKind = T { ModuleKind.CommonJSModule }
    object test extends CrossPlatformSources with ScalaJSTests with CommonTest
  }
}
