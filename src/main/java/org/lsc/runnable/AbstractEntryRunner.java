package org.lsc.runnable;

import java.util.Map.Entry;

import org.lsc.AbstractSynchronize;
import org.lsc.LscDatasets;
import org.lsc.Task;
import org.lsc.beans.InfoCounter;

public abstract class AbstractEntryRunner implements Runnable {
	
	protected String syncName;
	protected Entry<String, LscDatasets> id;
	protected InfoCounter counter;
	protected AbstractSynchronize abstractSynchronize;
	protected Task task;
	
	protected AbstractEntryRunner(final Task task, InfoCounter counter,
			AbstractSynchronize abstractSynchronize,
			Entry<String, LscDatasets> id) {
		this.syncName = task.getName();
		this.counter = counter;
		this.task = task;
		this.abstractSynchronize = abstractSynchronize;
		this.id = id;
	}
	
	public String getSyncName() {
		return syncName;
	}

	public InfoCounter getCounter() {
		return counter;
	}

	public AbstractSynchronize getAbstractSynchronize() {
		return abstractSynchronize;
	}

	public Entry<String, LscDatasets> getId() {
		return id;
	}
}
