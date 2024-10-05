package bloop.config

import scala.runtime.AbstractFunction3

// needed for binary compat with 2.0.2
trait SourceGeneratorCompanionPlatform
    extends AbstractFunction3[
      List[Config.SourcesGlobs],
      PlatformFiles.Path,
      List[String],
      Config.SourceGenerator
    ] {}
