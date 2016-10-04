package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.QuoteProcessor;
import entity.infoproc.SIP;
import entity.market.CDAMarket;
import entity.market.DummyMarketTime;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.MockMarket;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import systemmanager.Consts;
import systemmanager.Executor;
import systemmanager.Keys;
import activity.AgentStrategy;
import activity.Clear;
import activity.SubmitOrder;

public class ZIRPAgentTest {

	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;
	private static Random rand;
	private static EntityProperties agentProperties = 
			EntityProperties.fromPairs(
					Keys.REENTRY_RATE, 0,
					Keys.MAX_POSITION, 2,
					Keys.PRIVATE_VALUE_VAR, 0,
					Keys.BID_RANGE_MIN, 0,
					Keys.BID_RANGE_MAX, 5000,
					Keys.SIMULATION_LENGTH, 60000,
					Keys.FUNDAMENTAL_KAPPA, 0.05,
					Keys.FUNDAMENTAL_MEAN, 100000,
					Keys.ACCEPTABLE_PROFIT_THRESHOLD, 0.8
					);

	public ZIRPAgent createAgent(Object... parameters) {
		return createAgent(
				fundamental, 
				market, 
				rand, 
				parameters
				);
	}

	public ZIRPAgent createAgent(
			final FundamentalValue fundamental, 
			final Market market, 
			final Random rand, 
			final Object... parameters
			) {
		return new ZIRPAgent(
				exec, 
				TimeStamp.ZERO, 
				fundamental, 
				sip, 
				market,
				rand, 
				EntityProperties.copyFromPairs(
						agentProperties,	
						parameters
						)
				);
	}

	@BeforeClass
	public static void setUpClass() throws IOException{
		// Setting up the log file
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "ZIRPAgentTest.log"));

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

	@Test
	public void withdrawTest() {
		// verify that orders are correctly withdrawn at each re-entry
		ZIRPAgent agent = createAgent(
				Keys.MAX_POSITION, 2,
				Keys.WITHDRAW_ORDERS, true);

		// execute strategy once; then before reenter, change the position balance
		// that way, when execute strategy again, it won't submit new orders
		exec.executeActivity(new AgentStrategy(agent));
		// verify that order submitted
		assertEquals(1, agent.activeOrders.size());
		agent.positionBalance = 10;
		exec.executeActivity(new AgentStrategy(agent));
		// verify that order withdrawn
		assertEquals(0, agent.activeOrders.size());
	}

	@Test
	public void basicBuyerTest() {
		ZIRPAgent agent = createAgent(
				Keys.PRIVATE_VALUE_VAR, 100,
				Keys.MAX_POSITION, 2,
				Keys.BID_RANGE_MIN, 10000,
				Keys.BID_RANGE_MAX, 10000,
				Keys.WITHDRAW_ORDERS, true,
				Keys.ACCEPTABLE_PROFIT_THRESHOLD, 0.75);

		TimeStamp time = TimeStamp.ZERO;
		final int simLength = 60000;
		final double fundamentalKappa = 0.05;
		final double fundamentalMean = 100000;
		final double acceptableProfitFraction = 0.75;

		QuoteProcessor qp = agent.marketQuoteProcessor;
		MarketTime mktTime = new DummyMarketTime(time, 0);
		Quote q = new Quote(market, new Price(120000), 1, new Price(130000), 1, mktTime);
		sip.processQuote(market, q, time);
		qp.processQuote(market, q, time);

		agent.executeZIRPStrategy(BUY, 1, time, 
				simLength, fundamentalKappa, fundamentalMean, acceptableProfitFraction);

		Price val = agent.getEstimatedValuation(BUY, time, simLength, fundamentalKappa, fundamentalMean);

		// Verify that agent does shade
		// since 10000 * 0.75 > val - 130000;
		assertEquals(1, agent.activeOrders.size());
		Price p = Iterables.getLast(agent.activeOrders).getPrice();
		assertEquals(val.intValue() - 10000, p.intValue());
	}

	@Test
	public void basicBuyerTest2() {
		Market market2 = new CDAMarket(exec, sip, rand, TimeStamp.IMMEDIATE, 1);
		ZIRPAgent agent = createAgent(fundamental, market2, rand,
				Keys.PRIVATE_VALUE_VAR, 100,
				Keys.MAX_POSITION, 2,
				Keys.BID_RANGE_MIN, 10000,
				Keys.BID_RANGE_MAX, 10000,
				Keys.WITHDRAW_ORDERS, true,
				Keys.ACCEPTABLE_PROFIT_THRESHOLD, 0.75);

		TimeStamp time = TimeStamp.ZERO;
		final int simLength = 60000;
		final double fundamentalKappa = 0.05;
		final double fundamentalMean = 100000;
		final double acceptableProfitFraction = 0.75;

		MockAgent agent2 = new MockAgent(exec, fundamental, sip);
		MockAgent agent1 = new MockAgent(exec, fundamental, sip);
		exec.executeActivity(new SubmitOrder(agent2, market2, BUY, new Price(80000), 1));
		exec.executeActivity(new SubmitOrder(agent1, market2, SELL, new Price(85000), 1));
		exec.executeActivity(new Clear(market2));
		exec.executeUntil(TimeStamp.create(1));

		agent.executeZIRPStrategy(BUY, 1, time, 
				simLength, fundamentalKappa, fundamentalMean, acceptableProfitFraction);
		
		// when markup is not sufficient, then don't shade
		// since 10000 * 0.75 <= val - 85000
		// Verify that agent's order will trade immediately
		assertEquals(0, agent.activeOrders.size());	
		exec.executeActivity(new Clear(market2));
		assertEquals(1, agent.transactions.size());
		assertEquals(new Price(85000), Iterables.getLast(agent.transactions).getPrice());

	}

	@Test
	public void basicBuyerTest3() {
		// to test what the price of the agent's submitted order is
		ZIRPAgent zirp = createAgent(
				Keys.PRIVATE_VALUE_VAR, 100,
				Keys.MAX_POSITION, 2,
				Keys.BID_RANGE_MIN, 10000,
				Keys.BID_RANGE_MAX, 10000,
				Keys.WITHDRAW_ORDERS, true,
				Keys.ACCEPTABLE_PROFIT_THRESHOLD, 0.75);
		
		TimeStamp time = TimeStamp.ZERO;
		final int simLength = 60000;
		final double fundamentalKappa = 0.05;
		final double fundamentalMean = 100000;
		final double acceptableProfitFraction = 0.75;

		QuoteProcessor qp = zirp.marketQuoteProcessor;
		MarketTime mktTime = new DummyMarketTime(time, 0);
		Quote q = new Quote(market, new Price(80000), 1, new Price(85000), 1, mktTime);
		qp.processQuote(market, q, time);

		zirp.executeZIRPStrategy(BUY, 1, time, 
				simLength, fundamentalKappa, fundamentalMean, acceptableProfitFraction);
		Price val = zirp.getEstimatedValuation(BUY, time, simLength, fundamentalKappa, fundamentalMean);

		// when markup is not sufficient, then don't shade
		// since 10000 * 0.75 <= val - 85000
		assertEquals(1, zirp.activeOrders.size());
		Price p = Iterables.getLast(zirp.activeOrders).getPrice();
		assertEquals(val, p);
	}
	
	@Test
	public void basicSellerTest() {
		ZIRPAgent agent = createAgent(
				Keys.PRIVATE_VALUE_VAR, 100,
				Keys.MAX_POSITION, 2,
				Keys.BID_RANGE_MIN, 10000,
				Keys.BID_RANGE_MAX, 10000,
				Keys.WITHDRAW_ORDERS, true,
				Keys.ACCEPTABLE_PROFIT_THRESHOLD, 0.75);

		TimeStamp time = TimeStamp.ZERO;
		final int simLength = 60000;
		final double fundamentalKappa = 0.05;
		final double fundamentalMean = 100000;
		final double acceptableProfitFraction = 0.75;

		QuoteProcessor qp = agent.marketQuoteProcessor;
		MarketTime mktTime = new DummyMarketTime(time, 0);
		Quote q = new Quote(market, new Price(80000), 1, new Price(85000), 1, mktTime);
		sip.processQuote(market, q, time);
		qp.processQuote(market, q, time);

		agent.executeZIRPStrategy(SELL, 1, time, 
				simLength, fundamentalKappa, fundamentalMean, acceptableProfitFraction);
		Price val = agent.getEstimatedValuation(SELL, time, simLength, fundamentalKappa, fundamentalMean);
		
		// Verify that agent doesn't shade
		// since 10000 * 0.75 > 80000 - val
		assertEquals(new Price(80000), agent.marketQuoteProcessor.getQuote().getBidPrice());
		assertEquals(1, agent.activeOrders.size());
		Price p = Iterables.getLast(agent.activeOrders).getPrice();
		assertEquals(val.intValue() + 10000, p.intValue());

		exec.executeActivity(new Clear(market));
		assertEquals(0, agent.transactions.size());
	}

	@Test
	public void basicSellerTest2() {
		Market market2 = new CDAMarket(exec, sip, rand, TimeStamp.IMMEDIATE, 1);
		ZIRPAgent agent = createAgent(fundamental, market2, rand,
				Keys.PRIVATE_VALUE_VAR, 100,
				Keys.MAX_POSITION, 2,
				Keys.BID_RANGE_MIN, 10000,
				Keys.BID_RANGE_MAX, 10000,
				Keys.WITHDRAW_ORDERS, true,
				Keys.ACCEPTABLE_PROFIT_THRESHOLD, 0.75);

		TimeStamp time = TimeStamp.ZERO;
		final int simLength = 60000;
		final double fundamentalKappa = 0.05;
		final double fundamentalMean = 100000;
		final double acceptableProfitFraction = 0.75;

		MockAgent agent2 = new MockAgent(exec, fundamental, sip);
		MockAgent agent1 = new MockAgent(exec, fundamental, sip);

		exec.executeActivity(new SubmitOrder(agent2, market2, BUY, new Price(120000), 1));
		exec.executeActivity(new SubmitOrder(agent1, market2, SELL, new Price(130000), 1));
		exec.executeActivity(new Clear(market2));
		exec.executeUntil(TimeStamp.create(1));

		agent.executeZIRPStrategy(SELL, 1, time, 
				simLength, fundamentalKappa, fundamentalMean, acceptableProfitFraction);

		// when markup is not sufficient, then don't shade
		// since 10000 * 0.75 <= 120000 - val
		// Verify that agent's order will trade immediately
		assertEquals(0, agent.activeOrders.size());	
		exec.executeActivity(new Clear(market2));
		assertEquals(1, agent.transactions.size());
		assertEquals(new Price(120000), Iterables.getLast(agent.transactions).getPrice());
	}

	@Test
	public void basicSellerTest3() {
		// to test what the price of the agent's submitted order is
		ZIRPAgent zirp = createAgent(
				Keys.PRIVATE_VALUE_VAR, 100,
				Keys.MAX_POSITION, 2,
				Keys.BID_RANGE_MIN, 10000,
				Keys.BID_RANGE_MAX, 10000,
				Keys.WITHDRAW_ORDERS, true,
				Keys.ACCEPTABLE_PROFIT_THRESHOLD, 0.75);

		TimeStamp time = TimeStamp.ZERO;
		final int simLength = 60000;
		final double fundamentalKappa = 0.05;
		final double fundamentalMean = 100000;
		final double acceptableProfitFraction = 0.75;

		QuoteProcessor qp = zirp.marketQuoteProcessor;
		MarketTime mktTime = new DummyMarketTime(time, 0);
		Quote q = new Quote(market, new Price(120000), 1, new Price(130000), 1, mktTime);
		sip.processQuote(market, q, time);
		qp.processQuote(market, q, time);
		
		zirp.executeZIRPStrategy(SELL, 1, time, 
				simLength, fundamentalKappa, fundamentalMean, acceptableProfitFraction);
		Price val = zirp.getEstimatedValuation(SELL, time, simLength, fundamentalKappa, fundamentalMean);

		// when markup is not sufficient, then don't shade
		// since 10000 * 0.75 <= 120000 - val
		assertEquals(1, zirp.activeOrders.size());
		Price p = Iterables.getLast(zirp.activeOrders).getPrice();
		assertEquals(val, p);
	}

	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			basicBuyerTest();
			setup();
			basicBuyerTest2();
			setup();
			basicBuyerTest3();
			setup();
			basicSellerTest();
			setup();
			basicSellerTest2();
			setup();
			basicSellerTest3();
		}
	}


	// Test that returns empty if exceed max position
	@Test
	public void testZIRPStrat() {
		TimeStamp time = TimeStamp.ZERO;
		List<Price> values = Arrays.asList(new Price(100), new Price(10));
		PrivateValue pv = new DummyPrivateValue(1, values);
		FundamentalValue fundamental = new MockFundamental(100000);

		BackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market, pv, 0, 1000);

		final int simulationLength = 60000;
		final double fundamentalKappa = 0.05;
		final double fundamentalMean = 100000;
		final double acceptableProfitFraction = 0.8;

		agent.executeZIRPStrategy(
				BUY, 5, time, simulationLength, fundamentalKappa, fundamentalMean, acceptableProfitFraction
				);
		assertTrue(agent.activeOrders.isEmpty());
		assertTrue(agent.transactions.isEmpty());
		agent.executeZIRPStrategy(
				SELL, 5, time, simulationLength, fundamentalKappa, fundamentalMean, acceptableProfitFraction
				);
		assertTrue(agent.activeOrders.isEmpty());
		assertTrue(agent.transactions.isEmpty());

		// Test ZIRP strategy
		agent.executeZIRPStrategy(
				BUY, 1, time, simulationLength, fundamentalKappa, fundamentalMean, acceptableProfitFraction
				);
		assertEquals(1, agent.activeOrders.size());
	}
}
