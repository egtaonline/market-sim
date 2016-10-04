package data;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;

/**
 * Storage for time series objects.
 * 
 * Keeps track of the data points that are added to the time series and the time
 * they're added. Also expands it into a full list of values (with an element
 * for each time).
 * 
 * When filling in values, a new element is added to series if the time for the
 * new data point is different from the most recent time recorded.
 * 
 * TODO Would like to have the methods just return views instead of copying the
 * data :( This shouldn't be too hard. Methods should just return Iterables
 * instead of Lists
 * 
 * @author ewah
 * 
 */
public class TimeSeries implements Serializable {
	
	private static final long serialVersionUID = 7835744389750549565L;
	private static final Joiner joiner = Joiner.on(", ");
	
	protected final List<Point> points;
	protected final double defaultValue;

	protected TimeSeries(double defaultValue) {
		this.points = Lists.newArrayList();
		this.defaultValue = defaultValue;
	}
	
	protected TimeSeries() {
		this(Double.NaN);
	}
	
	public static TimeSeries create(double defaultValue) {
		return new TimeSeries(defaultValue);
	}

	public static TimeSeries create() {
		return new TimeSeries();
	}

	@Override
	public String toString() {
		return "[" + joiner.join(points) + "]";
	}

	/**
	 * Add a data point (int, double) to container
	 */
	public void add(long time, double value) {
		long lastTime = Iterables.getLast(points, new Point(0, Double.NaN)).time;
		checkArgument(time >= lastTime, "Can't add time before last time");
		
		points.add(new Point(time, value));
	}
	
	/**
	 * Sample values according to specified period and maximum time. Returns value at the END of
	 * each period. Example: For sampling interval of 100, the first item in the sampled array would
	 * be the 100th element (index 99).
	 * 
	 * Will also fill in values up to (not including) maxTime, if the last time stored is before
	 * maxTime.
	 * 
	 * If period == 1, then will include every time stamp.
	 */
	public List<Double> sample(long period, long maxTime) {
		return sample(period, maxTime, Predicates.<Long> alwaysTrue(), Predicates.<Double> alwaysTrue());
	}
	
	public List<Double> sample(long period, long maxTime, Predicate<Double> valuePred) {
		return sample(period, maxTime, Predicates.<Long> alwaysTrue(), valuePred);
	}
	
	public List<Double> sample(long period, long maxTime, Predicate<Long> timePred, Predicate<Double> valuePred) {
		return sample(period, maxTime, Iterables.filter(points, pointPredicate(timePred, valuePred)));
	}
	
	protected List<Double> sample(long period, long maxTime, Iterable<Point> toSample) {
		return sample(period, maxTime, toSample.iterator());
	}
	
	protected List<Double> sample(long period, long maxTime, Iterator<Point> toSample) {
		checkArgument(period > 0, "Period must be positive");
		
		List<Double> sampled = Lists.newArrayList();
		PeekingIterator<Point> it = Iterators.peekingIterator(toSample);
		Point current = new Point(0, defaultValue);
		
		for (long time = period - 1; time < maxTime; time += period) {
			while (it.hasNext() && it.peek().time <= time)
				current = it.next();
			sampled.add(current.value);
		}
		return sampled;
	}
	
	protected static Predicate<Point> pointPredicate(final Predicate<Long> timePred, final Predicate<Double> valuePred) {
		return new Predicate<Point>() {
			@Override
			public boolean apply(Point arg0) {
				return timePred.apply(arg0.time) && valuePred.apply(arg0.value);
			}
		};
	}
	
	protected static final class Point implements Entry<Long, Double>, Serializable {
		private static final long serialVersionUID = 8793011684140207592L;
		protected final long time;
		protected final double value;
		
		protected Point(long time, double value) {
			this.time = time;
			this.value = value;
		}
		
		@Override
		public String toString() {
			return "(" + time + ": " + value + ")";
		}

		@Override
		public Long getKey() {
			return time;
		}

		@Override
		public Double getValue() {
			return value;
		}

		@Override
		public Double setValue(Double value) {
			throw new UnsupportedOperationException();
		}
	}

}
