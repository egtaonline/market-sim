package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


import activity.AgentArrival;

import systemmanager.Consts;
import systemmanager.Executor;
import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import event.TimeStamp;

public class SchedulerAgentTest {

    private Executor exec;
    private FundamentalValue fundamental = new MockFundamental(100000);
    private Market market;
    private SIP sip;
    private static Random rand;
    private static final int simLength = 100;
    private static EntityProperties agentProperties = 
        EntityProperties.fromPairs(
            Keys.REENTRY_RATE, 0,
            Keys.MAX_POSITION, 2,
            Keys.PRIVATE_VALUE_VAR, 0,
            Keys.BID_RANGE_MIN, 0,
            Keys.BID_RANGE_MAX, 5000,
            Keys.SIMULATION_LENGTH, simLength
        );
    
    private static final EntityProperties marketMakerProps = EntityProperties.fromPairs(
        Keys.REENTRY_RATE, 0,
        Keys.TICK_SIZE, 1,
        Keys.TRUNCATE_LADDER, true,
        Keys.TICK_IMPROVEMENT, true,
        Keys.TICK_OUTSIDE, true,
        Keys.INITIAL_LADDER_MEAN, 0,
        Keys.INITIAL_LADDER_RANGE, 0,
        Keys.SIMULATION_LENGTH, simLength,
        Keys.FUNDAMENTAL_KAPPA, 0.05,
        Keys.FUNDAMENTAL_MEAN, 100000,
        Keys.SPREAD, -1,
        Keys.FUNDAMENTAL_ESTIMATE, -1
    );
    
    private static final EntityProperties bayesMMProps = EntityProperties.fromPairs(
        Keys.REENTRY_RATE, 0,
        Keys.TICK_SIZE, 1,
        Keys.TRUNCATE_LADDER, true,
        Keys.TICK_IMPROVEMENT, true,
        Keys.TICK_OUTSIDE, true,
        Keys.INITIAL_LADDER_MEAN, 0,
        Keys.INITIAL_LADDER_RANGE, 0,
        Keys.SIMULATION_LENGTH, simLength,
        Keys.FUNDAMENTAL_KAPPA, 0.05,
        Keys.FUNDAMENTAL_MEAN, 100000,
        Keys.SPREAD, -1,
        Keys.FUNDAMENTAL_ESTIMATE, -1,
        Keys.PROB_FUND_AGENT, 0.7,
        Keys.NOISE_STDEV, 5,
        Keys.FUNDAMENTAL_SHOCK_VAR, 1000000,
        Keys.BMM_SHADE_TICKS, 5,
        Keys.BMM_INVENTORY_FACTOR, 0.5
    );
    
    private class SchedulerAgentTestVersion extends SchedulerAgent {

        private static final long serialVersionUID = 2359140206288729284L;
        private int arrivalsCount;
        
        public SchedulerAgentTestVersion(Scheduler scheduler,
                FundamentalValue fundamental, SIP sip, Market market,
                Random rand, EntityProperties props) {
            super(scheduler, fundamental, sip, market, rand, props);
            this.arrivalsCount = 0;
        }
        
        @Override
        public void agentStrategy(final TimeStamp currentTime) {
            super.agentStrategy(currentTime);

            this.arrivalsCount++;
        }
        
        public int getArrivalsCount() {
            return this.arrivalsCount;
        }
    }
    
    private class FundamentalMarketMakerTestVersion extends FundamentalMarketMaker {

        private static final long serialVersionUID = 6794478607692665103L;
        private int arrivalsCount;

        public FundamentalMarketMakerTestVersion(Scheduler scheduler,
                FundamentalValue fundamental, SIP sip, Market market,
                Random rand, EntityProperties props) {
            super(scheduler, fundamental, sip, market, rand, props);
            this.arrivalsCount = 0;
        }
        
        @Override
        public void agentStrategy(final TimeStamp currentTime) {
            super.agentStrategy(currentTime);

            this.arrivalsCount++;
        }
        
        public int getArrivalsCount() {
            return this.arrivalsCount;
        }
    }
    
    private class BayesMarketMakerTestVersion extends BayesMarketMaker {

        private static final long serialVersionUID = 3259980519830061932L;
        private int arrivalsCount;

        public BayesMarketMakerTestVersion(Scheduler scheduler,
                FundamentalValue fundamental, SIP sip, Market market,
                Random rand, EntityProperties props) {
            super(scheduler, fundamental, sip, market, rand, props);
            this.arrivalsCount = 0;
        }
        
        @Override
        public void agentStrategy(final TimeStamp currentTime) {
            super.agentStrategy(currentTime);

            this.arrivalsCount++;
        }
        
        public int getArrivalsCount() {
            return this.arrivalsCount;
        }
    }
    
    private class ZIMOAgentTestVersion extends ZIMOAgent {

        private static final long serialVersionUID = 3024349394745547113L;
        private int arrivalsCount;

        public ZIMOAgentTestVersion(Scheduler scheduler, TimeStamp arrivalTime,
                FundamentalValue fundamental, SIP sip, Market market,
                Random rand, EntityProperties props) {
            super(scheduler, arrivalTime, fundamental, sip, market, rand, props);
        }
        
        @Override
        public void agentStrategy(final TimeStamp currentTime) {
            super.agentStrategy(currentTime);

            this.arrivalsCount++;
        }
        
        public int getArrivalsCount() {
            return this.arrivalsCount;
        }
    }
    
    private class FundamentalAgentTestVersion extends FundamentalAgent {

        private static final long serialVersionUID = -1373412023846844022L;
        private int arrivalsCount;
        public FundamentalAgentTestVersion(Scheduler scheduler,
                TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
                Market market, Random rand, EntityProperties props) {
            super(scheduler, arrivalTime, fundamental, sip, market, rand, props);
            this.arrivalsCount = 0;
        }
        
        @Override
        public void agentStrategy(final TimeStamp currentTime) {
            super.agentStrategy(currentTime);

            this.arrivalsCount++;
        }
        
        public int getArrivalsCount() {
            return this.arrivalsCount;
        }
    }
    
    public SchedulerAgentTestVersion createAgent(EntityProperties props) {
        final SchedulerAgentTestVersion result = 
            new SchedulerAgentTestVersion(
                exec, 
                fundamental, 
                sip, 
                market,
                new Random(rand.nextLong()), 
                props
            );
        
        setupAgent(result);
        return result;
    }
    
    public SchedulerAgentTestVersion createAgent(
        final EntityProperties props,
        final ZIMOAgent zimoA,
        final FundamentalAgent fundA,
        final MarketMaker marketMaker
    ) {
        final SchedulerAgentTestVersion result = 
            new SchedulerAgentTestVersion(
                exec, 
                fundamental, 
                sip, 
                market,
                new Random(rand.nextLong()), 
                props
            );
        
        setupAgent(
            result,
            zimoA,
            fundA,
            marketMaker
        );
        return result;
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException{
        // Setting up the log file
        log = Log.create(DEBUG, new File(
            Consts.TEST_OUTPUT_DIR + "SchedulerAgentTest.log")
        );

        // Creating the setup properties
        rand = new Random();        
    }
    
    @Before
    public void setup(){
        exec = new Executor();
        sip = new SIP(exec, TimeStamp.IMMEDIATE);
        // Creating the MockMarket
        market = new MockMarket(exec, sip);
        

    }
    
    // check that the simulation runs for the proper length
    // and nothing crashes with our SchedulerAgent
    @Test
    public void basicRunTest() {
        final SchedulerAgent schedA = createAgent(agentProperties);
        final TimeStamp finalTime = TimeStamp.create(simLength);
        exec.scheduleActivity(schedA.getArrivalTime(), new AgentArrival(schedA));
        exec.executeUntil(finalTime);
        assertEquals(exec.getCurrentTime().getInTicks(), simLength);
    }
    
    // scheduler agent should arrive 1 more time than the final time stamp's time,
    // because it arrives first at time 0, and once in each later time step.
    //
    // market maker should also arrive 1 more time than final time stamp time.
    // 
    // the two background traders' arrival counts, added together, should equal
    // the final step number plus 1, because one or the other arrives each step.
    //
    // each type of background trader should arrive at least once, almost surely.
    @Test
    public void arrivalCountTest() {
        final ZIMOAgentTestVersion zimoA = getZimo();
        final FundamentalAgentTestVersion fundA = getFundAgent();
        final FundamentalMarketMakerTestVersion marketMaker = 
            getFundMarketMaker();
        final SchedulerAgentTestVersion schedA = 
            createAgent(
                agentProperties,
                zimoA,
                fundA,
                marketMaker
            );
        final TimeStamp finalTime = TimeStamp.create(simLength);
        exec.scheduleActivity(schedA.getArrivalTime(), new AgentArrival(schedA));
        exec.executeUntil(finalTime);
        assertEquals(schedA.getArrivalsCount(), simLength + 1);
        assertEquals(marketMaker.getArrivalsCount(), simLength + 1);
        assertEquals(zimoA.getArrivalsCount() + fundA.getArrivalsCount(), simLength + 1);
        assertFalse(zimoA.getArrivalsCount() == 0);
    }
    
    @Test
    public void bayesMMArrivalCountTest() {
        final ZIMOAgentTestVersion zimoA = getZimo();
        final FundamentalAgentTestVersion fundA = getFundAgent();
        final BayesMarketMakerTestVersion bayesMM = 
            getBayesMarketMaker();
        final SchedulerAgentTestVersion schedA = 
            createAgent(
                agentProperties,
                zimoA,
                fundA,
                bayesMM
            );
        final TimeStamp finalTime = TimeStamp.create(simLength);
        exec.scheduleActivity(schedA.getArrivalTime(), new AgentArrival(schedA));
        exec.executeUntil(finalTime);
        assertEquals(schedA.getArrivalsCount(), simLength + 1);
        assertEquals(bayesMM.getArrivalsCount(), simLength + 1);
        assertEquals(zimoA.getArrivalsCount() + fundA.getArrivalsCount(), simLength + 1);
        assertFalse(zimoA.getArrivalsCount() == 0);
    }
    
    private void setupAgent(final SchedulerAgent schedAgent) {
        final ZIMOAgent zimoA = getZimo();
        final FundamentalAgent fundA = getFundAgent();
        final MarketMaker marketMaker = getFundMarketMaker();
        
        final List<Agent> agents = new ArrayList<Agent>();
        agents.add(zimoA);
        agents.add(fundA);
        agents.add(marketMaker);
        agents.add(schedAgent);
        
        schedAgent.setAgents(agents);
    }
    
    private static void setupAgent(
        final SchedulerAgent schedAgent,
        final ZIMOAgent zimoA,
        final FundamentalAgent fundA,
        final MarketMaker marketMaker
    ) {        
        final List<Agent> agents = new ArrayList<Agent>();
        agents.add(zimoA);
        agents.add(fundA);
        agents.add(marketMaker);
        agents.add(schedAgent);
        
        schedAgent.setAgents(agents);
    }
    
    private ZIMOAgentTestVersion getZimo() {
        return new ZIMOAgentTestVersion(
            exec, 
            TimeStamp.ZERO, 
            fundamental, 
            sip, 
            market,
            new Random(rand.nextLong()), 
            agentProperties
        );
    }
    
    private FundamentalAgentTestVersion getFundAgent() {
        return new FundamentalAgentTestVersion(
            exec, 
            TimeStamp.ZERO, 
            fundamental, 
            sip, 
            market,
            new Random(rand.nextLong()), 
            agentProperties
        );
    }
    
    private FundamentalMarketMakerTestVersion getFundMarketMaker() {
        return new FundamentalMarketMakerTestVersion(
            exec, fundamental, sip, market,
            new Random(rand.nextLong()), 
            marketMakerProps
        );
    }
    
    private BayesMarketMakerTestVersion getBayesMarketMaker() {
        return new BayesMarketMakerTestVersion(
            exec, fundamental, sip, market,
            new Random(rand.nextLong()), 
            bayesMMProps
        );
    }
}
