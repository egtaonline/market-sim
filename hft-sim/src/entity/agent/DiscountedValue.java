package entity.agent;

import java.util.EnumMap;

import systemmanager.Consts.DiscountFactor;

/**
 * This class represents any value that needs to be stored at all discount
 * values for the simulation. Currently it's only used for background agent
 * surplus.
 * 
 * @author erik
 * 
 */
class DiscountedValue {

	private EnumMap<DiscountFactor, Double> values;
	
	public DiscountedValue() {
		values = new EnumMap<DiscountFactor, Double>(DiscountFactor.class);
		for (DiscountFactor discount : DiscountFactor.values())
			values.put(discount, 0d);
	}
	
	public static DiscountedValue create() {
		return new DiscountedValue();
	}
	
	public void addValue(double value, double discountTime) {
		for (DiscountFactor discount : DiscountFactor.values())
			values.put(discount, values.get(discount) + Math.exp(-discount.discount * discountTime) * value);
	}
	
	/**
	 * This method is very inefficient. Much better to use the array of iterate
	 * if you know which one.
	 * 
	 * @param discount
	 * @return
	 */
	public double getValueAtDiscount(DiscountFactor discount) {
		return values.get(discount);
	}
	
}
