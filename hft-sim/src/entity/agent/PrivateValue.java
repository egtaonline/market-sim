package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static entity.market.Price.ZERO;
import static fourheap.Order.OrderType.BUY;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;

import sumstats.SumStats;
import utils.Rands;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import entity.market.Price;
import fourheap.Order.OrderType;

/**
 * PRIVATEVALUE
 * 
 * Encapsulation of an agent's private value. <code>getValueFromQuantity</code>
 * is the main method that should be used.
 * 
 * For multi-unit orders, consecutive private value transitions are summed. If
 * the quantity will exceed the maximum (offset), it will be summed up to the 
 * last valid element in values, after which it adds zero for each additional 
 * unit.
 * 
 * Note: A weird case is when the initial position is already outside the
 * valid range, in which case it will return 0 for all transitions up to the
 * valid range, after it sums as before. Agents, however, should always perform
 * a check to verify that they will not exceed their max position, before
 * submitting any orders.
 * 
 * @author ewah
 */
public class PrivateValue implements QuantityIndexedArray<Price> {

	private static final long serialVersionUID = -348702049295080442L;
	
	// maximum absolute value of number of units held
	protected final int offset;
	protected final List<Price> values;

	/**
	 * Constructor for an agent without private value. This will return
	 * Price.ZERO for every private value query.
	 */
	public PrivateValue() {
		offset = 0;
		values = Collections.singletonList(ZERO);
	}
	
	/**
	 * Randomly generate private values for positional changes
	 * 
	 * @param maxPosition
	 *            the maximum position the agent can take.
	 * @param var
	 *            Gaussian variance to use during random private value
	 *            initialization.
	 * @param rand
	 *            The random number generator to use for random generation.
	 */
	public PrivateValue(int maxPosition, double var, Random rand) {
		checkArgument(maxPosition > 0, "Max Position must be positive");
		
		// Identical to legacy generation in final output
		this.offset = maxPosition;
		double[] values = new double[maxPosition * 2];
		for (int i = 0; i < values.length; i++)
			values[i] = Rands.nextGaussian(rand, 0, var);
		Arrays.sort(values);
		ArrayUtils.reverse(values);
		
		Builder<Price> builder = ImmutableList.builder();
		for (double value : values)
			builder.add(new Price(value));
		
		this.values = builder.build();
	}
	
	/**
	 * Protected constructor for testing purposes (DummyPrivateValue)
	 */
	protected PrivateValue(int maxPosition, Collection<Price> values) {
		checkArgument(values.size() == 2*maxPosition, "Incorrect number of entries in list");
		Builder<Price> builder = ImmutableList.builder();
		builder.addAll(values);
		this.values = builder.build();
		offset = maxPosition;
	}

	/**
	 * @return offset (max absolute value position)
	 */
	@Override
	public int getMaxAbsPosition() {
		return offset;
	}
	
	/**
	 * Returns the private benefit of either buying or selling 1 unit 
	 * (indicated by type) when starting at the specified current position.
	 * 
	 * If current position is already at the max allowed, or if the
	 * position exceeds the max position allowed, return +/- infinity, 
	 * depending on whether long/short (negative infinity for long positions, 
	 * positive infinity for short positions, since private values are
	 * sorted in decreasing order).
	 * 
	 * @param position
	 *            Agent's position
	 * @param type
	 * 			  Buy or Sell
	 * @return The private value 
	 */
	@Override
	public Price getValue(int position, OrderType type) {
		switch (type) {
		case BUY:
			if (position + offset <= values.size() - 1 && position + offset >= 0) {
			    // NB: values.size() == 2 * offset
			    // rewriting:
			    // position + 1 <= offset && position >= - offset
			    // therefore, you're in a legal position for buying
			    // return the private value for buying 1 unit with these holdings
				return values.get(position + offset);
			}
			if (position + offset > values.size() - 1) {
			    // NB: values.size() == 2 * offset
			    // rewriting:
			    // position + 1 > offset
			    // therefore buying puts you above offset, which is illegal
			    return Price.NEG_INF;
			}
			if (position + offset < 0) {
			    // position < - offset, so it's infinitely good to buy?
			    return Price.INF;
			}
			throw new IllegalStateException();
		case SELL:
			if (position + offset - 1 <= values.size() - 1 && position + offset - 1 >= 0) {
			    // NB: values.size() == 2 * offset
			    // rewriting:
			    // position <= offset && position - 1 >= - offset
			    // therefore, you're in a legal position for selling
			    // return the private value for buying 1 unit with 1 less holdings,
			    // which is the negative of the loss from losing this unit you'd sell
				return values.get(position + offset - 1);
			}
			if (position + offset - 1 > values.size() - 1) {
			    // NB: values.size() == 2 * offset
			    // rewriting:
			    // position > offset
			    // this is an illegal state, so it's infintely good to sell?
			    return Price.NEG_INF;
			}
			if (position + offset - 1 < 0) {
			    // position - 1 < - offset
			    // therefore selling puts you in illegal holdings
			    // it's infinitely bad to sell
			    return Price.INF;
			}
			throw new IllegalStateException();
		default:
			return Price.ZERO;	// should never be reached
		}
	}
	
	/**
	 * Checks that the quantities are within the range to add; otherwise ignores.
	 * 
	 * @param currentPosition
	 * @param quantity
	 * @param type
	 * @return
	 */
	@Override
	public Price getValueFromQuantity(int currentPosition, int quantity, OrderType type) {
		checkArgument(quantity > 0, "Quantity must be positive");
		// checkArgument(Math.abs(currentPosition) <= offset, "Current position cannot exceed max");
		
		// how many units you'd have after all the trades
		int nextPosition = currentPosition + quantity * (type.equals(BUY) ? 1 : -1);
		
		//checkArgument(Math.abs(nextPosition) <= offset, "Future position cannot exceed max");
		
		// infinitely bad to exceed allowed holdings
		if (nextPosition > offset) return Price.NEG_INF;
		if (nextPosition < -offset) return Price.INF;
		
		int privateValue = 0;
		switch (type) {
		case BUY:
			for (int i = 0; i < quantity; i++)
				if (currentPosition + i + offset <= values.size() - 1 &&
						currentPosition + i + offset >= 0) {
				    // NB: values.size() == offset * 2
				    // rewriting:
				    // currentPosition + i + 1 <= offset && currentPosition + 1 >= -offset
				    // therefore, it is legal to buy this unit
					privateValue += getValue(currentPosition + i, type).intValue();
				}
			break;
		case SELL:
			for (int i = 0; i < quantity; i++)
				if (currentPosition - i + offset - 1 >= 0 &&
						currentPosition - i + offset - 1 <= values.size() - 1) {
				    // NB: values.size() == offset * 2
				    // rewriting:
				    // currentPostion - i - 1 >= - offset && currentPosition - i <= offset
				    // therefore, it is legal to sell this unit
					privateValue += getValue(currentPosition - i, type).intValue();
				}
			break;
		}
		return new Price(privateValue);
	}

	/**
	 * For control variates.
	 * @return
	 */
	public Price getMean() {
		SumStats pv = SumStats.create();
		for (Price price : values) {
			pv.add(price.doubleValue());
		}
		return new Price(pv.mean());
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(values, offset);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PrivateValue))
			return false;
		PrivateValue other = (PrivateValue) obj;
		return other.offset == this.offset && other.values.equals(values);
	}

	@Override
	public String toString() {
		return values.toString();
	}

}