package bloop.config

import java.nio.charset.StandardCharsets

import bloop.config.Config.File

class ConfigCodecsSpec extends munit.FunSuite {
  import ConfigCodecsSpec._

  override def afterAll() = {
    val filesToDelete =
      dummyFile.project.classpath ++ dummyFile.project.sources :+ dummyFile.project.classesDir :+ dummyFile.project.out
    filesToDelete.foreach(PlatformFiles.deleteTempFile)

  }

  def parseConfig(contents: String): Config.File = {
    bloop.config.read(contents.getBytes(StandardCharsets.UTF_8)) match {
      case Right(file)     => file
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

  test("simple-config-jsons") {
    parseFile(dummyFile)
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
  val dummyFile = File.dummyForTests
}
