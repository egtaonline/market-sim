package entity.agent;

import java.util.Random;

import systemmanager.Scheduler;
import data.FundamentalValue;
import entity.infoproc.MarketQuoteProcessor;
import entity.infoproc.MarketTransactionProcessor;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Quote;
import event.TimeStamp;

/**
 * SMAGENT
 * 
 * Single market (SM) agent, whose agent strategy is executed only within one
 * market. This does not mean that it can only trade with its specified market;
 * it means that it only checks price quotes from its primary market.
 * 
 * An SMAgent is capable of seeing the quote from its own market with zero
 * delay. It also tracks to which market it has most recently submitted a bid,
 * as it is only permitted to submit to one market at a time.
 * 
 * ORDER ROUTING (REGULATION NMS):
 * 
 * The agent's order will be routed to the alternate market ONLY if both the
 * NBBO quote is better than the primary market's quote and the submitted bid
 * will transact immediately given the price in the alternate market. The only
 * difference in outcome occurs when the NBBO is out-of-date and the agent's
 * order is routed to the main market when the alternate market is actually
 * better.
 * 
 * @author ewah
 */
public abstract class SMAgent extends Agent {

	private static final long serialVersionUID = 3156640550886695881L;
	
	protected final Market primaryMarket;
	protected final MarketQuoteProcessor marketQuoteProcessor;
	protected final MarketTransactionProcessor marketTransactionProcessor;

	public SMAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			int tickSize) {
		super(scheduler, arrivalTime, fundamental, sip, rand, tickSize);
		this.primaryMarket = market;
		this.marketQuoteProcessor = market.getQuoteProcessor();
		this.marketTransactionProcessor = market.getTransactionProcessor();
	}

	public Quote getQuote() {
		return marketQuoteProcessor.getQuote();
	}
	
}
