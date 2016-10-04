package entity.infoproc;

import static com.google.common.base.Preconditions.checkNotNull;
import static data.Observations.BUS;
import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import systemmanager.Scheduler;
import activity.Activity;
import activity.ProcessQuote;
import activity.ProcessTransactions;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import data.Observations.NBBOStatistic;
import entity.Entity;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Class that updates pertinent information for the system. Generally used for creating NBBO update
 * events. Serves the purpose of the Security Information Processor in Regulation NMS. Is the NBBO
 * for one market model. Also returns transactions, at the same latency
 * as the NBBO updates.
 * 
 * @author ewah
 */
public class SIP extends Entity implements QuoteProcessor, TransactionProcessor {

	private static final long serialVersionUID = -4600049787044894823L;
	
	protected final TimeStamp latency;
	protected final Map<Market, Quote> marketQuotes;
	protected final List<Transaction> transactions;
	protected BestBidAsk nbbo;

	public SIP(Scheduler scheduler, TimeStamp latency) {
		super(0, scheduler);
		this.latency = checkNotNull(latency);
		this.marketQuotes = Maps.newHashMap();
		this.transactions = Lists.newArrayList();
		this.nbbo = new BestBidAsk(null, null, 0, null, null, 0);
	}

	public BestBidAsk getNBBO() {
		return nbbo;
	}
	
	@Override
    public List<Transaction> getTransactions() {
		return Collections.unmodifiableList(transactions);
	}

	@Override
	public String toString() {
		return "SIP";
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
	public void sendToQuoteProcessor(Market market,
			Quote quote, TimeStamp currentTime) {
		Activity act = new ProcessQuote(this, market, quote);
		if (latency.equals(TimeStamp.IMMEDIATE))
			scheduler.executeActivity(act);
		else
			scheduler.scheduleActivity(currentTime.plus(latency), act);
	}

	@Override
	public void processQuote(Market market,
			Quote quote, TimeStamp currentTime) {
		Quote oldQuote = marketQuotes.get(market);
		// If we get a stale quote, ignore it.
		if (oldQuote != null && oldQuote.getQuoteTime() != null
				&& oldQuote.getQuoteTime().compareTo(quote.getQuoteTime()) > 0)
			return;

		marketQuotes.put(market, quote);
		
		log.log(INFO, "%s -> %s quote %s", market, this, quote);

		Price bestBid = null, bestAsk = null;
		int bestBidQuantity = 0, bestAskQuantity = 0;
		Market bestBidMkt = null, bestAskMkt = null;

		for (Entry<Market, Quote> marketQuote : marketQuotes.entrySet()) {
			Quote q = marketQuote.getValue();
			if (q.getAskPrice() != null && q.getAskPrice().lessThan(bestAsk)) {
				bestAsk = q.getAskPrice();
				bestAskQuantity = q.getAskQuantity();
				bestAskMkt = marketQuote.getKey();
			}
			if (q.getBidPrice() != null && q.getBidPrice().greaterThan(bestBid)) {
				bestBid = q.getBidPrice();
				bestBidQuantity = q.getBidQuantity();
				bestBidMkt = marketQuote.getKey();
			}
		}

		nbbo = new BestBidAsk(bestBidMkt, bestBid, bestBidQuantity, 
				bestAskMkt, bestAsk, bestAskQuantity);
		BUS.post(new NBBOStatistic(nbbo.getSpread(), currentTime));
	}

	@Override
	public TimeStamp getLatency() {
		return latency;
	}

}