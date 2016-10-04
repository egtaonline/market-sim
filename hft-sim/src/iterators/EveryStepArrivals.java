package iterators;

import com.google.common.collect.AbstractIterator;

import event.TimeStamp;

/**
 * The arrival lag iterator for an agent that arrives in every time step.
 * 
 * Used for the Das replication study, by the SchedulerAgent.
 * 
 * Just returns 1 every the time.
 * 
 * @author masonwright
 *
 */
public final class EveryStepArrivals extends AbstractIterator<TimeStamp> {

    @Override
    protected TimeStamp computeNext() {
        return TimeStamp.create(1);
    }
}
