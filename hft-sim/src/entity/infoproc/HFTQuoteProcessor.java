package entity.infoproc;

import systemmanager.Scheduler;
import activity.AgentStrategy;
import entity.agent.HFTAgent;
import entity.market.Market;
import entity.market.Quote;
import event.TimeStamp;

/**
 * Specific Information Processor for a single market, used by HFT agents
 * 
 * @author cnris
 */
public class HFTQuoteProcessor extends AbstractQuoteProcessor {

	private static final long serialVersionUID = -4104375974647291881L;
	
	protected final HFTAgent hftAgent;

	public HFTQuoteProcessor(Scheduler scheduler, TimeStamp latency,
			Market mkt, HFTAgent hftAgent) {
		super(scheduler, latency, mkt);
		this.hftAgent = hftAgent;
	}

	@Override
	public void processQuote(Market market, Quote quote, TimeStamp currentTime) {
		super.processQuote(market, quote, currentTime);
		scheduler.executeActivity(new AgentStrategy(hftAgent));
	}

	@Override
	public String toString() {
		return "(HFTQuoteProcessor " + id + " in " + associatedMarket + " for " + hftAgent + ')'; 
	}
	
}