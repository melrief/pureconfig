import Dependencies.Version._

name := "pureconfig-yaml"

crossScalaVersions := Seq(scala212, scala213)

libraryDependencies ++= Seq("org.yaml" % "snakeyaml" % "1.29")

developers := List(
  Developer("ruippeixotog", "Rui Gonçalves", "ruippeixotog@gmail.com", url("https://github.com/ruippeixotog"))
)

osgiSettings

OsgiKeys.exportPackage := Seq("pureconfig.module.yaml.*")
OsgiKeys.privatePackage := Seq()
OsgiKeys.importPackage := Seq(
  s"""scala.*;version="[${scalaBinaryVersion.value}.0,${scalaBinaryVersion.value}.50)"""",
  "*"
)
