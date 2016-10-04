package utils;

import static java.math.RoundingMode.HALF_EVEN;

import com.google.common.math.DoubleMath;

public class MathUtils {

	/**
	 * Quantize "n" in increments of "quanta". If n is halfway between quanta it
	 * will be rounded towards positive infinity. e.g. quantize(5, 10) = 10 but
	 * quantize(-5, 10) = 0
	 */
	public static int quantize(int n, int quanta) {
		return quanta * DoubleMath.roundToInt(n / (double) quanta, HALF_EVEN);
	}

	public static double quantize(double n, double quanta) {
		// Floor instead of round to prevent the case in round which converts
		// NaN and Inf to 0
		return quanta * Math.floor(n / quanta + .5d);
	}
	
	public static int bound(int num, int lower, int upper) {
		return Math.max(Math.min(num, upper), lower);
	}
	
	public static double bound(double num, double lower, double upper) {
		return Math.max(Math.min(num, upper), lower);
	}
	
	/**
	 * @param number
	 * @param base
	 * @return floor of log(number) in base base or -1 for any non-positive number
	 */
	public static int logn(int number, int base) {
		int log = -1;
		while (number > 0) {
			number /= base;
			log++;
		}
		return log;
	}

}
