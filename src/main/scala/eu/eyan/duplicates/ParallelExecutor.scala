package eu.eyan.duplicates

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import eu.eyan.util.java.lang.RunnablePlus
import eu.eyan.log.Log
import scala.collection.mutable.MutableList
import java.util.concurrent.atomic.AtomicInteger

class ParallelExecutor[T](processJob: (T, T => Option[T]) => T) {

  val lock = new Object
  val activeJobs = new AtomicInteger

  val maxJobs = Runtime.getRuntime.availableProcessors * 15 / 10
  val pool = Executors.newFixedThreadPool(maxJobs)
  val chunks = MutableList[T]()

  private def runAsyncOrSync(job: T): Option[T] = {
    if (activeJobs.get < maxJobs) {
      activeJobs.incrementAndGet
      pool.execute(RunnablePlus.runnable({
        try {
          val chunk = processJob(job, runAsyncOrSync)
          lock.synchronized { chunks += chunk }
        } finally activeJobs.decrementAndGet
      }))
      None
    } else Some(processJob(job, runAsyncOrSync))
    
  }

  def executeAndWaitUntilDone(firstChunk: T) = {
    runAsyncOrSync(firstChunk)
    var time = 0
    while (activeJobs.get > 0) {
      Thread.sleep(10)
      time += 10
      if (time % 1000 == 0) Log.info("Running threads " + activeJobs + " of " + Thread.activeCount) // FIXME make it debug/trace level
    }
    pool.shutdown
    pool.awaitTermination(1, TimeUnit.HOURS)
    chunks.toList
  }
}