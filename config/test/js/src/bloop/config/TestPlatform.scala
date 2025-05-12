package bloop.config

object TestPlatform {
  def getResourceAsString(resource: String): String =
    NodeFS.readFileSync(
      NodePath.join(Constants.workspace, "config", "test", "resources", resource),
      "utf8"
    )
}
