package entity.market;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import activity.Clear;
import data.EntityProperties;
import entity.agent.Agent;
import entity.infoproc.SIP;
import entity.market.clearingrule.EarliestPriceClear;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * Class for a continuous double auction market.
 * 
 * @author ewah
 */
public class CDAMarket extends Market {

	private static final long serialVersionUID = -6780130359417129449L;

	public CDAMarket(Scheduler scheduler, SIP sip, Random rand,
			TimeStamp latency, int tickSize) {
		
		this(scheduler, sip, rand, latency, latency, tickSize);
	}
	
	public CDAMarket(Scheduler scheduler, SIP sip, Random rand,
			TimeStamp quoteLatency, TimeStamp transactionLatency, int tickSize) {
		
		super(scheduler, sip, quoteLatency, transactionLatency,
				new EarliestPriceClear(tickSize), rand);
	}

	public CDAMarket(Scheduler scheduler, SIP sip, Random rand,
			EntityProperties props) {
		this(scheduler, sip, rand,
				TimeStamp.create(props.getAsInt(Keys.QUOTE_LATENCY, Keys.MARKET_LATENCY)),
				TimeStamp.create(props.getAsInt(Keys.TRANSACTION_LATENCY, Keys.MARKET_LATENCY)),
				props.getAsInt(Keys.MARKET_TICK_SIZE, Keys.TICK_SIZE));
	}

	@Override
	public void submitOrder(Agent agent, OrderType type, Price price,
			int quantity, TimeStamp currentTime, TimeStamp duration) {
		
		super.submitOrder(agent, type, price, quantity, currentTime, duration);
		scheduler.executeActivity(new Clear(this));
	}

	@Override
	public void withdrawOrder(Order order, int quantity, TimeStamp currentTime) {
		super.withdrawOrder(order, quantity, currentTime);
		updateQuote(currentTime);
	}

}
