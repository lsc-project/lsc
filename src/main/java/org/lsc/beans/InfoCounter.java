package org.lsc.beans;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This object is storing counters across all tasks Update methods are specified
 * as synchronized to avoid loosing counts of operations
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class InfoCounter {

    private AtomicInteger countAll = new AtomicInteger(0);
    private AtomicInteger countError = new AtomicInteger(0);
    private AtomicInteger countModifiable = new AtomicInteger(0);
    private AtomicInteger countCompleted = new AtomicInteger(0);

    public void incrementCountAll() {
        countAll.incrementAndGet();
    }

    public void incrementCountError() {
        countError.incrementAndGet();
	}

    public void incrementCountModifiable() {
        countModifiable.incrementAndGet();
	}

    public void incrementCountCompleted() {
        countCompleted.incrementAndGet();
	}

	/**
	 * Return the count of all objects concerned by synchronization It does not
	 * include objects in data source that are not selected by requests or
	 * filters, but it includes any of the objects retrieved from the data
	 * source
	 * 
	 * @return the count of all objects taken from the data source
	 */
    public int getCountAll() {
        return countAll.get();
	}

	/**
	 * Return the count of all objects that have encountered an error while
	 * synchronizing, either for a technical or for a functional reason
	 * 
	 * @return the number of objects in error
	 */
    public int getCountError() {
        return countError.get();
	}

	/**
	 * Return the count of all objects that should be modify
	 * 
	 * @return the count of all updates to do
	 */
    public int getCountModifiable() {
        return countModifiable.get();
	}

	/**
	 * Return the count of all objects that have been embraced in a data
	 * modification successfully
	 * 
	 * @return the count of all successful updates
	 */
    public int getCountCompleted() {
        return countCompleted.get();
	}
}
