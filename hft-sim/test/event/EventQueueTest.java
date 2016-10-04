package event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.Test;

import activity.Activity;
import activity.MockActivity;

import com.google.common.collect.ImmutableList;

public class EventQueueTest {

	@Test
	public void basicUsageTest() {
		EventQueue q = new EventQueue();
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
		
		Activity first = new MockActivity();
		q.add(TimeStamp.create(1), first);
		assertFalse(q.isEmpty());
		assertEquals(1, q.size());
		assertEquals(first, q.peek().getActivity());
		
		assertEquals(first, q.remove().getActivity());
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
		
		Activity second = new MockActivity();
		Activity third = new MockActivity();
		q.add(TimeStamp.create(3), third);
		q.add(TimeStamp.create(2), second);
		assertEquals(second, q.peek().getActivity());
		assertEquals(2, q.size());
		
		Activity inf1 = new MockActivity();
		Activity inf2 = new MockActivity();
		q.add(TimeStamp.IMMEDIATE, inf1);
		assertEquals(3, q.size());
		assertEquals(inf1, q.poll().getActivity());
		assertEquals(2, q.size());
		q.add(TimeStamp.IMMEDIATE, inf2);
		assertEquals(3, q.size());
		assertEquals(inf2, q.poll().getActivity());
		assertEquals(2, q.size());
		
		assertEquals(second, q.peek().getActivity());
		q.addAllOrdered(TimeStamp.IMMEDIATE, inf1, inf2);
		assertEquals(4, q.size());
		q.poll();
		q.poll();
		assertEquals(second, q.peek().getActivity());
		assertEquals(2, q.size());
		
		assertEquals(second, q.poll().getActivity());
		assertEquals(third, q.poll().getActivity());
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
	}
	
	@Test
	public void emptyPeekTest() {
		assertEquals(null, new EventQueue().peek());
	}
	
	@Test
	public void emptyPollTest() {
		assertEquals(null, new EventQueue().poll());
	}
	
	@Test (expected = NoSuchElementException.class)
	public void emptyElementTest() {
		new EventQueue().element();
	}
	
	@Test (expected = NoSuchElementException.class)
	public void emptyRemoveTest() {
		new EventQueue().remove();
	}
	
	@Test
	public void clearTest() {
		EventQueue q = new EventQueue();
		q.addAllOrdered(TimeStamp.ZERO, new MockActivity(), new MockActivity(),
				new MockActivity());
		assertFalse(q.isEmpty());
		q.clear();
		assertTrue(q.isEmpty());
	}
	
	@Test
	public void pollTest() {
		EventQueue q = new EventQueue();

		Activity first = new MockActivity();
		Activity second = new MockActivity();
		Activity third = new MockActivity();

		q.add(TimeStamp.create(1), first);
		q.add(TimeStamp.create(2), second);
		q.add(TimeStamp.create(3), third);
		
		// Check that poll will return activities in correct order & update size
		assertEquals(first, q.poll().getActivity());
		assertEquals(2, q.size());
		assertEquals(second, q.poll().getActivity());
		assertEquals(1, q.size());
		assertEquals(third, q.poll().getActivity());
		assertTrue(q.isEmpty());
		assertEquals(null, q.poll());
	}
	
	@Test
	public void addAllTest() {
		EventQueue q = new EventQueue();
			
		Activity first = new MockActivity();
		Activity second = new MockActivity();
		Activity third = new MockActivity();
		
		assertEquals("Incorrect initial size", 0, q.size());
		q.add(TimeStamp.create(1), first);
		q.add(TimeStamp.create(2), second);
		
		// Verify activities added correctly
		assertEquals("Size not updated", 2, q.size());
		assertEquals(first, q.poll().getActivity());
		assertEquals(second, q.poll().getActivity());
		assertTrue(q.isEmpty());
		
		// Verify correct order with list of activities not in chronological order
		q.add(TimeStamp.create(3), third);
		q.add(TimeStamp.create(2), second);
		assertEquals("Size not updated", 2, q.size());
		assertEquals(second, q.poll().getActivity());
		assertEquals(third, q.poll().getActivity());
		assertTrue(q.isEmpty());
	}
	
	@Test
	public void addImmediateTest() {
		EventQueue q = new EventQueue();
		
		Activity first = new MockActivity();
		Activity second = new MockActivity();
		Activity third = new MockActivity();
		Activity zero = new MockActivity();
		
		q.addAllOrdered(TimeStamp.IMMEDIATE, third, first);
		// Verify that third always will be at top of queue, since immediate
		assertEquals("Size not updated", 2, q.size());
		assertEquals(third, q.peek().getActivity());
				
		q.add(TimeStamp.IMMEDIATE, second);
		assertEquals("Size not updated", 3, q.size());
		q.add(TimeStamp.ZERO, zero);
		assertEquals("Size not updated", 4, q.size());
		
		// Verify that order correct (LIFO for immediate) but in order of insertion
		assertEquals(second, q.poll().getActivity());
		assertEquals(third, q.poll().getActivity());
		assertEquals(first, q.poll().getActivity());
		assertEquals(zero, q.poll().getActivity());
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			addImmediateTest();
		}
	}
	
	
	@Test
	public void toArrayTest() {
		EventQueue q = new EventQueue();

		List<? extends Activity> acts = ImmutableList.of(new MockActivity(),
				new MockActivity(), new MockActivity());
		for (int i = 0; i < acts.size(); ++i)
			q.add(TimeStamp.create(i), acts.get(i));
		
		for (Object o : q.toArray())
			assertTrue(acts.contains(((TimedActivity) o).getActivity()));
		for (TimedActivity a : q.toArray(new TimedActivity[0]))
			assertTrue(acts.contains(a.getActivity()));
	}
	
	@Test
	public void iteratorTest() {
		EventQueue q = new EventQueue();

		Activity first = new MockActivity();
		Activity second = new MockActivity();
		Activity third = new MockActivity();
		List<Activity> acts = ImmutableList.of(first, second, third);

		q.addAllOrdered(TimeStamp.ZERO, acts.toArray(new Activity[3]));
		for (TimedActivity a : q) {
			assertTrue(acts.contains(a.getActivity()));
		}
		
		assertEquals(3, q.size());
	}
	
	public void randomDeterminismTest() {
		Random rand = new Random();
		long seed = rand.nextLong();
		EventQueue q1 = new EventQueue(new Random(seed));
		EventQueue q2 = new EventQueue(new Random(seed));
		
		for (int i = 0; i < 1000; i++) {
			Activity a = new MockActivity();
			TimeStamp t = TimeStamp.create(rand.nextInt(100));
			q1.add(t, a);
			q2.add(t, a);
		}
		
		while (!q1.isEmpty())
			assertEquals(q1.remove(), q2.remove());
	}
}
