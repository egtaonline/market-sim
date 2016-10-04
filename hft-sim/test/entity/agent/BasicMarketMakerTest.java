package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import activity.ProcessQuote;
import activity.SubmitOrder;

import com.google.common.collect.ImmutableList;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.BestBidAsk;
import entity.infoproc.SIP;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class BasicMarketMakerTest {

	public static final TimeStamp one = TimeStamp.create(1);

	private Executor exec;
	private MockMarket market;
	private SIP sip;
	private static final FundamentalValue fundamental = new MockFundamental(100000);
	private static final EntityProperties agentProperties = EntityProperties.fromPairs(
			Keys.REENTRY_RATE, 0,
			Keys.TICK_IMPROVEMENT, false);

	@BeforeClass
	public static void setupClass() throws IOException {
		
		Log.log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "BasicMarketMakerTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
	}

	private BasicMarketMaker createBMM(Object... parameters) {
		return new BasicMarketMaker(exec, fundamental, sip, market,
				new Random(), EntityProperties.copyFromPairs(agentProperties, parameters));
	}

	@Test
	public void nullBidAsk() {
		// testing when no bid/ask, does not submit any orders
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = createBMM(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 1,
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

		MarketMaker mm = createBMM(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 1,
				Keys.INITIAL_LADDER_MEAN, 0,
				Keys.INITIAL_LADDER_RANGE, 0);
		mm.lastAsk = new Price(55);
		mm.lastBid = new Price(45);

		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(null, quote.getAskPrice());
		assertEquals(null, quote.getBidPrice());

		// Check activities inserted (none, other than reentry)
		mm.agentStrategy(time);
		assertTrue(mm.activeOrders.isEmpty());

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));

		// Check market quote
		quote = market.getQuoteProcessor().getQuote();
		assertEquals(null, quote.getAskPrice());
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

		MarketMaker mm = createBMM(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 1);

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
		assertEquals(new Price(40), orders.get(2).getPrice());
		assertEquals(OrderType.BUY, orders.get(2).getOrderType());
		assertEquals(mm, orders.get(3).getAgent());
		assertEquals(new Price(30), orders.get(3).getPrice());
		assertEquals(OrderType.BUY, orders.get(3).getOrderType());

		assertEquals(mm, orders.get(4).getAgent());
		assertEquals(new Price(50), orders.get(4).getPrice());
		assertEquals(OrderType.SELL, orders.get(4).getOrderType());
		assertEquals(mm, orders.get(5).getAgent());
		assertEquals(new Price(60), orders.get(5).getPrice());
		assertEquals(OrderType.SELL, orders.get(5).getOrderType());
	}

	/**
	 * Check when quote changes in between reentries
	 */
	@Test
	public void quoteChangeTest() {
		MarketMaker marketmaker = createBMM(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 1);

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
		assertEquals(new Price(48), quote.getAskPrice());
		assertEquals(new Price(42), quote.getBidPrice());

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

		Order order = orders.get(8);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(42), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(9);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(32), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());

		order = orders.get(10);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(48), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(11);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(58), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
	}

	/**
	 * Check changing numRungs, rungSize
	 */
	@Test
	public void rungsTest() {
		MarketMaker marketmaker = createBMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 12,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 5);

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
	public void truncateBidTest() {
		MarketMaker marketmaker = createBMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(102), 1));

		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(105), 1));
		exec.executeActivity(new Clear(market));


		// Updating NBBO quote
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(90), 1, new Price(100), 1, TimeStamp.ZERO);
		exec.executeActivity(new ProcessQuote(sip, market2, q));


		// Just to check that NBBO correct (it crosses)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(102), nbbo.getBestBid());

		// MM strategy
		exec.executeActivity(new AgentStrategy(marketmaker));


		// Check ladder of orders
		// market's orders contains all orders ever submitted
		ArrayList<Order> orders = new ArrayList<Order>(market.getOrders());
		assertEquals("Incorrect number of orders", 7, orders.size());
		// Verify that 2 rungs on truncated side
		Order order = orders.get(2);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(97), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(3);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(92), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		// 3 rungs on sell side
		order = orders.get(4);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(105), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(5);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(110), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(6);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(115), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
	}

	@Test
	public void truncateAskTest() {
		MarketMaker marketmaker = createBMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(70), 1));

		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(89), 1));
		exec.executeActivity(new Clear(market));


		// Updating NBBO quote
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(90), 1, new Price(100), 1, TimeStamp.ZERO);
		exec.executeActivity(new ProcessQuote(sip, market2, q));


		// Just to check that NBBO correct (it crosses)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(89), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(90), nbbo.getBestBid());

		// MM strategy
		exec.executeActivity(new AgentStrategy(marketmaker));


		// Check ladder of orders
		// market's orders contains all orders ever submitted
		ArrayList<Order> orders = new ArrayList<Order>(market.getOrders());
		assertEquals("Incorrect number of orders", 7, orders.size());
		// Verify that 3 rungs on buy side
		Order order = orders.get(2);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(70), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(3);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(65), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(4);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(60), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		// 2 rungs on truncated sell side
		order = orders.get(5);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(94), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(6);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(99), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
	}

	@Test
	public void withdrawLadderTest() {

		MarketMaker marketmaker = createBMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1);
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
				assertTrue(price == 42 || price == 37 || price == 32);
			else
				assertTrue(price == 49 || price == 54 || price == 59);
		}
	}

	/**
	 * Case where withdrawing the ladder causes the quote to become undefined
	 * (as well as the last NBBO quote)
	 */
	@Test
	public void withdrawUndefinedTest() {
		MarketMaker marketmaker = createBMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1);
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
				assertTrue(price == 42 || price == 37 || price == 32);
			else
				assertTrue(price == 50 || price == 55 || price == 60);
		}
	}


	@Test
	public void nullBidAskLadder() {
		MarketMaker marketmaker = createBMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
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
		assertEquals(ladderCenter + 5, (int) sells.getMin());
		assertEquals(ladderCenter - 5, (int) buys.getMax());
		assertEquals(5, sells.getMax() - sells.getMean(), 0.0001);
		assertEquals(5, buys.getMax() - buys.getMean(), 0.0001);
		assertEquals(5, sells.getMax() - sells.getMean(), 0.0001);
		assertEquals(5, buys.getMax() - buys.getMean(), 0.0001);
		assertEquals(5, sells.getMean() - sells.getMin(), 0.0001);
		assertEquals(5, buys.getMean() - buys.getMin(), 0.0001);
	}
	
	@Test
	public void oneBackgroundBuyer() {
		MarketMaker marketmaker = createBMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 50,
				Keys.INITIAL_LADDER_RANGE, 10);
		
		// Creating dummy agent
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new Clear(market));

		// Initial MM strategy
		exec.executeActivity(new AgentStrategy(marketmaker));

		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		
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
		
		// Checking one-sided ladder
		int ladderCenter = ((int) (buys.getMax() + sells.getMin()) / 2);
		assertTrue("ladder center outside range", ladderCenter >= 40 && ladderCenter <= 60);
		assertTrue("ladder sell outside range", sells.getMin() <= 60 && sells.getMin() >= 50);
		assertEquals(ladderCenter + 5, (int) sells.getMin());
		assertEquals(50, (int) sells.getMin());	// no need for tick improvement
		assertEquals(40 + 1, (int) buys.getMax()); // tick improvement outside
		assertEquals(5, sells.getMax() - sells.getMean(), 0.0001);
		assertEquals(5, buys.getMax() - buys.getMean(), 0.0001);
		assertEquals(5, sells.getMax() - sells.getMean(), 0.0001);
		assertEquals(5, buys.getMax() - buys.getMean(), 0.0001);
		assertEquals(5, sells.getMean() - sells.getMin(), 0.0001);
		assertEquals(5, buys.getMean() - buys.getMin(), 0.0001);
		assertEquals(0, market.getTransactions().size());
		
		// Verify that single background trader will transact with the MM
		market.submitOrder(agent1, BUY, new Price(80), 1, TimeStamp.create(10));
		market.clear(TimeStamp.create(10));
		assertEquals(1, market.getTransactions().size());
		// because MockMarket, cannot test price of transaction
	}
	
	@Test
	public void oneBackgroundSeller() {
		MarketMaker marketmaker = createBMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 50,
				Keys.INITIAL_LADDER_RANGE, 10);
		
		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, SELL, new Price(60), 1));
		exec.executeActivity(new Clear(market));

		// Initial MM strategy
		exec.executeActivity(new AgentStrategy(marketmaker));

		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		
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
		
		// Checking one-sided ladder
		int ladderCenter = ((int) (buys.getMax() + sells.getMin()) / 2);
		assertTrue("ladder center outside range", ladderCenter <= 60 && ladderCenter >= 40);
		assertTrue("ladder buy outside range", buys.getMin() <= 50 && buys.getMin() >= 40);
		assertEquals(60 - 1, (int) sells.getMin());
		assertEquals(50, (int) buys.getMax()); // tick improvement outside
		assertEquals(5, sells.getMax() - sells.getMean(), 0.0001);
		assertEquals(5, buys.getMax() - buys.getMean(), 0.0001);
		assertEquals(5, sells.getMax() - sells.getMean(), 0.0001);
		assertEquals(5, buys.getMax() - buys.getMean(), 0.0001);
		assertEquals(5, sells.getMean() - sells.getMin(), 0.0001);
		assertEquals(5, buys.getMean() - buys.getMin(), 0.0001);
	}
	
	@Test
	public void oneBackgroundTraderLadderRange() {
		// testing where 2*rungSize (step size) is larger than initial range
		
		MarketMaker marketmaker = createBMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 50,
				Keys.INITIAL_LADDER_RANGE, 7);
		
		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new Clear(market));

		// Initial MM strategy
		exec.executeActivity(new AgentStrategy(marketmaker));

		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		
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
		
		// Checking one-sided ladder
		int ladderCenter = ((int) (buys.getMax() + sells.getMin()) / 2);
		assertTrue("ladder center outside range", ladderCenter <= 60 && ladderCenter >= 40);
		assertTrue("ladder sell outside range", sells.getMin() <= 60 && sells.getMin() >= 47);
	}


	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			nullBidAskLadder();
			setup();
			oneBackgroundBuyer();
			setup();
			oneBackgroundSeller();
			setup();
			oneBackgroundTraderLadderRange();
		}
	}
}
