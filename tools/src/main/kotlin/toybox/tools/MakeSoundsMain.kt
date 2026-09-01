package toybox.tools

import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Entry point: `:tools:makeSounds` regenerates the four WAVs in place;
 * `:tools:checkSounds` byte-compares them against a fresh regeneration.
 */
fun main(args: Array<String>) {
    val root = Path.of(args.firstOrNull() ?: error("usage: <repo-root> [--check]"))
    val rawDir = root.resolve("app/src/main/res/raw")
    if (!Files.isDirectory(rawDir)) error("not a repo root: $root")
    if ("--check" in args) exitProcess(SoundGen.check(rawDir))
    SoundGen.generateAll(rawDir)
}
