import sbt.Keys._

val project = Project(
    id = "akka-streams-testing",
    base = file(".")
)
  .settings(
      organization := "uk.co.foyst",
      name := "akka-streams-testing",
      version := (version in ThisBuild).value,
      scalaVersion := "2.11.8",
      parallelExecution in Test := false,

      scalacOptions += "-language:postfixOps",
      scalacOptions += "-language:implicitConversions",

      libraryDependencies ++= Dependencies.all,

      resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      resolvers += "spray repo" at "http://repo.spray.io/",
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
      resolvers += "SpringSource Milestone Repository" at "http://repo.springsource.org/milestone",

      resolvers ++= Seq(
          "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
          "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
          "Sonatype repo" at "https://oss.sonatype.org/content/groups/scala-tools/",
          "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
          "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
          "Sonatype staging" at "http://oss.sonatype.org/content/repositories/staging",
          "Java.net Maven2 Repository" at "http://download.java.net/maven/2/",
          "Twitter Repository" at "http://maven.twttr.com",
          Resolver.bintrayRepo("websudos", "oss-releases"),
          Resolver.url("Websudos OSS", url("http://dl.bintray.com/websudos/oss-releases"))(Resolver.ivyStylePatterns)
      )
  )

fork in run := true
parallelExecution in Test := false