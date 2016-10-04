package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import systemmanager.Keys;
import activity.Clear;
import activity.ProcessQuote;
import activity.SubmitOrder;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.CDAMarket;
import entity.market.DummyMarketTime;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;

public class MarketMakerTest {

	private Executor exec;
	private MockMarket market;
	private SIP sip;
	private FundamentalValue fundamental = new MockFundamental(100000);

	@BeforeClass
	public static void setupClass() throws IOException {
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "MarketMakerTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
	}

	// FIXME should MMs be able to submit rung size=0 ladders? For now, do not permit
	
	@Test
	public void nullBidAsk() {
		// testing when no bid/ask, does not submit any orders
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false,
				Keys.INITIAL_LADDER_MEAN, 0,
				Keys.INITIAL_LADDER_RANGE, 0));

		// Check activities inserted (none, other than reentry)
		mm.agentStrategy(time);
		assertTrue(mm.activeOrders.isEmpty());
		
		mm.createOrderLadder(null,  null);
		assertTrue(mm.activeOrders.isEmpty());
	}

	@Test
	public void submitOrderLadderBasic() {
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, false));
		mm.stepSize = 5;

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		exec.executeActivity(new SubmitOrder(agent1, market, SELL, new Price(25), 1));
		
		mm.submitOrderLadder(new Price(30), new Price(40), new Price(50), new Price(60));
		assertEquals("Incorrect number of orders", 6, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 50 || price == 55 || price == 60);
			else
				assertTrue(price == 40 || price == 35 || price == 30);
		}
	}
	
	@Test
	public void submitOrderLadder() {
		Market cda = new CDAMarket(exec, sip, new Random(), EntityProperties.empty()); 
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, cda, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, false));
		mm.stepSize = 5;

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, cda);
		exec.executeActivity(new SubmitOrder(agent1, cda, SELL, new Price(25), 1));
		
		mm.submitOrderLadder(new Price(30), new Price(40), new Price(50), new Price(60));
		
		// Order at $40 (center buy rung) should transact first
		assertEquals("Incorrect number of orders", 5, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 50 || price == 55 || price == 60);
			else {
				assertTrue(price == 35 || price == 30);
				assertTrue(price != 40);
			}
		}
	}

	@Test
	public void createOrderLadderNullTest() {
		// if either ladder bid or ask is null, it needs to return
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, false,
				Keys.INITIAL_LADDER_MEAN, 0,
				Keys.INITIAL_LADDER_RANGE, 0));
		assertEquals(5, mm.stepSize);

		mm.createOrderLadder(null, new Price(50));
		assertTrue(mm.activeOrders.isEmpty());
	}

	@Test
	public void createOrderLadderTest() {
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, false));
		assertEquals(5, mm.stepSize);

		mm.createOrderLadder(new Price(40), new Price(50));
		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 50 || price == 55);
			else
				assertTrue(price == 40 || price == 35);
		}
	}


	@Test
	public void tickImprovement() {
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, true));
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		exec.executeActivity(new Clear(market));

		mm.createOrderLadder(new Price(40), new Price(50));
		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 51 || price == 56);
			else
				assertTrue(price == 39 || price == 34);
		}
	}

	@Test
	public void truncateLadder() {
		TimeStamp time = TimeStamp.ZERO;
		SIP sip = new SIP(exec, TimeStamp.create(10));
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, true));
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		exec.executeActivity(new Clear(market));
		
		// Updating NBBO quote (this will update immed, although SIP is delayed)
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(30), 1, new Price(38), 1, new DummyMarketTime(time, 1));
		exec.executeActivity(new ProcessQuote(sip, market2, q));

		mm.createOrderLadder(new Price(40), new Price(50));
		assertEquals("Incorrect number of orders", 3, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 51 || price == 56);
			else
				assertEquals(35, price);
		}
	}
	
	@Test
	public void truncateLadderFix() {
		TimeStamp time = TimeStamp.ZERO;
		SIP sip = new SIP(exec, TimeStamp.create(10));
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, true));
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		exec.executeActivity(new Clear(market));
		
		// Updating NBBO quote (this will update immed, although SIP is delayed)
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(30), 1, new Price(38), 1, new DummyMarketTime(time, 1));
		exec.executeActivity(new ProcessQuote(sip, market2, q));

		mm.createOrderLadder(new Price(40), new Price(50));
		assertEquals("Incorrect number of orders", 3, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 51 || price == 56);
			else
				assertEquals(35, price);
		}
	}
	
	@Test
	public void truncateLadderOnBuyRungFix() {
		TimeStamp time = TimeStamp.ZERO;

		SIP sip = new SIP(exec, TimeStamp.create(10));
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false));
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));

		// Updating NBBO quote (this will update immed, although SIP is delayed)
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(30), 1, new Price(35), 1, new DummyMarketTime(time, 1));
		exec.executeActivity(new ProcessQuote(sip, market2, q));

		mm.createOrderLadder(new Price(40), new Price(50));

		assertEquals("Incorrect number of orders", 2, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 49 || price == 54);
		}
	}
	
	@Test
	public void truncateLadderOnSellRungFix() {
		TimeStamp time = TimeStamp.ZERO;

		SIP sip = new SIP(exec, TimeStamp.create(10));
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false));
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));

		// Updating NBBO quote (this will update immed, although SIP is delayed)
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(55), 1, new Price(60), 1, new DummyMarketTime(time, 1));
		exec.executeActivity(new ProcessQuote(sip, market2, q));

		mm.createOrderLadder(new Price(40), new Price(50));

		assertEquals("Incorrect number of orders", 2, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 60 || price == 65);
		}
	}
	
	@Test
	public void truncateLadderOnBuyRung() {
		TimeStamp time = TimeStamp.ZERO;

		SIP sip = new SIP(exec, TimeStamp.create(10));
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false));
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));

		// Updating NBBO quote (this will update immed, although SIP is delayed)
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(30), 1, new Price(35), 1, new DummyMarketTime(time, 1));
		exec.executeActivity(new ProcessQuote(sip, market2, q));

		mm.createOrderLadder(new Price(40), new Price(50));

		assertEquals("Incorrect number of orders", 2, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 49 || price == 54);
		}
	}
	
	@Test
	public void truncateLadderOnSellRung() {
		TimeStamp time = TimeStamp.ZERO;

		SIP sip = new SIP(exec, TimeStamp.create(10));
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false));
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));

		// Updating NBBO quote (this will update immed, although SIP is delayed)
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(55), 1, new Price(60), 1, new DummyMarketTime(time, 1));
		exec.executeActivity(new ProcessQuote(sip, market2, q));

		mm.createOrderLadder(new Price(40), new Price(50));

		assertEquals("Incorrect number of orders", 2, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 60 || price == 65);
		}
	}
	
	@Test
	public void tickInside() {
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false));
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		
		mm.createOrderLadder(new Price(40), new Price(50));

		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 49 || price == 54);
			else
				assertTrue(price == 41 || price == 36);
		}
	}
	
	
	/**
	 * Creating ladder without bid/ask quote
	 */
	@Test
	public void initRandLadder() {
		SIP sip = new SIP(exec, TimeStamp.create(10));
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 100,
				Keys.INITIAL_LADDER_RANGE, 10));
		
		mm.createOrderLadder(null, null);
		
		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());
		int sellPrice = Price.INF.intValue(), buyPrice = 0;
		int sellPrice2 = sellPrice, buyPrice2 = buyPrice;
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) {
				sellPrice = Math.min(price, sellPrice);
			} else {
				buyPrice = Math.max(price, buyPrice);
			}
		}
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) {
				if (price != sellPrice) sellPrice2 = price;
			} else {
				if (price != buyPrice) buyPrice2 = price;
			}
		}
		int ladderCenter = (sellPrice + buyPrice) / 2; 
		assertTrue("ladder center " + ladderCenter + " is outside range",
				ladderCenter <= 110 && ladderCenter >= 90);
		assertEquals(ladderCenter + 5, sellPrice);
		assertEquals(ladderCenter - 5, buyPrice);
		assertEquals(sellPrice + 5, sellPrice2);
		assertEquals(buyPrice - 5, buyPrice2);
	}

	/**
	 * One side of ladder is undefined
	 */
	@Test
	public void oneSidedLadderBuy() {
		SIP sip = new SIP(exec, TimeStamp.create(10));
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 100,
				Keys.INITIAL_LADDER_RANGE, 10));
		
		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		
		mm.createOrderLadder(new Price(40), null);
		
		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());
		int sellPrice = Price.INF.intValue(), buyPrice = 0;
		int sellPrice2 = sellPrice, buyPrice2 = buyPrice;
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) {
				sellPrice = Math.min(price, sellPrice);
			} else {
				buyPrice = Math.max(price, buyPrice);
			}
		}
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) {
				if (price != sellPrice) sellPrice2 = price;
			} else {
				if (price != buyPrice) buyPrice2 = price;
			}
		}
		assertTrue(sellPrice >= 45 && sellPrice <= 50);
		assertEquals(41, buyPrice); // because tick improvement, outside quote
		assertEquals(sellPrice + 5, sellPrice2);
		assertEquals(buyPrice - 5, buyPrice2);
	}
	
	/**
	 * One side of ladder is undefined
	 */
	@Test
	public void oneSidedLadderSell() {
		SIP sip = new SIP(exec, TimeStamp.create(10));
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_OUTSIDE, false,
				Keys.INITIAL_LADDER_MEAN, 100,
				Keys.INITIAL_LADDER_RANGE, 10));
		
		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, SELL, new Price(50), 1));
		
		mm.createOrderLadder(null, new Price(50));
		
		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());
		int sellPrice = Price.INF.intValue(), buyPrice = 0;
		int sellPrice2 = sellPrice, buyPrice2 = buyPrice;
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) {
				sellPrice = Math.min(price, sellPrice);
			} else {
				buyPrice = Math.max(price, buyPrice);
			}
		}
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) {
				if (price != sellPrice) sellPrice2 = price;
			} else {
				if (price != buyPrice) buyPrice2 = price;
			}
		}
		assertTrue(buyPrice >= 40 && buyPrice <= 45);
		assertEquals(49, sellPrice); // because tick improvement, outside quote
		assertEquals(sellPrice + 5, sellPrice2);
		assertEquals(buyPrice - 5, buyPrice2);
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			initRandLadder();
			setup();
			oneSidedLadderBuy();
			setup();
			oneSidedLadderSell();
		}
	}
	 
	/**
	 * Must submit rungs in order such that inside rungs will transact first
	 * (setting truncate = false to test)
	 */
	@Test
	public void rungOrderTest() {
		// TODO
	}
	
	// TODO test truncation when quote latency (or using NBBO?)
}
