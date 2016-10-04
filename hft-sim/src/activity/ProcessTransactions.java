package activity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import entity.infoproc.TransactionProcessor;
import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Class for Activity processing transactions received from a given Market.
 * @author ewah
 */
public class ProcessTransactions extends Activity {

	protected final TransactionProcessor tp;
	protected final Market market;
	protected final List<Transaction> newTransactions;

	public ProcessTransactions(TransactionProcessor ip, Market market,
			List<Transaction> newTransactions) {
		this.tp = checkNotNull(ip, "TP");
		this.market = checkNotNull(market, "Market");
		this.newTransactions = checkNotNull(newTransactions, "New Transactions");
	}

	@Override
	public void execute(TimeStamp currentTime) {
		this.tp.processTransactions(market, newTransactions, currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + market + " -> " + tp + " : " + newTransactions;
	}
}
