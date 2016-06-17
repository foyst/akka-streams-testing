import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.slf4j.Logger
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpecLike
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.testkit.TestPublisher.Probe
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}

class StreamSupervisionSpec extends TestKit(ActorSystem("StreamSupervision", ConfigFactory.load())) with WordSpecLike {

    implicit val materialiser = ActorMaterializer()
    val log = Logger("Test")

    "Akka Streams supervision" should {

        "handle different supervision strategies for different parts of the stream" in {

            val supervisionStrategy: Supervision.Decider = {
                case e: Exception =>
                    log.error("Beef in the stream, restarting...", e)
                    Supervision.Restart
            }

            val source = TestSource.probe[String]
            val stage1 = Flow[String].log("Stage 1", msg => if (msg.equals("Test Message 1")) throw new Exception("BLAH") else msg).withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))
            val stage2 = Flow[String].log("Stage 2", msg => msg)
            val stage3 = Flow[String].log("Stage 3", msg => msg)

            val (pub, sub) = source.via(stage1).via(stage2).via(stage3).toMat(TestSink.probe)(Keep.both).run()
            pub.sendNext("Test Message 1")
            sub.request(1)
            sub.expectNoMsg()
            pub.sendNext("Test Message 2")
            sub.requestNext("Test Message 2")
        }

        "handle supervision strategies for subsections of a Flow that uses a GraphStage component" in {

            val supervisionStrategy: Supervision.Decider = {
                case e: Exception =>
                    log.error("Beef in the stream, restarting...", e)
                    Supervision.Restart
            }

            val source = TestSource.probe[String]
            val stage1 = Flow[String].log("Stage 1", msg => if (msg.equals("Test Message 1")) throw new Exception("BLAH") else msg).withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))
            val stage2 = Flow.fromGraph(logStage[String](msg => msg))
            val stage3 = Flow[String].log("Stage 3", msg => msg)

            val (pub, sub) = source.via(stage1).via(stage2).via(stage3).toMat(TestSink.probe)(Keep.both).run()
            pub.sendNext("Test Message 1")
            sub.request(1)
            sub.expectNoMsg()
            pub.sendNext("Test Message 2")
            sub.requestNext("Test Message 2")
        }

        "handle supervision strategies for subsections of a Graph where a GraphStage component is present elsewhere in same Graph" in {

            val supervisionStrategy: Supervision.Decider = {
                case e: Exception =>
                    log.error("Beef in the stream, restarting...", e)
                    Supervision.Restart
            }

            val source = TestSource.probe[String]
            val sink = TestSink.probe[String]

            val graph = GraphDSL.create(source, sink)((_, _)) { implicit builder: GraphDSL.Builder[(TestPublisher.Probe[String], TestSubscriber.Probe[String])] =>
                (source, sink) =>
                import GraphDSL.Implicits._

                    val stage1 = Flow[String].log("Stage 1", msg => if (msg.equals("Test Message 1")) throw new Exception("BLAH") else msg).withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))
                    val stage2 = Flow.fromGraph(logStage[String](msg => msg))
                    val stage3 = Flow[String].log("Stage 3", msg => msg)

                source ~> stage1 ~> stage2 ~> stage3 ~> sink
                ClosedShape
            }

            val (pub, sub) = RunnableGraph.fromGraph(graph).run()

            pub.sendNext("Test Message 1")
            sub.request(1)
            sub.expectNoMsg()
            pub.sendNext("Test Message 2")
            sub.requestNext("Test Message 2")
        }

        "not honour supervision strategies for subsections of a Graph that uses a GraphStage component" in {

            val supervisionStrategy: Supervision.Decider = {
                case e: Exception =>
                    log.error("Beef in the stream, restarting...", e)
                    Supervision.Restart
            }

            val source = TestSource.probe[String]
            val sink = TestSink.probe[String]

            val streamException = new Exception("Test Message Exception")

            val graph = GraphDSL.create(source, sink)((_, _)) { implicit builder: GraphDSL.Builder[(TestPublisher.Probe[String], TestSubscriber.Probe[String])] =>
                (source, sink) =>
                    import GraphDSL.Implicits._

                    val stage1 = Flow[String].log("Stage 1", msg => msg)
                    val stage2 = Flow.fromGraph(exceptionStage[String](streamException)).withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))
                    val stage3 = Flow[String].log("Stage 3", msg => msg)

                    source ~> stage1 ~> stage2 ~> stage3 ~> sink
                    ClosedShape
            }

            val (pub, sub) = RunnableGraph.fromGraph(graph).run()

            pub.sendNext("Test Message 1")
            sub.requestNext("Test Message 1")
            pub.sendNext("Test Message 2")
            sub.request(1)
            sub.expectError(streamException)
            pub.sendNext("Test Message 3")
            sub.request(1)
            sub.expectNoMsg()
        }
    }

    def logStage[T](extract: T ⇒ String) = {
        new GraphStage[FlowShape[T, T]] {
            val in = Inlet[T]("logging stage in")
            val out = Outlet[T]("logging stage out")

            override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
                setHandler(in, new InHandler {
                    override def onPush() = {
                        val msg = grab(in)
                        log.info(s"Log stage received message: $msg")
                        push(out, msg)
                    }
                })
                setHandler(out, new OutHandler {
                    override def onPull() = pull(in)
                })
            }

            override def shape: FlowShape[T, T] = FlowShape(in, out)
        }
    }

        def exceptionStage[T](exception: Throwable) = {
            new GraphStage[FlowShape[T, T]] {
                val in = Inlet[T]("logging stage in")
                val out = Outlet[T]("logging stage out")
                override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
                    setHandler(in, new InHandler {
                        override def onPush() = {
                            val msg = grab(in)
                            if (msg.equals("Test Message 2")) throw exception
                            push(out, msg)
                        }})
                    setHandler(out, new OutHandler {
                        override def onPull() = pull(in)
                    })}
                override def shape: FlowShape[T, T] = FlowShape(in, out)
            }
    }
}
