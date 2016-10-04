package utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

public class RandomQueueTest {
	
	private static Random rand;
	
	@BeforeClass
	public static void setup() {
		rand = new Random();
	}
	
	@Test
	public void emptyConstructorTest() {
		RandomQueue<Integer> a = RandomQueue.create();
		assertTrue(a.isEmpty());
	}
	
	@Test
	public void collectionConstructorTest() {
		Collection<Integer> numbers = randomNumbers(10);
		RandomQueue<Integer> a = RandomQueue.create(numbers);
		for (Integer i : numbers)
			assertTrue(a.contains(i));
	}
	
	@Test
	public void randomSeedTest() {
		long seed = rand.nextLong();
		Collection<Integer> numbers = randomNumbers(10);
		RandomQueue<Integer> a = RandomQueue.create(numbers, new Random(seed));
		RandomQueue<Integer> b = RandomQueue.create(numbers, new Random(seed));
		
		Iterator<Integer> ita = a.iterator();
		Iterator<Integer> itb = b.iterator();
		while (ita.hasNext())
			assertTrue(ita.next().equals(itb.next()));
		
		RandomQueue<Integer> c = RandomQueue.create(new Random(seed));
		c.addAll(numbers);
		
		itb = b.iterator();
		Iterator<Integer> itc = c.iterator();
		while (itc.hasNext())
			assertTrue(itb.next().equals(itc.next()));
	}
	
	@Test
	public void clearTest() {
		RandomQueue<Integer> a = RandomQueue.create(randomNumbers(10));
		assertFalse(a.isEmpty());
		a.clear();
		assertTrue(a.isEmpty());
	}
	
	@Test
	public void removeTest() {
		RandomQueue<Integer> a = RandomQueue.create(Ints.asList(1, 2, 3, 4, 5));
		assertTrue(a.contains(4));
		a.remove(4);
		assertFalse(a.contains(4));
		assertTrue(a.containsAll(Ints.asList(1, 2, 3, 5)));
	}
	
	@Test
	public void permutationTest() {
		// Note, this test could fail due to inconceivably small random chance ~1/1000! (that's factorial,
		// not an exclamation)
		Collection<Integer> numbers = randomNumbers(1000);
		RandomQueue<Integer> a = RandomQueue.create(numbers);
		
		Iterator<Integer> ita = a.iterator();
		Iterator<Integer> itb = numbers.iterator();
		while (ita.hasNext())
			if (!ita.next().equals(itb.next()))
				return;
		fail();
	}
	
	@Test
	public void offerPermutationTest() {
		// Note, this test could fail due to inconceivably small random chance as well. 1/1000^1000
		for (int i = 0; i < 1000; i++) {
			RandomQueue<Integer> a = RandomQueue.create(randomNumbers(1000));
			a.add(0);
			if (a.peek() != 0)
				return;
		}
		fail();
	}
	
	@Test
	public void offerNonpermutationTest() {
		// Note, this test could fail due to inconceivably small random chance as well. 1/2^10000 ~ 1/1000^1000
		for (int i = 0; i < 10000; i++) {
			RandomQueue<Integer> a = RandomQueue.create(randomNumbers(1));
			a.add(0);
			if (a.peek() == 0)
				return;
		}
		fail();
	}
	
	public static Collection<Integer> randomNumbers(int size) {
		Collection<Integer> numbers = Lists.newArrayListWithCapacity(size);
		for (int i = 0; i < size; i++)
			numbers.add(rand.nextInt());
		return numbers;
	}

}
