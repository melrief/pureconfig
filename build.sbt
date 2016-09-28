lazy val buildSettings = Seq(
  organization := "com.github.melrief",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.5", "2.11.8")
)

version in ThisBuild := "0.3.1.1"

lazy val compilerOptions = Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8", // yes, this is 2 args
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code"
)

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions ++ (
                                       CrossVersion.partialVersion(scalaVersion.value) match {
                                         case Some((2, scalaMajor)) if scalaMajor >= 11 => Seq(
                                           "-Ywarn-unused-import", // Not available in 2.10
                                           "-Ywarn-numeric-widen" // In 2.10 this produces a some strange spurious error
                                         )
                                       }
                                       ),
  scalacOptions in (Compile, console) ~= (_ filterNot Set("-Xfatal-warnings", "-Ywarn-unused-import")),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )
)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings

lazy val scalariSettings = scalariformSettings

lazy val pureconfig =
  project
  .in(file("."))
  .aggregate(core, examples)
  .settings(noPublishSettings)

lazy val core =
  project
  .in(file("core"))
  .settings(name := "pureconfig")
  .settings(moduleName := "pureconfig-core")
  .settings(libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % "2.3.0",
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    "com.typesafe" % "config" % "1.3.0",

    "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test",
    "joda-time" % "joda-time" % "2.9.2" % "test",
    "org.joda" % "joda-convert" % "1.8" % "test"
  ))
  .settings(allSettings:_*)
  .settings(initializeSetting)
  .settings(osgiConf)
  .settings(scalariSettings)

lazy val examples =
  project
  .in(file("examples"))
  .dependsOn(core)
  .settings(moduleName := "pureconfig-examples")
  .settings(allSettings:_*)
  .settings(noPublishSettings)

lazy val initializeSetting = Seq(
  initialize := {
    val required = "1.8"
    val current  = sys.props("java.specification.version")
    assert(current == required, s"Unsupported JDK: java.specification.version $current != $required")
  }
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
                 val nexus = "https://oss.sonatype.org/"
                 if (isSnapshot.value)
                   Some("snapshots" at nexus + "content/repositories/snapshots")
                 else
                   Some("releases"  at nexus + "service/local/staging/deploy/maven2")
               },
  publishArtifact in Test := false,
  homepage := Some(url("https://github.com/melrief/pureconfig")),
  licenses := Seq("Mozilla Public License, version 2.0" -> url("https://www.mozilla.org/MPL/2.0/")),
  pomExtra := (
              <scm>
                <url>git@github.com:melrief/pureconfig.git</url>
                <connection>scm:git:git@github.com:melrief/pureconfig.git</connection>
              </scm>
                <developers>
                  <developer>
                    <id>melrief</id>
                    <name>Mario Pastorelli</name>
                    <url>https://github.com/melrief</url>
                  </developer>
                </developers>
              )
)

lazy val osgiConf = osgiSettings ++ Seq(
  OsgiKeys.exportPackage := Seq("pureconfig", "pureconfig.syntax"),
  OsgiKeys.privatePackage := Seq(),
  OsgiKeys.importPackage := Seq(s"""scala.*;version="[${scalaBinaryVersion.value}.0,${scalaBinaryVersion.value}.50)"""", "*")
)
