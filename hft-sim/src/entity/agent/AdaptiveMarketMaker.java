package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

/**
 * ADAPTIVEMARKETMAKER
 *
 * Adaptive Market Maker
 *
 * Based on Abernethy & Kale, "Adaptive Market Making via Online Learning", PNIPS 2013
 *
 * @author Benno Stein (bjs2@williams.edu)
 */
public class AdaptiveMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 4228181375500843232L;

	protected Map<Integer, Double> weights;			// key = spread, value = weight
	protected final boolean useMedianSpread;
	protected final int volatilityBound;			// Delta in paper
	protected EvictingQueue<Integer> priceQueue;
	protected Price lastPrice;						// track price in previous reentry (period)
	protected int numReentries = 0;					// tracks num reentries
	protected final boolean useLastPrice;			// use prices from last period o/w use current reentry prices // XXX to maintain compatibility with previous version
	protected final boolean fastLearning;			// XXX to maintain compatibility with previous version

	public AdaptiveMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, double reentryRate,
			int tickSize, int numRungs, int rungSize, boolean truncateLadder,
			boolean tickImprovement, boolean tickOutside, int initLadderMean,
			int initLadderRange, int numHistorical, int[] spreads, boolean useMedianSpread,
			int volatilityBound, boolean fastLearning, boolean useLastPrice) {

		super(scheduler, fundamental, sip, market, rand, reentryRate, tickSize,
				numRungs, rungSize, truncateLadder, tickImprovement, tickOutside,
				initLadderMean, initLadderRange);

		checkArgument(numHistorical > 0, "Number of historical prices must be positive!");
		this.useMedianSpread = useMedianSpread;
		this.volatilityBound = volatilityBound;

		// XXX to maintain compatibility with previous version
		this.fastLearning = fastLearning;
		this.useLastPrice = useLastPrice;
		priceQueue = EvictingQueue.create(numHistorical);

		// Initialize weights, initially all equal = 1/N, where N = # windows
		// spreads = windows in paper, variable b
		weights = Maps.newHashMapWithExpectedSize(spreads.length);
		double initial_weight = 1.0 / spreads.length;
		for (int i : spreads)
		{ weights.put(i, initial_weight); }
	}


	public AdaptiveMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
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
				props.getAsInt(Keys.NUM_HISTORICAL), 	// set default to 8 to maintain compatibility with previous version
				props.getAsIntArray(Keys.STRATS),
				props.getAsBoolean(Keys.USE_MEDIAN_SPREAD),
				//To approximate volatility bound, use the fact that 
				//		next = prev + kappa(mean-prev) + nextGaussian(0,1)*sqrt(shock)
				//conservatively estimate |mean-prev|<= 0.25*mean; 98% confidence |nextGaussian| <= 2
				//so, delta ~= kappa * 0.25 * mean + 2sqrt(shock)
				(int) Math.round(0.25 * props.getAsDouble(Keys.FUNDAMENTAL_KAPPA) 
						* props.getAsInt(Keys.FUNDAMENTAL_MEAN) 
						+ 2 * Math.sqrt(props.getAsInt(Keys.FUNDAMENTAL_SHOCK_VAR))),
				props.getAsBoolean(Keys.FAST_LEARNING), // XXX set default to false to maintain compatibility with previous version
				props.getAsBoolean(Keys.USE_LAST_PRICE) // XXX set default to false to maintain compatibility with previous version
			);
	}

	/**
	 * Returns the spread to be used by the MM. If "useMedianSpread" parameter
	 * is true, then uses the median spread (based on distribution of weights). 
	 * Otherwise uses one at random.
	 *
	 * @return a spread value: either the median or one chosen at random.
	 */
	protected int getSpread(){
		double r = useMedianSpread ? 0.5 : rand.nextDouble();
		double sum = 0.0;

		Integer[] spreads = new Integer[weights.size()];
		weights.keySet().toArray(spreads);
		Arrays.sort(spreads);

		for(Integer spread : spreads){
			sum += weights.get(spread);
			if (sum >= r) { return spread; }
		}
		// This return will only be reached if r is very(!) close to 1 and 
		// rounding errors make the sum of the weights less than 1. Extremely unlikely.
		return 0;
	}

	/**
	 * Given a spread, determines the transactions that would have executed 
	 * during the last time step had that spread been chosen.
	 *
	 * @param spread 	spread to use for result computation
	 * @param bid 		The current bid price.
	 * @param ask 		The current ask price.
	 *
	 * @return TransactionResult: a pair of integers (holdingsChange, cashChange)
	 */
	protected TransactionResult lastTransactionResult(int spread, Price bid, Price ask){
		int delta_h = 0, delta_c = 0; //changes in holdings / cash, respectively
		if (lastBid != null && lastAsk != null && lastPrice != null) {
			for(int rung = 0; rung < numRungs; rung++) {
				int offset = spread/2 + rung * stepSize;
				// Step through prices in ladder
				if (lastPrice.intValue() - offset >= ask.intValue()) {
					//If this buy order would have transacted
					delta_h++; 
					delta_c -= (lastPrice.intValue() - offset);
				}
				if (lastPrice.intValue() + offset <= bid.intValue()) {
					//If this sell order would have transacted
					delta_h--;
					delta_c += (lastPrice.intValue() + offset);
				}
			}
		}
		/*If lastBid or lastAsk is null, just return the default delta_h=delta_c=0 so weights don't readjust */
		return new TransactionResult(delta_h, delta_c);
	}

	/**
	 * Recalculate weights based on their hypothetical performance in the last timestep
	 * In this case, use Multiplicative Weights.  Can be subclassed and overridden 
	 * to implement other learning algorithms.
	 * 
	 * In round t, updates using rule:
	 * 		w_next(b) = w_curr(b) * exp(eta_t * V_next(b) - V_curr(b))
	 * with w_next normalized. 
	 * 
	 * eta_t is set to bound the algorithm's regret by 13G sqrt(log(N) * T)
	 * 		G = 2B * volatilityBound + volatilityBound^2
	 * 		B = maximum window size/spread given.
	 * 
	 * XXX Use G = delta / 5 rather than G = delta*B*2 + delta^2 to have agent
	 * 		learn more aggressively/quickly
	 * 
	 * @param valueDeltas: a mapping of spread values to the net change in their 
	 * 		portfolio value over the last time step
	 * @param currentTime
	 */
	protected void recalculateWeights(Map<Integer, Integer> valueDeltas, TimeStamp currentTime){
		int maxSpread = 0;						// B = upper bound of spread sizes
		for(int spread : weights.keySet()) {
			maxSpread = Math.max( maxSpread, spread );
		}
		
		double G = 2 * maxSpread * volatilityBound + volatilityBound^2; 	
		if (fastLearning) {	// XXX just to make this backwards compatible
			G = (volatilityBound / 5);
		}
		
		double eta = Math.min( Math.sqrt( Math.log(weights.size()) / numReentries ), 1.0) 
							/ (2 * G);

		for (int spread : weights.keySet()) {
			double newWeight = Math.exp(eta * valueDeltas.get(spread));
			weights.put(spread, weights.get(spread) * newWeight);
		}
		normalizeWeights();
	}

	/**
	 * Adjusts all weights by a constant factor s.t. their sum is 1.
	 */
	protected void normalizeWeights(){
		double total = 0;
		for(double w : weights.values()) { total += w; }
		for(Map.Entry<Integer,Double> e : weights.entrySet()) { 
			e.setValue(e.getValue() / total); 
		}
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		super.agentStrategy(currentTime);
		numReentries++;

		Price bid = this.getQuote().getBidPrice();
		Price ask = this.getQuote().getAskPrice();
		
		// if no orders in the market yet
		if (!this.getQuote().isDefined()) {
			log.log(INFO, "%s in %s: Undefined quote in %s", this, primaryMarket, primaryMarket);
			this.createOrderLadder(bid, ask);
			return;
		}
		
		// Approximate the price as the midquote price
		int midQuotePrice = (bid.intValue() + ask.intValue()) / 2;
		priceQueue.add(midQuotePrice);

		double sumPrices = 0;
		for (int x : priceQueue) sumPrices += x;
		Price avgPrice = new Price(sumPrices / priceQueue.size());

		//For each spread, determine how it would have performed in the last round. (XXX current round?)
		ImmutableMap.Builder<Integer,Integer> value = new ImmutableMap.Builder<Integer,Integer>();
		for(int spread : weights.keySet()){
			TransactionResult result = lastTransactionResult(spread, bid, ask);
			// Value(t+1) = Cash(t+1) + price(t) * Holdings(t+1)
			
			if (!useLastPrice) {	// XXX for compatibility purposes
				value.put(spread, result.getCashChange() + (avgPrice.intValue() * result.getHoldingsChange()));
			} else {
				// XXX new version using last average price, not current (evaluate for last period)
				if (lastPrice != null)
					value.put(spread, result.getCashChange() + (lastPrice.intValue() * result.getHoldingsChange()));
				else {
					// XXX use midQuotePrice if lastPrice not defined
					value.put(spread, result.getCashChange() + (midQuotePrice * result.getHoldingsChange()));
				}
			}
		}
		ImmutableMap<Integer,Integer> valueDeltas = value.build();

		// Recalculate weights based on performances from the last timestep, 
		// using Multiplicative Weights (Abernethy&Kale 4.1)
		recalculateWeights(valueDeltas, currentTime);
		log.log(INFO, "%s in %s: Current spread weights: %s",
				this, primaryMarket, weights.toString());

		//Submit updated order ladder, using the spread chosen by the learning algorithm
		int offset = this.getSpread() / 2;
		int ladderSize = stepSize * (numRungs - 1);
		withdrawAllOrders();
		submitOrderLadder(new Price(avgPrice.intValue() - offset - ladderSize),  //minimum buy
				new Price(avgPrice.intValue() - offset), 			 	 //maximum buy
				new Price(avgPrice.intValue() + offset),				 //minimum sell
				new Price(avgPrice.intValue() + offset + ladderSize)); //maximum sell
		log.log(INFO, "%s in %s: submitting ladder with spread %d",
				this, primaryMarket, offset * 2);
		lastBid = bid; lastAsk = ask; lastPrice = avgPrice;
	}

	/**
	 * Stores net change in holdings & cash
	 * 
	 * TODO get rid of this class?
	 */
	protected static class TransactionResult extends utils.Pair<Integer, Integer>{
		protected TransactionResult(Integer holdingsChange, Integer cashChange)
		{ super(holdingsChange, cashChange); }
		public Integer getHoldingsChange() 	{ return this.left;  }
		public Integer getCashChange()	  	{ return this.right; }
	}

}
