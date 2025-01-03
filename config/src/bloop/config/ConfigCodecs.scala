package bloop.config

import java.nio.charset.StandardCharsets

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import bloop.config.PlatformFiles.Path

import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter
import com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig
import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.{core => jsoniter}

object ConfigCodecs {

  implicit val codecPath: JsonValueCodec[Path] = new JsonValueCodec[Path] {
    val nullValue: Path = PlatformFiles.emptyPath
    def encodeValue(x: Path, out: JsonWriter): Unit = out.writeVal(x.toString)
    def decodeValue(in: JsonReader, default: Path): Path =
      if (in.isNextToken('"')) {
        in.rollbackToken()
        Try(PlatformFiles.getPath(in.readString(""))).toOption
          .getOrElse(nullValue)
      } else {
        in.rollbackToken()
        nullValue
      }
  }

  implicit val codecCompileOrder: JsonValueCodec[Config.CompileOrder] = {
    new JsonValueCodec[Config.CompileOrder] {
      val nullValue: Config.CompileOrder =
        null.asInstanceOf[Config.CompileOrder]
      def encodeValue(x: Config.CompileOrder, out: JsonWriter): Unit =
        x match {
          case Config.Mixed => out.writeVal(Config.Mixed.id)
          case Config.JavaThenScala => out.writeVal(Config.JavaThenScala.id)
          case Config.ScalaThenJava => out.writeVal(Config.ScalaThenJava.id)
        }

      def decodeValue(
          in: JsonReader,
          default: Config.CompileOrder
      ): Config.CompileOrder =
        if (in.isNextToken('"')) {
          in.rollbackToken()
          in.readString(null) match {
            case Config.Mixed.id => Config.Mixed
            case Config.JavaThenScala.id => Config.JavaThenScala
            case Config.ScalaThenJava.id => Config.ScalaThenJava
            case _ =>
              in.decodeError(
                s"Expected compile order ${Config.CompileOrder.All.mkString("'", "', '", "'")}"
              )
          }
        } else {
          in.rollbackToken()
          nullValue
        }
    }
  }

  implicit val codecLinkerMode: JsonValueCodec[Config.LinkerMode] = {
    new JsonValueCodec[Config.LinkerMode] {
      val nullValue: Config.LinkerMode = null.asInstanceOf[Config.LinkerMode]
      def encodeValue(x: Config.LinkerMode, out: JsonWriter): Unit = {
        val str = x match {
          case Config.LinkerMode.Debug => Config.LinkerMode.Debug.id
          case Config.LinkerMode.Release => Config.LinkerMode.Release.id
        }
        out.writeVal(str)
      }
      def decodeValue(
          in: JsonReader,
          default: Config.LinkerMode
      ): Config.LinkerMode =
        if (in.isNextToken('"')) {
          in.rollbackToken()
          in.readString(null) match {
            case Config.LinkerMode.Debug.id => Config.LinkerMode.Debug
            case Config.LinkerMode.Release.id => Config.LinkerMode.Release
            case _ =>
              in.decodeError(
                s"Expected linker mode ${Config.LinkerMode.All.mkString("'", "', '", "'")}"
              )
          }
        } else {
          in.rollbackToken()
          nullValue
        }
    }
  }

  implicit val codecModuleKindJS: JsonValueCodec[Config.ModuleKindJS] = {
    new JsonValueCodec[Config.ModuleKindJS] {
      val nullValue: Config.ModuleKindJS =
        null.asInstanceOf[Config.ModuleKindJS]
      def encodeValue(x: Config.ModuleKindJS, out: JsonWriter): Unit = {
        val str = x match {
          case Config.ModuleKindJS.CommonJSModule =>
            Config.ModuleKindJS.CommonJSModule.id
          case Config.ModuleKindJS.NoModule => Config.ModuleKindJS.NoModule.id
          case Config.ModuleKindJS.ESModule => Config.ModuleKindJS.ESModule.id
        }
        out.writeVal(str)
      }
      def decodeValue(
          in: JsonReader,
          default: Config.ModuleKindJS
      ): Config.ModuleKindJS =
        if (in.isNextToken('"')) {
          in.rollbackToken()
          in.readString(null) match {
            case Config.ModuleKindJS.CommonJSModule.id =>
              Config.ModuleKindJS.CommonJSModule
            case Config.ModuleKindJS.NoModule.id => Config.ModuleKindJS.NoModule
            case Config.ModuleKindJS.ESModule.id => Config.ModuleKindJS.ESModule
            case _ =>
              in.decodeError(
                s"Expected linker mode ${Config.ModuleKindJS.All.mkString("'", "', '", "'")}"
              )
          }
        } else {
          in.rollbackToken()
          nullValue
        }
    }
  }

  implicit val codecModuleSplitStyleJS
      : JsonValueCodec[Config.ModuleSplitStyleJS] = {
    implicit val codecList: JsonValueCodec[List[String]] =
      JsonCodecMaker.makeWithRequiredCollectionFields[List[String]]
    new JsonValueCodec[Config.ModuleSplitStyleJS] {
      val nullValue: Config.ModuleSplitStyleJS =
        null.asInstanceOf[Config.ModuleSplitStyleJS]
      def encodeValue(x: Config.ModuleSplitStyleJS, out: JsonWriter): Unit = {
        out.writeObjectStart()
        out.writeKey("splitStyle")
        x match {
          case Config.ModuleSplitStyleJS.FewestModules =>
            out.writeVal(Config.ModuleSplitStyleJS.FewestModules.id)
          case Config.ModuleSplitStyleJS.SmallestModules =>
            out.writeVal(Config.ModuleSplitStyleJS.SmallestModules.id)
          case Config.ModuleSplitStyleJS.SmallModulesFor(packages) =>
            val style = Config.ModuleSplitStyleJS.SmallModulesFor(packages)
            out.writeVal(style.id)
            out.writeKey("packages")
            out.writeArrayStart()
            style.packages.foreach(out.writeVal)
            out.writeArrayEnd()
        }
        out.writeObjectEnd()
      }

      def decodeValue(
          in: JsonReader,
          default: Config.ModuleSplitStyleJS
      ): Config.ModuleSplitStyleJS = {
        in.setMark()
        in.isNextToken('{')
        in.skipToKey("splitStyle")
        val splitStyle = in.readString(null)

        val packages =
          if (
            splitStyle == Config.ModuleSplitStyleJS
              .SmallModulesFor(List.empty)
              .id
          ) {
            in.nextToken()
            if (in.skipToKey("packages")) {
              if (in.isNextToken('[')) {
                in.rollbackToken()
                codecList.decodeValue(in, Nil) match {
                  case Nil =>
                    in.decodeError("Field packages can not contain empty array")
                  case packages => packages
                }
              } else {
                in.decodeError("missing array of packages for field package")
              }
            } else {
              in.requiredFieldError("Missing required field packages ")
            }
          } else List.empty

        in.isNextToken('}')

        splitStyle match {
          case Config.ModuleSplitStyleJS.FewestModules.id =>
            Config.ModuleSplitStyleJS.FewestModules
          case Config.ModuleSplitStyleJS.SmallestModules.id =>
            Config.ModuleSplitStyleJS.SmallestModules
          case Config.ModuleSplitStyleJS.SmallModulesFor.id =>
            Config.ModuleSplitStyleJS.SmallModulesFor(packages)
          case _ => in.decodeError("Invalid splitStyle field")
        }
      }
    }
  }

  implicit val codecNativeLTO: JsonValueCodec[Config.NativeLTO] = {
    new JsonValueCodec[Config.NativeLTO] {
      val nullValue: Config.NativeLTO =
        null.asInstanceOf[Config.NativeLTO]
      def encodeValue(x: Config.NativeLTO, out: JsonWriter): Unit = {
        val str = x match {
          case Config.NativeLTO.None =>
            Config.NativeLTO.None.id
          case Config.NativeLTO.Thin =>
            Config.NativeLTO.Thin.id
          case Config.NativeLTO.Full =>
            Config.NativeLTO.Full.id
        }
        out.writeVal(str)
      }
      def decodeValue(
          in: JsonReader,
          default: Config.NativeLTO
      ): Config.NativeLTO =
        if (in.isNextToken('"')) {
          in.rollbackToken()
          in.readString(null) match {
            case Config.NativeLTO.None.id =>
              Config.NativeLTO.None
            case Config.NativeLTO.Thin.id =>
              Config.NativeLTO.Thin
            case Config.NativeLTO.Full.id =>
              Config.NativeLTO.Full
            case _ =>
              in.decodeError(
                s"Expected build target ${Config.NativeLTO.All.mkString("'", "', '", "'")}"
              )
          }
        } else {
          in.rollbackToken()
          nullValue
        }
    }
  }

  implicit val codecNativeLinkerReleaseMode
      : JsonValueCodec[Config.NativeLinkerReleaseMode] = {
    new JsonValueCodec[Config.NativeLinkerReleaseMode] {
      val nullValue: Config.NativeLinkerReleaseMode =
        null.asInstanceOf[Config.NativeLinkerReleaseMode]
      def encodeValue(
          x: Config.NativeLinkerReleaseMode,
          out: JsonWriter
      ): Unit = {
        val str = x match {
          case Config.NativeLinkerReleaseMode.ReleaseFast =>
            Config.NativeLinkerReleaseMode.ReleaseFast.id
          case Config.NativeLinkerReleaseMode.ReleaseSize =>
            Config.NativeLinkerReleaseMode.ReleaseSize.id
          case Config.NativeLinkerReleaseMode.ReleaseFull =>
            Config.NativeLinkerReleaseMode.ReleaseFull.id
        }
        out.writeVal(str)
      }
      def decodeValue(
          in: JsonReader,
          default: Config.NativeLinkerReleaseMode
      ): Config.NativeLinkerReleaseMode =
        if (in.isNextToken('"')) {
          in.rollbackToken()
          in.readString(null) match {
            case Config.NativeLinkerReleaseMode.ReleaseFast.id =>
              Config.NativeLinkerReleaseMode.ReleaseFast
            case Config.NativeLinkerReleaseMode.ReleaseSize.id =>
              Config.NativeLinkerReleaseMode.ReleaseSize
            case Config.NativeLinkerReleaseMode.ReleaseFull.id =>
              Config.NativeLinkerReleaseMode.ReleaseFull
            case _ =>
              in.decodeError(
                s"Expected build target ${Config.NativeLinkerReleaseMode.All.mkString("'", "', '", "'")}"
              )
          }
        } else {
          in.rollbackToken()
          nullValue
        }
    }
  }

  implicit val codecNativeSanitizer: JsonValueCodec[Config.NativeSanitizer] = {
    new JsonValueCodec[Config.NativeSanitizer] {
      val nullValue: Config.NativeSanitizer =
        null.asInstanceOf[Config.NativeSanitizer]
      def encodeValue(x: Config.NativeSanitizer, out: JsonWriter): Unit = {
        val str = x match {
          case Config.NativeSanitizer.AddressSanitizer =>
            Config.NativeSanitizer.AddressSanitizer.id
          case Config.NativeSanitizer.ThreadSanitizer =>
            Config.NativeSanitizer.ThreadSanitizer.id
          case Config.NativeSanitizer.UndefinedBehaviourSanitizer =>
            Config.NativeSanitizer.UndefinedBehaviourSanitizer.id
        }
        out.writeVal(str)
      }
      def decodeValue(
          in: JsonReader,
          default: Config.NativeSanitizer
      ): Config.NativeSanitizer =
        if (in.isNextToken('"')) {
          in.rollbackToken()
          in.readString(null) match {
            case Config.NativeSanitizer.AddressSanitizer.id =>
              Config.NativeSanitizer.AddressSanitizer
            case Config.NativeSanitizer.ThreadSanitizer.id =>
              Config.NativeSanitizer.ThreadSanitizer
            case Config.NativeSanitizer.UndefinedBehaviourSanitizer.id =>
              Config.NativeSanitizer.UndefinedBehaviourSanitizer
            case _ =>
              in.decodeError(
                s"Expected build target ${Config.NativeSanitizer.All.mkString("'", "', '", "'")}"
              )
          }
        } else {
          in.rollbackToken()
          nullValue
        }
    }
  }

  implicit val codecNativBuildTarget
      : JsonValueCodec[Config.NativeBuildTarget] = {
    new JsonValueCodec[Config.NativeBuildTarget] {
      val nullValue: Config.NativeBuildTarget =
        null.asInstanceOf[Config.NativeBuildTarget]
      def encodeValue(x: Config.NativeBuildTarget, out: JsonWriter): Unit = {
        val str = x match {
          case Config.NativeBuildTarget.Application =>
            Config.NativeBuildTarget.Application.id
          case Config.NativeBuildTarget.LibraryDynamic =>
            Config.NativeBuildTarget.LibraryDynamic.id
          case Config.NativeBuildTarget.LibraryStatic =>
            Config.NativeBuildTarget.LibraryStatic.id
        }
        out.writeVal(str)
      }
      def decodeValue(
          in: JsonReader,
          default: Config.NativeBuildTarget
      ): Config.NativeBuildTarget =
        if (in.isNextToken('"')) {
          in.rollbackToken()
          in.readString(null) match {
            case Config.NativeBuildTarget.Application.id =>
              Config.NativeBuildTarget.Application
            case Config.NativeBuildTarget.LibraryDynamic.id =>
              Config.NativeBuildTarget.LibraryDynamic
            case Config.NativeBuildTarget.LibraryStatic.id =>
              Config.NativeBuildTarget.LibraryStatic
            case _ =>
              in.decodeError(
                s"Expected build target ${Config.NativeBuildTarget.All.mkString("'", "', '", "'")}"
              )
          }
        } else {
          in.rollbackToken()
          nullValue
        }
    }
  }
  implicit val codecJvmConfig: JsonValueCodec[Config.JvmConfig] =
    JsonCodecMaker.makeWithRequiredCollectionFields[Config.JvmConfig]

  implicit val codecJsConfig: JsonValueCodec[Config.JsConfig] =
    JsonCodecMaker.makeWithRequiredCollectionFields[Config.JsConfig]

  implicit val codecNativeOptimizerConfig
      : JsonValueCodec[Config.NativeOptimizerConfig] =
    JsonCodecMaker
      .makeWithRequiredCollectionFields[Config.NativeOptimizerConfig]

  implicit val codecNativeConfig: JsonValueCodec[Config.NativeConfig] =
    JsonCodecMaker.makeWithRequiredCollectionFields[Config.NativeConfig]

  private case class MainClass(mainClass: Option[String])
  private implicit val codecMainClass: JsonValueCodec[MainClass] = {
    new JsonValueCodec[MainClass] {
      val nullValue: MainClass = null.asInstanceOf[MainClass]
      val codecOption: JsonValueCodec[Option[String]] =
        JsonCodecMaker.makeWithRequiredCollectionFields[Option[String]]
      val codecList: JsonValueCodec[List[String]] =
        JsonCodecMaker.makeWithRequiredCollectionFields[List[String]]
      def encodeValue(x: MainClass, out: JsonWriter): Unit = {
        // codecOption.encodeValue(x.mainClass, out)
        codecList.encodeValue(x.mainClass.toList, out)
      }

      def decodeValue(in: JsonReader, default: MainClass): MainClass = {
        if (in.isNextToken('[')) {
          in.rollbackToken()
          codecList.decodeValue(in, Nil) match {
            case Nil => MainClass(None)
            case List(mainClass) => MainClass(Some(mainClass))
            case mainClasses =>
              in.decodeError(
                s"Expected only one main class, obtained $mainClasses!"
              )
          }
        } else {
          in.rollbackToken()
          MainClass(codecOption.decodeValue(in, None))
        }
      }
    }
  }

  private sealed trait JsoniterPlatform
  private case class jvm(
      config: Config.JvmConfig,
      mainClass: MainClass,
      runtimeConfig: Option[Config.JvmConfig],
      classpath: Option[List[Path]],
      resources: Option[List[Path]]
  ) extends JsoniterPlatform
  private case class js(config: Config.JsConfig, mainClass: MainClass)
      extends JsoniterPlatform
  private case class native(config: Config.NativeConfig, mainClass: MainClass)
      extends JsoniterPlatform

  implicit val codecPlatform: JsonValueCodec[Config.Platform] =
    new JsonValueCodec[Config.Platform] {
      val codec: JsonValueCodec[JsoniterPlatform] =
        JsonCodecMaker
          .makeWithRequiredCollectionFieldsAndNameAsDiscriminatorFieldName[
            JsoniterPlatform
          ]
      val nullValue: Config.Platform = null.asInstanceOf[Config.Platform]
      def encodeValue(x: Config.Platform, out: JsonWriter): Unit = {
        codec.encodeValue(
          x match {
            case Config.Platform.Jvm(
                  config,
                  mainClass,
                  runtimeConfig,
                  classpath,
                  resources
                ) =>
              jvm(
                config,
                MainClass(mainClass),
                runtimeConfig,
                classpath,
                resources
              )
            case Config.Platform.Js(config, mainClass) =>
              js(config, MainClass(mainClass))
            case Config.Platform.Native(config, mainClass) =>
              native(config, MainClass(mainClass))
          },
          out
        )
      }
      def decodeValue(
          in: JsonReader,
          default: Config.Platform
      ): Config.Platform = {
        codec.decodeValue(in, null) match {
          case jvm(config, mainClass, runtimeConfig, classpath, resources) =>
            Config.Platform.Jvm(
              config,
              mainClass.mainClass,
              runtimeConfig,
              classpath,
              resources
            )
          case js(config, mainClass) =>
            Config.Platform.Js(config, mainClass.mainClass)
          case native(config, mainClass) =>
            Config.Platform.Native(config, mainClass.mainClass)
        }
      }
    }

  implicit val codecProject: JsonValueCodec[Config.Project] =
    JsonCodecMaker.makeWithRequiredCollectionFields[Config.Project]

  implicit val codecFile: JsonValueCodec[Config.File] =
    JsonCodecMaker.makeWithRequiredCollectionFields[Config.File]

  def read(configDir: Path): Either[Throwable, Config.File] = {
    read(PlatformFiles.readAllBytes(configDir))
  }

  def read(bytes: Array[Byte]): Either[Throwable, Config.File] = {
    Try(jsoniter.readFromArray[Config.File](bytes)) match {
      case Failure(exception) => Left(exception)
      case Success(value) => Right(value)
    }
  }

  def toStr(all: Config.File): String = {
    val config = WriterConfig.withIndentionStep(4)
    new String(
      jsoniter.writeToArray[Config.File](all, config),
      StandardCharsets.UTF_8
    )
  }
}
