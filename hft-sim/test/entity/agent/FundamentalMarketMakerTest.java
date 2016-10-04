package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
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
import com.google.common.collect.Iterables;

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

/**
 * @author ewah
 *
 */
public class FundamentalMarketMakerTest {

	public static final TimeStamp one = TimeStamp.create(1);

	private static Executor exec;
	private static MockMarket market;
	private static SIP sip;
	private static final FundamentalValue fundamental = new MockFundamental(100000);
	private static final EntityProperties agentProperties = EntityProperties.fromPairs(
			Keys.REENTRY_RATE, 0,
			Keys.TICK_SIZE, 1,
			Keys.TRUNCATE_LADDER, true,
			Keys.TICK_IMPROVEMENT, true,
			Keys.TICK_OUTSIDE, true,
			Keys.INITIAL_LADDER_MEAN, 0,
			Keys.INITIAL_LADDER_RANGE, 0,
			Keys.SIMULATION_LENGTH, 1000,
			Keys.FUNDAMENTAL_KAPPA, 0.05,
			Keys.FUNDAMENTAL_MEAN, 100000,
			Keys.SPREAD, -1,
			Keys.FUNDAMENTAL_ESTIMATE, -1
			);

	@BeforeClass
	public static void setupClass() throws IOException {
		Log.log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "FundamentalMarketMakerTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
	}

	private static FundamentalMarketMaker createFundMM(Object... parameters) {
		return new FundamentalMarketMaker(exec, fundamental, sip, market,
				new Random(), EntityProperties.copyFromPairs(agentProperties, parameters));
	}

	// TODO test const spread < 0, = 0
	
	// TODO test computation of spread with no const spread and null bid / ask

	
	@Test
	public void estimatedFundamentalTest() {
		TimeStamp time = TimeStamp.ZERO;

		FundamentalMarketMaker mm = createFundMM(
			Keys.NUM_RUNGS, 1,
			Keys.RUNG_SIZE, 10,
			Keys.TRUNCATE_LADDER, false,
			Keys.TICK_IMPROVEMENT, false,
			Keys.TICK_OUTSIDE, false,
			Keys.TICK_SIZE, 1,
			Keys.FUNDAMENTAL_ESTIMATE, -1);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids and asks
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(103000), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(105000), 1));
		exec.executeActivity(new Clear(market));

		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(105000), quote.getAskPrice());
		assertEquals(new Price(103000), quote.getBidPrice());

		// Check activities inserted (2 submit orders plus agent reentry)
		mm.agentStrategy(time);
		
		assertTrue(mm.fundamentalEstimate.intValue() > 0);
		int est = mm.fundamentalEstimate.intValue();
		
		assertEquals(2, mm.activeOrders.size());
		Price p1 = Iterables.get(mm.activeOrders, 0, null).getPrice();
		Price p2 = Iterables.getLast(mm.activeOrders).getPrice();
		assertEquals(2000, Math.abs(p1.intValue() - p2.intValue()));
		// check the rest of its submitted ladder
		if (p1.intValue() > p2.intValue()) {
			assertEquals(new Price(est - 1000), p2);
			assertEquals(new Price(est + 1000), p1);
		} else {
			assertEquals(new Price(est - 1000), p1);
			assertEquals(new Price(est + 1000), p2);
		}
	}
	
	@Test
	public void constantSpreadTest() {
		TimeStamp time = TimeStamp.ZERO;

		FundamentalMarketMaker mm = createFundMM(
				Keys.NUM_RUNGS, 1,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_IMPROVEMENT, false,
				Keys.TICK_OUTSIDE, false,
				Keys.TICK_SIZE, 1,
				Keys.FUNDAMENTAL_ESTIMATE, -1,
				Keys.SPREAD, 1000);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(103000), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(105000), 1));
		exec.executeActivity(new Clear(market));

		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(105000), quote.getAskPrice());
		assertEquals(new Price(103000), quote.getBidPrice());

		// Check activities inserted (2 submit orders plus agent reentry)
		mm.agentStrategy(time);
		
		assertTrue(mm.fundamentalEstimate.intValue() > 0);
		int est = mm.fundamentalEstimate.intValue();
		
		assertEquals(2, mm.activeOrders.size());
		Price p1 = Iterables.get(mm.activeOrders, 0, null).getPrice();
		Price p2 = Iterables.getLast(mm.activeOrders).getPrice();
		assertEquals(1000, Math.abs(p1.intValue() - p2.intValue()));
		// check the rest of its submitted ladder
		if (p1.intValue() > p2.intValue()) {
			assertEquals(new Price(est - 500), p2);
			assertEquals(new Price(est + 500), p1);
		} else {
			assertEquals(new Price(est - 500), p1);
			assertEquals(new Price(est + 500), p2);
		}
	}
	
	@Test
	public void nullBidAsk() {
		// testing when no bid/ask, still submits orders
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = createFundMM(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_SIZE, 1);

		// Check activities inserted (none, other than reentry)
		mm.agentStrategy(time);
		assertEquals(4, mm.activeOrders.size());
	}

	@Test
	public void basicLadderTest() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = createFundMM(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_IMPROVEMENT, false,
				Keys.TICK_OUTSIDE, false,
				Keys.TICK_SIZE, 1,
				Keys.FUNDAMENTAL_ESTIMATE, 45);

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
		MarketMaker marketmaker = createFundMM(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.TICK_SIZE, 1,
				Keys.FUNDAMENTAL_ESTIMATE, 45);

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
		// New MM orders should have spread of 6, centered around 45
		// and because of tick improvement, it will be inside the quote
		ArrayList<Order> orders = new ArrayList<Order>(market.getOrders());
		assertEquals("Incorrect number of orders", 12, orders.size());
		assertEquals(agent1, orders.get(6).getAgent());
		assertEquals(new Price(42), orders.get(6).getPrice());
		assertEquals(agent2, orders.get(7).getAgent());
		assertEquals(new Price(48), orders.get(7).getPrice());

		Order order = orders.get(8);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(43), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(9);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(33), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());

		order = orders.get(10);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(47), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(11);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(57), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
	}

	/**
	 * Check changing numRungs, rungSize
	 */
	@Test
	public void rungsTest() {
		MarketMaker marketmaker = createFundMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 12,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_IMPROVEMENT, false,
				Keys.TICK_OUTSIDE, false,
				Keys.TICK_SIZE, 5,
				Keys.FUNDAMENTAL_ESTIMATE, 45);

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
		MarketMaker marketmaker = createFundMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, false,
				Keys.TICK_OUTSIDE, true,
				Keys.FUNDAMENTAL_ESTIMATE, 104,
				Keys.TICK_SIZE, 1);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(102), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(106), 1));
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
		assertEquals(new Price(106), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(5);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(111), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(6);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(116), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
	}

	@Test
	public void truncateAskTest() {
		MarketMaker marketmaker = createFundMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, false,
				Keys.TICK_OUTSIDE, true,
				Keys.FUNDAMENTAL_ESTIMATE, 79,
				Keys.TICK_SIZE, 1);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(69), 1));
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
		assertEquals(new Price(69), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(3);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(64), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(4);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(59), order.getPrice());
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

		MarketMaker marketmaker = createFundMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, false,
				Keys.TICK_OUTSIDE, false,
				Keys.FUNDAMENTAL_ESTIMATE, 45,
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
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(41), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(49), 1));

		// Verify that it withdraws ladder entirely & submits new ladder
		exec.executeActivity(new AgentStrategy(marketmaker));
		exec.executeUntil(one);
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		for (Order o : marketmaker.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 41 || price == 36 || price == 31);
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
		MarketMaker marketmaker = createFundMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, false,
				Keys.TICK_OUTSIDE, false,
				Keys.FUNDAMENTAL_ESTIMATE, 45,
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
		// so it will submit a ladder with the lastBid used for spread calculation
		// which means its spread will be 8, centered around 45
		exec.executeUntil(one);
		exec.executeActivity(new AgentStrategy(marketmaker));

		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		for (Order o : marketmaker.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 41 || price == 36 || price == 31);
			else
				assertTrue(price == 49 || price == 54 || price == 59);
		}
	}


	@Test
	public void nullBidAskLadder() {
		MarketMaker marketmaker = createFundMM(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_SIZE, 1,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 50,
				Keys.INITIAL_LADDER_RANGE, 10,
				Keys.FUNDAMENTAL_ESTIMATE, 45);
		
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
}
