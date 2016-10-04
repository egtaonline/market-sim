package data;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

/**
 * Class with extra methods on DescriptiveStatistics Objects.
 * 
 * @author erik
 * 
 */
/*
 * XXX Erik: I'm not a huge fan of this class or the apache common math package,
 * and would be in favor of using something else
 */
public abstract class DSPlus {

	protected static final Median median = new Median();
	
	public static DescriptiveStatistics from() {
		return new DescriptiveStatistics();
	}
	
	public static DescriptiveStatistics from(Iterator<Double> initialValues) {
		return from(ImmutableList.copyOf(initialValues));
	}
	
	public static DescriptiveStatistics from(Iterable<Double> initialValues) {
		return from(ImmutableList.copyOf(initialValues));
	}
	
	public static DescriptiveStatistics from(Collection<Double> initialValues) {
		return new DescriptiveStatistics(Doubles.toArray(initialValues));
	}
	
	public static DescriptiveStatistics fromLogRatioOf(Iterable<Double> initialValues) {
		if (Iterables.isEmpty(initialValues))
			return from(Collections.<Double> emptyList());
		
		List<Double> logRatio = Lists.newArrayList();
		double last = Iterables.getFirst(initialValues, Double.NaN);
		for (double next : Iterables.skip(initialValues, 1)) {
			double ratio = Math.log(next) - Math.log(last);
			if (!Double.isNaN(ratio)) logRatio.add(ratio);
			last = next;
		}
		return from(logRatio);
	}

	public static double rmsd(DescriptiveStatistics first, DescriptiveStatistics other) {
		double rmsd = 0;
		double[] x1 = first.getValues();
		double[] x2 = other.getValues();
		int len = Math.min(x1.length, x2.length);
		int n = 0; // count number of non-NaN values

		// iterate through number of elements in shorter array
		for (int i = 0; i < len; i++) {
			if (!Double.isNaN(x1[i]) && !Double.isNaN(x2[i])) {
				rmsd += (x1[i] - x2[i]) * (x1[i] - x2[i]);
				++n;
			}
		}
		return Math.sqrt(rmsd / n);
	}

	/**
	 * Gets median without NaNs, because NaN's don't make sense for a median calculation.
	 * Disregarding them is equivalent to alternatively making them positive and negative infinity.
	 */
	public static double median(DescriptiveStatistics ds) {
		Iterable<Double> filtered = Iterables.filter(Doubles.asList(ds.getValues()), not(equalTo(Double.NaN)));
		return median.evaluate(Doubles.toArray(ImmutableList.copyOf(filtered)));
	}

}
