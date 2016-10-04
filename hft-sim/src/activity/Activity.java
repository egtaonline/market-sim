package activity;

import event.TimeStamp;

/**
 * Base class for any method that Entities may invoke. Includes abstract
 * method execute and a timestamp.
 * 
 * Based on the Command Pattern.
 * 
 * @author ewah
 */
public abstract class Activity {

	/**
	 * Executes the activity on the given Entity.
	 * 
	 * @return hash table of generated Activity vectors, hashed by TimeStamp
	 */
	public abstract void execute(TimeStamp currentTime);
	
	// Every activity is unique
	@Override
	public final int hashCode() {
		return super.hashCode();
	}
	
	// Every activity is unique
	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " :: ";
	}
	
}
