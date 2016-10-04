package systemmanager;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import systemmanager.SystemManager.Result;

public class SystemManagerTest {

	/*
	 * TODO Put system level tests here. E.g. run full simulations and make
	 * sure certain invariants hold true, stuff gets output correctly etc.
	 */
	
	@Test
	public void emptyTest() {
		assertTrue(true);
	}

	// test if ZIRPAgent arrivals are exponential, 
	// according to background re-entry rate.
	@Test
	public void zirpArrivalTest() {
	    try {
            final double toleranceMin = 0.8;
            final double toleranceMax = 1.2;
            
            final List<Result> results1 = SystemManager.getMeanResultList(1, 1000);
            final double trueMean1 = 2000.0;
            final double trueVariance1 = 4000000.0;
            for (Result result: results1) {
                assertTrue(result.mean / trueMean1 >= toleranceMin);
                assertTrue(result.mean / trueMean1 <= toleranceMax);
                assertTrue(result.variance / trueVariance1 >= toleranceMin);
                assertTrue(result.variance / trueVariance1 <= toleranceMax);
            }
            
            final List<Result> results2 = SystemManager.getMeanResultList(2, 1000);
            final double trueMean2 = 200.0;
            final double trueVariance2 = 40000.0;
            for (Result result: results2) {
                assertTrue(result.mean / trueMean2 >= toleranceMin);
                assertTrue(result.mean / trueMean2 <= toleranceMax);
                assertTrue(result.variance / trueVariance2 >= toleranceMin);
                assertTrue(result.variance / trueVariance2 <= toleranceMax);
            }
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
	}
}
