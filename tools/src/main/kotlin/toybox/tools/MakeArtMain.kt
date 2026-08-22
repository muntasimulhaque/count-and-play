package toybox.tools

import java.io.File

/**
 * Entry point: `:tools:makeArt` regenerates the store icon, the feature
 * graphic and every launcher density from the design system in this module.
 */
fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: error("usage: <repo-root>"))
    require(root.resolve("settings.gradle.kts").isFile) { "not a repo root: $root" }
    featureGraphic(root)
    storeIcon(root)
    launcherIcons(root)
}
