package sumstats;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.Test;

public class SumStatsTest {
	
	private static final Random rand = new Random();

	@Test
	public void positiveTest() {
		SumStats test = SumStats.create();
		BigStat truth = new BigStat();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i) {
			double d = rand.nextDouble();
			test.add(d);
			truth.add(d);
		}
		assertEquals(truth.sum(), test.sum(), 1e-8);
		assertEquals(truth.mean(), test.mean(), 1e-8);
		assertEquals(truth.variance(), test.variance(), 1e-8);
		assertEquals(truth.stddev(), test.stddev(), 1e-8);
	}
	
	@Test
	public void negativeTest() {
		SumStats test = SumStats.create();
		BigStat truth = new BigStat();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i) {
			double d = -rand.nextDouble();
			test.add(d);
			truth.add(d);
		}
		assertEquals(truth.sum(), test.sum(), 1e-8);
		assertEquals(truth.mean(), test.mean(), 1e-8);
		assertEquals(truth.variance(), test.variance(), 1e-8);
		assertEquals(truth.stddev(), test.stddev(), 1e-8);
	}
	
	@Test
	public void mixedTest() {
		SumStats test = SumStats.create();
		BigStat truth = new BigStat();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i) {
			double d = rand.nextDouble() - .5;
			test.add(d);
			truth.add(d);
		}
		assertEquals(truth.sum(), test.sum(), 1e-8);
		assertEquals(truth.mean(), test.mean(), 1e-8);
		assertEquals(truth.variance(), test.variance(), 1e-8);
		assertEquals(truth.stddev(), test.stddev(), 1e-8);
	}
	
	@Test
	public void extraRandomTests() {
		for (int i = 0; i < 100; ++i) {
			positiveTest();
			negativeTest();
			mixedTest();
		}
	}
	
	public static class BigStat {

		long n;
		BigDecimal sum, sumsq;
		
		BigStat() {
			n = 0;
			sum = new BigDecimal(0);
			sumsq = new BigDecimal(0);
		}
		
		void add(double val) {
			++n;
			sum = sum.add(new BigDecimal(val));
			sumsq = sumsq.add(new BigDecimal(val).pow(2));
		}
		
		double sum() {
			return sum.doubleValue();
		}
		
		double mean() {
			return sum.doubleValue() / n;
		}
		
		double variance() {
			return sumsq.multiply(new BigDecimal(n)).subtract(sum.pow(2)).doubleValue() / (n * (n - 1));
		}
		
		double stddev() {
			return Math.sqrt(variance());
		}
		
	}

}
