package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;

import com.google.common.collect.Iterators;

import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

/**
 * ZIAGENT
 * 
 * A zero-intelligence (ZI) agent.
 * 
 * This agent bases its private value on a stochastic process, the parameters of which are specified
 * at the beginning of the simulation by the spec file. The agent's private valuation is determined
 * by value of the random process at the time it enters, with some randomization added by using an
 * individual variance parameter. The private value is used to calculate the agent's surplus (and
 * thus the market's allocative efficiency).
 * 
 * This agent submits only ONE limit order during its lifetime.
 * 
 * NOTE: The limit order price is uniformly distributed over a range that is twice the size of
 * bidRange in either a positive or negative direction from the agent's private value.
 * 
 * @author ewah
 */
public class ZIAgent extends BackgroundAgent {

	private static final long serialVersionUID = 1148707664467962927L;

	public ZIAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			PrivateValue privateValue, int tickSize, int bidRangeMin,
			int bidRangeMax) {
		
		super(scheduler, arrivalTime, fundamental, sip, market, rand, Iterators
				.<TimeStamp> emptyIterator(), privateValue, tickSize,
				bidRangeMin, bidRangeMax);
	}

	public ZIAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			EntityProperties props) {
		
		this(scheduler, arrivalTime, fundamental, sip, market, rand,
				new PrivateValue(1, props.getAsDouble(Keys.PRIVATE_VALUE_VAR), rand), 
				props.getAsInt(Keys.AGENT_TICK_SIZE, Keys.TICK_SIZE),
				props.getAsInt(Keys.BID_RANGE_MIN),
				props.getAsInt(Keys.BID_RANGE_MAX));
	}
	
	@Override
	public void agentStrategy(TimeStamp currentTime) {
		// 50% chance of being either long or short
		this.executeZIStrategy(rand.nextBoolean() ? BUY : SELL, 1, currentTime);
	}
	
}
