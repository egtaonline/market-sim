package entity.agent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import systemmanager.Scheduler;

import com.google.common.collect.Iterators;

import data.AgentProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class AgentFactory {

	protected final Scheduler scheduler;
	protected final Random rand;
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	protected final Collection<Market> markets;
	protected final Iterator<Market> marketAssignment;

	public AgentFactory(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip,
			Collection<Market> markets, Iterator<Market> marketProcess,
			Random rand) {
		this.scheduler = scheduler;
		this.rand = rand;
		this.fundamental = fundamental;
		this.sip = sip;
		this.markets = markets;
		this.marketAssignment = marketProcess;
	}

	/**
	 * SMAgent factory with Poisson arrivals and round robin market selection.
	 * 
	 * @param fundamental
	 * @param sip
	 * @param markets
	 * @param arrivalRate
	 * @param rand
	 */
	public AgentFactory(Scheduler scheduler, FundamentalValue fundamental,
			SIP sip, Collection<Market> markets, Random rand) {
		
		this(scheduler, fundamental, sip, markets,
				Iterators.cycle(markets), rand);
	}

	// All Agents should advance rand, marketAssignment, and arrivalProcess
	public Agent createAgent(AgentProperties props, TimeStamp arrivalTime) {
		switch (props.getAgentType()) {
		case AA:
			return new AAAgent(scheduler, arrivalTime, fundamental,
					sip, marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case ZIP:
			return new ZIPAgent(scheduler, arrivalTime, fundamental,
					sip, marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case ZIRP:
			return new ZIRPAgent(scheduler, arrivalTime, fundamental,
				sip, marketAssignment.next(), new Random(rand.nextLong()),
				props);
	     case ZIMO:
            return new ZIMOAgent(scheduler, arrivalTime, fundamental,
                sip, marketAssignment.next(), new Random(rand.nextLong()),
                props); 
	     case FUNDA:
	         return new FundamentalAgent(scheduler, arrivalTime, fundamental,
                 sip, marketAssignment.next(), new Random(rand.nextLong()),
                 props); 
	     case SCHEDA:
	         return new SchedulerAgent(scheduler, fundamental, sip, marketAssignment.next(), 
                 new Random(rand.nextLong()), props);
		case ZIR:
			return new ZIRAgent(scheduler, arrivalTime, fundamental,
					sip, marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case ZI:
			return new ZIAgent(scheduler, arrivalTime, fundamental,
					sip, marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case MAXEFFICIENCY:
			return new MaxEfficiencyAgent(scheduler, fundamental, sip, 
					marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case MARKETDATA:
			return new MarketDataAgent(scheduler, fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()), props);
		case BASICMM:
			return new BasicMarketMaker(scheduler, fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()), props);
		case MAMM:
			return new MAMarketMaker(scheduler, fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case WMAMM:
			return new WMAMarketMaker(scheduler, fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case BAYESMM:
            return new BayesMarketMaker(scheduler, fundamental, sip,
                    marketAssignment.next(), new Random(rand.nextLong()),
                    props);		    
		case ADAPTIVEMM:
			return new AdaptiveMarketMaker(scheduler, fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()),
					props);
		case FUNDAMENTALMM:
			return new FundamentalMarketMaker(scheduler, fundamental, sip,
					marketAssignment.next(), new Random(rand.nextLong()),
					props);  
		case LA:
			marketAssignment.next();
			return new LAAgent(scheduler, fundamental, sip, markets, new Random(
					rand.nextLong()), props);
		case NOOP:
			marketAssignment.next();
			return new NoOpAgent(scheduler, fundamental, sip, new Random(rand.nextLong()),
					props);

		default:
			throw new IllegalArgumentException("Can't create AgentType: "
					+ props.getAgentType());
		}
	}

}
