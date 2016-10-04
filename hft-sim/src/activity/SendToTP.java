package activity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import entity.infoproc.TransactionProcessor;
import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Class for Activity of sending new quote information to an information
 * processor, including the Security Information Processor (SIP). This should
 * happen as soon as a quote is generated.
 * 
 * @author ewah
 */
public class SendToTP extends Activity {

	protected final Market market;
	protected final TransactionProcessor tp;
	protected final List<Transaction> transactions;

	public SendToTP(Market market, List<Transaction> transactions,
			TransactionProcessor ip) {
		this.market = checkNotNull(market, "Market");
		this.tp = checkNotNull(ip, "TP");
		this.transactions = checkNotNull(transactions, "Transactions");
	}

	@Override
	public void execute(TimeStamp currentTime) {
		tp.sendToTransactionProcessor(market, transactions, currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + market + " -> " + tp;
	}
}
