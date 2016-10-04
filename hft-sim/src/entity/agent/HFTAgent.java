package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import systemmanager.Scheduler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import data.FundamentalValue;
import entity.infoproc.HFTQuoteProcessor;
import entity.infoproc.HFTTransactionProcessor;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Abstract class for high-frequency traders. Creates the necessary information
 * processors and links them to the appropriate markets.
 * 
 * @author ewah
 */
public abstract class HFTAgent extends MMAgent {

	private static final long serialVersionUID = -1483633963238206201L;
	
	protected final Map<Market, HFTQuoteProcessor> quoteProcessors;
	protected final Map<Market, HFTTransactionProcessor> transactionProcessors;

	public HFTAgent(Scheduler scheduler, TimeStamp latency, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Collection<Market> markets, 
			Random rand, int tickSize) {
		this(scheduler, latency, latency, arrivalTime, fundamental, sip, markets, rand, tickSize);
	}

	public HFTAgent(Scheduler scheduler, TimeStamp quoteLatency,
			TimeStamp transactionLatency, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Collection<Market> markets,
			Random rand, int tickSize) {
		
		super(scheduler, arrivalTime, fundamental, sip, markets, rand, tickSize);
		
		Builder<Market, HFTQuoteProcessor> quoteProcessorBuilder = ImmutableMap.builder(); 
		Builder<Market, HFTTransactionProcessor> transactionProcessorBuilder = ImmutableMap.builder(); 
		
		for (Market market : markets) {
			HFTQuoteProcessor qp = new HFTQuoteProcessor(scheduler, quoteLatency, market, this);
			quoteProcessorBuilder.put(market, qp);
			market.addQP(qp); 
			
			HFTTransactionProcessor tp = new HFTTransactionProcessor(scheduler, transactionLatency,
					market, this);
			transactionProcessorBuilder.put(market, tp);
			market.addTP(tp);
		}
		
		quoteProcessors = quoteProcessorBuilder.build();
		transactionProcessors = transactionProcessorBuilder.build();
	}
	
	/**
	 * Get quote in the specified market.
	 * @param market
	 * @return
	 */
	public Quote getQuote(Market market) {
		checkNotNull(market);
		if (quoteProcessors.containsKey(market))
			return quoteProcessors.get(market).getQuote();
		return new Quote(null, null, 0, null, 0, TimeStamp.ZERO);
	}
	
	/**
	 * Get transactions in the specified market.
	 * @param market
	 * @return
	 */
	public List<Transaction> getTransactions(Market market) {
		checkNotNull(market);
		if (quoteProcessors.containsKey(market))
			return transactionProcessors.get(market).getTransactions();
		return ImmutableList.<Transaction> of();
	}
}
