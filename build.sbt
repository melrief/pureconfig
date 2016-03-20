scalariformSettings

name := "pureconfig"

organization := "com.github.melrief"

version := "0.1.5"

homepage := Some(url("https://github.com/melrief/pureconfig"))

licenses := Seq("Mozilla Public License, version 2.0" -> url("https://www.mozilla.org/MPL/2.0/"))

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.10.5", "2.11.8")

scalacOptions ++= Seq("-feature")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.0",
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  "com.typesafe" % "config" % "1.3.0",

  "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test",
  "joda-time" % "joda-time" % "2.9.2" % "test",
  "org.joda" % "joda-convert" % "1.8" % "test"
  )

initialize := {
  val required = "1.8"
  val current  = sys.props("java.specification.version")
  assert(current == required, s"Unsupported JDK: java.specification.version $current != $required")
}

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

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
    </developers>)

osgiSettings

OsgiKeys.exportPackage := Seq("pureconfig", "pureconfig.conf", "pureconfig.conf.namespace")

OsgiKeys.privatePackage := Seq()

OsgiKeys.importPackage := Seq(s"""scala.*;version="[${scalaBinaryVersion.value}.0,${scalaBinaryVersion.value}.50)"""", "*")
