// This is a standalone Gradle build, deliberately NOT included in the root settings.gradle.
//
// The root build's `subprojects { }` block applies spotless (with the KeY GPL header), the Checker
// Framework, and maven-publish/signing to every included module, and CI runs `assemble`, `test`,
// `spotlessCheck` and `publishMavenJavaPublicationToKEYLABRepository` across the whole repo. An
// IDEA plugin module inherits all of that, and it pulls a ~1 GB IDE distribution into every one of
// those tasks. Keeping it out of the tree costs one `-p` flag and buys a repo build that is
// unchanged by this module's existence.
//
// See docs/idea-setup.md.
rootProject.name = "keyext.solidity.idea"

// The reverse direction is fine and is what the KeY file type needs: the generated
// KeYSolidityDLLexer lives in keyext.solidity.core, and including the repo root *from here* leaves
// the root build untouched — Gradle substitutes the dependency below for that project and builds
// only its jar. The grammar is never copied.
includeBuild("..")
