name := "pureconfig-http4s-blaze-server"

crossScalaVersions ~= { _.filterNot(_.startsWith("2.13")) }

scalacOptions += "-Ypartial-unification"

libraryDependencies ++= Seq("org.http4s" %% "http4s-blaze-server" % "0.20.17")

developers := List(
  Developer(
    "sideeffffect",
    "Ondra Pelech",
    "ondra.pelech@gmail.com",
    url("https://github.com/sideeffffect")
  )
)

osgiSettings

OsgiKeys.exportPackage := Seq("pureconfig.module.http4s.blaze.server.*")
OsgiKeys.privatePackage := Seq()
OsgiKeys.importPackage := Seq(
  s"""scala.*;version="[${scalaBinaryVersion.value}.0,${scalaBinaryVersion.value}.50)"""",
  "*"
)
