package entity.market;

import static java.math.RoundingMode.HALF_EVEN;

import utils.MathUtils;

import com.google.common.base.Objects;
import com.google.common.collect.Ordering;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Ints;

/**
 * Price class is wrapper for long; one unit represents one thousandth of a
 * dollar.
 * 
 * @author ewah
 */
public class Price extends Number implements Comparable<Price> {

	private static final long serialVersionUID = 772101228717034473L;

	/*
	 * XXX Take infinite price into account for subtraction, addition, toString,
	 * etc. Basically, should infinite price be special?
	 */
	// MIN_VALUE + 1 so that -(-INF) = INF
	public static final Price INF = new Price(Integer.MAX_VALUE);
	public static final Price NEG_INF = new Price(Integer.MIN_VALUE + 1);
	public static final Price ZERO = new Price(0);

	public static int TICKS_PER_DOLLAR = 1000;

	protected final int ticks; // in ticks

	/**
	 * Constructor taking in a int.
	 * 
	 * @param ticks
	 */
	public Price(int ticks) {
		this.ticks = ticks;
	}
	
	public Price(double ticks) {
		if (ticks >= Integer.MAX_VALUE) 
			this.ticks = INF.ticks;
		else 
			this.ticks = DoubleMath.roundToInt(ticks, HALF_EVEN);
	}
	
	@Override
	public int intValue() {
		return ticks;
	}

	@Override
	public long longValue() {
		return ticks;
	}

	@Override
	public float floatValue() {
		return ticks;
	}

	@Override
	public double doubleValue() {
		return ticks;
	}

	public Price quantize(int quanta) {
		return new Price(MathUtils.quantize(ticks, quanta));
	}
	
	/**
	 * @return price in dollars
	 */
	public double getInDollars() {
		return ticks / (double) TICKS_PER_DOLLAR;
	}

	/**
	 * Return 0 if price is negative
	 * @return Non-negative version of the price.
	 */
	public Price nonnegative() {
		return Ordering.natural().max(this, ZERO);
	}

	/**
	 * Any price is greater than null
	 */
	@Override
	public int compareTo(Price o) {
		if (o == null)
			return 1;
		return Ints.compare(ticks, o.ticks);
	}

	/**
	 * True if p is null or this price is strictly greater
	 */
	public boolean greaterThan(Price p) {
		return p == null || compareTo(p) > 0;
	}

	/**
	 * True if p is null or this price is strictly less
	 */
	public boolean lessThan(Price p) {
		return p == null || compareTo(p) < 0;
	}

	/**
	 * True if p is null or this price is greater or equal
	 */
	public boolean greaterThanEqual(Price p) {
		return p == null || compareTo(p) >= 0;
	}

	/**
	 * True if p is null or this price is less or equal
	 */
	public boolean lessThanEqual(Price p) {
		return p == null || compareTo(p) <= 0;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(ticks);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Price))
			return false;
		Price other = (Price) obj;
		return ticks == other.ticks;
	}
	
	@Override
	public String toString() {
		return '$' + Long.toString(ticks);
	}
	
	public String toDollarString() {
		int absTicks = Math.abs(ticks); 
		int dollars = absTicks / TICKS_PER_DOLLAR;
		int digits = MathUtils.logn(TICKS_PER_DOLLAR, 10);
		int cents = absTicks % TICKS_PER_DOLLAR;
		while (digits > 2 && cents % 10 == 0) {
			cents /= 10;
			digits--;
		}
		
		return String.format("%s$%d.%0" + digits + "d", ticks < 0 ? "-" : "", dollars, cents);
	}

}
