package entity.market;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import activity.Clear;
import data.EntityProperties;
import entity.infoproc.SIP;
import entity.market.clearingrule.UniformPriceClear;
import event.TimeStamp;

/**
 * Class for a call market. The order book is closed, therefore agents will only
 * be able to see the price of the last clear as well as the bid/ask immediately
 * after the clear, i.e. they will be able to see the best available buy and
 * sell prices for the bids left in the order book after each market clear.
 * 
 * NOTE: First Clear Activity is initialized in the SystemManager.
 * 
 * @author ewah
 */
public class CallMarket extends Market {
	
	private static final long serialVersionUID = -1736458709580878467L;
	
	protected final TimeStamp clearInterval;
	protected TimeStamp nextClearTime;

	public CallMarket(Scheduler scheduler, SIP sip, Random rand,
			TimeStamp latency, int tickSize, double pricingPolicy,
			TimeStamp clearInterval) {
		
		this(scheduler, sip, rand, latency, latency, tickSize, pricingPolicy,
				clearInterval);
	}

	public CallMarket(Scheduler scheduler, SIP sip, Random rand,
			TimeStamp quoteLatency, TimeStamp transactionLatency, int tickSize,
			double pricingPolicy, TimeStamp clearInterval) {
		
		super(scheduler, sip, quoteLatency, transactionLatency,
				new UniformPriceClear(pricingPolicy, tickSize), rand);
		checkArgument(clearInterval.after(TimeStamp.ZERO),
				"Can't create a call market with clearing interval 0. Create a CDA instead.");

		this.clearInterval = clearInterval;
		this.nextClearTime = TimeStamp.ZERO;
	}

	public CallMarket(Scheduler scheduler, SIP sip, Random rand,
			EntityProperties props) {
		
		this(scheduler, sip, rand,
				TimeStamp.create(props.getAsInt(Keys.QUOTE_LATENCY, Keys.MARKET_LATENCY)),
				TimeStamp.create(props.getAsInt(Keys.TRANSACTION_LATENCY, Keys.MARKET_LATENCY)),
				props.getAsInt(Keys.MARKET_TICK_SIZE, Keys.TICK_SIZE),
				props.getAsDouble(Keys.PRICING_POLICY),
				TimeStamp.create(props.getAsInt(Keys.CLEAR_INTERVAL)));
	}

	@Override
	public void clear(TimeStamp currentTime) {
		nextClearTime = currentTime.plus(clearInterval);
		super.clear(currentTime);
		scheduler.scheduleActivity(nextClearTime, new Clear(this));
	}

}
