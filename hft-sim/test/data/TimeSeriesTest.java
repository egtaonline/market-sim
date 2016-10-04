package data;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.google.common.primitives.Doubles;

public class TimeSeriesTest {
	
	protected final static Random rand = new Random();

	@Test
	public void lengthTest() {
		TimeSeries t;
		List<Double> values;
		
		t = new TimeSeries();
		values = t.sample(1, 100);
		assertEquals(100, values.size());
		
		t = new TimeSeries();
		values = t.sample(2, 100);
		assertEquals(50, values.size());
		
		t = new TimeSeries();
		values = t.sample(3, 100);
		assertEquals(33, values.size());
		
		for (int i = 0; i < 100; i++) {
			int length = rand.nextInt(500) + 100;
			int period = rand.nextInt(length) + 1;
			t = new TimeSeries();
			values = t.sample(period, length);
			assertEquals(length / period, values.size());
		}
	}
	
	@Test
	public void duplicateTest() {
		TimeSeries t;
		List<Double> values;
		
		t = new TimeSeries();
		values = t.sample(1, 100);
		for (double d : values)
			assertTrue(Double.isNaN(d));
		
		t = new TimeSeries();
		t.add(0, 5.6);
		values = t.sample(1, 100);
		for (double d : values)
			assertEquals(5.6, d, 0);
	}
	
	@Test
	public void truncationTest() {
		TimeSeries t;
		List<Double> values;
		
		t = new TimeSeries();
		t.add(0, 5.6);
		t.add(101, 3.6);
		values = t.sample(1, 100);
		for (double d : values)
			assertEquals(5.6, d, 0);
		
		t = new TimeSeries();
		t.add(0, 5.6);
		t.add(99, 3.6);
		values = t.sample(1, 100);
		for (double d : values.subList(0, 99))
			assertEquals(5.6, d, 0);
		assertEquals(3.6, values.get(99), 0);
	}
	
	@Test
	public void filterTest() {
		TimeSeries t;
		List<Double> values;
		
		t = new TimeSeries();
		t.add(0, 4.5);
		t.add(50, Double.NaN);
		values = t.sample(2, 100, not(equalTo(Double.NaN)));
		assertEquals(50, values.size());
		for (double d : values)
			assertEquals(4.5, d, 0);
		
		t = new TimeSeries();
		t.add(0, 5.6);
		t.add(25, 7.4);
		t.add(30, Double.NaN);
		t.add(50, 3.9);
		
		values = t.sample(25, 75);
		assertEquals(3, values.size());
		assertEquals(Doubles.asList(5.6, Double.NaN, 3.9), values);
		
		values = t.sample(25, 75, not(equalTo(Double.NaN)));
		assertEquals(3, values.size());
		assertEquals(Doubles.asList(5.6, 7.4, 3.9), values);
	}

}
