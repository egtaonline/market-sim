package entity.market;

import java.util.Collection;
import java.util.Random;

import systemmanager.Scheduler;
import entity.agent.Agent;
import entity.infoproc.SIP;
import entity.market.clearingrule.MockClearingRule;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * MockMarket for testing purposes.
 * 
 * NOTE: A market clear must be explicitly called or scheduled.
 */
public class MockMarket extends Market {

	private static final long serialVersionUID = 1L;

	public MockMarket(Scheduler scheduler, SIP sip) {
		this(scheduler, sip, TimeStamp.IMMEDIATE);
	}

	public MockMarket(Scheduler scheduler, SIP sip, TimeStamp latency) {
		super(scheduler, sip, latency, new MockClearingRule(), new Random());
	}
	
	public MockMarket(Scheduler scheduler, SIP sip, TimeStamp quoteLatency, TimeStamp transactionLatency) {
		super(scheduler, sip, quoteLatency, transactionLatency, new MockClearingRule(), new Random());
	}
	
	public Collection<Order> getOrders() {
		return this.orders;
	}
	
	@Override
	public void submitOrder(Agent agent, OrderType type,
			Price price, int quantity, TimeStamp currentTime) {
		super.submitOrder(agent, type, price, quantity, currentTime);
		updateQuote(currentTime);
	}

	@Override
	public void withdrawOrder(Order order, int quantity, TimeStamp currentTime) {
		super.withdrawOrder(order, quantity, currentTime);
		updateQuote(currentTime);
	}

	public boolean containsOrder(Order order) {
		return orderbook.contains(order);
	}

}
