package data;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;

import com.google.common.primitives.Doubles;

public class DSPlusTest {

	@Test
	public void logRatioTest() {
		DescriptiveStatistics ds;
		
		ds = DSPlus.fromLogRatioOf(Doubles.asList(1, Math.E, 1/Math.E));
		assertEquals(1, ds.getMax(), 0.001);
		assertEquals(-2, ds.getMin(), 0.001);
	}

}
