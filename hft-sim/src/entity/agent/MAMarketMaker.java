package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;

import com.google.common.collect.EvictingQueue;

import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

/**
 * MAMARKETMAKER
 * 
 * Moving Average Market Maker
 * 
 * NOTE: Because the prices are stored in an EvictingQueue, which does not
 * accept null elements, the number of elements in the bid/ask queues may not
 * be equivalent.
 * 
 * @author zzy, ewah
 */
public class MAMarketMaker extends MarketMaker {

	private static final long serialVersionUID = -4766539518925397355L;
	
	protected EvictingQueue<Price> bidQueue;
	protected EvictingQueue<Price> askQueue;

	public MAMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, double reentryRate,
			int tickSize, int numRungs, int rungSize, boolean truncateLadder,
			boolean tickImprovement, boolean tickOutside,
			int initLadderMean, int initLadderRange, int numHistorical) {

		super(scheduler, fundamental, sip, market, rand, reentryRate, tickSize,
				numRungs, rungSize, truncateLadder, tickImprovement, tickOutside,
				initLadderMean, initLadderRange);

		checkArgument(numHistorical > 0, "Number of historical prices must be positive!");
		bidQueue = EvictingQueue.create(numHistorical);
		askQueue = EvictingQueue.create(numHistorical);
	}

	public MAMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, EntityProperties props) {

		this(scheduler, fundamental, sip, market, rand,
				props.getAsDouble(Keys.MARKETMAKER_REENTRY_RATE, Keys.REENTRY_RATE),
				props.getAsInt(Keys.AGENT_TICK_SIZE, Keys.TICK_SIZE),
				props.getAsInt(Keys.NUM_RUNGS),
				props.getAsInt(Keys.RUNG_SIZE),
				props.getAsBoolean(Keys.TRUNCATE_LADDER), 
				props.getAsBoolean(Keys.TICK_IMPROVEMENT),
				props.getAsBoolean(Keys.TICK_OUTSIDE),
				props.getAsInt(Keys.INITIAL_LADDER_MEAN, Keys.FUNDAMENTAL_MEAN),
				props.getAsInt(Keys.INITIAL_LADDER_RANGE),
				props.getAsInt(Keys.NUM_HISTORICAL));
	}
	
	@Override
	public void agentStrategy(TimeStamp currentTime) {
		super.agentStrategy(currentTime);
		
		Price bid = this.getQuote().getBidPrice();
		Price ask = this.getQuote().getAskPrice();

		if (bid == null && lastBid == null && ask == null && lastAsk == null) {
			log.log(INFO, "%s in %s: Undefined quote in %s", this, primaryMarket, primaryMarket);
			this.createOrderLadder(bid, ask);	
			
		} else if ((bid == null && lastBid != null)
				|| (bid != null && !bid.equals(lastBid))
				|| (bid != null && lastBid == null)
				|| (ask == null && lastAsk != null)
				|| (ask != null && !ask.equals(lastAsk))
				|| (ask != null && lastAsk == null)) {
			
			if (!this.getQuote().isDefined()) {
				log.log(INFO, "%s in %s: Undefined quote in %s", this, primaryMarket, primaryMarket);
				this.createOrderLadder(bid, ask);
				
			} else {
				// Quote changed, still valid, withdraw all orders
				log.log(INFO, "%s in %s: Withdraw all orders", this, primaryMarket);
				withdrawAllOrders();	
				
				bid = this.getQuote().getBidPrice();
				ask = this.getQuote().getAskPrice();
				
				// Use last known bid/ask if undefined post-withdrawal
				if (!this.getQuote().isDefined()) {
					Price oldBid = bid, oldAsk = ask;
					if (bid == null && lastBid != null) bid = lastBid;
					if (ask == null && lastAsk != null) ask = lastAsk;
					log.log(INFO, "%s in %s: Ladder MID (%s, %s)-->(%s, %s)", 
							this, primaryMarket, oldBid, oldAsk, bid, ask);
				}
				
				// Compute moving average
				if (bid != null) bidQueue.add(bid);
				if (ask != null) askQueue.add(ask);
				
				Price ladderBid = null, ladderAsk = null;
				
				if (!bidQueue.isEmpty()) {
					double sumBids = 0;
					for (Price x : bidQueue) sumBids += x.intValue();
					ladderBid = new Price(sumBids / bidQueue.size());
				}
				if (!askQueue.isEmpty()) {
					double sumAsks = 0;
					for (Price y : askQueue) sumAsks += y.intValue();
					ladderAsk = new Price(sumAsks / askQueue.size());
				}

				this.createOrderLadder(ladderBid, ladderAsk);

			} // if quote defined
			
		} else {
			log.log(INFO, "%s in %s: No change in submitted ladder", this, primaryMarket);
		}
		// update latest bid/ask prices
		lastAsk = ask; lastBid = bid;
	}
	
}

