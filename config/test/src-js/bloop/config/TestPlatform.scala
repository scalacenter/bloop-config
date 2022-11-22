package bloop.config

object TestPlatform {
  def getResourceAsString(resource: String): String =
    NodeFS.readFileSync(
      NodePath.join("config", "test", "resources", resource),
      "utf8"
    )
}
