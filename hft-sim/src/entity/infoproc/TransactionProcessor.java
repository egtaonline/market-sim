package entity.infoproc;

import java.util.List;

import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

public interface TransactionProcessor {

	public static int nextID = 1;
	
	/* TODO Maybe these methods don't need the associatedMarket? */
	public void sendToTransactionProcessor(Market market,
			List<Transaction> newTransactions, TimeStamp currentTime);

	public void processTransactions(Market market,
			List<Transaction> newTransactions, TimeStamp currentTime);

	/**
	 * Transaction times are guaranteed to be in ascending order.
	 * @return
	 */
	public List<Transaction> getTransactions();

	public TimeStamp getLatency();
}
