package entity.market;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PriceTest {

	@Test
	public void toDollarStringTest() {
		assertEquals("$100.00", new Price(100000).toDollarString());
		assertEquals("$100.01", new Price(100010).toDollarString());
		assertEquals("$100.001", new Price(100001).toDollarString());
		assertEquals("-$100.00", new Price(-100000).toDollarString());
		assertEquals("-$100.01", new Price(-100010).toDollarString());
		assertEquals("-$100.001", new Price(-100001).toDollarString());
	}
	
	@Test
	public void infinitePriceTest() {
		// note INF prices occur in the AA strategy
		assertEquals(Price.INF, new Price((double) Integer.MAX_VALUE));
		assertEquals(Price.INF, new Price((double) Integer.MAX_VALUE+1));
		assertEquals(Price.INF, new Price((double) Integer.MAX_VALUE+3));
	}

}
