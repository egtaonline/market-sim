package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Random;

import systemmanager.Scheduler;
import data.FundamentalValue;
import entity.infoproc.HFTQuoteProcessor;
import entity.infoproc.HFTTransactionProcessor;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class MockHFTAgent extends HFTAgent {

	private static final long serialVersionUID = 1L;

	public MockHFTAgent(Scheduler scheduler, TimeStamp latency, FundamentalValue fundamental, 
			SIP sip, Collection<Market> markets) {
		this(scheduler, latency, latency, fundamental, sip, markets);
	}
	
	public MockHFTAgent(Scheduler scheduler, TimeStamp quoteLatency, TimeStamp transactionLatency,
			FundamentalValue fundamental, SIP sip, Collection<Market> markets) {
		super(scheduler, quoteLatency, transactionLatency, TimeStamp.create(0), fundamental, 
				sip, markets, new Random(), 1);
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		
	}

	/**
	 * Get HFTQuoteProcessor directly (for testing purposes)
	 * @param market
	 * @return
	 */
	public HFTQuoteProcessor getHFTQuoteProcessor(Market market) {
		checkNotNull(market);
		if (quoteProcessors.containsKey(market)) return quoteProcessors.get(market);
		return null;
	}
	
	/**
	 * Get HFTTransactionProcessor directly (for testing purposes)
	 * @param market
	 * @return
	 */
	public HFTTransactionProcessor getHFTTransactionProcessor(Market market) {
		checkNotNull(market);
		if (transactionProcessors.containsKey(market)) return transactionProcessors.get(market);
		return null;
	}
}
