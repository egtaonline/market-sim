package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logger.Log;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import systemmanager.Keys;
import activity.AgentStrategy;
import activity.Clear;
import activity.SubmitOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.agent.AdaptiveMarketMaker.TransactionResult;
import entity.infoproc.SIP;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class AdaptiveMarketMakerTest {

	public static final TimeStamp one = TimeStamp.create(1);

	private Executor exec;
	private MockMarket market;
	private SIP sip;
	private static final FundamentalValue fundamental = new MockFundamental(100000);
	private static final EntityProperties agentProperties = EntityProperties.fromPairs(
			Keys.TICK_IMPROVEMENT, false,
			Keys.FUNDAMENTAL_KAPPA, 0.05,
			Keys.NUM_HISTORICAL, 1,
			Keys.FUNDAMENTAL_MEAN, 50,
			Keys.FUNDAMENTAL_SHOCK_VAR, 10,
			Keys.REENTRY_RATE, 0.005,
			Keys.TICK_SIZE, 1);

	@BeforeClass
	public static void setupClass() throws IOException {
		
		Log.log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "AdaptiveMarketMakerTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
	}

	private AdaptiveMarketMaker createAMM(Object... parameters) {
		return new AdaptiveMarketMaker(exec, fundamental, sip, market,
				new Random(), EntityProperties.copyFromPairs(agentProperties, parameters));
	}

	@Test
	public void nullBidAsk() {
		// testing when no bid/ask, does not submit any orders
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = createAMM(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 1,
				Keys.STRATS, "2-4-6-8",
				Keys.INITIAL_LADDER_MEAN, 0,
				Keys.INITIAL_LADDER_RANGE, 0);

		// Check activities inserted (none, other than reentry)
		mm.agentStrategy(time);
		assertTrue(mm.activeOrders.isEmpty());
	}

	/**
	 * When the quote is undefined (either bid or ask is null) but prior quote
	 * was defined, then the market maker should not do anything.
	 */
	@Test
	public void quoteUndefined() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = createAMM(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 1,
				Keys.STRATS, "2-4-6-8",
				Keys.INITIAL_LADDER_MEAN, 0,
				Keys.INITIAL_LADDER_RANGE, 0);
		mm.lastAsk = new Price(55);
		mm.lastBid = new Price(45);

		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertNull(quote.getAskPrice());
		assertNull(quote.getBidPrice());

		// Check activities inserted (none, other than reentry)
		mm.agentStrategy(time);
		assertTrue(mm.activeOrders.isEmpty());

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));

		// Check market quote
		quote = market.getQuoteProcessor().getQuote();
		assertNull(quote.getAskPrice());
		assertEquals(new Price(40), quote.getBidPrice());

		// Check activities inserted (none, other than reentry)
		mm.lastAsk = new Price(55);
		mm.lastBid = new Price(45);
		mm.agentStrategy(time);
		assertTrue(mm.activeOrders.isEmpty());
	}


	@Test
	public void basicLadderTest() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = createAMM(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 1,
				Keys.STRATS, "2-4-6-8");

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));

		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		exec.executeActivity(new Clear(market));

		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(50), quote.getAskPrice());
		assertEquals(new Price(40), quote.getBidPrice());

		// Check activities inserted (4 submit orders plus agent reentry)
		mm.agentStrategy(time);

		// Check ladder of orders (use market's collection b/c ordering consistent)
		List<Order> orders = ImmutableList.copyOf(market.getOrders());
		assertEquals("Incorrect number of orders", 6, orders.size());

		assertEquals(agent1, orders.get(0).getAgent());
		assertEquals(new Price(40), orders.get(0).getPrice());
		assertEquals(agent2, orders.get(1).getAgent());
		assertEquals(new Price(50), orders.get(1).getPrice());

		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());

		assertEquals(mm, orders.get(2).getAgent());
		assertEquals(mm, orders.get(3).getAgent());
		assertEquals(mm, orders.get(4).getAgent());
		assertEquals(mm, orders.get(5).getAgent());
		assertEquals(OrderType.BUY, orders.get(2).getOrderType());
		assertEquals(OrderType.BUY, orders.get(3).getOrderType());
		assertEquals(OrderType.SELL, orders.get(4).getOrderType());
		assertEquals(OrderType.SELL, orders.get(5).getOrderType());

		int approxUnitPrice = 45;
		assertEquals(orders.get(5).getPrice().intValue() - approxUnitPrice,
				approxUnitPrice - orders.get(3).getPrice().intValue());
		assertEquals(orders.get(4).getPrice().intValue() - approxUnitPrice,
				approxUnitPrice - orders.get(2).getPrice().intValue());		
	}

	/**
	 * Check when quote changes in between reentries
	 */
	@Test
	public void quoteChangeTest() {
		AdaptiveMarketMaker marketmaker = createAMM(
				Keys.REENTRY_RATE, 0,
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 1,
				Keys.STRATS, "2");

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		exec.executeActivity(new Clear(market));

		// Initial MM strategy
		exec.executeActivity(new AgentStrategy(marketmaker));

		// Quote change
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(42), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(48), 1));
		exec.executeActivity(new Clear(market));

		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(46), quote.getAskPrice());
		assertEquals(new Price(44), quote.getBidPrice());

		exec.executeUntil(TimeStamp.create(10));
		
		// Next MM strategy execution
		exec.executeActivity(new AgentStrategy(marketmaker));

		// Check ladder of orders, previous orders withdrawn
		// market's orders contains all orders ever submitted
		ArrayList<Order> orders = new ArrayList<Order>(market.getOrders());
		assertEquals("Incorrect number of orders", 12, orders.size());
		assertEquals(agent1, orders.get(6).getAgent());
		assertEquals(new Price(42), orders.get(6).getPrice());
		assertEquals(agent2, orders.get(7).getAgent());
		assertEquals(new Price(48), orders.get(7).getPrice());

		Order order = orders.get(10);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(46), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(11);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(56), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());

		order = orders.get(8);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(44), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(9);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(34), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());

	}

	/**
	 * Check changing numRungs, rungSize
	 */
	@Test
	public void rungsTest() {
		MarketMaker marketmaker = createAMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 12,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 5,
				Keys.STRATS, "10");

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		exec.executeActivity(new Clear(market));
		// Initial MM strategy
		exec.executeActivity(new AgentStrategy(marketmaker));

		// Check ladder of orders
		// market's orders contains all orders ever submitted
		ArrayList<Order> orders = new ArrayList<Order>(market.getOrders());
		assertEquals("Incorrect number of orders", 8, orders.size());
		// Verify that 3 rungs on each side
		// Rung size was 12 quantized by tick size 5
		Order order = orders.get(2);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(40), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(3);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(30), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(4);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(20), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());

		order = orders.get(5);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(50), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(6);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(60), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(7);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(70), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
	}

	@Test
	public void withdrawLadderTest() {

		MarketMaker marketmaker = createAMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
				Keys.STRATS, "2");
		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));


		// Initial MM strategy; submits ladder with numRungs=3
		exec.executeActivity(new AgentStrategy(marketmaker));

		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());

		// Withdraw other orders & submit new orders
		agent1.withdrawAllOrders();
		assertEquals(0, agent1.activeOrders.size());
		agent2.withdrawAllOrders();
		assertEquals(0, agent2.activeOrders.size());
		exec.executeUntil(one);
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(42), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(49), 1));

		// Verify that it withdraws ladder entirely & submits new ladder
		exec.executeActivity(new AgentStrategy(marketmaker));
		exec.executeUntil(one);
		assertNotNull(marketmaker.lastBid);
		assertNotNull(marketmaker.lastAsk);
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		for (Order o : marketmaker.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 44 || price == 39 || price == 34);
			else
				assertTrue(price == 46 || price == 51 || price == 56);
		}
	}

	/**
	 * Case where withdrawing the ladder causes the quote to become undefined
	 * (as well as the last NBBO quote)
	 */
	@Test
	public void withdrawUndefinedTest() {
		MarketMaker marketmaker = createAMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
				Keys.STRATS, "2");
		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));

		// Initial MM strategy; submits ladder with numRungs=3
		exec.executeActivity(new AgentStrategy(marketmaker));

		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());

		// Withdraw other orders
		agent1.withdrawAllOrders();
		assertTrue(agent1.activeOrders.isEmpty());
		marketmaker.lastBid = new Price(42); // to make sure MM will withdraw its orders

		// Verify that it withdraws ladder entirely
		// Note that now the quote is undefined, after it withdraws its ladder
		// so it will submit a ladder with the lastBid
		exec.executeUntil(one);
		exec.executeActivity(new AgentStrategy(marketmaker));

		assertNotNull(marketmaker.lastBid);
		assertNotNull(marketmaker.lastAsk);
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		for (Order o : marketmaker.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 44 || price == 39 || price == 34);
			else
				assertTrue(price == 46 || price == 51 || price == 56);
		}
	}


	@Test
	public void nullBidAskLadder() {
		MarketMaker marketmaker = createAMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
				Keys.STRATS, "2",
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, true,
				Keys.INITIAL_LADDER_MEAN, 50,
				Keys.INITIAL_LADDER_RANGE, 10);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));

		exec.executeActivity(new Clear(market));

		// Initial MM strategy
		exec.executeActivity(new AgentStrategy(marketmaker));

		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());

		// Quote change
		// Withdraw other orders
		agent1.withdrawAllOrders();
		assertTrue(agent1.activeOrders.isEmpty());
		agent2.withdrawAllOrders();
		assertEquals(0, agent2.activeOrders.size());
		assertTrue(marketmaker.lastAsk != null);
		assertTrue(marketmaker.lastBid != null);

		// Note that now the quote is undefined, after it withdraws its ladder
		exec.executeUntil(one);

		// Next MM strategy execution
		exec.executeActivity(new AgentStrategy(marketmaker));

		// Check ladder of orders, previous orders withdrawn
		// market's orders contains all orders ever submitted (include background traders)
		ArrayList<Order> orders = new ArrayList<Order>(market.getOrders());
		assertEquals("Incorrect number of orders", 14, orders.size());

		// Storing buy/sell orders
		SummaryStatistics buys = new SummaryStatistics();
		SummaryStatistics sells = new SummaryStatistics();
		for (Order o : marketmaker.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY)
				buys.addValue(price);
			else
				sells.addValue(price);
		}

		// Checking randomly generated ladder
		int ladderCenter = ((int) (buys.getMax() + sells.getMin()) / 2);
		assertTrue("ladder center outside range", ladderCenter <= 60 && ladderCenter >= 40);
		assertEquals(ladderCenter + 1, (int) sells.getMin());
		assertEquals(ladderCenter - 1, (int) buys.getMax());
		assertEquals(5, sells.getMax() - sells.getMean(), 0.0001);
		assertEquals(5, buys.getMax() - buys.getMean(), 0.0001);
		assertEquals(5, sells.getMax() - sells.getMean(), 0.0001);
		assertEquals(5, buys.getMax() - buys.getMean(), 0.0001);
		assertEquals(5, sells.getMean() - sells.getMin(), 0.0001);
		assertEquals(5, buys.getMean() - buys.getMin(), 0.0001);
	}

	@Test
	public void chooseMedianWeight() {
		MarketMaker marketmaker = createAMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
				Keys.STRATS, "2-4-6",
				Keys.USE_MEDIAN_SPREAD, true);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		exec.executeActivity(new Clear(market));

		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(50), quote.getAskPrice());
		assertEquals(new Price(40), quote.getBidPrice());

		//Run agent strategy
		marketmaker.agentStrategy(TimeStamp.ZERO);

		//Check correct number of orders
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());

		//Check orders submitted using median spread of 4
		for(Order o : marketmaker.activeOrders){
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 43 || price == 38 || price == 33);
			else
				assertTrue(price == 47 || price == 52 || price == 57);
		}
	}
	
	@Test
	public void recalculateWeights(){
		AdaptiveMarketMaker marketmaker = createAMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
				Keys.STRATS, "2-40-50",
				Keys.NUM_HISTORICAL, 1,
				Keys.USE_LAST_PRICE, true,
				Keys.FAST_LEARNING, true,
				Keys.USE_MEDIAN_SPREAD, true);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(80), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(120), 1));
		exec.executeActivity(new Clear(market));

		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(120), quote.getAskPrice());
		assertEquals(new Price(80), quote.getBidPrice());

		//Run agent strategy; check that lastPrice updates
		assertNull(marketmaker.lastPrice);
		marketmaker.agentStrategy(TimeStamp.ZERO);
		assertEquals(new Price(100), marketmaker.lastPrice);

		//Check weights initially equal
		Map<Integer,Double> weights = ImmutableMap.copyOf(marketmaker.weights);
		assertEquals(weights.get(2),  weights.get(40), 0.0001);
		assertEquals(weights.get(40), weights.get(50), 0.0001);
		assertEquals(weights.get(50), weights.get(2),  0.0001);
		assertEquals(1, weights.get(2) + weights.get(40) + weights.get(50), 0.001);

		//Check correct number of orders
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());

		//Check orders submitted using median spread of 40
		assertEquals(40, marketmaker.getSpread());
		for(Order o : marketmaker.activeOrders){
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 80 || price == 75 || price == 70);
			else
				assertTrue(price == 120 || price == 125 || price == 130);
		}

		//Make a trade that yields profit for one of the spreads(2) and is useless for the other two(40,50)
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(118), 1));
		exec.executeActivity(new Clear(market));
		assertEquals(0, agent1.positionBalance);
		quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(120), quote.getAskPrice());
		assertEquals(new Price(118), quote.getBidPrice());
		
		//check that spread = 2 is now weighted slightly higher
		marketmaker.agentStrategy(one);
		weights = ImmutableMap.copyOf(marketmaker.weights);
		assertEquals(1, weights.get(2) + weights.get(40) + weights.get(50), 0.001);
		assertTrue(weights.get(2) > weights.get(40));
		assertTrue(weights.get(2) > weights.get(50));

		//check that the other two spreads, neither of which transacted, still have same weight
		assertEquals(weights.get(40), weights.get(50), 0.0001);
	}
	
	@Test
	public void movingAverage() {
		AdaptiveMarketMaker marketmaker = createAMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
				Keys.STRATS, "2-30-50",
				Keys.NUM_HISTORICAL, 5,
				Keys.USE_LAST_PRICE, true,
				Keys.FAST_LEARNING, true,
				Keys.USE_MEDIAN_SPREAD, true);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(80), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(120), 1));
		exec.executeActivity(new Clear(market));

		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(120), quote.getAskPrice());
		assertEquals(new Price(80), quote.getBidPrice());

		//Run agent strategy;
		marketmaker.agentStrategy(TimeStamp.ZERO);
		assertEquals(new Price(100), marketmaker.lastPrice);

		//Check weights initially equal
		Map<Integer,Double> weights = ImmutableMap.copyOf(marketmaker.weights);
		assertEquals(weights.get(2),  weights.get(30), 0.0001);
		assertEquals(weights.get(30), weights.get(50), 0.0001);
		assertEquals(weights.get(50), weights.get(2),  0.0001);
		assertEquals(1, weights.get(2) + weights.get(30) + weights.get(50), 0.001);

		//Check correct number of orders
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());

		//Check orders submitted using median spread of 30
		assertEquals(30, marketmaker.getSpread());
		for(Order o : marketmaker.activeOrders){
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 75 || price == 80 || price == 85);
			else
				assertTrue(price == 115 || price == 120 || price == 125);
		}

		//Make a trade that yields profit for one of the spreads(2) and is useless for the other two(30,40)
		agent1.withdrawAllOrders();
		agent2.withdrawAllOrders();
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(116), 2));
		exec.executeActivity(new Clear(market));
		assertEquals(1, agent1.positionBalance);
		quote = market.getQuoteProcessor().getQuote();
		 // $115 traded with agent1 buying at $116, so ASK now 120
		assertEquals(new Price(120), quote.getAskPrice());
		assertEquals(new Price(116), quote.getBidPrice());
		
		// Check that spread = 2 is now weighted slightly higher
		// check that spread = 30 also now has more weight
		marketmaker.agentStrategy(one);
		assertEquals(2, marketmaker.getSpread());
		// generates ladder centered around lastPrice of 109, with spread 2
		for(Order o : marketmaker.activeOrders){
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 98 || price == 103 || price == 108);
			else
				assertTrue(price == 110 || price == 115 || price == 120);
		}
		weights = ImmutableMap.copyOf(marketmaker.weights);
		assertEquals(1, weights.get(2) + weights.get(30) + weights.get(50), 0.001);
		assertTrue(weights.get(2) > weights.get(30));
		assertTrue(weights.get(2) > weights.get(50));
		assertTrue(weights.get(30) > weights.get(50));
		double weight2 = weights.get(2);
		double weight30 = weights.get(30);

		// check moving average price stored as last price
		assertEquals(new Price(109), marketmaker.lastPrice);
		
		// Third test: only spread = 2 should get more weight
		// $116 from agent1 executes with $110 MM
		exec.executeActivity(new Clear(market));
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(118), 2));
		// $118 from agent1 executes with $115 MM
		exec.executeActivity(new Clear(market));
		quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(120), quote.getAskPrice());
		assertEquals(new Price(118), quote.getBidPrice()); 
		
		// check that spread = 2 is now weighted slightly higher than before
		// check that spread = 30 is less weight than prior iteration, but still > weight for 50
		marketmaker.agentStrategy(one);
		weights = ImmutableMap.copyOf(marketmaker.weights);
		assertEquals(1, weights.get(2) + weights.get(30) + weights.get(50), 0.001);
		assertTrue(weights.get(2) > weights.get(30));
		assertTrue(weights.get(2) > weight2);
		assertTrue(weights.get(2) > weights.get(50));
		assertTrue(weights.get(30) > weights.get(50));
		assertTrue(weights.get(30) < weight30);
		
		assertEquals(new Price(112), marketmaker.lastPrice);
	}
	
	@Test
	public void lastTransactionResult() {
		AdaptiveMarketMaker marketmaker = createAMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
				Keys.STRATS, "2-40-50",
				Keys.USE_MEDIAN_SPREAD, true);

		marketmaker.lastAsk = new Price(100);
		marketmaker.lastBid = new Price(80);
		marketmaker.lastPrice = new Price(90);
		
		TransactionResult test = marketmaker.lastTransactionResult(10, new Price(81), new Price(91));
		// rungs generated should be SELL(95,100,105), BUY(85,80,75)
		// nothing should trade in this scenario
		assertEquals(0, (int) test.getCashChange());
		assertEquals(0, (int) test.getHoldingsChange());
		
		test = marketmaker.lastTransactionResult(2, new Price(81), new Price(83));
		// rungs generated should be SELL(91,96,101), BUY(89,84,81)
		// two orders should trade, a buy rung at price 89 & one at 84 
		assertEquals(2, (int) test.getHoldingsChange());
		assertEquals(-(90-1)-(90-6), (int) test.getCashChange());
		
		test = marketmaker.lastTransactionResult(2, new Price(97), new Price(99));
		// rungs generated should be SELL(91,96,101), BUY(89,84,81)
		// two orders should trade, a sell rung at price 91 & one at 96 
		assertEquals(-2, (int) test.getHoldingsChange());
		assertEquals((90+1)+(90+6), (int) test.getCashChange());
		
		marketmaker.lastPrice = new Price(83);
		test = marketmaker.lastTransactionResult(2, new Price(81), new Price(83));
		// rungs generated should be SELL(91,96,101), BUY(89,84,81)
		// two orders should trade, a buy rung at price 89 & one at 84 
		assertEquals(0, (int) test.getHoldingsChange());
		assertEquals(0, (int) test.getCashChange());
		
		marketmaker.lastPrice = new Price(95);
		test = marketmaker.lastTransactionResult(2, new Price(97), new Price(99));
		// rungs generated should be SELL(91,96,101), BUY(89,84,81)
		// two orders should trade, a sell rung at price 91 & one at 96 
		assertEquals(-1, (int) test.getHoldingsChange());
		assertEquals((95+1), (int) test.getCashChange());
	}
}
