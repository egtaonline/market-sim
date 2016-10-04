package sumstats;

import com.google.common.primitives.Doubles;

/**
 * Compact Summary Statistics Class meant to only calculate sum and standard
 * deviation. This generally has more accuracy than the Apache Commons Math
 * SummaryStatistics class.
 * 
 * Internally it is backed by the KahanSum class to do efficient sums of
 * floating point values. There are potentially more robust ways to do this, and
 * this method is somewhat unproven, but it seems to be more accurate than the
 * method proposed by Knuth that is implemented many places including Apache
 * Commons Math.
 * 
 * @author erik
 * 
 */
public class SumStats {

	protected long n;
	protected KahanSum sum, sumsq;
	
	protected SumStats(long n, KahanSum sum, KahanSum sumsq) {
		this.n = n;
		this.sum = sum;
		this.sumsq = sumsq;
	}
	
	/**
	 * Create a new SumStats object with no data
	 */
	public static SumStats create() {
		return new SumStats(0, KahanSum.create(), KahanSum.create());
	}
	
	/**
	 * Copy the current state of a SumStats object
	 */
	public static SumStats copy(SumStats original) {
		return new SumStats(original.n, KahanSum.copy(original.sum), KahanSum.copy(original.sumsq));
	}
	
	/**
	 * Create a SumStats object with initial data
	 */
	public static SumStats fromData(Iterable<Double> data) {
		SumStats stats = SumStats.create();
		for (double d : data)
			stats.add(d);
		return stats;
	}
	
	/**
	 * Create a SumStats object with initial data
	 */
	public static SumStats fromData(double[] data) {
		return fromData(Doubles.asList(data));
	}
	
	/**
	 * Add a data point
	 */
	public void add(double val) {
		++n;
		sum.add(val);
		sumsq.add(val * val);
	}
	
	/**
	 * Return the sum of all of the data added so far
	 * @return
	 */
	public double sum() {
		return sum.sum();
	}
	
	/**
	 * Return the mean of all data added so far
	 */
	public double mean() {
		return sum.sum() / n;
	}
	
	/**
	 * Return the sample variance of all data added so far
	 * @return
	 */
	public double variance() {
		return (sumsq.sum() - (sum.sum() * sum.sum()) / n) / (n - 1);
	}
	
	/**
	 * Return the sample statndard deviation of all data added so far
	 */
	public double stddev() {
		return Math.sqrt(variance());
	}
	
	/**
	 * @return n	for testing purposes primarily
	 */
	public long getN() {
		return n;
	}
}
