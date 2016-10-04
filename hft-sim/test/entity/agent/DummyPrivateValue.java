package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.DEBUG;

import java.util.Arrays;
import java.util.Collection;

import entity.market.Price;

/**
 * DummyPrivateValue 
 * 
 * Helper class for ZIAgentTest
 * 
 * Initialized to all zero, so private value will equal fundamental
 * -OR-
 * Initialized with a list of predefined  values (Will check if maxPosition is correct)
 * 
 * Prints contents into log file. 
 * 
 * @author yngchen
 * 
 */
public class DummyPrivateValue extends PrivateValue {
	
	private static final long serialVersionUID = 1L;
	
	public DummyPrivateValue(int absMaxPosition, Collection<Price> prices){
		super(absMaxPosition, prices);
		log.log(DEBUG, "DummyPrivateValue elements: " + Arrays.toString(prices.toArray()));
	}
}
