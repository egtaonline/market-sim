package entity.agent;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import event.TimeStamp;

public class NoOpAgent extends Agent {
	
	private static final long serialVersionUID = -7232513254416667984L;

	public NoOpAgent(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Random rand, int tickSize) {
		super(scheduler, TimeStamp.ZERO, fundamental, sip, rand, tickSize);
	}

	public NoOpAgent(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Random rand, EntityProperties props) {
		this(scheduler, fundamental, sip, rand,
				props.getAsInt(Keys.AGENT_TICK_SIZE, Keys.TICK_SIZE));
	}

	@Override
	public void agentArrival() {
		
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		
	}
	
}
