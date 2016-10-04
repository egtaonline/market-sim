package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static data.Observations.BUS;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import systemmanager.Consts.DiscountFactor;
import systemmanager.Keys;
import systemmanager.Scheduler;
import utils.Rands;
import activity.SubmitNMSOrder;

import com.google.common.collect.ImmutableMap;

import data.FundamentalValue;
import data.Observations.ZIRPStatistic;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * Abstract class for background traders.
 * 
 * All background traders can submit ZI-strategy limit orders.
 * 
 * @author ewah
 */
public abstract class BackgroundAgent extends ReentryAgent {
	
	private static final long serialVersionUID = 7742389103679854398L;
	
	protected final PrivateValue privateValue;
	protected final DiscountedValue surplus;
	
	public final int bidRangeMax; 		// range for limit order
	protected int bidRangeMin;
	
	public BackgroundAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			Iterator<TimeStamp> reentry, PrivateValue pv, int tickSize,
			int bidRangeMin, int bidRangeMax) {
		
		super(scheduler, arrivalTime, fundamental, sip, market, rand, reentry, tickSize);
	
		this.privateValue = checkNotNull(pv);
		this.surplus = DiscountedValue.create();
		this.bidRangeMin = bidRangeMin;
		this.bidRangeMax = bidRangeMax;
	}
	
	/**
	 * Submits a NMS-routed Zero-Intelligence limit order.
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * 
	 * @return
	 */
	public void executeZIStrategy(OrderType type, int quantity, TimeStamp currentTime) {
		if (this.withinMaxPosition(type, quantity)) {
			
			Price val = getValuation(type, currentTime);
			Price price = new Price((val.doubleValue() + (type.equals(SELL) ? 1 : -1) * 
					Rands.nextUniform(rand, bidRangeMin, bidRangeMax))).nonnegative().quantize(tickSize);
			
			log.log(INFO, "%s executing ZI strategy position=%d, for q=%d, value=%s + %s=%s",
					this, positionBalance, quantity, fundamental.getValueAt(currentTime),
					privateValue.getValue(positionBalance, type), val);
			
			scheduler.executeActivity(new SubmitNMSOrder(this, primaryMarket,
					type, price, quantity));
		}
	}
	
	/**
	 * If current quotes are favorable relative to noisy signal of current
	 * fundamental value plus private value: Buy (or sell) exactly 1 unit of the equity, 
	 * via a "simulated market order" at current prices.
	 * 
	 * That is, if there is already a bid (ask) quote in the market, submit a limit order for 1 unit
     * at the quoted price. If there is not already a bid (ask) quote in the market, do nothing.
     * 
     * The agent adds a noise term to the current fundamental value.
     * 
     * If this noisy fundamental signal plus the private value gain 
     * from obtaining 1 more share is greater than
     * the current ask price, the agent will buy 1 share.
     * 
     * Else if the noisy fundamental signal plus the (negative) "gain" 
     * from losing 1 share is less than the current
     * bid price, the agent will sell 1 share.
     * 
     * (If both conditions are true, the agent will buy only, not buy and sell.)
	 * 
	 * @param currentTime the current time stamp
	 * @param noiseStdev standard deviation of the zero-mean Gaussian noise
	 * in the background trader's signal of the current fundamental value
	 */
	public void executeFundamentalStrategy(
        final TimeStamp currentTime,
        final int noiseStdev
    ) {
       // always trade 1 unit (unless no bid (ask) order in the book, then do nothing)
        final int quantity = 1;
        
        // estimate fundamental value as current fundamental, plus zero-mean Gaussian noise,
        // with standard deviation of noiseStdev
        final Price currentFundEstimate = new Price(
            Rands.nextGaussian(
                rand, // random generator
                fundamental.getValueAt(currentTime).intValue(), // mean
                noiseStdev * noiseStdev // variance
        )).nonnegative();
        
        final Quote quote = marketQuoteProcessor.getQuote();
        
        // only place an order if there is already a desirable quote in the limit order book
        if (quote.getAskPrice() != null) {
            final Price askPrice = new Price(quote.getAskPrice().doubleValue());
            final OrderType type = BUY;
            final Price currentBuyValueEstimate = new Price(
                currentFundEstimate.doubleValue() + 
                privateValue.getValueFromQuantity(positionBalance, quantity, type).doubleValue()
            ).nonnegative();
            
            if (currentBuyValueEstimate.greaterThan(askPrice) && this.withinMaxPosition(type, quantity)) {
                log.log(
                    INFO, 
                    "%s executing Fundamental strategy position=%d, for q=%d, buyVal=%f, curPrice=%d",
                    this, positionBalance, quantity, currentBuyValueEstimate.doubleValue(),
                    askPrice.intValue()
                );
                scheduler.executeActivity(new SubmitNMSOrder(this, primaryMarket,
                    type, askPrice, quantity));
                /*
                System.out.println(
                    "v: " + fundamental.getValueAt(currentTime).intValue() 
                    + ", vEst: " + currentFundEstimate.intValue()
                    + ", bid: " + quote.getBidPrice().intValue() 
                    + ", ask: " + quote.getAskPrice().intValue() 
                    + ", buy"
                );
                */
                return;
            }
        }
        
        if (quote.getBidPrice() != null) {
            final Price bidPrice = new Price(quote.getBidPrice().doubleValue());
            final OrderType type = SELL;
            final Price currentSellValueEstimate = new Price(
                currentFundEstimate.doubleValue() + 
                privateValue.getValueFromQuantity(positionBalance, quantity, type).doubleValue()
            ).nonnegative();
            
            if (currentSellValueEstimate.lessThan(bidPrice) && this.withinMaxPosition(type, quantity)) {
                log.log(
                    INFO, 
                    "%s executing Fundamental strategy position=%d, for q=%d, buyVal=%f, curPrice=%d",
                    this, positionBalance, quantity, currentSellValueEstimate.doubleValue(),
                    bidPrice.intValue()
                );
                scheduler.executeActivity(new SubmitNMSOrder(this, primaryMarket,
                    type, bidPrice, quantity));
                /*
                System.out.println(
                    "v: " + fundamental.getValueAt(currentTime).intValue() 
                    + ", vEst: " + currentFundEstimate.intValue()
                    + ", bid: " + quote.getBidPrice().intValue() 
                    + ", ask: " + quote.getAskPrice().intValue() 
                    + ", sell"
                );
                */
                return;
            }
        }
        
        /*
        System.out.println(
            "v: " + fundamental.getValueAt(currentTime).intValue() 
            + ", vEst: " + currentFundEstimate.intValue()
            + ", bid: " + quote.getBidPrice().intValue() 
            + ", ask: " + quote.getAskPrice().intValue() 
            + ", no trade"
        );
        */
        
        // no trade if execution made it here
        log.log(INFO, "%s executing Fundamental strategy, no trade", this);
	}
	
	/**
	 * Checks if new order, if submitted, would be within max position; returns
	 * true if would be within position limits.
	 *  
	 * @param type
	 * @param quantity
	 * @return
	 */
	public boolean withinMaxPosition(OrderType type, int quantity) {
		int newPosition = (type.equals(BUY) ? 1 : -1) * quantity + positionBalance;
		if (Math.abs(newPosition) > privateValue.getMaxAbsPosition()) {
			// if exceed max position, then don't submit a new bid
			log.log(INFO, "%s submitting new order would exceed max position %d ; no submission",
					this, privateValue.getMaxAbsPosition());
			return false;
		}
		return true;
	}
	
	/**
	 * Buy (or sell) exactly 1 unit of the equity, via a "simulated market order"
	 * at current prices, if it is within quantity limits.
	 * 
	 * That is, if there is already a bid (ask) quote in the market, submit a limit order for 1 unit
	 * at the quoted price.
	 * 
	 * If there is not already a bid (ask) quote in the market, do nothing.
	 * 
	 * @param type buy or sell
	 * @param currentTime current time stamp, for bookkeeping only
	 */
	public final void executeZIMOStrategy(
        final OrderType type,
        final TimeStamp currentTime
    ) {
	    // always trade 1 unit (unless no bid (ask) order in the book, then do nothing)
	    final int quantity = 1;
        if (this.withinMaxPosition(type, quantity)) {
            
            final Quote quote = marketQuoteProcessor.getQuote();
            
            Price price = new Price(-1.0);
            if (type == BUY) {
                if (quote.getAskPrice() == null) {
                    log.log(INFO, "%s executing ZIMO strategy, no ask price in book; will not buy", this);
                    return;
                }
                
                price = new Price(quote.getAskPrice().doubleValue());
            } else if (type == SELL) {
                if (quote.getBidPrice() == null) {
                    log.log(INFO, "%s executing ZIMO strategy, no bid price in book; will not sell", this);
                    return;
                } 
                    
                price = new Price(quote.getBidPrice().doubleValue());
            } else {
                throw new IllegalStateException();
            }
            if (price.doubleValue() < 0.0) {
                throw new IllegalStateException();
            }
                        
            log.log(INFO, "%s executing ZIMO strategy position=%d, for q=%d, price=%s",
                this, positionBalance, quantity, price.doubleValue());
            
            scheduler.executeActivity(new SubmitNMSOrder(this, primaryMarket,
                type, price, quantity));
        }
    }
	
	public final void executeZIRPStrategy(
		final OrderType type, 
		final int quantity, 
		final TimeStamp currentTime,
		final int simulationLength,
		final double fundamentalKappa,
		final double fundamentalMean,
		final double acceptableProfitFraction
	) {		
		if (withinMaxPosition(type, quantity)) {
			Price val = getEstimatedValuation(type, currentTime, 
				simulationLength, fundamentalKappa, fundamentalMean);
			Price price = new Price((val.doubleValue() + (type.equals(SELL) ? 1 : -1) 
					* Rands.nextUniform(rand, bidRangeMin, bidRangeMax
					))).nonnegative().quantize(tickSize);

			final Price rHat = this.getEstimatedFundamental(currentTime, simulationLength, 
					fundamentalKappa, fundamentalMean);  
			
			log.log(INFO, "%s at position=%d, for %s %d units, r_t=%s & %s steps left, value=rHat+pv=%s+%s= %s",
					this, positionBalance, type, quantity,
					fundamental.getValueAt(currentTime).intValue(),
					simulationLength - currentTime.getInTicks(),
					rHat, privateValue.getValue(positionBalance, type), 
					val);
			
			Quote quote = marketQuoteProcessor.getQuote();
			if (
				quote != null 
				&& quote.getBidPrice() != null 
				&& quote.getAskPrice() != null
			) {
				if (type.equals(SELL)) {
					// estimated surplus from selling at the above price
					final int shading = price.intValue() - val.intValue();
					final int bidPrice = quote.getBidPrice().intValue();
					
					// estimated surplus from selling all units at the bid price
					final int bidMarkup = bidPrice * quantity - val.intValue();
					
					// if you would make at least acceptableProfitFraction of your
					// markup at the bid, submit order at estimated fundamental
					if (shading * acceptableProfitFraction <= bidMarkup) {
						price = new Price(val.doubleValue() / quantity).quantize(tickSize).nonnegative();
						
						log.log(INFO, "%s executing ZIRP strategy GREEDY SELL @ %s, shading %s * desiredFrac %s <= bidMarkup=%s at BID=%s",
							this, price, shading, acceptableProfitFraction, bidMarkup, bidPrice);
						BUS.post(new ZIRPStatistic(this, true));
					} else {
						log.log(INFO, "%s executing ZIRP strategy no greedy sell, shading %s * desiredFrac %s = %s > bidMarkup of %s",
							this, shading, acceptableProfitFraction, 
							(shading * acceptableProfitFraction), bidMarkup);
						BUS.post(new ZIRPStatistic(this, false));
					}
				} else {
					// estimated surplus from buying at the above price
					final int shading = val.intValue() - price.intValue();
					final int askPrice = quote.getAskPrice().intValue();
					
					// estimated surplus from buying all units at the ask price
					final int askMarkup = val.intValue() - askPrice * quantity;
					
					// if you would make at least acceptableProfitFraction of your 
					// markup at the ask, submit order at estimated fundamental
					if (shading * acceptableProfitFraction <= askMarkup) {
						price = new Price(val.doubleValue() / quantity).quantize(tickSize).nonnegative();
						
						log.log(INFO, "%s executing ZIRP strategy GREEDY BUY @ %s, shading %s * desiredFrac %s <= askMarkup=%s at ASK=%s",
								this, price, shading, acceptableProfitFraction, askMarkup, askPrice);
						BUS.post(new ZIRPStatistic(this, true));
					} else {
						log.log(INFO, "%s executing ZIRP strategy no greedy sell, shading %s * desiredFrac %s = %s > askMarkup of %s",
							this, shading, acceptableProfitFraction, 
							(shading * acceptableProfitFraction), askMarkup); 
						BUS.post(new ZIRPStatistic(this, false));
					}
				}
			}
			scheduler.executeActivity(new SubmitNMSOrder( this,	primaryMarket,type, 
					price, quantity));
		} else {
			// if exceed max position, then don't submit a new bid
			log.log(INFO, "%s executing ZIRP strategy new order would exceed max position %d ; no submission",
					this, privateValue.getMaxAbsPosition());
		}
	}
	
	@Override
	public void processTransaction(Transaction trans) {
		
		super.processTransaction(trans);
		TimeStamp submissionTime;
		OrderType type;
		
		if (trans.getBuyer().equals(trans.getSeller())) {
			// FIXME Handle buyer = seller appropriately... Maybe this is appropriate?
			return;
		} else if (trans.getBuyer().equals(this)) {
			submissionTime = trans.getBuyOrder().getSubmitTime();
			type = OrderType.BUY;
		} else {
			submissionTime = trans.getSellOrder().getSubmitTime();
			type = OrderType.SELL;
		}
		TimeStamp timeToExecution = trans.getExecTime().minus(submissionTime);

		int privateValue = getTransactionValuation(type, trans.getQuantity(), 
				trans.getExecTime()).intValue();
		int cost = trans.getPrice().intValue() * trans.getQuantity();
		int transactionSurplus = (privateValue - cost) * (type.equals(BUY) ? 1 : -1) ;
		
		surplus.addValue(transactionSurplus, timeToExecution.getInTicks());
	}

	/**
	 * @return undiscounted surplus for player observation
	 */
	@Override
	public double getPayoff() {
		return this.getLiquidationProfit() + surplus.getValueAtDiscount(DiscountFactor.NO_DISC);
	}
	
	/**
	 * @return private-value control variables for player features
	 */
	@Override
	public Map<String, Double> getFeatures() {
		ImmutableMap.Builder<String, Double> features = ImmutableMap.builder();
		Price buyPV = privateValue.getValue(0, BUY);
		Price sellPV = privateValue.getValue(0, SELL);
		
		features.put(Keys.PV_POSITION1_MAX_ABS, Math.max(Math.abs(buyPV.doubleValue()), 
				Math.abs(sellPV.doubleValue())));
		features.put(Keys.PV_BUY1, buyPV.doubleValue());
		features.put(Keys.PV_SELL1, sellPV.doubleValue());
		
		return features.build();
	}
	
	/**
	 * @param discount
	 * @return
	 */
	public double getDiscountedSurplus(DiscountFactor discount) {
		return surplus.getValueAtDiscount(discount);
	}

	
	/**
	 * For control variates
	 * @return
	 */
	public Price getPrivateValueMean() {
		return privateValue.getMean();
	}
	
	/**
	 * Returns the limit price (i.e. valuation) for the agent for buying/selling 1 unit.
	 * 
	 * valuation = fundamental + private_value
	 * 
	 * @param type
	 * @param currentTime
	 * @return
	 */
	protected Price getValuation(OrderType type, TimeStamp currentTime) {
		return getValuation(type, 1, currentTime);
	}
	
	protected Price getEstimatedValuation(
		final OrderType type, 
		final TimeStamp currentTime,
		final int simulationLength,
		final double fundamentalKappa,
		final double fundamentalMean
	) {
		return getEstimatedValuation(type, 1, currentTime, 
			simulationLength, fundamentalKappa, fundamentalMean);
	}
	
	protected Price getEstimatedValuation(
		OrderType type, 
		int quantity, 
		TimeStamp currentTime,
		final int simLength,
		final double fundamentalKappa,
		final double fundamentalMean
	) {
		final Price rHat = this.getEstimatedFundamental(currentTime, simLength, 
				fundamentalKappa, fundamentalMean); 
			
		return new Price(rHat.intValue() * quantity
				+ privateValue.getValueFromQuantity(positionBalance, quantity, type).intValue()
				).nonnegative();
	}
	
	/**
	 * Returns valuation = fundamental + private value (value of cumulative
	 * gain if over quantity > 1).
	 * 
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * @return
	 */
	protected Price getValuation(OrderType type, int quantity, TimeStamp currentTime) {
		return new Price(fundamental.getValueAt(currentTime).intValue() * quantity
				+ privateValue.getValueFromQuantity(positionBalance, quantity, type).intValue()
				).nonnegative();
	}

	/**
	 * Returns only the private value for trading (assuming agents all liquidate
	 * at the end).
	 * 
	 * Note that this method has to subtract the transacted quantity from
	 * position balance (using the pre-transaction balance to determine the
	 * valuation).
	 * 
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * @return
	 */
	protected Price getTransactionValuation(OrderType type, int quantity,
			TimeStamp currentTime) {
		
		// Determine the pre-transaction balance
		int originalBalance = this.positionBalance + (type.equals(BUY) ? -1 : 1) * quantity;
		return this.privateValue.getValueFromQuantity(originalBalance, 
				quantity, type);
	}
	
	/**
	 * @param type
	 * @param currentTime
	 * @return
	 */
	protected Price getTransactionValuation(OrderType type,
			TimeStamp currentTime) {
		return getTransactionValuation(type, 1, currentTime);
	}
	
//	protected Price getEstimatedLimitPrice(
//		final OrderType type, 
//		final TimeStamp currentTime,
//		final int simulationLength,
//		final double fundamentalKappa,
//		final double fundamentalMean
//	) {
//		return getEstimatedLimitPrice(
//			type, 1, currentTime, simulationLength, fundamentalKappa, fundamentalMean
//		);
//	}
//	
//	protected Price getEstimatedLimitPrice(
//		final OrderType type, 
//		final int quantity, 
//		final TimeStamp currentTime,
//		final int simulationLength,
//		final double fundamentalKappa,
//		final double fundamentalMean
//	) {
//		return new Price(
//			getEstimatedValuation(
//				type, 
//				quantity, 
//				currentTime, 
//				simulationLength, 
//				fundamentalKappa, 
//				fundamentalMean
//			).doubleValue() / quantity
//		).nonnegative();
//	}
	
	/**
	 * Returns the limit price for a new order of quantity 1.
	 * 
	 * @param type
	 * @param currentTime
	 * @return
	 */
	protected Price getLimitPrice(OrderType type, TimeStamp currentTime) {
		return getLimitPrice(type, 1, currentTime);
	}
	
	/**
	 * Returns the limit price for the agent given potential quantity for which
	 * the agent plans to submit an order.
	 * 
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * @return
	 */
	protected Price getLimitPrice(OrderType type, int quantity, TimeStamp currentTime) {
		return new Price(getValuation(type, quantity, currentTime).doubleValue() 
				/ quantity).nonnegative();
	}
}
