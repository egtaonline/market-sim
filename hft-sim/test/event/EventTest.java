package event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import activity.Activity;
import activity.MockActivity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class EventTest {

	protected static final Random rand = new Random();
	
	@Test
	public void orderInvariantTest() {
		TimeStamp time = TimeStamp.create(rand.nextInt(3) - 1);
		Event e = new Event(time, rand);
		List<List<Activity>> order = Lists.newArrayList();
		for (int i = 0; i < rand.nextInt(8) + 2; i++) {
			List<Activity> acts = Lists.newArrayList();
			for (int j = 0; j < rand.nextInt(9) + 1; j++)
				acts.add(new MockActivity());
			e.addAllOrderd(acts);
			order.add(acts);
		}
		
		Builder<Activity> builder = ImmutableList.builder();
		while (!e.isEmpty()) builder.add(e.poll());
		ImmutableList<Activity> execOrder = builder.build();
		
		for (List<Activity> acts : order) {
			Builder<Integer> actOrder = ImmutableList.builder();
			for (Activity act : acts) actOrder.add(execOrder.indexOf(act));
			
			assertTrue(Ordering.natural().isOrdered(actOrder.build()));
		}
	}
	
	@Test
	public void multiOrderInvariantTest() {
		for (int i = 0; i < 1000; i++) orderInvariantTest();
	}
	
	@Test
	public void orderLifoTest() {
		Event e = new Event(TimeStamp.IMMEDIATE, rand);
		List<List<Activity>> order = Lists.newArrayList();
		for (int i = 0; i < rand.nextInt(8) + 2; i++) {
			List<Activity> acts = Lists.newArrayList();
			for (int j = 0; j < rand.nextInt(9) + 1; j++)
				acts.add(new MockActivity());
			e.addAllOrderd(acts);
			order.add(acts);
		}
		Collections.reverse(order);
		
		for (Activity act : Iterables.concat(order))
			assertEquals(act, e.poll());
	}
	
	@Test
	public void multiOrderLifoTest() {
		for (int i = 0; i < 1000; i++) orderLifoTest();
	}

}
