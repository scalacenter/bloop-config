package bloop.config

import scala.annotation.unroll

import bloop.config.PlatformFiles.Path
import bloop.config.PlatformFiles.emptyPath

object Config {
  case class Java(options: List[String])

  case class TestFramework(names: List[String])

  object TestFramework {
    val utest: TestFramework = Config.TestFramework(
      List("utest.runner.Framework")
    )

    val munit: TestFramework = Config.TestFramework(
      List("munit.Framework")
    )

    val ScalaCheck: TestFramework = Config.TestFramework(
      List(
        "org.scalacheck.ScalaCheckFramework"
      )
    )

    val ScalaTest: TestFramework = Config.TestFramework(
      List(
        "org.scalatest.tools.Framework",
        "org.scalatest.tools.ScalaTestFramework"
      )
    )

    val Specs2: TestFramework = Config.TestFramework(
      List(
        "org.specs.runner.SpecsFramework",
        "org.specs2.runner.Specs2Framework",
        "org.specs2.runner.SpecsFramework"
      )
    )

    val JUnit: TestFramework = Config.TestFramework(
      List(
        "com.novocode.junit.JUnitFramework"
      )
    )

    val TestNG: TestFramework = Config.TestFramework(
      List(
        "mill.testng.TestNGFramework"
      )
    )

    val DefaultFrameworks: List[TestFramework] =
      List(JUnit, ScalaTest, ScalaCheck, Specs2, utest, munit, TestNG)
  }

  case class TestArgument(args: List[String], framework: Option[TestFramework])
  case class TestOptions(excludes: List[String], arguments: List[TestArgument])
  object TestOptions { val empty: TestOptions = TestOptions(Nil, Nil) }

  case class Test(frameworks: List[TestFramework], options: TestOptions)
  object Test {
    def defaultConfiguration: Test = {
      val junit = List(
        Config.TestArgument(List("-v", "-a"), Some(Config.TestFramework.JUnit))
      )
      Config.Test(
        Config.TestFramework.DefaultFrameworks,
        Config.TestOptions(Nil, junit)
      )
    }
  }

  case class Sbt(
      sbtVersion: String,
      autoImports: List[String]
  )

  sealed abstract class CompileOrder(val id: String)
  case object Mixed extends CompileOrder("mixed")
  case object JavaThenScala extends CompileOrder("java->scala")
  case object ScalaThenJava extends CompileOrder("scala->java")

  object CompileOrder {
    final val All: List[String] =
      List(Mixed.id, JavaThenScala.id, ScalaThenJava.id)
  }

  case class CompileSetup(
      order: CompileOrder,
      addLibraryToBootClasspath: Boolean,
      addCompilerToClasspath: Boolean,
      addExtraJarsToClasspath: Boolean,
      manageBootClasspath: Boolean,
      filterLibraryFromClasspath: Boolean
  )

  object CompileSetup {
    val empty: CompileSetup =
      CompileSetup(Mixed, true, false, false, true, true)
  }

  case class Scala(
      organization: String,
      name: String,
      version: String,
      options: List[String],
      jars: List[Path],
      analysis: Option[Path],
      setup: Option[CompileSetup],
      bridgeJars: Option[List[Path]]
  )

  sealed abstract class Platform(val name: String) {
    type Config <: PlatformConfig
    def config: Config
    def mainClass: Option[String]
  }

  object Platform {
    val default: Platform = Jvm(JvmConfig.empty, None, None, None, None)

    object Js { val name: String = "js" }
    case class Js(
        override val config: JsConfig,
        override val mainClass: Option[String]
    ) extends Platform(Js.name) {
      type Config = JsConfig
    }

    object Jvm { val name: String = "jvm" }
    case class Jvm(
        override val config: JvmConfig,
        override val mainClass: Option[String],
        runtimeConfig: Option[JvmConfig],
        classpath: Option[List[Path]],
        resources: Option[List[Path]]
    ) extends Platform(Jvm.name) {
      type Config = JvmConfig
    }

    object Native { val name: String = "native" }
    case class Native(
        override val config: NativeConfig,
        override val mainClass: Option[String]
    ) extends Platform(Native.name) {
      type Config = NativeConfig
    }

    final val All: List[String] = List(Jvm.name, Js.name, Native.name)
  }

  sealed trait PlatformConfig
  case class JvmConfig(home: Option[Path], options: List[String])
      extends PlatformConfig
  object JvmConfig { val empty: JvmConfig = JvmConfig(None, Nil) }

  sealed abstract class LinkerMode(val id: String)
  object LinkerMode {
    case object Debug extends LinkerMode("debug")
    case object Release extends LinkerMode("release")
    val All: List[String] = List(Debug.id, Release.id)
  }

  sealed abstract class ModuleKindJS(val id: String)
  object ModuleKindJS {
    case object NoModule extends ModuleKindJS("none")
    case object CommonJSModule extends ModuleKindJS("commonjs")
    case object ESModule extends ModuleKindJS("esmodule")
    val All: List[String] = List(NoModule.id, CommonJSModule.id, ESModule.id)
  }

  sealed abstract class ModuleSplitStyleJS(val id: String)
  object ModuleSplitStyleJS {
    case object FewestModules extends ModuleSplitStyleJS("FewestModules")
    case object SmallestModules extends ModuleSplitStyleJS("SmallestModules")
    final case class SmallModulesFor(packages: List[String])
        extends ModuleSplitStyleJS("SmallModulesFor")

    object SmallModulesFor {
      val id: String = SmallModulesFor(List.empty).id
    }

    val All: List[String] =
      List(FewestModules.id, SmallestModules.id, SmallModulesFor.id)
  }

  case class JsConfig(
      version: String,
      mode: LinkerMode,
      kind: ModuleKindJS,
      emitSourceMaps: Boolean,
      jsdom: Option[Boolean],
      output: Option[Path],
      nodePath: Option[Path],
      toolchain: List[Path],
      @unroll moduleSplitStyle: Option[ModuleSplitStyleJS] = None
  ) extends PlatformConfig

  object JsConfig {
    val empty: JsConfig =
      JsConfig(
        "",
        LinkerMode.Debug,
        ModuleKindJS.NoModule,
        false,
        None,
        None,
        None,
        Nil
      )
  }

  /*
   * Represents the native platform and all the options it takes.
   *
   * For the description of these fields, see:
   * https://static.javadoc.io/org.scala-native/tools_2.12/0.3.9/scala/scalanative/build/Config.html
   *
   * The only field that has been replaced for user-friendliness is `targetTriple` by `platform`.
   */
  case class NativeConfig(
      version: String,
      mode: LinkerMode,
      gc: String,
      targetTriple: Option[String],
      clang: Path,
      clangpp: Path,
      toolchain: List[Path],
      options: NativeOptions,
      linkStubs: Boolean,
      check: Boolean,
      dump: Boolean,
      output: Option[Path],
      @unroll buildTarget: Option[NativeBuildTarget] = None,
      @unroll sanitizer: Option[NativeSanitizer] = None,
      @unroll nativeModeAndLTO: NativeModeAndLTO = NativeModeAndLTO.empty,
      @unroll nativeFlags: NativeFlags = NativeFlags.empty,
      @unroll nativeResourcePatterns: NativeResourcePatterns =
        NativeResourcePatterns.empty,
      @unroll serviceProviders: Map[String, List[String]] = Map.empty,
      @unroll baseName: String = "",
      @unroll nativeOptimizerConfig: Option[NativeOptimizerConfig] = None
  ) extends PlatformConfig

  object NativeConfig {
    // FORMAT: OFF
    val empty: NativeConfig = NativeConfig("", LinkerMode.Debug, "", None, emptyPath, emptyPath, Nil, NativeOptions.empty, false, false, false, None, None, None, NativeModeAndLTO.empty, NativeFlags.empty, NativeResourcePatterns.empty, Map.empty, "", None)
    // FORMAT: ON
  }

  case class NativeModeAndLTO(
      nativeLinkerReleaseMode: Option[NativeLinkerReleaseMode],
      lto: Option[NativeLTO]
  )

  object NativeModeAndLTO {
    def empty: NativeModeAndLTO = NativeModeAndLTO(None, None)
  }
  case class NativeResourcePatterns(
      resourceIncludePatterns: List[String],
      resourceExcludePatterns: List[String]
  )

  object NativeResourcePatterns {
    def empty: NativeResourcePatterns =
      NativeResourcePatterns(List("**"), List.empty)
  }

  case class NativeFlags(
      checkFatalWarnings: Boolean,
      checkFeatures: Boolean,
      optimize: Boolean,
      useIncrementalCompilation: Boolean,
      embedResources: Boolean,
      multithreading: Option[Boolean] = None
  )

  object NativeFlags {
    def empty = NativeFlags(
      checkFatalWarnings = false,
      checkFeatures = false,
      optimize = true,
      useIncrementalCompilation = true,
      embedResources = false,
      multithreading = None
    )
  }

  case class NativeOptimizerConfig(
      maxInlineDepth: Int,
      maxCallerSize: Int,
      maxCalleeSize: Int,
      smallFunctionSize: Int
  )

  sealed abstract class NativeSanitizer(val id: String)
  object NativeSanitizer {
    case object AddressSanitizer extends NativeSanitizer("address")
    case object ThreadSanitizer extends NativeSanitizer("thread")
    case object UndefinedBehaviourSanitizer extends NativeSanitizer("undefined")

    val All: List[String] =
      List(
        AddressSanitizer.id,
        ThreadSanitizer.id,
        UndefinedBehaviourSanitizer.id
      )
  }

  sealed abstract class NativeLTO(val id: String)
  object NativeLTO {
    case object None extends NativeLTO("none")
    case object Thin extends NativeLTO("thin")
    case object Full extends NativeLTO("full")

    val All: List[String] =
      List(None.id, Thin.id, Full.id)
  }

  sealed abstract class NativeLinkerReleaseMode(val id: String)
  object NativeLinkerReleaseMode {
    case object ReleaseFast extends NativeLinkerReleaseMode("release-fast")
    case object ReleaseSize extends NativeLinkerReleaseMode("release-size")
    case object ReleaseFull extends NativeLinkerReleaseMode("release-full")

    val All: List[String] =
      List(ReleaseFast.id, ReleaseSize.id, ReleaseFull.id)
  }

  sealed abstract class NativeBuildTarget(val id: String)
  object NativeBuildTarget {
    case object Application extends NativeBuildTarget("application")
    case object LibraryDynamic extends NativeBuildTarget("dynamic")
    case object LibraryStatic extends NativeBuildTarget("static")

    val All: List[String] =
      List(Application.id, LibraryDynamic.id, LibraryStatic.id)
  }

  case class NativeOptions(linker: List[String], compiler: List[String])
  object NativeOptions {
    val empty: NativeOptions = NativeOptions(Nil, Nil)
  }

  case class Checksum(
      `type`: String,
      digest: String
  )

  case class Artifact(
      name: String,
      classifier: Option[String],
      checksum: Option[Checksum],
      path: Path
  )

  case class Module(
      organization: String,
      name: String,
      version: String,
      configurations: Option[String],
      artifacts: List[Artifact]
  )

  object Module {
    private[bloop] val empty: Module = Module("", "", "", None, Nil)
  }

  case class Resolution(
      modules: List[Module]
  )

  case class SourcesGlobs(
      directory: Path,
      walkDepth: Option[Int],
      includes: List[String],
      excludes: List[String]
  )

  case class SourceGenerator(
      sourcesGlobs: List[SourcesGlobs],
      outputDirectory: Path,
      command: List[String],
      @unroll unmanagedInputs: List[Path] = Nil,
      @unroll commandTemplate: Option[List[String]] = None
  )

  object SourceGenerator extends SourceGeneratorCompanionPlatform

  case class Project(
      name: String,
      directory: Path,
      workspaceDir: Option[Path],
      sources: List[Path],
      sourcesGlobs: Option[List[SourcesGlobs]],
      sourceRoots: Option[List[Path]],
      dependencies: List[String],
      classpath: List[Path],
      out: Path,
      classesDir: Path,
      resources: Option[List[Path]],
      `scala`: Option[Scala],
      java: Option[Java],
      sbt: Option[Sbt],
      test: Option[Test],
      platform: Option[Platform],
      resolution: Option[Resolution],
      tags: Option[List[String]],
      sourceGenerators: Option[List[SourceGenerator]]
  )

  object Project {
    // FORMAT: OFF
    private[bloop] val empty: Project = Project("", emptyPath, None, List(), None, None, List(), List(),  emptyPath, emptyPath, None, None, None, None, None, None, None, None, None)
    // FORMAT: ON

    def analysisFileName(projectName: String): String =
      s"$projectName-analysis.bin"
  }

  case class File(version: String, project: Project)
  object File {
    // We cannot have the version coming from the build tool
    final val LatestVersion = "1.4.0"
    private[bloop] val empty = File(LatestVersion, Project.empty)

    private[bloop] def dummyForTests(platformType: String): File = {
      val workingDirectory = PlatformFiles.userDir
      val sourceFile = PlatformFiles.createTempFile("Foo", ".scala")

      // Just add one classpath with the scala library in it
      val scalaLibraryJar =
        PlatformFiles.createTempFile("scala-library", ".jar")

      // This is like `target` in sbt.
      val outDir = PlatformFiles.createTempFile("out", "test")

      val outAnalysisFile = PlatformFiles.resolve(outDir, "out-analysis.bin")

      val classesDir = PlatformFiles.createTempFile("classes", "test")

      val classpath = List(scalaLibraryJar)
      val resources = Some(List(PlatformFiles.resolve(outDir, "resource1.xml")))

      val jdk8Path = PlatformFiles.getPath("/usr/lib/jvm/java-8-jdk")
      val jdk11Path = PlatformFiles.getPath("/usr/lib/jvm/java-11-jdk")

      val platform = {
        Platform.Jvm(
          JvmConfig(Some(jdk8Path), Nil),
          Some("module.Main"),
          Some(JvmConfig(Some(jdk11Path), Nil)),
          Some(classpath),
          resources
        )
      }

      val platformJS = {
        Platform.Js(
          JsConfig(
            "1.17.0",
            LinkerMode.Release,
            ModuleKindJS.ESModule,
            false,
            None,
            None,
            None,
            List(jdk8Path),
            Some(ModuleSplitStyleJS.SmallestModules)
          ),
          None
        )
      }

      val project = Project(
        "dummy-project",
        workingDirectory,
        Some(workingDirectory),
        List(sourceFile),
        None,
        None,
        List("dummy-2"),
        classpath,
        outDir,
        classesDir,
        resources,
        Some(
          Scala(
            "org.scala-lang",
            "scala-compiler",
            "2.12.4",
            List("-warn"),
            List(),
            Some(outAnalysisFile),
            Some(CompileSetup.empty),
            None
          )
        ),
        Some(Java(List("-version"))),
        Some(Sbt("1.1.0", Nil)),
        Some(Test(List(), TestOptions(Nil, Nil))),
        if (platformType == "JVM") {
          Some(platform)
        } else if (platformType == "JS") {
          Some(platformJS)
        } else None,
        Some(Resolution(Nil)),
        None,
        None
      )

      File(LatestVersion, project)
    }
  }
}
