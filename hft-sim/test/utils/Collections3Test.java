package utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class Collections3Test {

	private static final Random rand = new Random();
	
	/**
	 * Test that polling random elements returns them in the reverse order with a backed array list
	 */
	@Test
	public void pollTest() {
		List<Integer> random = Lists.newArrayList();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i)
			random.add(rand.nextInt());
		
		Queue<Integer> q = Collections3.asLifoQueue(Lists.newArrayList(random)); // Copy backed list
		for (int i : Lists.reverse(random))
			assertEquals(i, (int) q.poll());
	}
	
	/**
	 * Test that polling random elements returns them in the reverse order with a backed linked list
	 */
	@Test
	public void pollListTest() {
		List<Integer> random = Lists.newArrayList();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i)
			random.add(rand.nextInt());
		
		Queue<Integer> q = Collections3.asLifoQueue(Lists.newLinkedList(random)); // Copy backed list
		for (int i : Lists.reverse(random))
			assertEquals(i, (int) q.poll());
	}
	
	/**
	 * Test that removing random elements returns them in the reverse order with a backed array list
	 */
	@Test
	public void removeTest() {
		List<Integer> random = Lists.newArrayList();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i)
			random.add(rand.nextInt());
		
		Queue<Integer> q = Collections3.asLifoQueue(Lists.newArrayList(random)); // Copy backed list
		for (int i : Lists.reverse(random))
			assertEquals(i, (int) q.poll());
	}
	
	/**
	 * Tests that andding and removing random elements with the queue not empty still return elements in proper reverse order
	 */
	@Test
	public void existingElementsTest() {
		Queue<Integer> q = Collections3.asLifoQueue(Lists.<Integer> newArrayList());
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i)
			q.add(rand.nextInt());
		
		for (int i = 0; i < rand.nextInt(10) + 10; ++i) {
			List<Integer> rands = Lists.newArrayList();
			n = rand.nextInt(1000) + 1000;
			for (int j = 0; j < n; ++j) {
				int e = rand.nextInt();
				q.add(e);
				rands.add(e);
			}
			
			for (int j : Lists.reverse(rands))
				assertEquals(j, (int) q.poll());
		}
	}
	
	/**
	 * Test that modifications to the underlying array still effect the queue
	 */
	@Test
	public void backedArrayTest() {
		List<Integer> random = Lists.newArrayList();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i)
			random.add(rand.nextInt());
		
		Queue<Integer> q = Collections3.asLifoQueue(random); // Don't copy backed List
		for (Iterator<Integer> it = random.iterator(); it.hasNext();) {
			it.next();
			if (rand.nextBoolean()) it.remove();
		}
		
		for (int i : Lists.reverse(ImmutableList.copyOf(random)))
			assertEquals(i, (int) q.poll());
	}
	
	/**
	 * Test that all methods on an empty array work
	 */
	@Test
	public void emptyTest() {
		Queue<Integer> q = Collections3.newArrayStack();
		checkEmpty(q);
		for (int i = 0; i < rand.nextInt(10) + 10; ++i) {
			int n = rand.nextInt(1000);
			for (int j = 0; j < n; ++j)
				q.add(rand.nextInt());
			assertTrue(q.isEmpty() ^ n != 0);
			for (int j = 0; j < n; ++j)
				q.remove();
			checkEmpty(q);
		}
	}

	/**
	 * Checks the queue for various things to make sure it behaves properly when
	 * empty
	 * 
	 * @param q
	 */
	private static void checkEmpty(Queue<?> q) {
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
		assertNull(q.peek());
		assertNull(q.poll());
		assertFalse(q.iterator().hasNext());
		boolean exception = false;
		try {
			q.element();
		} catch (NoSuchElementException e) {
			exception = true;
		}
		assertTrue(exception);
		exception = false;
		try {
			q.remove();
		} catch (NoSuchElementException e) {
			exception = true;
		}
	}
	
	@Test
	public void randomTests() {
		for (int i = 0; i < 100; ++i) {
			pollTest();
			pollListTest();
			removeTest();
			existingElementsTest();
			backedArrayTest();
			emptyTest();
		}
	}

}
