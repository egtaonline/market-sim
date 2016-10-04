package data;

import java.util.Random;

import entity.market.Price;
import event.TimeStamp;

/**
 * Dummy class to make unit testing of agents easier.
 * Will simply return meanVal regardless of time.
 * 
 * @author drhurd
 *
 */
public class MockFundamental extends FundamentalValue {
	
	private static final long serialVersionUID = 1L;

	public MockFundamental(int mean) {
		super(0, mean, 0, 1.0, new Random());
	}
	
	@Override
	public Price getValueAt(TimeStamp t) {
		return new Price(meanValue);
	}

}
