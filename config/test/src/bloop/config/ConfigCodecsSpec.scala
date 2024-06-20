package bloop.config

import java.nio.charset.StandardCharsets

import bloop.config.Config.File
import com.github.plokhotnyuk.jsoniter_scala.{core => jsoniter}

class ConfigCodecsSpec extends munit.FunSuite {
  import ConfigCodecsSpec._

  override def afterAll() = {
    val filesToDelete =
      dummyFileJVM.project.classpath ++ dummyFileJVM.project.sources :+ dummyFileJVM.project.classesDir :+ dummyFileJVM.project.out

    val jsFilesToDelete =
      dummyFileJS.project.classpath ++ dummyFileJS.project.sources :+ dummyFileJS.project.classesDir :+ dummyFileJS.project.out
    filesToDelete.foreach(PlatformFiles.deleteTempFile)
    jsFilesToDelete.foreach(PlatformFiles.deleteTempFile)

  }

  def parseConfig(contents: String): Config.File = {
    bloop.config.read(contents.getBytes(StandardCharsets.UTF_8)) match {
      case Right(file) => file
      case Left(throwable) => throw throwable
    }
  }

  def parseFile(configFile: File): Unit = {
    val jsonConfig = bloop.config.write(configFile)
    val parsedConfig = parseConfig(jsonConfig)
    assertEquals(configFile, parsedConfig)
  }

  test("test-empty-config-json") {
    val configFile = File.empty
    val jsonConfig = bloop.config.write(configFile)
    // Assert that empty collection fields such as sources are present in the format
    assert(jsonConfig.contains("\"sources\""))
    val parsedConfig = parseConfig(jsonConfig)
    assertEquals(configFile, parsedConfig)

  }

  test("simple-config-jsons-jvm") {
    parseFile(dummyFileJVM)
  }

  test("simple-config-jsonsjs") {
    parseFile(dummyFileJS)
  }

  test("write-module-split-style-smallestmodules") {

    val config: Config.ModuleSplitStyleJS =
      Config.ModuleSplitStyleJS.SmallestModules
    val written = jsoniter.writeToString(config)(
      implicitly(bloop.config.ConfigCodecs.codecModuleSplitStyleJS)
    )
    assertEquals(written, """{"splitStyle":"SmallestModules"}""")
  }

  test("read-module-split-style-smallestmodules") {

    val config: Config.ModuleSplitStyleJS =
      Config.ModuleSplitStyleJS.SmallestModules
    val read = jsoniter.readFromString[Config.ModuleSplitStyleJS](
      """{"splitStyle":"SmallestModules"}"""
    )(
      implicitly(bloop.config.ConfigCodecs.codecModuleSplitStyleJS)
    )
    assertEquals(read, config)
  }

  test("write-module-split-style-smallmodulesfor") {

    val config: Config.ModuleSplitStyleJS =
      Config.ModuleSplitStyleJS.SmallModulesFor(List("one", "two"))
    val written = jsoniter.writeToString(config)(
      implicitly(bloop.config.ConfigCodecs.codecModuleSplitStyleJS)
    )
    assertEquals(
      written,
      """{"splitStyle":"SmallModulesFor","packages":["one","two"]}"""
    )
  }

  test("read-module-split-style-smallmodulesfor") {

    val config: Config.ModuleSplitStyleJS =
      Config.ModuleSplitStyleJS.SmallModulesFor(List("one", "two"))
    val read = jsoniter.readFromString[Config.ModuleSplitStyleJS](
      """{"splitStyle":"SmallModulesFor","packages":["one","two"]}"""
    )(
      implicitly(bloop.config.ConfigCodecs.codecModuleSplitStyleJS)
    )
    assertEquals(
      read,
      config
    )
  }

  test("test-idea") {
    val jsonConfig =
      """
        |{
        |    "version" : "1.0.0",
        |    "project" : {
        |        "name" : "",
        |        "directory" : "",
        |        "sources" : [
        |        ],
        |        "dependencies" : [
        |        ],
        |        "classpath" : [
        |        ],
        |        "out" : "",
        |        "classesDir" : "",
        |        "hello": 1,
        |        "iAmBinaryCompatible": [
        |          1, 2, 3, 4
        |        ]
        |    }
        |}
      """.stripMargin
    parseConfig(jsonConfig)
    ()

  }

  test("real-worl-json-file") {
    parseConfig(TestPlatform.getResourceAsString("real-world-config.json"))
    ()
  }

}

object ConfigCodecsSpec {
  val dummyFileJVM = File.dummyForTests("JVM")
  val dummyFileJS = File.dummyForTests("JS")
}
