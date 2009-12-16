package org.lsc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronizeThreadPoolExecutor extends ThreadPoolExecutor {

	private static long keepAliveTime = 60;

	BlockingQueue<Runnable> queue;

	static final Logger LOGGER = LoggerFactory
			.getLogger(SynchronizeThreadPoolExecutor.class);

	public SynchronizeThreadPoolExecutor(int threads, int maxTasksCount) {
		super(threads, threads, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(maxTasksCount));
		queue = getQueue(); 
	}
	
	public void runTask(SynchronizeTask task) {
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
	
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		if(r instanceof SynchronizeTask) {
			SynchronizeTask task = (SynchronizeTask) r;
			t.setName(task.getSyncName() + "-" + t.getId());
		}
	}
}
