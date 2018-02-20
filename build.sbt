name := "dpoibattleship"

version := "1.1"

lazy val `dpoibattleship` = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "org.mongodb.scala" %% "mongo-scala-driver" % "1.0.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.7.3"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"  