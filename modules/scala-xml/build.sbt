import Dependencies.Version._

name := "pureconfig-scala-xml"

crossScalaVersions := Seq(scala212, scala213)

libraryDependencies ++= Seq("org.scala-lang.modules" %% "scala-xml" % "2.0.0")

developers := List(Developer("derekmorr", "Derek Morr", "morr.derek@gmail.com", url("https://github.com/derekmorr")))

osgiSettings

OsgiKeys.exportPackage := Seq("pureconfig.module.scalaxml.*")
OsgiKeys.privatePackage := Seq()
OsgiKeys.importPackage := Seq(
  s"""scala.*;version="[${scalaBinaryVersion.value}.0,${scalaBinaryVersion.value}.50)"""",
  "*"
)
