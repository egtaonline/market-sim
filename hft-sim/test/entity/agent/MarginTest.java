package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import fourheap.Order.OrderType;

public class MarginTest {

	
	@Test
	public void emptyMargin() {
		Margin margin = new Margin();
		assertEquals(0, margin.getMaxAbsPosition());
		assertEquals(0, margin.values.size());
	}
	
	@Test
	public void basicMargin() {
		Margin margin = new Margin(1, new Random(), 1, 1);
		assertEquals(2, margin.values.size());
		assertEquals(1, margin.getMaxAbsPosition());
		// check that positive margin for selling, negative for buying
		assertEquals(new Double(1), margin.values.get(0));
		assertEquals(new Double(-1), margin.values.get(1));
	}
	
	@Test
	public void multiUnitMargin() {
		Margin margin = new Margin(2, new Random(), 0, 1);
		assertEquals(4, margin.values.size());
		for (Double value : margin.values) {
			double absValue = Math.abs(value.doubleValue());
			assertTrue("Margins outside range", absValue >= 0 && absValue <= 1);
		}
	}
	
	@Test
	public void getValueTest() {
		Margin margin = new Margin(2, new Random(), 0, 1);
		Double zero = new Double(0);
		Double m0 = margin.values.get(0);
		Double m1 = margin.values.get(1);
		Double m2 = margin.values.get(2);
		Double m3 = margin.values.get(3);
		
		assertEquals(m2, margin.getValue(0, OrderType.BUY));
		assertEquals(m3, margin.getValue(1, OrderType.BUY));
		assertEquals(m1, margin.getValue(0, OrderType.SELL));
		assertEquals(m0, margin.getValue(-1, OrderType.SELL));
		// returns 0 when exceed max position
		assertEquals(zero, margin.getValue(-2, OrderType.SELL));
		assertEquals(zero, margin.getValue(2, OrderType.BUY));
	}
	
	@Test
	public void setValueTest() {
		Margin margin = new Margin(2, new Random(), 0, 1);
		Double zero = new Double(0);
		Double m2 = margin.values.get(2);
		
		// basic set
		margin.setValue(0, OrderType.BUY, zero);
		assertNotEquals(m2, margin.getValue(0, OrderType.BUY));
		assertEquals(zero, margin.getValue(0, OrderType.BUY));
	
		// test setting outside valid position (won't change anything)
		margin.setValue(-2, OrderType.SELL, new Double(1));
		assertNotEquals(new Double(1), margin.getValue(-2, OrderType.SELL));
		assertEquals(zero, margin.getValue(-2, OrderType.SELL));
	}
	
	@Test
	public void setMultipleValueTest() {
		Margin margin = new Margin(2, new Random(), 0, 1);
		Double zero = new Double(0);
		
		// set
		margin.setValue(0, BUY, new Double(1));
		margin.setValue(1, BUY, new Double(2));
		margin.setValue(0, SELL, new Double(-1));
		margin.setValue(-1, SELL, zero);
		
		// check
		Double[] trueValues = new Double[4];
		trueValues[0] = zero;
		trueValues[1] = new Double(-1);
		trueValues[2] = new Double(1);
		trueValues[3] = new Double(2);
		for (int i = 0; i < 4; i++) {
			assertEquals(trueValues[i], margin.values.get(i));
		}
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			multiUnitMargin();
		}
	}
}
