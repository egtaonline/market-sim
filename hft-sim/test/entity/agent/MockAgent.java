package entity.agent;

import java.util.Collection;
import java.util.Random;

import systemmanager.Scheduler;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Order;
import event.TimeStamp;

public class MockAgent extends Agent {

	private static final long serialVersionUID = 1L;

	public MockAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip) {
		super(scheduler, TimeStamp.ZERO, fundamental, sip, new Random(), 1);
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		
	}
	
	public Collection<Order> getOrders() {
		return this.activeOrders;
	}
	
}
