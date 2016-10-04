package entity.agent;

import iterators.ExpInterarrivals;

import java.util.Iterator;
import java.util.Random;

import systemmanager.Scheduler;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class MockReentryAgent extends ReentryAgent {

	private static final long serialVersionUID = 1L;

	public MockReentryAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, Iterator<TimeStamp> reentry, int tickSize) {
		super(scheduler, TimeStamp.ZERO, fundamental, sip, market, rand, reentry, tickSize);
	}
	
	public MockReentryAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, double reentryRate, int tickSize) {
		this(scheduler, fundamental, sip, market, rand, ExpInterarrivals.create(reentryRate, rand), 
				tickSize);
	}

	public Iterator<TimeStamp> getReentryTimes() {
		return reentry;
	}
}
