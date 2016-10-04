package event;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;

import activity.Activity;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

/**
 * The EventQueue is a a queue of upcoming events which are composed of
 * activities to be executed. The actual events are abstracted in this
 * implementation, so this functions as a queue of activities. The main methods
 * that should be used are "add," which adds a single activity to the queue,
 * "addAll," which adds a collection of activities to the queue, and "remove,"
 * which removes the activity at the head of the queue. Events that are added at
 * the same TimeStamp will be dequeued in a uniform random order. To make an
 * event occur instantaneously give it a time of TimeStamp.IMMEDIATE;
 * 
 * Note that because of the dequeuing mechanism, if Activity A is supposed to
 * happen after Activity B, Activity A should queue up Activity B. Anything else
 * may not guarantee that A always happens before B.
 * 
 * @author ebrink
 */
public class EventQueue extends AbstractQueue<TimedActivity> {
	
	/*
	 * Invariant that no event is ever empty at the end of execution.
	 * 
	 * In general the rule should be, if one activity comes logically after
	 * another activity it should be scheduled by the activity that always
	 * proceeds it. Activities scheduled at the same time (even infinitely fast)
	 * may occur in any order.
	 */

	/*
	 * TODO Keep modCount to detect concurrent modification exception during
	 * iteration. See java collections source code.
	 */
	
	protected SortedMap<TimeStamp, Event> eventQueue;
	protected int size;
	protected Random rand;

	public EventQueue(Random seed) {
		eventQueue = Maps.newTreeMap();
		size = 0;
		rand = seed;
	}

	public EventQueue() {
		this(new Random());
	}

	@Override
	public void clear() {
		eventQueue.clear();
		size = 0;
	}

	@Override
	public Iterator<TimedActivity> iterator() {
		return new EventQueueIterator(eventQueue.entrySet().iterator());
	}

	/**
	 * Remove is not supported
	 */
	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException("Arbitrary remove is not supported");
	}

	@Override
	public boolean offer(TimedActivity act) {
		checkNotNull(act, "Activity");
		return add(act.getTime(), act.getActivity());
	}
	
	public boolean add(TimeStamp scheduledTime, Activity act) {
		checkNotNull(scheduledTime);
		checkNotNull(act);
		Event e = eventQueue.get(scheduledTime);
		if (e == null) {
			// This makes all event's use the same random number generator. This
			// may a little chaotic, but at the moment I can't think of a meaningful way
			// around it.
			e = new Event(scheduledTime, rand);
			eventQueue.put(scheduledTime, e);
		}
		e.add(act);
		size++;
		return true;
	}

	@Override
	public TimedActivity poll() {
		if (isEmpty()) return null;
		
		Iterator<Entry<TimeStamp, Event>> it = eventQueue.entrySet().iterator();
		Entry<TimeStamp, Event> first = it.next();
		TimedActivity head = new TimedActivity(first.getKey(), first.getValue().poll());
		--size;
		if (first.getValue().isEmpty()) // no empty Events
			it.remove();
		return head;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public TimedActivity peek() {
		if (isEmpty()) return null;
		Entry<TimeStamp, Event> first = eventQueue.entrySet().iterator().next();
		return new TimedActivity(first.getKey(), first.getValue().peek());
	}
	
	// Add collections in reverse. This ensures invariant.
	public boolean addAllOrdered(TimeStamp scheduledTime, Activity... acts) {
		if (acts.length == 0) return false;

		Event e = eventQueue.get(scheduledTime);
		if (e == null) {
			e = new Event(scheduledTime, rand);
			eventQueue.put(scheduledTime, e);
		}
		e.addAllOrderd(Arrays.asList(acts));
		size += acts.length;
		return true;
	}

	@Override
	public String toString() {
		if (isEmpty()) return "[]";
		Entry<TimeStamp, Event> first = eventQueue.entrySet().iterator().next();
		if (eventQueue.size() == 1) {
		    return "[" + first.getKey() + " | " + first.getValue() + "]";
		}
		
		return "[" + first.getKey() + " | " + first.getValue() + ", ...]";
	}

	private class EventQueueIterator implements Iterator<TimedActivity> {

		protected Iterator<Entry<TimeStamp, Event>> eventIterator;
		protected Iterator<Activity> activityIterator;
		protected TimeStamp currentTime;
		// These two booleans keep track of whether every activity found in the
		// event so far has been removed. If that's the case, and you remove an
		// activity when there are no more activities left in the event, then it
		// also removed the event. Thus preserving the invariant that there are
		// no empty events in the eventQueue.
		protected boolean removedEveryActivity;
		protected boolean removedCurrentActivity;

		protected EventQueueIterator(Iterator<Entry<TimeStamp, Event>> events) {
			eventIterator = events;
			activityIterator = Iterators.emptyIterator();
			currentTime = null;
			removedEveryActivity = true;
			removedCurrentActivity = false;
		}

		@Override
		public boolean hasNext() {
			return eventIterator.hasNext() || activityIterator.hasNext();
		}

		@Override
		public TimedActivity next() {
			removedEveryActivity &= removedCurrentActivity;
			removedCurrentActivity = false;
			if (!activityIterator.hasNext()) {
				Entry<TimeStamp, Event> next = eventIterator.next();
				activityIterator = next.getValue().iterator();
				currentTime = next.getKey();
				removedEveryActivity = true;
			}
			return new TimedActivity(currentTime, activityIterator.next());
		}

		@Override
		public void remove() {
			activityIterator.remove();
			size--;
			removedCurrentActivity = true;
			if (removedEveryActivity && !activityIterator.hasNext())
				eventIterator.remove();
		}

	}

}
