package iterators;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;

import utils.Rands;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

import event.TimeStamp;

public final class ExpInterarrivals extends AbstractIterator<TimeStamp> implements Serializable {
	
	private static final long serialVersionUID = 3285386017387161748L;
	
	protected final Random rand;
	protected final double rate;

	private ExpInterarrivals(double rate, Random rand) {
		checkArgument(rate > 0);
		this.rand = rand;
		this.rate = rate;
	}
	
	public static Iterator<TimeStamp> create(double rate, Random rand) {
		if (rate == 0)
			return Iterators.emptyIterator();
		return new ExpInterarrivals(rate, rand);
	}

	@Override
	protected TimeStamp computeNext() {
		return TimeStamp.create(
				(long) Math.ceil(Rands.nextExponential(rand, rate)));
	}

}
