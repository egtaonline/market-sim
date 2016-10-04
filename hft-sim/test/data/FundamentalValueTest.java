package data;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import entity.market.Price;
import event.TimeStamp;

public class FundamentalValueTest {
    
    private static final int simLength = 1000;

	@Test
	public void asTimeSeriesTest() {
		for (int i = 0; i < 1000; ++i) {
			Random rand = new Random();
			FundamentalValue fund = FundamentalValue.create(
		        rand.nextDouble(), 
		        rand.nextInt(Integer.MAX_VALUE) + 1, 
		        rand.nextDouble()*100 + 1, 
		        1.0, 
		        rand
	        );
			fund.computeFundamentalTo(1000);
			assertEquals(fund.meanRevertProcess, fund.asTimeSeries().sample(1, 1001));
		}
	}

    @Test
    public void zeroJumpTest() {
        Random rand = new Random();
        final double shockProb = 0.0;
        FundamentalValue fund = FundamentalValue.create(
            rand.nextDouble(), 
            rand.nextInt(Integer.MAX_VALUE) + 1, 
            rand.nextDouble()*100 + 1, 
            shockProb, 
            rand
        );

        final Price initialPrice = fund.getValueAt(TimeStamp.ZERO);
        final TimeStamp finalTime = TimeStamp.create(simLength);
        for (int time = 1; time < 100; time++) {
            assertEquals(
                initialPrice, 
                fund.getValueAt(finalTime)
            );
        }
    }
    
    @Test
    public void someNotAllJumpTest() {
        Random rand = new Random();
        final double shockProb = 0.5; // jump probability is 0.5
        FundamentalValue fund = FundamentalValue.create(
            rand.nextDouble(), 
            rand.nextInt(Integer.MAX_VALUE) + 1, 
            rand.nextDouble()*100 + 1, 
            shockProb, 
            rand
        );
        
        // some, but not all, of 100 time steps should have a jump.
        // the probability of failure if implemented correctly is
        // 1/2^100 + 1/2^100 = 1/2^99, or 1 in about 10^30.
        Price previousPrice = fund.getValueAt(TimeStamp.ZERO);
        boolean hasJump = false;
        boolean hasNoJump = false;
        for (int time = 1; time < 100; time++) {
            Price currentPrice = fund.getValueAt(TimeStamp.create(time));
            if (previousPrice.equals(currentPrice)) {
                hasNoJump = true;
            } else {
                hasJump = true;
            }
            
            previousPrice = currentPrice;
        }
        
        assertEquals(hasNoJump, true);
        assertEquals(hasJump, true);
    }
    
    @Test
    public void zeroProbJumpTest() {
        Random rand = new Random();
        int mean = rand.nextInt(100000);
        FundamentalValue fund =
            FundamentalValue.create(
                0, // kappa (multiplied by mean). no mean reversion at 0
                mean, 
                0, // jump variance
                1.0, // jump probability
                rand
            );

        Price meanPrice = new Price(mean);

        for (int time = 0; time < 100; time++) {
            assertEquals(meanPrice, fund.getValueAt(TimeStamp.create(time)));
        }
    }
    
    @Test
    public void extraTest() {
        for (int i = 0; i < 100; i++) {
            zeroJumpTest();
            someNotAllJumpTest();
            zeroProbJumpTest();
        }
    }
}
