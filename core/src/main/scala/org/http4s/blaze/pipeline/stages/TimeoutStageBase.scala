package org.http4s.blaze.pipeline.stages

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import org.http4s.blaze.pipeline.{Command, MidStage}
import org.http4s.blaze.util.{Cancellable, TickWheelExecutor}

import org.http4s.blaze.util.Execution.scheduler
import java.util.concurrent.atomic.AtomicReference

abstract class TimeoutStageBase[T](timeout: Duration, exec: TickWheelExecutor) extends MidStage[T, T] { stage =>

  // Constructor
  require(timeout.isFinite() && timeout.toMillis != 0, s"Invalid timeout: $timeout")

  override def name: String = s"${this.getClass.getName} Stage: $timeout"

  /////////// Private impl bits //////////////////////////////////////////

  private val lastTimeout = new AtomicReference[Cancellable](null)

  private val killswitch = new Runnable {
    override def run(): Unit = {
      stage.sendOutboundCommand(Command.Disconnect)
    }
  }

  protected def setAndCancel(next: Cancellable): Unit = {
    val prev = lastTimeout.getAndSet(next)
    if (prev != null) prev.cancel()
  }

  /////////// Pass through implementations ////////////////////////////////

  override def readRequest(size: Int): Future[T] = channelRead(size)

  override def writeRequest(data: T): Future[Unit] = channelWrite(data)

  override def writeRequest(data: Seq[T]): Future[Unit] = channelWrite(data)

  /////////// Protected impl bits //////////////////////////////////////////

  override protected def stageShutdown(): Unit = {
    val prev = lastTimeout.getAndSet(null)
    if (prev != null) prev.cancel()
    super.stageShutdown()
  }

  final protected def resetTimeout(): Unit = setAndCancel(scheduler.schedule(killswitch, timeout))

  final protected def cancelTimeout(): Unit = setAndCancel(null)

  final protected def startTimeout(): Unit = resetTimeout()


}
