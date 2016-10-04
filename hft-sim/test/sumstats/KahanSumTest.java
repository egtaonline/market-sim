package sumstats;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.Test;

public class KahanSumTest {

	private static final Random rand = new Random();
	
	@Test
	public void positiveTest() {
		KahanSum test = KahanSum.create();
		BigDecimal truth = new BigDecimal(0d);
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i) {
			double d = rand.nextDouble();
			test.add(d);
			truth = truth.add(new BigDecimal(d));
		}
		assertEquals(truth.doubleValue(), test.sum(), 1e-8);
	}
	
	@Test
	public void negativeTest() {
		KahanSum test = KahanSum.create();
		BigDecimal truth = new BigDecimal(0d);
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i) {
			double d = -rand.nextDouble();
			test.add(d);
			truth = truth.add(new BigDecimal(d));
		}
		assertEquals(truth.doubleValue(), test.sum(), 1e-8);
	}
	
	@Test
	public void mixedTest() {
		KahanSum test = KahanSum.create();
		BigDecimal truth = new BigDecimal(0d);
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i) {
			double d = rand.nextDouble() - .5;
			test.add(d);
			truth = truth.add(new BigDecimal(d));
		}
		assertEquals(truth.doubleValue(), test.sum(), 1e-8);
	}
	
	@Test
	public void extraRandomTests() {
		for (int i = 0; i < 100; ++i) {
			positiveTest();
			negativeTest();
			mixedTest();
		}
	}

}
