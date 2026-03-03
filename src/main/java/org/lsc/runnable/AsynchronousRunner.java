package org.lsc.runnable;

import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.lsc.AbstractSynchronize;
import org.lsc.LscDatasets;
import org.lsc.Task;
import org.lsc.beans.InfoCounter;
import org.lsc.exception.LscServiceException;
import org.lsc.service.IAsynchronousService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsynchronousRunner implements Runnable {

	static final Logger LOGGER = LoggerFactory.getLogger(AsynchronousRunner.class);

	private AbstractSynchronize abstractSynchronize;
	private Task task;
	private InfoCounter counter;

	public AsynchronousRunner(Task task, AbstractSynchronize abstractSynchronize) {
		this.task = task;
		this.abstractSynchronize = abstractSynchronize;
	}

	public void run() {
        int nThreads = abstractSynchronize.getThreads();
		counter = new InfoCounter();

		final IAsynchronousService asyncService;
		final boolean fromSource;
		
		if (task.getDestinationService() instanceof IAsynchronousService) {
		    asyncService = (IAsynchronousService) task.getDestinationService();
			fromSource = false;
		} else if (task.getSourceService() instanceof IAsynchronousService) {
		    asyncService = (IAsynchronousService) task.getSourceService();
		    fromSource = true;
		} else {
			LOGGER.error("LSC should never reach this point! Please consider debugging the code because "
			        + "we are trying to launch an asynchronous sync without any asynchronous service!"); 
			return;
		}

		LOGGER.debug("Asynchronous synchronize {}", task.getName());
        LOGGER.debug("Running tasks with {} threads", nThreads);
        
        // We need to protect the getNextid() function against concurrent access
        Lock lock = new ReentrantLock();

        // Supplier of tasks, will be called by the thread pool,
        // returns null if no more tasks to run
        Supplier<Callable<Object>> supplier = () -> {
            try {
                Entry<String, LscDatasets> nextId;

                // Fetch an entry from the source
                lock.lock();
                
                try {
                    // This is a blocking call, we may wait until we get
                    // a response
                    nextId = asyncService.getNextId();
                } finally {
                    lock.unlock();
                }
                
                if (nextId == null) {
                    // we're done
                    return null;
                }
                
                // Now compute the modifications
                return new Callable<Object>() {
                    @Override
                    public String call() throws Exception {
                        // Process the entry
                        SynchronizeEntryRunner entryRunner = new SynchronizeEntryRunner(
                                task, counter, abstractSynchronize, nextId, fromSource);
                        
                        entryRunner.run();
                        
                        return nextId.getKey() + " processed";
                    }
                };
            } catch (LscServiceException e) {
                counter.incrementCountError();
                abstractSynchronize.logActionError(null, null, e);
            }
            
            return null;
        };

        // Thread pool, each thread loops until the task supplier returns a null Callable
        // Basically, fetch an entry from the source, and process the associated callable
        // It's a fixed sized thread pool, once created the threads are up forever
        for (int i = 0; i < nThreads; i++) {
            int id = i;
            
            new Thread(() -> {
                while (true) {
                    try {
                        Callable<Object> task = supplier.get();
                        
                        if (task != null) {
                            Object result = task.call();
                            LOGGER.debug("Thread {} ran task {}", id, result);
                        } else {
                            LOGGER.info("--- Thread {}: no task to run.", id);
                        }
                    } catch (Exception whatShallWeDo) {
                        whatShallWeDo.printStackTrace();
                    }
                }
            }).start();
        }
    }

	public InfoCounter getCounter() {
		return counter;
	}
}
