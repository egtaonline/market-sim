package systemmanager;

import java.util.Random;

import event.TimeStamp;
import event.TimedActivity;

/**
 * Class that provides the ability to "simulate" an even manager. This
 * guarantees that residual effects of actions get propagated (such as
 * information spreading), as well as the ability to set the time
 * 
 * @author erik
 * 
 */
public class Executor extends Scheduler {

	private static final Random rand = new Random();
	
	public Executor() {
		super(rand);
	}

	/**
	 * Also sets the time to "time." Thus after you call executeUntil(x), you
	 * can now call executeActivity(a) to have a execute immediately at time x.
	 */
	@Override
	public void executeUntil(TimeStamp time) {
		super.executeUntil(time);
		this.currentTime = time;
	}
	
	public TimedActivity peek() {
		return eventQueue.peek();
	}
	
	@Override
    public boolean isEmpty() {
		return eventQueue.isEmpty();
	}

}
