package sumstats;

/**
 * This class allows robust summation of doubles. The accuracy is akin to adding
 * with the Java BigDecimal class, except this is significantly more efficient
 * in both memory and time.
 * 
 * @author erik
 * 
 */
public final class KahanSum {
	
	private double sum, c;
	
	private KahanSum(double sum, double c) {
		this.sum = sum;
		this.c = c;
	}
	
	/**
	 * Create a new sumpty sum
	 */
	public static KahanSum create() {
		return new KahanSum(0d, 0d);
	}
	
	/**
	 * Copy the current state of a sum
	 */
	public static KahanSum copy(KahanSum original) {
		return new KahanSum(original.sum, original.c);
	}
	
	/**
	 * Add a value to the sum
	 */
	public void add(double val) {
		double y = val - c;
		double t = sum + y;
		c = (t - sum) - y;
		sum = t;
	}
	
	/**
	 * Get the sum of the values added up to this point
	 */
	public double sum() {
		return sum;
	}
	
}
