package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.INFO;
import iterators.ExpInterarrivals;

import java.util.Iterator;
import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;
import fourheap.Order.OrderType;

public final class ZIRPAgent extends BackgroundAgent {

	private static final long serialVersionUID = -8805640643365079141L;

	private final int simulationLength;
	private final double fundamentalKappa;
	private final int fundamentalMean;
	private final boolean withdrawOrders;
	private final double acceptableProfitFraction;
	
	public ZIRPAgent(
		final Scheduler scheduler, 
		final TimeStamp arrivalTime,
		final FundamentalValue fundamental, 
		final SIP sip, 
		final Market market, 
		final Random rand,
		final EntityProperties props
	) {
		this(scheduler,	arrivalTime, fundamental, sip, market, rand,
			ExpInterarrivals.create(props.getAsDouble(Keys.BACKGROUND_REENTRY_RATE, Keys.REENTRY_RATE),	rand),
			props.getAsDouble(Keys.PRIVATE_VALUE_VAR),
			props.getAsInt(Keys.AGENT_TICK_SIZE, Keys.TICK_SIZE),
			props.getAsInt(Keys.MAX_POSITION),
			props.getAsInt(Keys.BID_RANGE_MIN),
			props.getAsInt(Keys.BID_RANGE_MAX),
			props.getAsBoolean(Keys.WITHDRAW_ORDERS),
			props.getAsInt(Keys.SIMULATION_LENGTH),
			props.getAsDouble(Keys.FUNDAMENTAL_KAPPA),
			props.getAsInt(Keys.FUNDAMENTAL_MEAN),
			props.getAsDouble(Keys.ACCEPTABLE_PROFIT_THRESHOLD)
		);
		
	}

	private ZIRPAgent(
			final Scheduler scheduler, 
			final TimeStamp arrivalTime,
			final FundamentalValue fundamental, 
			final SIP sip,
			final Market market, 
			final Random rand,
			final Iterator<TimeStamp> interarrivals, 
			final double pvVar, 
			final int tickSize,
			final int maxAbsPosition, 
			final int bidRangeMin, 
			final int bidRangeMax,
			final boolean aWithdrawOrders,
			final int aSimulationLength, 
			final double aFundamentalKappa,
			final int aFundamentalMean,
			final double aAcceptableProfitFraction
			) {

		super(scheduler, arrivalTime, fundamental, sip, market, rand,
				interarrivals, new PrivateValue(maxAbsPosition, pvVar, rand),
				tickSize, bidRangeMin, bidRangeMax);

		if (aAcceptableProfitFraction < 0 || aAcceptableProfitFraction > 1) {
			throw new IllegalArgumentException("Acceptable profit fraction must be in [0, 1]. " 
					+ aAcceptableProfitFraction
					);
		}
		simulationLength = aSimulationLength;
		fundamentalKappa = aFundamentalKappa;
		fundamentalMean = aFundamentalMean;
		withdrawOrders = aWithdrawOrders;
		acceptableProfitFraction = aAcceptableProfitFraction;
	}

	@Override
	public void agentStrategy(final TimeStamp currentTime) {
		super.agentStrategy(currentTime);

		if (!currentTime.equals(arrivalTime)) log.log(INFO, "%s Wake up.", this);
		if (withdrawOrders) {
			log.log(INFO, "%s Withdraw all orders.", this);
			withdrawAllOrders();
		}

		// 50% chance of being either long or short
		OrderType orderType = BUY;
		if (rand.nextBoolean()) orderType = SELL;

		// log.log(INFO, "%s Submit %s order.", this, orderType);
		executeZIRPStrategy(orderType, 1, currentTime, simulationLength, fundamentalKappa, 
				fundamentalMean, acceptableProfitFraction);

	}
}
