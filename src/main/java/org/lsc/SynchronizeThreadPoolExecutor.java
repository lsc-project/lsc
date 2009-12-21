package org.lsc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This object is managing LSC tasks. It is now a wrapper for JDK 
 * ThreadPoolExecutor but may rely on a different implementation  
 * @author Sebastien Bahloul <seb@lsc-project.org>
 *
 */
public class SynchronizeThreadPoolExecutor extends ThreadPoolExecutor {

	static long keepAliveTime = 60;

	BlockingQueue<Runnable> queue;

	/** Default logger */
	final Logger LOGGER = LoggerFactory
			.getLogger(SynchronizeThreadPoolExecutor.class);

	protected SynchronizeThreadPoolExecutor(int threads, int maxTasksCount) {
		super(threads, threads, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(maxTasksCount));
		queue = getQueue(); 
	}

	/**
	 * Run a task
	 * In the threadpoolexecutor implementation, store it in the queue and let
	 * the pool consume it as soon as it can, in a FIFO way without any priority
	 * @param task the runnable object
	 */
	protected void runTask(SynchronizeTask task) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Task count.." + getTaskCount());
			LOGGER.debug("Queue Size before assigning the task.."
					+ queue.size());
		}
		execute(task);
		this.beforeExecute(new Thread(task.getSyncName() + "-" + task.getId().getKey()), task);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Queue Size after assigning the task.."
							+ queue.size());
			LOGGER.debug("Pool Size after assigning the task.." + getActiveCount());
			LOGGER.debug("Task count.." + getTaskCount());
			LOGGER.debug("Task count.." + queue.size());
		}

	}
	
	/**
	 * This method just rename the thread with the taskname as prefix 
	 * suffixed by a threads counter
	 * @param t the thread
	 * @param r the runnable task
	 */
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		if(r instanceof SynchronizeTask) {
			SynchronizeTask task = (SynchronizeTask) r;
			t.setName(task.getSyncName() + "-" + t.getId());
		}
	}
}
