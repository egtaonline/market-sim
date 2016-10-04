package entity.agent;

import java.util.Random;

import systemmanager.Scheduler;

import com.google.common.collect.Iterators;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class MockWindowAgent extends WindowAgent {

	private static final long serialVersionUID = -3770040793738482441L;

	public MockWindowAgent(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, int windowLength) {
		this(scheduler, fundamental, sip, market, new MockPrivateValue(), 0, 0,
				windowLength);
	}

	public MockWindowAgent(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, PrivateValue pv, int bidRangeMin,
			int bidRangeMax, int windowLength) {
		super(scheduler, TimeStamp.ZERO, fundamental, sip, market,
				new Random(), Iterators.<TimeStamp> emptyIterator(), pv, 1,
				bidRangeMin, bidRangeMax, windowLength);
	}
	
}
