package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;

import java.util.Random;

import com.google.common.collect.Iterators;

import activity.SubmitNMSOrder;
import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.CallMarket;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

/**
 * Created solely for purpose of measuring maximum allocative efficiency in 
 * a market. 
 * 
 * Arrives at time 0. 
 * 
 * @author ewah
 *
 */
public class MaxEfficiencyAgent extends BackgroundAgent {

	private static final long serialVersionUID = -8915874536659571239L;

	public MaxEfficiencyAgent(Scheduler scheduler, FundamentalValue fundamental, 
			SIP sip, Market market, Random rand, double pvVar, int tickSize, 
			int maxAbsPosition) {
		
		super(scheduler, TimeStamp.ZERO, fundamental, sip, market, rand, 
				Iterators.<TimeStamp> emptyIterator(), 
				new PrivateValue(maxAbsPosition, pvVar, rand),
				tickSize, 0, 0);
	}

	public MaxEfficiencyAgent(Scheduler scheduler, FundamentalValue fundamental, 
			SIP sip, Market market, Random rand, EntityProperties props) {

		this(scheduler, fundamental, sip, market, rand,
				props.getAsDouble(Keys.PRIVATE_VALUE_VAR),
				props.getAsInt(Keys.AGENT_TICK_SIZE, Keys.TICK_SIZE),
				props.getAsInt(Keys.MAX_POSITION));
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		if (this.primaryMarket instanceof CallMarket) {
			// submit 1-unit limit orders for all values in its private vector

			// sells
			for (int qty = -privateValue.getMaxAbsPosition() + 1; qty <= 0; qty++) {
				Price price = new Price(fundamental.getMeanValue() + 
						privateValue.getValueFromQuantity(qty, 1, SELL)
							.intValue()).nonnegative().quantize(tickSize);
				scheduler.executeActivity(new SubmitNMSOrder(this, primaryMarket,
						SELL, price, 1));
			}
			// buys
			for (int qty = 0; qty < privateValue.getMaxAbsPosition(); qty++) {
				Price price = new Price(fundamental.getMeanValue() + 
						privateValue.getValueFromQuantity(qty, 1, BUY)
							.intValue()).nonnegative().quantize(tickSize);
				scheduler.executeActivity(new SubmitNMSOrder(this, primaryMarket,
						BUY, price, 1));
			}
			// and then it doesn't reenter

		} else {
			throw new IllegalArgumentException("Primary market " + primaryMarket + " must be a call market!"); 
		}
	}
	
	public int getPosition() {
		return this.positionBalance;
	}
}
