package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Random;

import systemmanager.Scheduler;
import activity.AgentStrategy;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public abstract class ReentryAgent extends SMAgent {

	private static final long serialVersionUID = 4722377972197300345L;

	protected Iterator<TimeStamp> reentry; // wait times between reentry

	public ReentryAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			Iterator<TimeStamp> reentry, int tickSize) {
		super(scheduler, arrivalTime, fundamental, sip, market, rand, tickSize);

		this.reentry = checkNotNull(reentry);
	}
	
	@Override
	public void agentStrategy(TimeStamp currentTime) {
		if (!reentry.hasNext()) return;
		TimeStamp waitTime = reentry.next();
		scheduler.scheduleActivity(currentTime.plus(waitTime), new AgentStrategy(this));
        this.strategyArrivals++;
	}
}
