package data;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logger.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import systemmanager.Keys;
import systemmanager.SimulationSpec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import entity.agent.Agent;
import entity.agent.BackgroundAgent;
import entity.agent.BasicMarketMaker;
import entity.agent.DummyPrivateValue;
import entity.agent.FundamentalMarketMaker;
import entity.agent.MarketMaker;
import entity.agent.MockAgent;
import entity.agent.MockBackgroundAgent;
import entity.agent.MockMarketMaker;
import entity.agent.PrivateValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;

public class ObservationsTest {
	
	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market1, market2;
	private SIP sip;
	private Observations obs;

	@BeforeClass
	public static void setupClass() throws IOException {
		Log.log = Log.create(Log.Level.DEBUG, new File(Consts.TEST_OUTPUT_DIR + "ObservationsTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market1 = new MockMarket(exec, sip);
		market2 = new MockMarket(exec, sip);
	}
	
	@After
	public void tearDown() {
		if (obs != null)
			Observations.BUS.unregister(obs);
	}

	// TODO test ZIRP statistic logging
	
	/*
	 * TODO Still have some hard things to test, that should for the most part
	 * be correct. Most of these are basically fully tested elsewhere, but just
	 * need to verify that they get written correctly. These tests seem tedious
	 * and hard to write, so I'm not writing them for now.
	 */
	
	@Test
	public void spreadsTest() {
		Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		setupObservations(agent1, agent2);
		
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent1, BUY, new Price(104), 1, TimeStamp.ZERO);
		assertEquals(Double.POSITIVE_INFINITY, obs.spreads.get(market1).sample(1,1).get(0), 0.001);
		
		market1.submitOrder(agent2, SELL, new Price(105), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(106), 1, TimeStamp.ZERO);
		assertEquals(1, obs.spreads.get(market1).sample(1,1).get(0), 0.001);
		
		// Also sets median for market1 to 3
		market1.submitOrder(agent2, SELL, new Price(103), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(3, obs.spreads.get(market1).sample(1,1).get(0), 0.001);
		
		// Sets median of market2 to 5
		market2.submitOrder(agent2, SELL, new Price(103), 1, TimeStamp.ZERO);
		market2.submitOrder(agent2, BUY, new Price(98), 1, TimeStamp.ZERO);
		market2.clear(TimeStamp.ZERO);
		assertEquals(5, obs.spreads.get(market2).sample(1,1).get(0), 0.001);
		
		// Mean of 3 and 5
		assertEquals(4, obs.getFeatures().get("spreads_mean_markets"), 0.001);
	}
	
	@Test
	public void asksTest() {
	    Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
        Agent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
        setupObservations(agent1, agent2);
        
        market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
        market1.submitOrder(agent1, BUY, new Price(104), 1, TimeStamp.ZERO);
        assertEquals(Double.NaN, obs.asks.get(market1).sample(1,1).get(0), 0.001);
	        
        market1.submitOrder(agent2, SELL, new Price(106), 1, TimeStamp.ZERO);
        market1.submitOrder(agent2, SELL, new Price(107), 1, TimeStamp.ZERO);
        assertEquals(106, obs.asks.get(market1).sample(1,1).get(0), 0.001);
	        
        market1.submitOrder(agent2, SELL, new Price(103), 1, TimeStamp.ZERO);
        market1.clear(TimeStamp.ZERO);
        assertEquals(106, obs.asks.get(market1).sample(1,1).get(0), 0.001);
	}
	
    @Test
    public void bidsTest() {
        Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
        Agent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
        setupObservations(agent1, agent2);
        
        market1.submitOrder(agent2, SELL, new Price(106), 1, TimeStamp.ZERO);
        market1.submitOrder(agent2, SELL, new Price(107), 1, TimeStamp.ZERO);
        assertEquals(Double.NaN, obs.bids.get(market1).sample(1,1).get(0), 0.001);
        
        market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
        market1.submitOrder(agent1, BUY, new Price(104), 1, TimeStamp.ZERO);
        assertEquals(104, obs.bids.get(market1).sample(1,1).get(0), 0.001);
            
        market1.submitOrder(agent2, SELL, new Price(103), 1, TimeStamp.ZERO);
        market1.clear(TimeStamp.ZERO);
        assertEquals(102, obs.bids.get(market1).sample(1,1).get(0), 0.001);
    }
	
	@Test
	public void midquotesTest() {
		Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		setupObservations(agent1, agent2);
		
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent1, BUY, new Price(104), 1, TimeStamp.ZERO);
		assertEquals(Double.NaN, obs.midQuotes.get(market1).sample(1,1).get(0), 0.001);
		
		market1.submitOrder(agent2, SELL, new Price(106), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(107), 1, TimeStamp.ZERO);
		assertEquals(105, obs.midQuotes.get(market1).sample(1,1).get(0), 0.001);
		
		market1.submitOrder(agent2, SELL, new Price(103), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(104, obs.midQuotes.get(market1).sample(1,1).get(0), 0.001);
	}
	
	@Test
	public void nbboSpreadsTest() {
		setupObservations();
		Quote q = new Quote(market1, new Price(80), 1, new Price(100), 1, TimeStamp.ZERO);
		sip.processQuote(market1, q, TimeStamp.ZERO);
		
		// Check that correct spread stored
		List<Double> list = obs.nbboSpreads.sample(1, 1);
		assertEquals(1, list.size());
		assertEquals(20, list.get(0), 0.001);
		
		q = new Quote(market2, new Price(70), 1, new Price(90), 1, TimeStamp.ZERO);
		sip.processQuote(market2, q, TimeStamp.ZERO);
		
		// Check that new quote overwrites the previously stored spread at time 0
		list = obs.nbboSpreads.sample(1, 1);
		assertEquals(1, list.size());
		assertEquals(10, list.get(0), 0.001);
		
		// Test with actual agents
		Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		market1.submitOrder(agent1, BUY, new Price(80), 1, TimeStamp.ZERO);
		market2.submitOrder(agent2, SELL, new Price(100), 1, TimeStamp.ZERO);
		list = obs.nbboSpreads.sample(1, 1);
		assertEquals(1, list.size());
		assertEquals(20, list.get(0), 0.001);
	}
	
    @Test
	public void numTransTest() {
		Map<String, Double> features;
		TimeStamp time = TimeStamp.ZERO;

		Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		Agent agent3 = new MockAgent(exec, fundamental, sip);
		
		// Two orders from one agent
		setupObservations(agent1);
		market1.submitOrder(agent1, BUY, new Price(102), 1, time);
		market1.submitOrder(agent1, SELL, new Price(102), 1, time);
		market1.clear(time);
		// One for each order type
		features = obs.getFeatures();
		assertEquals(2, obs.numTrans.count(MockBackgroundAgent.class));
		assertEquals(0, obs.numTrans.count(MockAgent.class));
		assertEquals(2, features.get("trans_mockbackgroundagent_num"), 0.001);

		// Two orders from same agent type
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, time);
		market1.submitOrder(agent2, SELL, new Price(102), 1, time);
		market1.clear(time);
		// One for each agent
		features = obs.getFeatures();
		assertEquals(2, obs.numTrans.count(MockBackgroundAgent.class));
		assertEquals(0, obs.numTrans.count(MockAgent.class));
		assertEquals(2, features.get("trans_mockbackgroundagent_num"), 0.001);
		
		// Two orders from different agent types
		setupObservations(agent1, agent3);
		market1.submitOrder(agent1, BUY, new Price(102), 1, time);
		market1.submitOrder(agent3, SELL, new Price(102), 1, time);
		market1.clear(time);
		// One for each agent
		features = obs.getFeatures();
		assertEquals(1, obs.numTrans.count(MockBackgroundAgent.class));
		assertEquals(1, obs.numTrans.count(MockAgent.class));
		assertEquals(1, features.get("trans_mockbackgroundagent_num"), 0.001);
		assertEquals(1, features.get("trans_mockagent_num"), 0.001);
		
		// One order is split among two, so transactions is "doubled"
		setupObservations(agent1, agent2, agent3);
		market1.submitOrder(agent1, BUY, new Price(102), 2, time);
		market1.submitOrder(agent2, SELL, new Price(102), 1, time);
		market1.submitOrder(agent3, SELL, new Price(102), 1, time);
		market1.clear(time);
		// Two for agent 1's split order, 1 for each of the other agents
		features = obs.getFeatures();
		assertEquals(3, obs.numTrans.count(MockBackgroundAgent.class));new MockBackgroundAgent(exec, fundamental, sip, market1);
		assertEquals(1, obs.numTrans.count(MockAgent.class));
		assertEquals(3, features.get("trans_mockbackgroundagent_num"), 0.001);
		assertEquals(1, features.get("trans_mockagent_num"), 0.001);
	}
	
	@Test
	public void executionTimesTest() {
		Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		// Same times
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(0, obs.executionTimes.mean(), 0.001);
		assertEquals(2, obs.executionTimes.getN());
		
		// Same times
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.create(1));
		assertEquals(1, obs.executionTimes.mean(), 0.001);
		assertEquals(2, obs.executionTimes.getN()); 	// because obs setup again

		// Quantity weighted
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.create(1));
		market1.submitOrder(agent1, BUY, new Price(102), 3, TimeStamp.create(2));
		market1.submitOrder(agent2, SELL, new Price(102), 3, TimeStamp.create(2));
		market1.clear(TimeStamp.create(4));
		assertEquals(1.75, obs.executionTimes.mean(), 0.001);
		
		// Split order
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 2, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.create(1));
		market1.clear(TimeStamp.create(1));
		assertEquals(0.75, obs.executionTimes.mean(), 0.001);

		// Same agent
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.create(0));
		market1.submitOrder(agent1, SELL, new Price(102), 1, TimeStamp.create(1));
		market1.clear(TimeStamp.create(2));
		assertEquals(1.5, obs.executionTimes.mean(), 0.001);
	}
	
	/**
	 * Test that only background trader execution times are measured.
	 */
	@Test
	public void backgroundExecutionTimesTest() {
		Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market1, 2, 10);
		
		// Same times
		setupObservations(agent1, mm);
		market1.submitOrder(agent1, BUY, new Price(100), 1, TimeStamp.ZERO);
		mm.submitOrderLadder(new Price(75), new Price(85), new Price(95), new Price(105));
		market1.clear(TimeStamp.ZERO);
		assertEquals(1, market1.getTransactions().size());
		assertEquals(0, obs.executionTimes.mean(), 0.001);
		assertEquals(1, obs.executionTimes.getN());
		
		// Another transaction (still 0 because executes instantly)
		market1.submitOrder(agent1, BUY, new Price(110), 1, TimeStamp.create(3));
		market1.clear(TimeStamp.create(3));
		assertEquals(2, market1.getTransactions().size());
		assertEquals(0, obs.executionTimes.mean(), 0.001);
		assertEquals(2, obs.executionTimes.getN());	// adds on to previous
		
		// MM ladder submitted later
		EntityProperties agentProperties = EntityProperties.fromPairs(
				Keys.REENTRY_RATE, 0,
				Keys.TICK_IMPROVEMENT, false,
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 1,
				Keys.INITIAL_LADDER_MEAN, 85,
				Keys.INITIAL_LADDER_RANGE, 10);
		mm = new BasicMarketMaker(exec, fundamental, sip, market1,
				new Random(), agentProperties);
		setupObservations(agent1, mm);
		
		market1.submitOrder(agent1, BUY, new Price(200), 1, TimeStamp.ZERO);
		mm.agentStrategy(TimeStamp.create(10));
		market1.clear(TimeStamp.create(10));
		assertEquals(3, market1.getTransactions().size());
		assertEquals(10, obs.executionTimes.mean(), 0.001);
		assertEquals(1, obs.executionTimes.getN());	// MM time not included
	}
	
    @Test
    public void marketMakerStatsTest() {
        Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
        EntityProperties agentProperties = EntityProperties.fromPairs(
                Keys.REENTRY_RATE, 0,
                Keys.TICK_IMPROVEMENT, false,
                Keys.NUM_RUNGS, 2,
                Keys.RUNG_SIZE, 10,
                Keys.TRUNCATE_LADDER, false,
                Keys.TICK_SIZE, 1,
                Keys.INITIAL_LADDER_MEAN, 85,
                Keys.INITIAL_LADDER_RANGE, 10);
        FundamentalMarketMaker mm = new FundamentalMarketMaker(exec, fundamental, sip, market1,
            new Random(), agentProperties);
        setupObservations(agent1, mm);
        
        market1.submitOrder(agent1, BUY, new Price(100200), 1, TimeStamp.ZERO);
        mm.submitOrderLadder(new Price(100075), new Price(100085), new Price(100095), new Price(100105));
        market1.clear(TimeStamp.create(10));
        assertEquals(1, market1.getTransactions().size());
        assertEquals(1, obs.executionTimes.getN());
        assertEquals(-1, mm.getPositionBalance());
        assertEquals(0, mm.getPositioningProfit());
        assertEquals(200, mm.getSpreadProfit());
        
        market1.submitOrder(agent1, BUY, new Price(100300), 1, TimeStamp.create(11));
        market1.clear(TimeStamp.create(12));
        assertEquals(2, market1.getTransactions().size());
        assertEquals(2, obs.executionTimes.getN()); // adds on to previous
        assertEquals(-2, mm.getPositionBalance());
        assertEquals(0, mm.getPositioningProfit());
    }
	
	@Test
	public void marketmakerExecutionTimesTest() {
		Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market1, 2, 10);

		// Same times
		setupObservations(agent1, mm);
		market1.submitOrder(agent1, BUY, new Price(100), 1, TimeStamp.ZERO);
		mm.submitOrderLadder(new Price(75), new Price(85), new Price(95), new Price(105));
		market1.clear(TimeStamp.ZERO);
		assertEquals(0, obs.marketmakerExecutionTimes.mean(), 0.001);
		assertEquals(1, obs.marketmakerExecutionTimes.getN());

		// Another transaction (still 0 because executes instantly)
		market1.submitOrder(agent1, BUY, new Price(110), 1, TimeStamp.create(4));
		market1.clear(TimeStamp.create(4));
		assertEquals(0, obs.executionTimes.mean(), 0.001);
		assertEquals(2, obs.marketmakerExecutionTimes.mean(), 0.001);
		assertEquals(2, obs.marketmakerExecutionTimes.getN());	// adds on to previous
	}
	
	/**
	 * Test collection of market maker data
	 */
	@Test
	public void marketmakerSpreadsTest() {		
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market1, 2, 10);
		
		setupObservations(mm);
		mm.submitOrderLadder(new Price(75), new Price(85), new Price(95), new Price(105));
		
		assertEquals(10, obs.marketmakerSpreads.mean(), 0.001);
		assertEquals(90, obs.marketmakerLadderCenter.mean(), 0.001);
		
		mm.withdrawAllOrders();
		mm.submitOrderLadder(new Price(79), new Price(89), new Price(95), new Price(105));
		
		assertEquals(8, obs.marketmakerSpreads.mean(), 0.001);
		assertEquals(91, obs.marketmakerLadderCenter.mean(), 0.001);
		assertEquals(2, obs.marketmakerSpreads.getN());
		assertEquals(2, obs.marketmakerLadderCenter.getN());
	}
	
	
	@Test
	public void pricesTest() {
		Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		// Basic case
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(102, obs.prices.mean(), 0.001);

		// Same agent
		setupObservations(agent1);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent1, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(102, obs.prices.mean(), 0.001);
		
		// Multi quantity
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(100), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(100), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		market1.submitOrder(agent1, BUY, new Price(200), 3, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(200), 3, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(150, obs.prices.mean(), 0.001);
		
		// Split order NOTE: Clearing price is buy order
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(100), 4, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(80), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(60), 3, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(100, obs.prices.mean(), 0.001);

		// Split order NOTE: Clearing price is buy order
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, SELL, new Price(50), 4, TimeStamp.ZERO);
		market1.submitOrder(agent2, BUY, new Price(80), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, BUY, new Price(60), 3, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(70, obs.prices.mean(), 0.001);
	}
	
	@Test
	public void transPricesTest() {
		Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		setupObservations(agent1, agent2);
		
		// Basic stuff from same agent 
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent1, SELL, new Price(102), 1, TimeStamp.ZERO);
		assertEquals(Double.NaN, obs.transPrices.sample(1,1).get(0), 0.001);
		market1.clear(TimeStamp.ZERO);
		assertEquals(102, obs.transPrices.sample(1,1).get(0), 0.001);
		
		// Now with different agent, check overwriting
		market1.submitOrder(agent1, BUY, new Price(104), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(104), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(104, obs.transPrices.sample(1,1).get(0), 0.001);

		/*
		 * XXX What if two transactions happen at the same time with different
		 * prices. What price should be reflected? I believe it will reflect the
		 * last transaction to be matched, which ultimately depends on how the
		 * fourheap matches orders
		 */
		
		// Now add a transaction at a new time with multi quantity in a different market
		market2.submitOrder(agent1, BUY, new Price(106), 2, TimeStamp.ZERO);
		market2.submitOrder(agent2, SELL, new Price(106), 2, TimeStamp.ZERO);
		market2.clear(TimeStamp.create(1));
		assertEquals(ImmutableList.of(104d, 106d), obs.transPrices.sample(1,2));
		
		// Overwrite that with a split order
		market1.submitOrder(agent1, BUY, new Price(108), 1, TimeStamp.create(1));
		market1.submitOrder(agent2, SELL, new Price(108), 2, TimeStamp.create(1));
		market1.submitOrder(agent2, BUY, new Price(108), 1, TimeStamp.create(1));
		market1.clear(TimeStamp.create(1));
		assertEquals(ImmutableList.of(104d, 108d), obs.transPrices.sample(1,2));
	}
	
	@Test
	public void controlPVTest() {
		DummyPrivateValue pv = new DummyPrivateValue(2, ImmutableList.of(
				new Price(1200), new Price(1000), new Price(-200), new Price(-500)));
		Agent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1, pv, 0, 5000);
		Agent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1, pv, 0, 5000);
		setupObservations(agent1, agent2);
		assertEquals(375, obs.getFeatures().get("control_mean_private"), 0.001);
	}
	
//	@Test
//	public void controlFundamentalTest() {
//		Random rand = new Random(1);
//		FundamentalValue fund = FundamentalValue.create(0.5, 100000, 100000000, rand);
//		fund.computeFundamentalTo(5);
//		double tot = 0, lastVal = 0;
//		for (double v : fund.meanRevertProcess) {
//			tot += v;
//			lastVal = v;
//		}
//		
//		Agent agent1 = new MockBackgroundAgent(exec, fund, sip, market1);
//		Agent agent2 = new MockBackgroundAgent(exec, fund, sip, market1);
//		setupObservations(fund, agent1, agent2);
//		
//		assertEquals((tot + (obs.simLength-6)*lastVal) / (obs.simLength-1), 
//				obs.getFeatures().get("control_mean_fund"), 0.001);
//	}
	
	@Test
	public void playerTest() {
		Agent backgroundAgent, agent;
		Player backgroundPlayer, player;
		PrivateValue pv = new DummyPrivateValue(1, ImmutableList.of(
				new Price(1000), new Price(-2000)));
		
		// Basic case
		backgroundAgent = new MockBackgroundAgent(exec, fundamental, sip, market1, pv, 0, 1000);
		agent = new MockAgent(exec, fundamental, sip);
		backgroundPlayer = new Player("background", "a", backgroundAgent);
		player = new Player("foreground", "b", agent);
		setupObservations(backgroundPlayer, player);
		
		// Double fundamental
		market1.submitOrder(agent, BUY, new Price(200000), 1, TimeStamp.ZERO);
		market1.submitOrder(backgroundAgent, SELL, new Price(200000), 1, TimeStamp.ZERO);
		// To make sure agent1 sees clear
		market1.clear(TimeStamp.ZERO);
		agent.liquidateAtFundamental(TimeStamp.ZERO);
		backgroundAgent.liquidateAtFundamental(TimeStamp.ZERO);
		assertEquals(-100000, backgroundAgent.getLiquidationProfit());
		for (PlayerObservation po : obs.getPlayerObservations()) {
			if (po.role.equals("foreground")) {
				assertEquals("b", po.strategy);
				// payoff is profit (buy @ 200000) minus liquidate @ 100000
				// ie pay 200000 then sell at 100000
				assertEquals(-100000, po.payoff, 0.001);
			} else { // Background
				assertEquals("a", po.strategy);
				// payoff is (price - PV) + (position * fundamental)
				assertEquals(200000 - 1000 - 100000, po.payoff, 0.001);
			}
		}
	}
	
	@Test
	public void playerMultiQuantityTest() {
		Agent backgroundAgent, agent;
		Player backgroundPlayer, player;
		PrivateValue pv = new DummyPrivateValue(2, ImmutableList.of(
				new Price(2000), new Price(1000), new Price(-2000), new Price(4000)));
		
		// Multiple Quantity
		backgroundAgent = new MockBackgroundAgent(exec, fundamental, sip, market1, pv, 0, 1000);
		agent = new MockAgent(exec, fundamental, sip);
		backgroundPlayer = new Player("background", "a", backgroundAgent);
		player = new Player("foreground", "b", agent);
		setupObservations(backgroundPlayer, player);
		
		market1.submitOrder(agent, BUY, new Price(200000), 2, TimeStamp.ZERO);
		market1.submitOrder(backgroundAgent, SELL, new Price(200000), 2, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		agent.liquidateAtFundamental(TimeStamp.ZERO);
		backgroundAgent.liquidateAtFundamental(TimeStamp.ZERO);
		assertEquals(-2*100000, backgroundAgent.getLiquidationProfit());
		for (PlayerObservation po : obs.getPlayerObservations()) {
			if (po.role.equals("foreground")) {
				assertEquals("b", po.strategy);
				// payoff is profit (buy 2 @ 200000) minus liquidate 2 @ 100000
				// ie pay 2*200000 then sell 2@100000
				assertEquals(-200000, po.payoff, 0.001);
			} else { // Background
				assertEquals("a", po.strategy);
				// payoff is (price - PV) + (position * price)
				assertEquals((200000-2000) + (200000-1000) + (-2*100000), po.payoff, 0.001);
			}
		}
	}
	
	@Test
	public void playerSplitOrdersTest() {
		Agent backgroundAgent, agent;
		Player backgroundPlayer, player;
		PrivateValue pv = new DummyPrivateValue(2, ImmutableList.of(
				new Price(2000), new Price(1000), new Price(-2000), new Price(4000)));

		// Split Orders
		backgroundAgent = new MockBackgroundAgent(exec, fundamental, sip, market1, pv, 0, 1000);
		agent = new MockAgent(exec, fundamental, sip);
		backgroundPlayer = new Player("background", "a", backgroundAgent);
		player = new Player("foreground", "b", agent);
		setupObservations(backgroundPlayer, player);
		
		market1.submitOrder(agent, BUY, new Price(200000), 2, TimeStamp.ZERO);
		market1.submitOrder(backgroundAgent, SELL, new Price(200000), 1, TimeStamp.ZERO);
		market1.submitOrder(backgroundAgent, SELL, new Price(200000), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		agent.liquidateAtFundamental(TimeStamp.ZERO);
		backgroundAgent.liquidateAtFundamental(TimeStamp.ZERO);
		for (PlayerObservation po : obs.getPlayerObservations()) {
			if (po.role.equals("foreground")) {
				assertEquals("b", po.strategy);
				assertEquals(-200000, po.payoff, 0.001);
			} else { // Background
				assertEquals("a", po.strategy);
				// payoff is (price - PV) + (position * price)
				assertEquals((200000-2000) + (200000-1000) + (-2*100000), po.payoff, 0.001);
			}
		}
	}
	
	@Test
	public void playerLiquidationTest() {
		Agent backgroundAgent, agent;
		Player backgroundPlayer, player;
		PrivateValue pv = new DummyPrivateValue(1, ImmutableList.of(
				new Price(1000), new Price(-2000)));
		
		// Liquidate at different price
		backgroundAgent = new MockBackgroundAgent(exec, fundamental, sip, market1, pv, 0, 1000);
		agent = new MockAgent(exec, fundamental, sip);
		backgroundPlayer = new Player("background", "a", backgroundAgent);
		player = new Player("foreground", "b", agent);
		setupObservations(backgroundPlayer, player);
		
		market1.submitOrder(agent, BUY, new Price(200000), 1, TimeStamp.ZERO);
		market1.submitOrder(backgroundAgent, SELL, new Price(200000), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		agent.liquidateAtPrice(new Price(300000), TimeStamp.create(100));
		backgroundAgent.liquidateAtPrice(new Price(300000), TimeStamp.create(100));
		for (PlayerObservation po : obs.getPlayerObservations()) {
			if (po.role.equals("foreground")) {
				assertEquals("b", po.strategy);
				assertEquals(100000, po.payoff, 0.001);
			} else { // Background
				assertEquals("a", po.strategy);
				// payoff is (price - PV) + (position * price)
				assertEquals(200000-1000 + -1*300000, po.payoff, 0.001);
			}
		}
	}
	
	@Test
	public void privateValueFeatureTest() {
		PrivateValue pv = new PrivateValue(5, 1000000, new Random());
		FundamentalValue randFundamental = new FundamentalValue(0.2, 100000, 10000, 1.0, new Random());
		BackgroundAgent backgroundAgent = new MockBackgroundAgent(exec, randFundamental, sip, market1, pv, 0, 1000);

		// Get valuation for various positionBalances
		int pv1 = pv.getValue(0, BUY).intValue();
		int pv_1 = pv.getValue(0, SELL).intValue();
		
		Player backgroundPlayer = new Player("background", "a", backgroundAgent);
		setupObservations(backgroundPlayer);
		
		for (PlayerObservation po : obs.getPlayerObservations()) {
			assertEquals(new Double(pv1), po.features.get(Keys.PV_BUY1));
			assertEquals(new Double(pv_1), po.features.get(Keys.PV_SELL1));
			assertEquals(new Double(Math.max(Math.abs(pv1), Math.abs(pv_1))), 
					po.features.get(Keys.PV_POSITION1_MAX_ABS));
		}
	}
	
	private void setupObservations() {
		if (obs != null)
			Observations.BUS.unregister(obs);
		
		obs = new Observations(new SimulationSpec(),
				ImmutableList.of(market1, market2), ImmutableList.<Agent> of(),
				ImmutableList.<Player> of(), fundamental);
		
		Observations.BUS.register(obs);
	}
	
	private void setupObservations(Agent... agents) {
		if (obs != null)
			Observations.BUS.unregister(obs);
		
		obs = new Observations(new SimulationSpec(),
				ImmutableList.of(market1, market2), Arrays.asList(agents),
				ImmutableList.<Player> of(), fundamental);
		
		Observations.BUS.register(obs);
	}
	
	@SuppressWarnings("unused")
	private void setupObservations(FundamentalValue fundamental, Agent... agents) {
		if (obs != null)
			Observations.BUS.unregister(obs);
		
		obs = new Observations(new SimulationSpec(),
				ImmutableList.of(market1, market2), Arrays.asList(agents),
				ImmutableList.<Player> of(), fundamental);
		
		Observations.BUS.register(obs);
	}
	
	private void setupObservations(Player... players) {
		if (obs != null)
			Observations.BUS.unregister(obs);
		
		Builder<Agent> agentList = ImmutableList.builder();
		for (Player player : players)
			agentList.add(player.getAgent());
		
		obs = new Observations(new SimulationSpec(),
				ImmutableList.of(market1, market2), agentList.build(),
				Arrays.asList(players), fundamental);
		
		Observations.BUS.register(obs);
	}

}
