package entity.agent;

import java.io.IOException;
import java.util.Random;

import systemmanager.Consts;
import systemmanager.Keys;
import systemmanager.Scheduler;
import activity.AgentStrategy;
import activity.SubmitNMSOrder;

import com.google.common.collect.PeekingIterator;

import data.AgentProperties;
import data.FundamentalValue;
import data.MarketDataParser;
import data.NYSEParser;
import data.NasdaqParser;
import data.OrderDatum;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class MarketDataAgent extends SMAgent {

	/*
	 * When building from eclipse you should use the generated serialVersionUID
	 * (which generates a random long) instead of the default 1. serialization
	 * is a java interface that allows all objects to be saved. This random
	 * number essentially says what version this object is, so it knows when it
	 * tries to load an object if its actually trying to load the same object.
	 */
	private static final long serialVersionUID = 7690956351534734324L;

	protected MarketDataParser marketDataParser;
	protected PeekingIterator<OrderDatum> orderDatumIterator;

	public MarketDataAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, String fileName) {
		super(scheduler, TimeStamp.ZERO, fundamental, sip, market, rand, 1);

		// Processing the file 
		try {
			// Determining the market file type
			if(fileName.toLowerCase().contains(Consts.NYSE)){
				this.marketDataParser = new NYSEParser(fileName);
				this.orderDatumIterator = this.marketDataParser.getIterator();
			}
			else if(fileName.toLowerCase().contains(Consts.NASDAQ)) {
				this.marketDataParser = new NasdaqParser(fileName);
				this.orderDatumIterator = this.marketDataParser.getIterator();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error: could not open file: " + fileName.toString());
			System.exit(1);
		}
		
		// FIXME Shouldn't it schedule the first time, not automatically submit a bid?
		scheduler.executeActivity(new AgentStrategy(this));
	}
	
	public MarketDataAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market, 
			Random rand, AgentProperties props) {
		this(scheduler, fundamental, sip, market, rand, props.getAsString(Keys.FILENAME));
	}
	
	@Override
    public void agentStrategy(TimeStamp currentTime) {
		
		if (!orderDatumIterator.hasNext()) {
			return;
		}
		
		OrderDatum nextOrder = orderDatumIterator.next();
		SubmitNMSOrder act = new SubmitNMSOrder(this, primaryMarket,
				nextOrder.getOrderType(), nextOrder.getPrice(),
				nextOrder.getQuantity(), nextOrder.getDuration());
		scheduler.scheduleActivity(nextOrder.getTimeStamp(), act);
		
		// Schedule reentry
		if (orderDatumIterator.hasNext()) {
			scheduler.scheduleActivity(orderDatumIterator.peek().getTimeStamp(), new AgentStrategy(this));
		}
		
	}

}
