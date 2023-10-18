package org.lsc.runnable;

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.lsc.AbstractSynchronize;
import org.lsc.LscDatasets;
import org.lsc.SynchronizeThreadPoolExecutor;
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
		counter = new InfoCounter();

		SynchronizeThreadPoolExecutor threadPool = new SynchronizeThreadPoolExecutor(abstractSynchronize.getThreads());

		Entry<String, LscDatasets> nextId = null;
		try {
			IAsynchronousService aService = null;
			boolean fromSource = true;
			if (task.getDestinationService() instanceof IAsynchronousService) {
				aService = (IAsynchronousService) task.getDestinationService();
				fromSource = false;
			} else if (task.getSourceService() instanceof IAsynchronousService) {
				aService = (IAsynchronousService) task.getSourceService();
			} else {
				LOGGER.error("LSC should never reach this point! Please consider debugging the code because we are trying to launch an asynchronous sync without any asynchronous service!"); 
				return;
			}

			LOGGER.debug("Asynchronous synchronize {}", task.getName());

			boolean interrupted = false;
			while (!interrupted) {
				nextId = aService.getNextId();
				if (nextId != null) {
					threadPool.runTask(new SynchronizeEntryRunner(task, counter, abstractSynchronize, nextId, fromSource));
				} else {
					try {
						Thread.sleep(aService.getInterval());
					} catch (InterruptedException e) {
						LOGGER.debug("Synchronization thread interrupted !");
						interrupted = true;
					}
				}
			}
		} catch (LscServiceException e) {
			counter.incrementCountError();
			abstractSynchronize.logActionError(null, nextId, e);
		}

		try {
			threadPool.shutdown();
			threadPool.awaitTermination(abstractSynchronize.getTimeLimit(), TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("Tasks terminated according to time limit: " + e.toString(), e);
			LOGGER.info("If you want to avoid this message, " + "increase the time limit by using dedicated parameter.");
		}

	}

	public InfoCounter getCounter() {
		return counter;
	}
}
