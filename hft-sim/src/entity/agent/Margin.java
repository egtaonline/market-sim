package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import utils.Rands;

import com.google.common.collect.Lists;

import fourheap.Order.OrderType;

/**
 * Idea from: Tesauro & Das, "High-Performance Bidding Agents for the Continuous
 * Double Auction," EC-01.
 * 
 * When agents can trade multiple units, we use an array of profit margins
 * of the size of the number of units. Different units have different
 * limit prices, so they require different profit margins to trade at
 * equilibrium. There is one margin per traded unit.
 * 
 * In the paper, the margins are not statistically independent; the limit prices
 * of the less valuable units influence the initial margins of the more
 * valuable units---TODO how? or perhaps margins should be independent? 
 * 
 * Margins: <code>getValue</code> is based on current (or projected) position
 * balance. <code>setValue</code> is similar.
 * 
 * NOTE: Margins only work with single quantity changes.
 * 
 * @author ewah
 *
 */
class Margin implements QuantityIndexedArray<Double> {

	private static final long serialVersionUID = -3749423779545857329L;
	
	protected final int offset;
	protected List<Double> values;
	
	public Margin() {
		this.offset = 0;
		this.values = Collections.emptyList();
	}
	
	/**
	 * @param maxPosition
	 * @param rand
	 * @param a
	 * @param b
	 */
	public Margin(int maxPosition, Random rand, double a, double b) {
		checkArgument(maxPosition > 0, "Max Position must be positive");
		
		// Identical to legacy generation in final output
		this.offset = maxPosition;
		this.values = Lists.newArrayList();
		
		double[] values = new double[maxPosition * 2];
		for (int i = 0; i < values.length; i++)
			values[i] = Rands.nextUniform(rand, a, b) *	(i >= maxPosition ? -1 : 1);
			// margins for buy orders are negative
		
		for (double value : values)
			this.values.add(new Double(value));
	}
	
	/**
	 * Protected constructor for testing purposes.
	 * 
	 * @param maxPosition
	 * @param values
	 */
	protected Margin(int maxPosition, Collection<Double> values) {
		checkArgument(values.size() == 2*maxPosition, "Incorrect number of entries in list");
		this.values = Lists.newArrayList();
		this.values.addAll(values);
		offset = maxPosition;
	}
	
	@Override
	public int getMaxAbsPosition() {
		return offset;
	}

	/**
	 * Gets margin for single-unit trades. If the projected position would
	 * exceed the maximum, the profit margin is 0.
	 * 
	 * @param currentPosition
	 * @param type
	 * @return
	 */
	// XXX Erik: Is there a reason this is a Double instead of a double?
	@Override
	public Double getValue(int currentPosition, OrderType type) {
		switch (type) {
		case BUY:
			if (currentPosition + offset <= values.size() - 1 &&
					currentPosition + offset >= 0)
				return values.get(currentPosition + offset);
			break;
		case SELL:
			if (currentPosition + offset - 1 <= values.size() - 1 && 
					currentPosition + offset - 1 >= 0)
				return values.get(currentPosition + offset - 1);
			break;
		}
		return 0d;
	}

	/**
	 * @param currentPosition
	 * @param type
	 * @param value
	 */
	public void setValue(int currentPosition, OrderType type,
			double value) {
		switch (type) {
		case BUY:
			if (currentPosition + offset <= values.size() - 1 &&
					currentPosition + offset >= 0)
				values.set(currentPosition + offset, value);
			break;
		case SELL:
			if (currentPosition + offset - 1 <= values.size() - 1 && 
					currentPosition + offset - 1 >= 0)
				values.set(currentPosition + offset - 1, value);
			break;
		}
	}
	
	@Override
	public Double getValueFromQuantity(int currentPosition, int quantity,
			OrderType type) {
		checkArgument(quantity > 0, "Quantity must be positive");
		
		// TODO need to implement for multiple units
		return 0d;
	}
}