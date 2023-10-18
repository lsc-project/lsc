package org.lsc.beans;

/**
 * This object is storing counters across all tasks Update methods are specified
 * as synchronized to avoid loosing counts of operations
 * 
 * @author Sebastien Bahloul &lt;seb@lsc-project.org&gt;
 */
public class InfoCounter {

	private int countAll = 0;
	private int countError = 0;
	private int countModifiable = 0;
	private int countCompleted = 0;

	public synchronized void incrementCountAll() {
		countAll++;
	}

	public synchronized void incrementCountError() {
		countError++;
	}

	public synchronized void incrementCountModifiable() {
		countModifiable++;
	}

	public synchronized void incrementCountCompleted() {
		countCompleted++;
	}

	/**
	 * Return the count of all objects concerned by synchronization It does not
	 * include objects in data source that are not selected by requests or
	 * filters, but it includes any of the objects retrieved from the data
	 * source
	 * 
	 * @return the count of all objects taken from the data source
	 */
	public synchronized int getCountAll() {
		return countAll;
	}

	/**
	 * Return the count of all objects that have encountered an error while
	 * synchronizing, either for a technical or for a functional reason
	 * 
	 * @return the number of objects in error
	 */
	public synchronized int getCountError() {
		return countError;
	}

	/**
	 * Return the count of all objects that should be modify
	 * 
	 * @return the count of all updates to do
	 */
	public synchronized int getCountModifiable() {
		return countModifiable;
	}

	/**
	 * Return the count of all objects that have been embraced in a data
	 * modification successfully
	 * 
	 * @return the count of all successful updates
	 */
	public synchronized int getCountCompleted() {
		return countCompleted;
	}
}
