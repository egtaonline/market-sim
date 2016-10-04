package utils;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import sumstats.SumStats;

public class RandsTest {
	
	@Test
	public void gaussianTest() {
		double delta = 0.1;
		
		SumStats ds = SumStats.create();
		Random rand = new Random();
		double mean = 100;
		double variance = 10;
		
		for (int i = 0; i < 100000; i++) {
			ds.add(Rands.nextGaussian(rand, mean, variance));
		}
		
		assertEquals(mean, ds.mean(), delta);
		assertEquals(Math.sqrt(variance), ds.stddev(), delta);
	}
	
	@Test
	public void exponentialTest() {
		double delta = 10;
		
		SumStats ds = SumStats.create();
		Random rand = new Random();
		double rate = 0.001;
		
		assertEquals(Double.POSITIVE_INFINITY, Rands.nextExponential(rand, 0), 0);
		
		for (int i = 0; i < 1000000; i++) {
			ds.add(Rands.nextExponential(rand, rate));
		}
		
		assertEquals(1 / rate, ds.mean(), delta);
		assertEquals(1 / rate, ds.stddev(), delta);
	}
	
	@Test
	public void uniformTest() {
		double delta = 0.5;
		
		SumStats ds = SumStats.create();
		Random rand = new Random();
		double a = 0;
		double b = 100;
		
		for (int i = 0; i < 100000; i++) {
			ds.add(Rands.nextUniform(rand, a, b));
		}
		
		assertEquals((a + b)/2, ds.mean(), delta);
		assertEquals(Math.sqrt(Math.pow(b - a, 2) / 12), ds.stddev(), delta);
	}
}
