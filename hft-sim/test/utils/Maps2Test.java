package utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class Maps2Test {

	@Test(expected=IllegalArgumentException.class)
	public void notEvenTest() {
		Maps2.fromPairs("Hello", "Goodbye", "Hello");
	}
	
	@Test
	public void smallTest() {
		assertEquals(ImmutableMap.of("Hello", "Goodbye"), Maps2.fromPairs("Hello", "Goodbye"));
		assertEquals(ImmutableMap.of("Hello", "4"), Maps2.fromPairs("Hello", 4));
		assertEquals(ImmutableMap.of("34", "Goodbye", "Hello", "3.14"), Maps2.fromPairs(34, "Goodbye", "Hello", 3.14));
	}

}
