package event;

import utils.Pair;
import activity.Activity;

/**
 * Assigns an execution time to an activity
 * 
 * @author erik
 * 
 */
public class TimedActivity extends Pair<TimeStamp, Activity> implements Comparable<TimedActivity> {

	protected TimedActivity(TimeStamp time, Activity act) {
		super(time, act);
	}
	
	public TimeStamp getTime() {
		return left;
	}
	
	public Activity getActivity() {
		return right;
	}
	
	@Override
    public String toString() {
		return left + " | " + right;
	}

	@Override
	public int compareTo(TimedActivity other) {
		return getTime().compareTo(other.getTime());
	}

}
