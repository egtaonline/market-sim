package entity.agent;

import java.util.Collection;
import java.util.Random;

import systemmanager.Scheduler;

import com.google.common.base.Joiner;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

/**
 * MMAGENT
 * 
 * Multi-market agent. An MMAgent arrives in all markets in a model, and its
 * strategy is executed across multiple markets.
 * 
 * An MMAgent is capable of seeing the quotes in multiple markets with zero
 * delay. These agents also bypass Regulation NMS restrictions as they have
 * access to private data feeds, enabling them to compute their own version of
 * the NBBO.
 * 
 * @author ewah
 */
public abstract class MMAgent extends Agent {

	private static final long serialVersionUID = 2297636044775909734L;
	protected final static Joiner marketJoiner = Joiner.on(',');
	
	protected final Collection<Market> markets; 
	
	// TODO Just keep the SMIPs and not the actual markets...
	public MMAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Collection<Market> markets,
			Random rand, int tickSize) {
		super(scheduler, arrivalTime, fundamental, sip, rand, tickSize);
		this.markets = markets;
	}

}
