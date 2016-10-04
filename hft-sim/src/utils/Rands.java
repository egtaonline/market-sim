package utils;

import java.util.Random;

/**
 * Class with extra random methods
 * 
 * @author erik
 * 
 */
public abstract class Rands {

	public static synchronized double nextGaussian(Random rand, double mean, double variance) {
		return mean + rand.nextGaussian() * Math.sqrt(variance);
	}
	
	public static double nextExponential(Random rand, double rate) {
		if (rate == 0) return Double.POSITIVE_INFINITY;
		return -Math.log(rand.nextDouble()) / rate;
	}

	public static double nextUniform(Random rand, double a, double b) {
		return rand.nextDouble()*(b-a) + a;
	}
	
}
