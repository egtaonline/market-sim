package data;

import static logger.Log.log;
import static logger.Log.Level.ERROR;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import utils.Rands;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import entity.market.Price;
import event.TimeStamp;

/**
 * Class to store and compute a stochastic process used as a base to determine
 * the private valuations of background agents.
 * 
 * shockProb must be in [0, 1].
 * 
 * If schockProb == 1, mean reversion and a jump occur in each time step.
 * 
 * If shockProb < 1, then mean reversion occurs in exactly those time steps in which
 * a jump occurs
 * 
 * @author ewah
 */
// XXX Erik: Potentially move this to another package?
public class FundamentalValue implements Serializable {

	private static final long serialVersionUID = 6764216196138108452L;
	
	protected final ArrayList<Double> meanRevertProcess;
	protected final double kappa;
	protected final int meanValue;
	protected final double shockVar;
	protected final Random rand;
	private final double shockProb; // used for jump processes. if not a jump process, should be 1.0

	/**
	 * @param kap rate which the process reverts to the mean value
	 * @param meanVal mean process
	 * @param var Gaussian Process variance
	 * @param rand Random generator
	 */
	protected FundamentalValue(
        double kap, int meanVal, double var, double prob, Random rand
    ) {
		this.rand = rand;
		this.kappa = kap;
		this.meanValue = meanVal;
		this.shockVar = var;
		
		if (prob < 0 || prob > 1) {
		    throw new IllegalArgumentException();
		}
		
		this.shockProb = prob;

		// stochastic initial conditions for random process
		meanRevertProcess = Lists.newArrayList();
		meanRevertProcess.add(Rands.nextGaussian(rand, meanValue, shockVar));
	}
	
	/**
	 * Creates a mean reverting Gaussian Process that supports random access to small (int) TimeStamps
	 * 
	 * @param kap
	 * @param meanVal
	 * @param var
	 * @param prob probability of a jump in fundamental value
	 * @param rand
	 * @return
	 */
	public static FundamentalValue create(
        double kap, int meanVal, double var, double prob, Random rand
    ) {
		return new FundamentalValue(kap, meanVal, var, prob, rand);
	}

	/**
	 * Helper method to ensure that maxQuery exists in the data structure.
	 * 
	 * Uses shockProb to determine whether or not a jump occurs in each time
	 * step. As noted above, in a time step with no jump, there is no mean
	 * reversion either, so the fundamental value remains constant.
	 * 
	 * @param maxQuery
	 */
	protected void computeFundamentalTo(int maxQuery) {
		for (int i = meanRevertProcess.size(); i <= maxQuery; i++) {
			double prevValue = Iterables.getLast(meanRevertProcess);
			
			// test for shockProb == 1 is just for short-circuit
			// efficiency, for runs that set shockProb to 1.
			if (shockProb == 1 || rand.nextDouble() < shockProb) {
			    // a jump occurs at this time step
		        double nextValue = Rands.nextGaussian(rand, meanValue * kappa
                    + (1 - kappa) * prevValue, shockVar);
	            meanRevertProcess.add(nextValue);
			} else {
			    // a jump does not occur in this time step
			    meanRevertProcess.add(prevValue);
			}
		}
	}

	/**
	 * Returns the global fundamental value at time ts. If undefined, return 0.
	 */
	public Price getValueAt(TimeStamp t) {
		int index = (int) t.getInTicks();
		if (index < 0) { // In case of overflow
			log.log(ERROR, "Tried to access out of bounds TimeStamp: %s (%d)", t, index);
			return new Price(0);
		}
		computeFundamentalTo(index);
		return new Price((int) (double) meanRevertProcess.get(index)).nonnegative();
	}
	
	/**
	 * Returns a TimeSeries copy of the fundamental data. This makes a copy, and
	 * does not return a view into the FundamentalValue. This makes it
	 * expensive, and not super great. This could be fixed if TimeSeries were an
	 * interface, but that would require more thought.
	 */
	public TimeSeries asTimeSeries() {
		TimeSeries copy = TimeSeries.create();
		int time = 0;
		for (double v : meanRevertProcess)
			copy.add(time++, v);
		return copy;
	}
	
	/**
	 * @return mean value of fundamental
	 */
	public int getMeanValue() {
		return this.meanValue;
	}
}
