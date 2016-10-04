package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

/**
 * FundamentalMM
 * 
 * Quotes a spread fixed around an estimate of the fundamental.
 * 
 * Estimate can be based on a fixed input argument (fundEstimate)
 * but if this value is not specified, then it uses the r_hat estimation
 * function (same as what's used by ZIRPs) to estimate the fundamental value.
 * 
 * MM will lose time priority if use last bid & ask, so use tick improvement
 * and quote inside the spread.
 * 
 * @author ewah
 */
public class FundamentalMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 9057600979711100221L;
	
	protected Price fundamentalEstimate;
	protected int simulationLength;
	protected double fundamentalKappa;
	protected double fundamentalMean;
	protected Price constSpread;
	protected boolean update;

	public FundamentalMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Market market, Random rand, double reentryRate,
			int tickSize, int numRungs, int rungSize, boolean truncateLadder,
			boolean tickImprovement, boolean tickOutside, int initLadderMean, 
			int initLadderRange, int simLength, double kappa,
			double fundamentalMean, int fundamentalEstimate, int constSpread, 
			boolean update) {

		super(scheduler, fundamental, sip, market, rand, reentryRate, tickSize,
				numRungs, rungSize, truncateLadder, tickImprovement, tickOutside,
				initLadderMean, initLadderRange);
		
		this.update = update;	// if true, update estimate on subsequent rounds
		
		this.fundamentalEstimate = (fundamentalEstimate > 0) ? new Price(fundamentalEstimate) : null;
		simulationLength = simLength;
		fundamentalKappa = kappa;
		this.fundamentalMean = fundamentalMean;
		this.constSpread = null;
		if (constSpread > 0) this.constSpread = new Price(constSpread);
	}

	public FundamentalMarketMaker(Scheduler scheduler, FundamentalValue fundamental,
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
				props.getAsInt(Keys.SIMULATION_LENGTH),
				props.getAsDouble(Keys.FUNDAMENTAL_KAPPA),
				props.getAsInt(Keys.FUNDAMENTAL_MEAN), 
				props.getAsInt(Keys.FUNDAMENTAL_ESTIMATE),
				props.getAsInt(Keys.SPREAD),
				props.getAsBoolean(Keys.UPDATE));
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		super.agentStrategy(currentTime);

		Price bid = this.getQuote().getBidPrice();
		Price ask = this.getQuote().getAskPrice();

		log.log(INFO, "%s in %s: Withdraw all orders.", this, primaryMarket);
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
		int offset = this.initLadderRange / 2;
		if (bid != null && ask != null) { 
			offset = (ask.intValue() - bid.intValue()) / 2;
		}
		if (this.constSpread != null) {
			offset = this.constSpread.intValue() / 2;
		}
		if (fundamentalEstimate == null) {
			fundamentalEstimate = this.getEstimatedFundamental(currentTime, simulationLength, 
					fundamentalKappa, fundamentalMean);
		}
		if (update) {
			fundamentalEstimate = this.getEstimatedFundamental(currentTime, simulationLength, 
					fundamentalKappa, fundamentalMean);
		}
		log.log(INFO, "%s in %s: Spread of %s around estimated fundamental %s, ladderBid=%s, ladderAsk=%s", 
				this, primaryMarket, new Price(offset), fundamentalEstimate,
				new Price(fundamentalEstimate.intValue() - offset),
				new Price(fundamentalEstimate.intValue() + offset));
		this.createOrderLadder(new Price(fundamentalEstimate.intValue() - offset),
				new Price(fundamentalEstimate.intValue() + offset));
	}
}
