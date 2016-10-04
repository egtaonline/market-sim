package entity.agent;

import java.util.Collection;
import java.util.Random;

import systemmanager.Scheduler;
import activity.MockActivity;

import com.google.common.collect.Iterators;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Order;
import event.TimeStamp;

public class MockBackgroundAgent extends BackgroundAgent {

	private static final long serialVersionUID = 1L;

	public MockBackgroundAgent(Scheduler scheduler,
			FundamentalValue fundamental, SIP sip, Market market) {
		this(scheduler, fundamental, sip, market, new MockPrivateValue(), 0, 0);
	}
	
	public MockBackgroundAgent(Scheduler scheduler,
			FundamentalValue fundamental, SIP sip, Market market,
			PrivateValue pv, int bidRangeMin, int bidRangeMax) {
		super(scheduler, TimeStamp.ZERO, fundamental, sip, market,
				new Random(), Iterators.<TimeStamp> emptyIterator(), pv, 1,
				bidRangeMin, bidRangeMax);
	}
	
	public Collection<Order> getOrders() {
		return this.activeOrders;
	}

	public void addMockActivity(TimeStamp currentTime) {
		scheduler.scheduleActivity(currentTime, new MockActivity());
	}

}
