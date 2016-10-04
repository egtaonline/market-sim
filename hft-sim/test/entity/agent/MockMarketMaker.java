package entity.agent;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;

public class MockMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 1L;

	public MockMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market,
			int numRungs, int rungSize) {
		this(scheduler, fundamental, sip, market, numRungs, rungSize, false, false, true);
	}
	
	public MockMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip,
			Market market, int numRungs, int rungSize, boolean truncateLadder, 
			boolean tickImprovement, boolean tickInside) {
		super(scheduler, fundamental, sip, market, new Random(), 0, 1, numRungs, 
				rungSize, truncateLadder, tickImprovement, tickInside, 0, 0);
	}
	
	public MockMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip,
			Market market, int numRungs, int rungSize, boolean truncateLadder, 
			boolean tickImprovement, boolean tickInside, int initLadderMean,
			int initLadderRange) {
		super(scheduler, fundamental, sip, market, new Random(), 0, 1, numRungs, 
				rungSize, truncateLadder, tickImprovement, tickInside, initLadderMean,
				initLadderRange);
	}

	public MockMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market,
			EntityProperties props) {
		this(scheduler, fundamental, sip, market, 
				props.getAsInt(Keys.NUM_RUNGS),
				props.getAsInt(Keys.RUNG_SIZE),
				props.getAsBoolean(Keys.TRUNCATE_LADDER), 
				props.getAsBoolean(Keys.TICK_IMPROVEMENT),
				props.getAsBoolean(Keys.TICK_OUTSIDE),
				props.getAsInt(Keys.INITIAL_LADDER_MEAN, Keys.FUNDAMENTAL_MEAN),
				props.getAsInt(Keys.INITIAL_LADDER_RANGE));	
	}
}
