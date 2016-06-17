import sbt._

object Dependencies {

    object Version {
        val akka = "2.4.3"
        val logback = "1.1.3"
    }

    object Compile {
        val akkaActor = "com.typesafe.akka" %% "akka-actor" % Version.akka
        val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % Version.akka
        val akkaSharding = "com.typesafe.akka"             %% "akka-cluster-sharding" % Version.akka
        val akkaStream = "com.typesafe.akka" %% "akka-stream" % Version.akka
        val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % Version.akka
        val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % Version.akka
        val akkaData =  "com.typesafe.akka" %% "akka-distributed-data-experimental" % Version.akka
        val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % Version.akka
        val logbackClassic = "ch.qos.logback" % "logback-classic" % Version.logback
        val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json-experimental" % Version.akka
    }


    object Test {
        val scalaTest = "org.scalatest" %% "scalatest" % "2.2.6" % "test"
        val pegdown = "org.pegdown" % "pegdown" % "1.6.0" % "test"
        val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % Version.akka % "test"
        val akkaMultiNodeTestKit = "com.typesafe.akka" %% "akka-multi-node-testkit" % Version.akka % "test"
        val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % Version.akka % "test"
        val akkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka % "test"
    }

    import Compile._

    private val testing = Seq(Test.scalaTest, Test.pegdown, Test.akkaHttpTestkit,
        Test.akkaMultiNodeTestKit, Test.akkaTestKit, Test.akkaStreamTestkit)
    private val streams = Seq(akkaStream)
    private val logging = Seq(akkaSlf4j, logbackClassic)

    val core = Seq(akkaActor) ++ streams ++ testing ++ logging
    val engine = Seq(akkaActor) ++ testing ++ logging
    val service = Seq(akkaActor, akkaHttpCore, akkaHttp, akkaHttpSprayJson, akkaData) ++ testing ++ logging

    // all in one project, to be usable from Activator
    val all = core ++ engine ++ service
}
