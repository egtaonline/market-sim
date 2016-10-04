package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.INFO;
import iterators.ExpInterarrivals;

import java.util.Iterator;
import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * ZIRAGENT
 * 
 * A zero-intelligence agent with re-submission (ZIR).
 *
 * The ZIR agent is primarily associated with a single market. It wakes up
 * periodically to submit a new bid.
 * 
 * This agent bases its private value on a stochastic process, the parameters
 * of which are specified at the beginning of the simulation by the spec file.
 * The agent's private valuation is determined by value of the random process at
 * the time it enters, with some randomization added by using an individual 
 * variance parameter. The private value is used to calculate the agent's surplus 
 * (and thus the market's allocative efficiency).
 *
 * This agent submits a single limit order at a time. It will modify its private
 * value if its bid has transacted by the time it wakes up.
 * 
 * NOTE: Each limit order price is uniformly distributed over a range that is twice
 * the size of bidRange (min, max) in either a positive or negative direction from 
 * the agent's private value.
 *
 * @author ewah
 */
public class ZIRAgent extends BackgroundAgent {

	private static final long serialVersionUID = -1155740218390579581L;

	protected boolean withdrawOrders; 	// true if withdraw orders at each reentry

	protected ZIRAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			Iterator<TimeStamp> interarrivals, double pvVar, int tickSize,
			int maxAbsPosition, int bidRangeMin, int bidRangeMax,
			boolean withdrawOrders) {
		
		super(scheduler, arrivalTime, fundamental, sip, market, rand,
				interarrivals, new PrivateValue(maxAbsPosition, pvVar, rand),
				tickSize, bidRangeMin, bidRangeMax);

		this.withdrawOrders = withdrawOrders;
	}

	public ZIRAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			double reentryRate, double pvVar, int tickSize, int maxAbsPosition,
			int bidRangeMin, int bidRangeMax, boolean withdrawOrders) {
		
		this(scheduler, arrivalTime, fundamental, sip, market, rand,
				ExpInterarrivals.create(reentryRate, rand), pvVar, tickSize,
				maxAbsPosition, bidRangeMin, bidRangeMax, withdrawOrders);
	}

	public ZIRAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			EntityProperties props) {
		
		this(scheduler, arrivalTime, fundamental, sip, market, rand,
				props.getAsDouble(Keys.BACKGROUND_REENTRY_RATE, Keys.REENTRY_RATE), 
				props.getAsDouble(Keys.PRIVATE_VALUE_VAR),
				props.getAsInt(Keys.AGENT_TICK_SIZE, Keys.TICK_SIZE),
				props.getAsInt(Keys.MAX_POSITION),
				props.getAsInt(Keys.BID_RANGE_MIN),
				props.getAsInt(Keys.BID_RANGE_MAX),
				props.getAsBoolean(Keys.WITHDRAW_ORDERS));
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		super.agentStrategy(currentTime);

		if (!currentTime.equals(arrivalTime)) {
			log.log(INFO, "%s wake up.", this);
		}
		// XXX should it go to sleep?
//		if (!activeOrders.isEmpty()) return;

		if (withdrawOrders) {
			log.log(INFO, "%s Withdraw all orders.", this);
			withdrawAllOrders();
		}
		// 0.50% chance of being either long or short
		OrderType type = rand.nextBoolean() ? BUY : SELL;
		log.log(INFO, "%s Submit %s order", this, type);
		executeZIStrategy(type, 1, currentTime);
	}
	
}
