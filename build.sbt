import Dependencies.Version._
import Utilities._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

organization in ThisBuild := "com.github.pureconfig"

lazy val core = (project in file("core"))
  .enablePlugins(BoilerplatePlugin, SbtOsgi, TutPlugin)
  .settings(commonSettings)
  .dependsOn(macros)

// Two special modules for now, since `tests` depend on them. We should improve this organization later by separating
// the test helpers (which all projects' tests should depend on) from the core+generic test implementations.
lazy val `generic-base` = (project in file("modules/generic-base"))
  .enablePlugins(SbtOsgi, TutPlugin)
  .dependsOn(core)
  .settings(commonSettings, tutTargetDirectory := baseDirectory.value)

lazy val generic = (project in file("modules/generic"))
  .enablePlugins(SbtOsgi, TutPlugin)
  .dependsOn(core, `generic-base`)
  .settings(commonSettings, tutTargetDirectory := baseDirectory.value)
// -----

lazy val macros = (project in file("macros")).enablePlugins(TutPlugin).settings(commonSettings)

lazy val tests = (project in file("tests"))
  .enablePlugins(BoilerplatePlugin)
  .settings(commonSettings)
  .dependsOn(core, generic)
  .dependsOn(macros % "test->test") // provides helpers to test pureconfig macros

// aggregates pureconfig-core and pureconfig-generic with the original "pureconfig" name
lazy val bundle = (project in file("bundle"))
  .enablePlugins(SbtOsgi, TutPlugin)
  .settings(commonSettings, tutTargetDirectory := file("."))
  .dependsOn(core, generic)

lazy val docs = (project in file("docs"))
  .enablePlugins(MicrositesPlugin)
  .settings(commonSettings, publishArtifact := false)
  .settings(docsSettings)
  .dependsOn(bundle)

def module(proj: Project) = proj
  .enablePlugins(SbtOsgi, TutPlugin)
  .dependsOn(core)
  .dependsOn(tests % "test")
  .dependsOn(generic % "Tut") // Allow auto-derivation in documentation
  .settings(commonSettings, tutTargetDirectory := baseDirectory.value)

lazy val akka = module(project) in file("modules/akka")
lazy val `akka-http` = module(project) in file("modules/akka-http")
lazy val cats = module(project) in file("modules/cats")
lazy val `cats-effect` = module(project) in file("modules/cats-effect")
lazy val circe = module(project) in file("modules/circe")
lazy val cron4s = module(project) in file("modules/cron4s")
lazy val enum = module(project) in file("modules/enum")
lazy val enumeratum = module(project) in file("modules/enumeratum")
lazy val fs2 = module(project) in file("modules/fs2")
lazy val hadoop = module(project) in file("modules/hadoop")
lazy val http4s = module(project) in file("modules/http4s")
lazy val javax = module(project) in file("modules/javax")
lazy val joda = module(project) in file("modules/joda")
lazy val magnolia = module(project) in file("modules/magnolia") dependsOn `generic-base`
lazy val `scala-xml` = module(project) in file("modules/scala-xml")
lazy val scalaz = module(project) in file("modules/scalaz")
lazy val squants = module(project) in file("modules/squants")
lazy val sttp = module(project) in file("modules/sttp")
lazy val yaml = module(project) in file("modules/yaml")

lazy val commonSettings = Seq(
  // format: off
  homepage := Some(url("https://github.com/pureconfig/pureconfig")),
  licenses := Seq("Mozilla Public License, version 2.0" -> url("https://www.mozilla.org/MPL/2.0/")),

  developers := List(
    Developer("melrief", "Mario Pastorelli", "pastorelli.mario@gmail.com", url("https://github.com/melrief")),
    Developer("leifwickland", "Leif Wickland", "leifwickland@gmail.com", url("https://github.com/leifwickland")),
    Developer("jcazevedo", "Joao Azevedo", "joao.c.azevedo@gmail.com", url("https://github.com/jcazevedo")),
    Developer("ruippeixotog", "Rui Gonçalves", "ruippeixotog@gmail.com", url("https://github.com/ruippeixotog")),
    Developer("derekmorr", "Derek Morr", "morr.derek@gmail.com", url("https://github.com/derekmorr"))
  ),

  crossScalaVersions := Seq(scala211, scala212, scala213),
  scalaVersion := scala212,

  resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),

  crossVersionSharedSources(unmanagedSourceDirectories in Compile),
  crossVersionSharedSources(unmanagedSourceDirectories in Test),

  scalacOptions ++= lintFlags.value,

  scalacOptions in Test ~= { _.filterNot(_.contains("-Ywarn-unused")) },
  scalacOptions in Test += "-Xmacro-settings:materialize-derivations",

  scalacOptions in (Compile, console) --= Seq("-Xfatal-warnings", "-Ywarn-unused-import", "-Ywarn-unused:_,-implicits"),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
  scalacOptions in Tut --= Seq("-Ywarn-unused-import", "-Xmacro-settings:materialize-derivations"),

  scalafmtOnCompile := true,

  autoAPIMappings := true,

  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := sonatypePublishToBundle.value
  // format: on
)

lazy val docsSettings = Seq(
  micrositeName := "PureConfig",
  micrositeDescription := "A boilerplate-free library for loading configuration files",
  micrositeAuthor := "com.github.pureconfig",
  micrositeHomepage := "https://pureconfig.github.io/",
  //micrositeBaseUrl := "pureconfig", // keep this empty to not have a base URL
  micrositeDocumentationUrl := "docs/",
  micrositeGithubOwner := "pureconfig",
  micrositeGithubRepo := "pureconfig",
  micrositeTheme := "pattern",
  micrositeHighlightTheme := "default",
  micrositePalette := Map(
    "brand-primary" -> "#ab4b4b", // link color
    "brand-secondary" -> "#4b4b4b", // nav/sidebar back
    "brand-tertiary" -> "#292929", // sidebar top back
    "gray-dark" -> "#453E46", // section title
    "gray" -> "#837F84", // text color
    "gray-light" -> "#E3E2E3", // star back
    "gray-lighter" -> "#F4F3F4", // code back
    "white-color" -> "#FFFFFF"
  ),
  micrositeGitterChannel := false, // ugly
  mdocExtraArguments += "--no-link-hygiene",
  mdocVariables := Map("VERSION" -> version.value)
)

// add support for Scala version ranges such as "scala-2.12+" in source folders (single version folders such as
// "scala-2.11" are natively supported by SBT).
// In order to keep this simple, we're doing this case by case, taking advantage of the fact that we intend to support
// only 3 major versions at any given moment.
def crossVersionSharedSources(unmanagedSrcs: SettingKey[Seq[File]]) = {
  unmanagedSrcs ++= {
    val minor = CrossVersion.partialVersion(scalaVersion.value).map(_._2)
    List(
      if (minor.exists(_ <= 12)) unmanagedSrcs.value.map { dir => new File(dir.getPath + "-2.12-") }
      else Nil,
      if (minor.exists(_ >= 12)) unmanagedSrcs.value.map { dir => new File(dir.getPath + "-2.12+") }
      else Nil
    ).flatten
  }
}

lazy val lintFlags = {
  lazy val allVersionLintFlags = List(
    "-encoding",
    "UTF-8", // arg for -encoding
    "-feature",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen"
  )

  def withCommon(flags: String*) =
    allVersionLintFlags ++ flags

  forScalaVersions {
    case (2, 11) =>
      withCommon("-deprecation", "-Xlint", "-Xfatal-warnings", "-Yno-adapted-args", "-Ywarn-unused-import")

    case (2, 12) =>
      withCommon(
        "-deprecation", // Either#right is deprecated on Scala 2.13
        "-Xlint:_,-unused",
        "-Xfatal-warnings",
        "-Yno-adapted-args",
        "-Ywarn-unused:_,-implicits" // Some implicits are intentionally used just as evidences, triggering warnings
      )

    case (2, 13) =>
      withCommon("-Ywarn-unused:_,-implicits")

    case _ =>
      withCommon()
  }
}

// Use the same Scala 2.12 version in the root project as in subprojects
scalaVersion := scala212

// do not publish the root project
skip in publish := true

releaseCrossBuild := true
releaseTagComment := s"Release ${(version in ThisBuild).value}"
releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}"

// redefine the release process due to https://github.com/sbt/sbt-release/issues/184
// and to append `sonatypeReleaseAll`
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
