package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import utils.Pair;
import activity.SubmitOrder;

import com.google.common.base.Functions;
import com.google.common.collect.Maps;

import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.HFTQuoteProcessor;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.FourHeap;
import fourheap.MatchedOrders;
import fourheap.Order;

/**
 * LAAGENT
 * 
 * High-frequency trader employing latency arbitrage strategy.
 * 
 * @author ewah
 */
public class LAAgent extends HFTAgent {

	private static final long serialVersionUID = 1479379512311568959L;
	
	protected final double alpha; // LA profit gap
	protected boolean executingStrategy; // Lock during arbitrage
	protected final Map<Market, SafeTimes> safeTime; // This map indicates when it's safe to look at a market for arbitrage

	public LAAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip,
			Collection<Market> markets, Random rand, TimeStamp latency,
			int tickSize, double alpha) {
		super(scheduler, latency, TimeStamp.ZERO, fundamental, sip, markets,
				rand, tickSize);

		this.alpha = alpha;
		this.executingStrategy = false;
		this.safeTime = Maps.toMap(markets, Functions.constant(new SafeTimes(TimeStamp.ZERO, TimeStamp.ZERO)));
	}

	public LAAgent(Scheduler scheduler, FundamentalValue fundamental, SIP sip,
			Collection<Market> markets, Random rand, EntityProperties props) {

		this(scheduler, fundamental, sip, markets, rand,
				TimeStamp.create(props.getAsLong(Keys.LA_LATENCY)),
				props.getAsInt(Keys.AGENT_TICK_SIZE, Keys.TICK_SIZE),
				props.getAsDouble(Keys.ALPHA));
	}

	@Override
	// TODO Need strategy for orders that don't execute
	/*
	 * TODO Currently schedules safeTime at currentTime + latency. We may
	 * instead want it to be plus one. At the current setting, if an order came
	 * in before (but with the same timestamp) of a delayed LA action, then the
	 * the LA will consider the market valid to act upon even though its order
	 * hasn't really taken place. This even seems pretty rare. This could be
	 * changed by making !before -> after, and chanigng the initial timeStamps
	 * to -1.
	 */
	public void agentStrategy(TimeStamp currentTime) {
		if (executingStrategy)
			return; // Lock
		
		Price bestBid = null, bestAsk = null;
		Market bestBidMarket = null, bestAskMarket = null;
		TimeStamp bestBidLatency = null, bestAskLatency = null;

		for (Entry<Market, HFTQuoteProcessor> ipEntry : quoteProcessors.entrySet()) {
			Quote q = ipEntry.getValue().getQuote();
			Market market = ipEntry.getKey();
			SafeTimes st = safeTime.get(market);
			
			if (q.getAskPrice() != null && q.getAskPrice().lessThan(bestAsk) && !q.getQuoteTime().before(st.getAskTime())) {
				bestAsk = q.getAskPrice();
				bestAskMarket = market;
				bestAskLatency = ipEntry.getValue().getLatency();
			}
			if (q.getBidPrice() != null && q.getBidPrice().greaterThan(bestBid) && !q.getQuoteTime().before(st.getBidTime())) {
				bestBid = q.getBidPrice();
				bestBidMarket = market;
				bestBidLatency = ipEntry.getValue().getLatency();
			}
		}

		if (bestBid == null || bestAsk == null
				|| bestAsk.doubleValue() * (1 + alpha) > bestBid.doubleValue())
			return;

		log.log(INFO, "%s detected arbitrage between %s %s and %s %s", this, 
				bestBidMarket, quoteProcessors.get(bestBidMarket).getQuote(),
				bestAskMarket, quoteProcessors.get(bestAskMarket).getQuote());
		Price midPoint = new Price((bestBid.doubleValue() + bestAsk.doubleValue()) * .5).quantize(tickSize);
		
		safeTime.get(bestAskMarket).setAskTime(currentTime.plus(bestAskLatency));
		safeTime.get(bestBidMarket).setBidTime(currentTime.plus(bestBidLatency));
		
		executingStrategy = true;
		scheduler.executeActivity(new SubmitOrder(this, bestBidMarket, SELL, midPoint, 1));
		scheduler.executeActivity(new SubmitOrder(this, bestAskMarket, BUY, midPoint, 1));
		executingStrategy = false;
	}

	/*
	 * This should be a natural extension of the arbitrage strategy extended to
	 * multi quantities, which should be necessary if the arbitrageur has a
	 * latency. FIXME For some reason this is not the same as the above
	 * strategy, sometimes making more profit, sometimes less, and I'm unsure
	 * why.
	 */
	public void agentStrategy2() {
		if (executingStrategy) return; // Lock
		
		FourHeap<Price, Integer, Order<Price, Integer>> fh = FourHeap.<Price, Integer, Order<Price, Integer>> create();
		Map<Order<Price, Integer>, Market> orderMap = Maps.newHashMap();
		
		for (Entry<Market, HFTQuoteProcessor> ipEntry : quoteProcessors.entrySet()) {
			Quote q = ipEntry.getValue().getQuote();
			if (q.getBidPrice() != null && q.getBidQuantity() > 0) {
				Order<Price, Integer> order = Order.create(BUY, q.getBidPrice(), q.getBidQuantity(), 0);
				orderMap.put(order, ipEntry.getKey());
				fh.insertOrder(order);
			}
			if (q.getAskPrice() != null && q.getAskQuantity() > 0) {
				Order<Price, Integer> order = Order.create(SELL, q.getAskPrice(), q.getAskQuantity(), 0);
				orderMap.put(order, ipEntry.getKey());
				fh.insertOrder(order);
			}
		}
		
		Collection<MatchedOrders<Price, Integer, Order<Price, Integer>>> matchedOrders = fh.clear();
		for (MatchedOrders<Price, Integer, Order<Price, Integer>> trans : matchedOrders) {
			Order<Price, Integer> buy = trans.getBuy(), sell = trans.getSell();
			if (sell.getPrice().doubleValue() * (1 + alpha) > buy.getPrice().doubleValue())
				continue;
			double midPoint = .5 * (buy.getPrice().doubleValue() + sell.getPrice().doubleValue()); 
			Price midPrice = new Price(midPoint).quantize(tickSize);
			
			executingStrategy = true;
			scheduler.executeActivity(new SubmitOrder(this, orderMap.get(sell), BUY, midPrice, trans.getQuantity()));
			scheduler.executeActivity(new SubmitOrder(this, orderMap.get(buy), SELL, midPrice, trans.getQuantity()));
			executingStrategy = false;
		}
	}
	
	protected static class SafeTimes extends Pair<TimeStamp, TimeStamp> {

		protected SafeTimes(TimeStamp bidTime, TimeStamp askTime) {
			super(bidTime, askTime);
		}
		
		public void setBidTime(TimeStamp bidTime) {
			this.left = bidTime;
		}
		
		public void setAskTime(TimeStamp askTime) {
			this.right = askTime;
		}
		
		public TimeStamp getBidTime() {
			return this.left;
		}
		
		public TimeStamp getAskTime() {
			return this.right;
		}
		
	}
}
