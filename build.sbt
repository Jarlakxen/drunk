// ··· Project Settings ···

// scalafixSettings

// ··· Project Info ···

val projectName = "drunk"

name := projectName

organization := "com.github.jarlakxen"

crossScalaVersions := Seq("2.12.7", "2.11.12")

scalaVersion := crossScalaVersions.value.head

organizationName := "Facundo Viale"
startYear := Some(2018)
licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))

publishMavenStyle := true

// ··· Project Options ···

lazy val root = (project in file("."))

scalacOptions ++= Seq(
  "-encoding",
  "utf8",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-unchecked",
  "-deprecation",
  "-Ymacro-expand:normal"
)

// ··· Project Repositories ···

resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots"))

// ··· Project Dependencies ···
val sangriaV        = "1.4.+"
val sangriaCirceV   = "1.2.1"
val akkaHttpV       = "10.1.+"
val akkaHttpCircleV = "1.22.+"
val circeV          = "0.10.+"
val slf4JV          = "1.7.25"
val logbackV        = "1.2.3"
val scalatestV      = "3.0.5"

libraryDependencies ++= Seq(
  // --- GraphQL --
  "org.sangria-graphql" %% "sangria"          % sangriaV,
  "org.sangria-graphql" %% "sangria-circe"    % sangriaCirceV,
  // --- Akka --
  "com.typesafe.akka"   %% "akka-http"        % akkaHttpV,
  "de.heikoseeberger"   %% "akka-http-circe"  % akkaHttpCircleV,
  // --- Utils ---
  "io.circe"            %% "circe-generic"    % circeV,
  "io.circe"            %% "circe-parser"     % circeV,
  // --- Logger ---
  "org.slf4j"           %  "slf4j-api"        % slf4JV,
  "ch.qos.logback"      %  "logback-classic"  % logbackV        % Test,
  // --- Testing ---
  "com.typesafe.akka"   %% "akka-http-testkit"  % akkaHttpV     % Test,
  "org.scalatest"       %% "scalatest"          % scalatestV    % Test
)

// ··· Testing Configuration ···

fork in (Test, run) := false

scalacOptions in Test ++= Seq("-Yrangepos")
