package event;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;

import utils.Collections3;
import utils.RandomQueue;
import activity.Activity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Queues;

/**
 * Class representing an event in time. Each event is really a queue of queues of activities. The
 * inner queues are always FIFO, the outer queue is LIFO for infinite time and Random for any other
 * time. The purpose of this organization is to enforce the invariant that added collections are
 * always executed in the order they are added. e.g. if you add a collection of two activities
 * [WithdrawOrder, SubmitOrder], the WithdrawOrder will always be executed before the SubmitOrder,
 * although other activities may be polled inbetween the two.
 * 
 * @author ebrink
 */
public class Event extends AbstractQueue<Activity> {

	// Is a LiFo queue for infinite time activities, and a random queue otherwise
	protected final Queue<Queue<Activity>> backedQueue;
	protected int size;

	public Event(TimeStamp time, Random seed) {
		size = 0;
		if (time.equals(TimeStamp.IMMEDIATE))
			backedQueue = Collections3.newArrayStack();
		else
			backedQueue = RandomQueue.create(seed);
	}
	
	public boolean addAllOrderd(Iterable<? extends Activity> acts) {
		if (Iterables.isEmpty(acts)) return false;		
		Queue<Activity> newQueue = Queues.newArrayDeque(acts);
		size += newQueue.size();
		return backedQueue.add(newQueue);
	}

	@Override
	public boolean offer(Activity e) {
		return addAllOrderd(ImmutableList.of(e));
	}

	@Override
	public Activity poll() {
		if (backedQueue.isEmpty()) return null;
		Queue<Activity> seq = backedQueue.poll();
		Activity ret = seq.poll();
		size--;
		if (!seq.isEmpty()) backedQueue.offer(seq);
		return ret;
	}

	@Override
	public Activity peek() {
		if (backedQueue.isEmpty()) return null;
		return backedQueue.peek().peek();
	}

	@Override
	public Iterator<Activity> iterator() {
		// TODO Remove from this iterator won't update size appropriately
		return Iterators.unmodifiableIterator(Iterables.concat(backedQueue).iterator());
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public String toString() {
		return super.toString();
	}

}
