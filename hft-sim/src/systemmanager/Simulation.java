package systemmanager;

import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import systemmanager.Consts.AgentType;
import utils.Rands;

import activity.AgentArrival;
import activity.Clear;
import activity.LiquidateAtFundamental;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import data.AgentProperties;
import data.EntityProperties;
import data.FundamentalValue;
import data.MarketProperties;
import data.Observations;
import data.Player;
import entity.agent.Agent;
import entity.agent.AgentFactory;
import entity.agent.FundamentalAgent;
import entity.agent.SchedulerAgent;
import entity.agent.ZIMOAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MarketFactory;
import event.TimeStamp;

/**
 * This class represents a single simulation. Standard usage is:
 * <ol>
 * <li>Create Simulation Object</li>
 * <li>executeEvents</li>
 * <li>getObservations</li>
 * </ol>
 * 
 * @author erik
 * 
 */
public class Simulation {

	protected final Scheduler scheduler;
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	protected final Collection<Market> markets;
	protected final Collection<Agent> agents;
	protected final Collection<Player> players;

	protected final SimulationSpec specification;
	protected final TimeStamp simulationLength;
	protected final Observations observations;
	
	/**
	 * Create the simulation with specified random seed and simulation spec
	 * file.
	 */
	public Simulation(SimulationSpec spec, Random rand) {
		this.specification = spec;

		EntityProperties simProps = spec.getSimulationProps();
		Collection<MarketProperties> marketProps = spec.getMarketProps();
		Collection<AgentProperties> agentProps = spec.getAgentProps();
		Map<String, Multiset<AgentProperties>> playerConfig = spec.getPlayerProps();

		this.simulationLength = TimeStamp.create(simProps.getAsLong(Keys.SIMULATION_LENGTH));
		
		this.fundamental = FundamentalValue.create(
				simProps.getAsDouble(Keys.FUNDAMENTAL_KAPPA),
				simProps.getAsInt(Keys.FUNDAMENTAL_MEAN),
				simProps.getAsDouble(Keys.FUNDAMENTAL_SHOCK_VAR),
				simProps.getAsDouble(Keys.FUNDAMENTAL_SHOCK_PROB),
				new Random(rand.nextLong()));
		this.scheduler = new Scheduler(new Random(rand.nextLong()));
		this.sip = new SIP(scheduler, TimeStamp.create(simProps.getAsInt(Keys.NBBO_LATENCY)));
		this.markets = setupMarkets(marketProps, rand);
		this.agents = setupAgents(agentProps, rand);
		this.players = setupPlayers(playerConfig, rand);
		this.observations = new Observations(specification, markets, agents,
				players, fundamental);

        SchedulerAgent mySchedAgent = null;
        for (Agent agent: agents) {
            if (agent instanceof SchedulerAgent) {
                mySchedAgent = (SchedulerAgent) agent;
            }
       }
		for (Market market : markets) {
			scheduler.executeActivity(new Clear(market));
			// Market may use SchedulerAgent to notify other agents
			// if their orders have cleared.
			if (mySchedAgent != null) {
			    market.setSchedulerAgent(mySchedAgent);
			}
		}
		for (Agent agent : agents) {
		    if (agent instanceof FundamentalAgent || agent instanceof ZIMOAgent) {
		        // FundamentalAgent and ZIMOAgent are presently used for the Das replication study,
	            // in which these agents are scheduled to arrive by a SchedulerAgent.
	            // they are not meant to arrive "on their own" initially.
		        // if we want to let them arrive like the other agents, we'll have to add more
		        // run parameters instead of checking their classes.
		        continue;
		    }
		    if (agent instanceof SchedulerAgent) {
		        // SchedulerAgent should be given the set of all agents, so it can call the ones
		        // that it wants.
		        final SchedulerAgent schedAgent = (SchedulerAgent) agent;
		        schedAgent.setAgents(this.agents);
		    }
			scheduler.scheduleActivity(agent.getArrivalTime(), new AgentArrival(agent));
		}
	}
	
	/**
	 * Sets up the markets
	 */
	protected Collection<Market> setupMarkets(Collection<MarketProperties> marketProps, Random rand) {
		Builder<Market> markets = ImmutableList.builder();
		Random ran = new Random(rand.nextLong());
		for (MarketProperties mktProps : marketProps) {
			MarketFactory factory = new MarketFactory(scheduler, sip, ran);
			for (int i = 0; i < mktProps.getAsInt(Keys.NUM); i++)
				markets.add(factory.createMarket(mktProps));
		}
		return markets.build();
	}

	private static double getArrivalTime(
        final Random rand,
        final AgentProperties agentProperties
    ) {
	    final AgentType agentType = agentProperties.getAgentType();
	    final double reentryRate = agentProperties.getAsDouble(Keys.REENTRY_RATE);
	    final double backgroundReentryRate = 
            agentProperties.getAsDouble(Keys.BACKGROUND_REENTRY_RATE);
	    
	    switch (agentType) {
        case AA:
        case ZIP:
            return Rands.nextExponential(rand, reentryRate);
        case FUNDA:
        case MAXEFFICIENCY:
        case ZI:
        case ZIMO:
        case ZIR:
        case ZIRP:
            return Rands.nextExponential(rand, backgroundReentryRate);
        case ADAPTIVEMM:
        case BASICMM:
        case BAYESMM:
        case CONSTMM:
        case FUNDAMENTALMM:
        case LA:
        case MAMM:
        case MARKETDATA:
        case NOOP:
        case SCHEDA:
        case WMAMM:
            return 0.0;
        default:
            throw new IllegalArgumentException();
	    }
	}

	/**
	 * Sets up non player agents
	 */
	protected Collection<Agent> setupAgents(Collection<AgentProperties> agentProps, Random rand) {
		// Not immutable because players also add agents
		Collection<Agent> agents = Lists.newArrayList();
		for (AgentProperties agProps : agentProps) {
			int number = agProps.getAsInt(Keys.NUM);

            AgentFactory factory =
                new AgentFactory(scheduler, fundamental, sip, markets,
			        new Random(rand.nextLong()));

			for (int i = 0; i < number; i++) {
			    TimeStamp arrivalTime =
		            TimeStamp.create((long) getArrivalTime(rand, agProps));
				agents.add(factory.createAgent(agProps, arrivalTime));
			}
		}
		return agents;
	}

	/**
	 * Sets up player agents
	 */
	protected Collection<Player> setupPlayers(Map<String, Multiset<AgentProperties>> playerConfig,
	    Random rand) {
		Builder<Player> players = ImmutableList.builder();

		// Generate Players
		for (Entry<String, Multiset<AgentProperties>> e : playerConfig.entrySet()) {
			String role = e.getKey();
			for (Multiset.Entry<AgentProperties> propCounts : e.getValue().entrySet()) {
				AgentProperties agProp = propCounts.getElement();
				AgentFactory factory = new AgentFactory(scheduler, fundamental, sip, markets,
				       new Random(rand.nextLong()));

				for (int i = 0; i < propCounts.getCount(); i++) {
	                TimeStamp arrivalTime =
                        TimeStamp.create((long) getArrivalTime(rand, agProp));
					Agent agent = factory.createAgent(propCounts.getElement(), arrivalTime);
					agents.add(agent);
					Player player = new Player(role, agProp.getConfigString(), agent);
					players.add(player);
				}
			}
		}
		return players.build();
	}

	/**
	 * Method to execute all events in the Event Queue.
	 */
	public void executeEvents() {
		Observations.BUS.register(observations);
		scheduler.executeUntil(simulationLength.minus(TimeStamp.create(1)));
		for (Agent agent : agents) {
			scheduler.scheduleActivity( simulationLength.minus(TimeStamp.create(1)), 
					new LiquidateAtFundamental(agent));
		}
		scheduler.executeUntil(simulationLength.minus(TimeStamp.create(1)));
		log.log(INFO, "[[[ Simulation Over ]]]");
		Observations.BUS.unregister(observations);
	}
	
	public TimeStamp getCurrentTime() {
		return scheduler.currentTime;
	}
	
	/**
	 * Gets the observations, which are only useful at the end of the simulation.
	 */
	public Observations getObservations() {
		return observations;
	}

}
