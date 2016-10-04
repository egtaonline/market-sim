package entity.agent;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import systemmanager.Scheduler;

import com.google.common.collect.Lists;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * This agent looks at the past window period at each reentry to execute its 
 * agent strategy.
 * 
 * If current time is T, it looks at activities occurring in between
 * T-window+1 to T, inclusive.
 * 
 * WindowAgents base all the updates on whatever's happened in the past window.
 * For each reentry, they essentially "reset." This is a way to address
 * the issue of employing trading strategies reliant on fixed valuations.
 * 
 * XXX Question: using windowing, should it use the same estimated values? Or reset
 * every time? Probably should reset every time...? Or can treat as initialized to 
 * those values? (could try either way)
 * 
 * @author ewah
 *
 */
public abstract class WindowAgent extends BackgroundAgent {
	
	private static final long serialVersionUID = -8112884516819617629L;

	protected TimeStamp windowLength;

	public WindowAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			Iterator<TimeStamp> interarrivalTimes, PrivateValue pv,
			int tickSize, int bidRangeMin, int bidRangeMax, int windowLength) {
		
		super(scheduler, arrivalTime, fundamental, sip, market, rand,
				interarrivalTimes, pv, tickSize, bidRangeMin, bidRangeMax);
		
		this.windowLength = TimeStamp.create(windowLength);
	}

	/**
	 * Get all transactions (from SIP [XXX not true, but intended?] plus its own transactions)
	 * in the window that is of period windowLength prior to currentTime, i.e.
	 * from currentTime-windowLength+1 to currentTime, inclusive.
	 * 
	 * @param currentTime
	 * @return
	 */
	public List<Transaction> getWindowTransactions(TimeStamp currentTime) {
		TimeStamp firstTimeInWindow = currentTime.minus(windowLength);

		/*
		 * XXX To add more transaction sources that are also sorted, google has
		 * Iterables.mergeSorted(iterables, comparator) that can merge sorted
		 * iterables. The best way would probably to iterate over something like
		 * 
		 * Iterables.mergeSorted(ImmutableList.of(Lists.reverse(marketTransactionProcessor.getTransactions())), Ordering.<Transaction> natural().reverse());
		 * 
		 * This will return transactions in reverse order to be copied into a
		 * list. They could be deduped as they're read off. This will avoid most
		 * unnecessary copying.
		 */
		List<Transaction> allTransactions = marketTransactionProcessor.getTransactions();
		
		int startIndex = allTransactions.size();
		for (Transaction trans : Lists.reverse(allTransactions)) {
			if (!trans.getExecTime().after(firstTimeInWindow))
				break;
			--startIndex;
		}
		return allTransactions.subList(startIndex, allTransactions.size());
	}
	
	public TimeStamp getWindowLength() {
		return windowLength;
	}
}
