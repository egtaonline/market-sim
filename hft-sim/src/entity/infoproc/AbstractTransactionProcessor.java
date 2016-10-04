package entity.infoproc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;

import systemmanager.Scheduler;
import activity.Activity;
import activity.ProcessTransactions;

import com.google.common.collect.Lists;

import entity.Entity;
import entity.agent.Agent;
import entity.market.Market;
import entity.market.Order;
import entity.market.Transaction;
import event.TimeStamp;

abstract class AbstractTransactionProcessor extends Entity implements TransactionProcessor {

	private static final long serialVersionUID = -8130023032097833791L;
	
	protected final TimeStamp latency;
	protected final Market associatedMarket;
	protected final List<Transaction> transactions;

	public AbstractTransactionProcessor(Scheduler scheduler, TimeStamp latency,
			Market associatedMarket) {
		super(ProcessorIDs.nextID++, scheduler);
		this.latency = latency;
		this.associatedMarket = checkNotNull(associatedMarket);
		this.transactions = Lists.newArrayList();
	}

	@Override
	public void sendToTransactionProcessor(Market market,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		Activity act = new ProcessTransactions(this, market, newTransactions);
		if (latency.equals(TimeStamp.IMMEDIATE))
			scheduler.executeActivity(act);
		else
			scheduler.scheduleActivity(currentTime.plus(latency), act);
	}

	@Override
	public void processTransactions(Market market,
			List<Transaction> newTransactions, TimeStamp currentTime) {
		if (newTransactions.isEmpty()) return;
		TimeStamp transactionTime = newTransactions.get(0).getExecTime();
		// Find the proper insertion index (likely at the end of the list)
		int insertionIndex = transactions.size();
		for (Transaction trans : Lists.reverse(transactions)) {
			if (trans.getExecTime().before(transactionTime))
				break;
			--insertionIndex;
		}
		// Insert at appropriate location
		transactions.addAll(insertionIndex, newTransactions);
	}

	@Override
    public List<Transaction> getTransactions() {
		// So that we don't copy the list a bunch of times
		return Collections.unmodifiableList(transactions);
	}

	@Override
	public TimeStamp getLatency() {
		return latency;
	}
	
	// Tells an agent about a transaction and removes the order if everything has transacted
	protected void updateAgent(Agent agent, Order order, Transaction transaction) {
		if (order.getQuantity() == 0)
			agent.removeOrder(order);
		agent.processTransaction(transaction);
	}

}
