package entity.infoproc;

import java.util.List;

import systemmanager.Scheduler;
import entity.agent.HFTAgent;
import entity.market.Market;
import entity.market.Order;
import entity.market.Transaction;
import event.TimeStamp;

public class HFTTransactionProcessor extends AbstractTransactionProcessor {

	private static final long serialVersionUID = 2897824399529496851L;
	
	protected final HFTAgent hftAgent;

	public HFTTransactionProcessor(Scheduler scheduler, TimeStamp latency,
			Market mkt, HFTAgent hftAgent) {
		super(scheduler, latency, mkt);
		this.hftAgent = hftAgent;
	}

	/*
	 * XXX may need to have HFT agent who acts immediately upon getting new
	 * transaction info. I think we should just mandate / have mandated that if
	 * Transactions are created, the quote will be updated. Thus, we only need
	 * to execute strategy during quote update. With the separation of
	 * transaction and quote processors, it is very clear that transactions are
	 * processed first, and then quotes.
	 */

	@Override
	public void processTransactions(Market market,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		super.processTransactions(market, newTransactions, currentTime);
		
		for (Transaction trans : newTransactions) {
			Order buy = trans.getBuyOrder(), sell = trans.getSellOrder();
			
			if (buy.getAgent().equals(hftAgent))
				updateAgent(buy.getAgent(), buy, trans);
			if (sell.getAgent().equals(hftAgent))
				updateAgent(sell.getAgent(), sell, trans);
		}

	}

	@Override
	public String toString() {
		return "(HFTTransactionProcessor " + id + " in " + associatedMarket + " for " + hftAgent + ')'; 
	}

}
