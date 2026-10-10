plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// Working copy is not a git clone, so git-based versioning is disabled (see gradle.properties).
// Provide an explicit version here.
//
// Format: <major>.<minor>.<revision>-rc<GTNH build>. The rcN segment names the GTNH release this
// build targets and does NOT move when the code changes - rc1 is GTNH 2.9.0-RC-1, and it stays rc1
// until we target a different GTNH. Every round of changes instead raises the leading numbers, so the
// jar name always identifies one exact build: 1.0.6-rc1 -> ... -> 1.0.31-rc1 -> 1.0.32-rc1 -> 1.0.33-rc1 -> ...
extra["modVersion"] = "1.0.49-rc2"

tasks.withType<JavaCompile>().configureEach {
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}

// The magnet card's trouble-shooting trace: the click, the packet, the server-side flip and the
// per-tick pickup decision. Off by default in a release, on for every runClient/runServer so a test
// session always leaves the whole chain in the log.
tasks.withType<JavaExec>().configureEach {
    systemProperty("wtct.debug.magnet", "true")
    systemProperty("wtct.debug.tab", "true")
}
