package entity.infoproc;

import java.util.List;

import systemmanager.Scheduler;
import entity.agent.HFTAgent;
import entity.market.Market;
import entity.market.Order;
import entity.market.Transaction;
import event.TimeStamp;

public class MarketTransactionProcessor extends AbstractTransactionProcessor {

	private static final long serialVersionUID = 4550103178485854572L;

	public MarketTransactionProcessor(Scheduler scheduler, TimeStamp latency,
			Market market) {
		super(scheduler, latency, market);
	}

	@Override
	public void processTransactions(Market market,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		super.processTransactions(market, newTransactions, currentTime);
		
		for (Transaction trans : newTransactions) {
			Order buy = trans.getBuyOrder(), sell = trans.getSellOrder();
			// XXX Right now every agent but HFT's should function this way
			// (e.g. Market makers). Maybe this will change
			if (!(buy.getAgent() instanceof HFTAgent))
				updateAgent(buy.getAgent(), buy, trans);
			if (!(sell.getAgent() instanceof HFTAgent))
				updateAgent(sell.getAgent(), sell, trans);
		}
	}

	@Override
    public String toString() {
		return "(TransactionProcessor " + id + " in " + associatedMarket + ')';
	}

}
