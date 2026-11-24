package org.lsc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.lsc.runnable.AbstractEntryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This object is managing LSC tasks. It is now a wrapper for JDK 
 * ThreadPoolExecutor but may rely on a different implementation  
 * 
 * The queue size is computed based on the number of requested threads, 
 * and have a minimum value of 100.
 * 
 * The RejectedExecution handler will simply block when the queue is full,
 * so the caller will wait until a thread completes and consume the queue.
 *
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 *
 */
public class SynchronizeThreadPoolExecutor extends ThreadPoolExecutor {

	static long keepAliveTime = 60;
    
    /** The default queue size. We default to a small size */
    static final int DEFAULT_QUEUE_CAPACITY = 100;

	BlockingQueue<Runnable> queue;

	/** Default logger */
	final Logger LOGGER = LoggerFactory.getLogger(SynchronizeThreadPoolExecutor.class);

	public SynchronizeThreadPoolExecutor(int threads) {
	       // The queue size is computed based on the number of threads
        // A first approach is to require 10 slots in the queue per requested thread
        this(threads, Math.max(DEFAULT_QUEUE_CAPACITY, threads * 10));
	}
	
    public SynchronizeThreadPoolExecutor(int threads, int queueCapacity) {
        super(threads, threads, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(queueCapacity), 
                new RejectedExecutionHandler() {
        
        			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        				// this will block if the queue is full
        				try {
        						executor.getQueue().put(r);
        					} catch (InterruptedException e) {
        						Thread.currentThread().interrupt();
        						throw new RuntimeException(e);
        					}
        				}
        			}
        		);
		queue = getQueue(); 
	}

	/**
	 * Run a task
	 * In the threadpoolexecutor implementation, store it in the queue and let
	 * the pool consume it as soon as it can, in a FIFO way without any priority
	 * @param task the runnable object
	 */
	public void runTask(AbstractEntryRunner task) {
		execute(task);
	}
	
	/**
	 * This method just rename the thread with the taskname as prefix 
	 * suffixed by a threads counter
	 * @param t the thread
	 * @param r the runnable task
	 */
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		if(r instanceof AbstractEntryRunner) {
			AbstractEntryRunner task = (AbstractEntryRunner) r;
			t.setName(task.getSyncName() + "-" + t.getId());
		}
		super.beforeExecute(t, r);
	}
}
