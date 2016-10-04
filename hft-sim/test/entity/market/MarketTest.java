package entity.market;

import static event.TimeStamp.ZERO;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import activity.Clear;
import activity.SendToQP;
import activity.SendToTP;
import activity.SubmitOrder;

import com.google.common.collect.ImmutableList;

import data.FundamentalValue;
import data.MockFundamental;
import entity.agent.Agent;
import entity.agent.MockBackgroundAgent;
import entity.infoproc.QuoteProcessor;
import entity.infoproc.SIP;
import entity.infoproc.TransactionProcessor;
import event.TimeStamp;

public class MarketTest {

	// TODO Add test for individual agent transaction latency, maybe in AgentTest?

	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;

	@BeforeClass
	public static void setupClass() throws IOException {
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "MarketTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
	}

	@Test
	public void addBid() {
		TimeStamp time = TimeStamp.create(0);
		Agent agent = new MockBackgroundAgent(exec, fundamental, sip, market);
		market.submitOrder(agent, BUY, new Price(1), 1, time);

		Collection<Order> orders = market.orders;
		assertFalse(orders.isEmpty());
		Order order = orders.iterator().next();
		assertEquals(new Price(1), order.getPrice());
		assertEquals(BUY, order.getOrderType());
		assertEquals(1, order.getQuantity());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market, order.getMarket());
		Quote quote = market.quote; 
		assertEquals(null, quote.ask);
		assertEquals(0, quote.askQuantity);
		assertEquals(new Price(1), quote.bid);
		assertEquals(1, quote.bidQuantity);
	}

	@Test
	public void addAsk() {
		TimeStamp time = TimeStamp.ZERO;
		Agent agent = new MockBackgroundAgent(exec, fundamental, sip, market);
		market.submitOrder(agent, SELL, new Price(1), 1, time);

		Collection<Order> orders = market.orders;
		assertFalse(orders.isEmpty());
		Order order = orders.iterator().next();
		assertEquals(new Price(1), order.getPrice());
		assertEquals(SELL, order.getOrderType());
		assertEquals(1, order.getQuantity());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market, order.getMarket());
		Quote quote = market.quote; 
		assertEquals(new Price(1), quote.ask);
		assertEquals(1, quote.askQuantity);
		assertEquals(null, quote.bid);
		assertEquals(0, quote.bidQuantity);
	}

	@Test
	public void basicEqualClear() {
		TimeStamp time = TimeStamp.ZERO;

		//Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		market.submitOrder(agent1, BUY, new Price(100), 1, time);
		market.submitOrder(agent2, SELL, new Price(100), 1, time);

		// Testing the market for the correct transaction
		market.clear(time);
		assertEquals(1, market.getTransactions().size());
		for (Transaction tr : market.getTransactions()) {
			assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
			assertEquals("Incorrect Seller", agent2, tr.getSeller());
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		}

		Quote quote = market.quote;
		assertEquals(null, quote.getAskPrice());
		assertEquals(null, quote.getBidPrice());
	}

	@Test
	public void basicOverlapClear() {
		TimeStamp time = TimeStamp.ZERO;

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		market.submitOrder(agent1, BUY, new Price(200), 1, time);
		market.submitOrder(agent2, SELL, new Price(50), 1, time);

		// Testing the market for the correct transaction
		market.clear(time);
		assertEquals(1, market.getTransactions().size());
		for (Transaction tr : market.getTransactions()) {
			assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
			assertEquals("Incorrect Seller", agent2, tr.getSeller());
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		}
		Quote quote = market.quote;
		assertEquals(null, quote.getAskPrice());
		assertEquals(null, quote.getBidPrice());
	}

	@Test
	public void multiBidSingleClear() {
		TimeStamp time = TimeStamp.ZERO;

		//Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent3 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent4 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		market.submitOrder(agent1, BUY, new Price(150), 1, time);
		market.submitOrder(agent2, BUY, new Price(100), 1, time);
		market.submitOrder(agent3, SELL, new Price(180), 1, time);
		market.submitOrder(agent4, SELL, new Price(120), 1, time);
		market.clear(time);
		;
		// Testing the market for the correct transactions
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());

		Quote quote = market.quote;
		assertEquals(new Price(100), quote.getBidPrice());
		assertEquals(new Price(180), quote.getAskPrice());
		assertEquals(1, quote.getBidQuantity());
		assertEquals(1, quote.getAskQuantity());
	}

	@Test
	public void multiOverlapClear() {
		TimeStamp time = TimeStamp.ZERO;

		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent3 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent4 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		market.submitOrder(agent1, BUY, new Price(150), 1, time);
		market.submitOrder(agent2, SELL, new Price(100), 1, time);
		market.submitOrder(agent3, BUY, new Price(200), 1, time);
		market.submitOrder(agent4, SELL, new Price(130), 1, time);
		assertEquals(0, market.getTransactions().size());

		// Testing the market for the correct transactions
		market.clear(time);
		assertEquals( 2, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent3, tr.getBuyer());
		assertEquals("Incorrect Seller", agent2, tr.getSeller());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
	}

	/**
	 * Scenario with two possible matches, but only one pair transacts at the
	 * uniform price.
	 */
	@Test
	public void partialOverlapClear() {
		TimeStamp time = TimeStamp.ZERO;

		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent3 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent4 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		market.submitOrder(agent3, BUY, new Price(200), 1, time);
		market.submitOrder(agent4, SELL, new Price(130), 1, time);
		market.submitOrder(agent1, BUY, new Price(110), 1, time);
		market.submitOrder(agent2, SELL, new Price(100), 1, time);
		assertEquals(0, market.getTransactions().size());

		// Testing the market for the correct transactions
		market.clear(time);
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent3, tr.getBuyer());
		assertEquals("Incorrect Seller", agent2, tr.getSeller());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());

		Quote quote = market.quote;
		assertEquals(new Price(110), quote.getBidPrice());
		assertEquals(new Price(130), quote.getAskPrice());
		assertEquals(1, quote.getBidQuantity());
		assertEquals(1, quote.getAskQuantity());
	}

	@Test
	public void extraTest() {
		for(int i=0; i < 100; i++) {
			setup();
			multiBidSingleClear();
			setup();
			multiOverlapClear();
			setup();
			partialOverlapClear();
		}
	}


	/**
	 * Test quantities of partially transacted orders. 
	 */
	@Test
	public void partialQuantity() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time2 = TimeStamp.create(1);

		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		market.submitOrder(agent1, SELL, new Price(100), 2, time);
		market.submitOrder(agent2, BUY, new Price(150), 5, time2);
		market.clear(time2);

		// Check that two units transact
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent1, tr.getSeller());
		assertEquals("Incorrect Quantity", 2, tr.getQuantity());

		// Check that post-trade BID is correct (3 buy units at 150)
		market.updateQuote(time2);
		Quote q = market.quote;
		assertEquals("Incorrect ASK", null, q.ask);
		assertEquals("Incorrect BID", new Price(150), q.bid);
		assertEquals("Incorrect ASK quantity", 0, q.askQuantity);
		assertEquals("Incorrect BID quantity", 3, q.bidQuantity);
	}

	@Test
	public void multiQuantity() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);

		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		market.submitOrder(agent1, SELL, new Price(150), 1, time0);
		market.submitOrder(agent1, SELL, new Price(140), 1, time0);
		market.clear(time0);

		// Both agents' sell orders should transact b/c partial quantity withdrawn
		market.submitOrder(agent2, BUY, new Price(160), 2, time1);
		market.clear(time1);
		assertEquals( 2, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());

		Quote q = market.quote;
		assertEquals("Incorrect ASK", null, q.ask);
		assertEquals("Incorrect BID", null, q.bid);
		assertEquals("Incorrect ASK quantity", 0, q.askQuantity);
		assertEquals("Incorrect BID quantity", 0, q.bidQuantity);
	}

	@Test
	public void basicWithdraw() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);
		TimeStamp time2 = TimeStamp.create(2);

		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		market.submitOrder(agent1, SELL, new Price(100), 1, time0);
		market.clear(time0); 
		// Check that quotes are correct (no bid, ask @100)
		Quote q = market.quote;
		assertEquals("Incorrect ASK", new Price(100),  q.ask );
		assertEquals("Incorrect BID", null,  q.bid);
		assertEquals("Incorrect ASK quantity",  1,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );

		// Withdraw order
		Collection<Order> orders = agent1.getOrders();
		Order toWithdraw = orders.iterator().next(); // get first (& only) order
		market.withdrawOrder(toWithdraw, time0);
		market.clear(time0);

		// Check that quotes are correct (no bid, no ask)
		q = market.quote;
		assertEquals("Incorrect ASK", null,  q.ask );
		assertEquals("Incorrect BID", null,  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );

		// Check that no transaction, because agent1 withdrew its order
		market.submitOrder(agent2, BUY, new Price(125), 1, time1);
		assertEquals( 0, market.getTransactions().size() );

		market.submitOrder(agent2, BUY, new Price(115), 1, time1);
		orders = agent2.getOrders();
		toWithdraw = null;
		for (Order o : orders)
			if (o.getPrice().equals(new Price(125))) toWithdraw = o;
		market.withdrawOrder(toWithdraw, time1);
		market.clear(time1);

		// Check that it transacts with order (@115) that was not withdrawn
		market.submitOrder(agent1, SELL, new Price(105), 1, time2);
		market.clear(time2);
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());

		q = market.quote;
		assertEquals("Incorrect ASK", null,  q.ask );
		assertEquals("Incorrect BID", null,  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
	}

	@Test
	public void multiQuantityWithdraw() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);

		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		market.submitOrder(agent1, SELL, new Price(150), 1, time0);
		market.submitOrder(agent1, SELL, new Price(140), 2, time0);
		market.clear(time0);
		Collection<Order> orders = agent1.getOrders();
		Order toWithdraw = null;
		for (Order o : orders)
			if (o.getPrice().equals(new Price(140))) toWithdraw = o;
		market.withdrawOrder(toWithdraw, 1, time0);
		market.clear(time0);

		// Both agents' sell orders should transact b/c partial quantity withdrawn
		market.submitOrder(agent2, BUY, new Price(160), 1, time1);
		market.submitOrder(agent2, BUY, new Price(160), 2, time1);
		market.clear(time1);
		assertEquals( 2, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());

		Quote q = market.quote;
		assertEquals("Incorrect ASK", null, q.ask );
		assertEquals("Incorrect BID", new Price(160), q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );
	}


	@Test
	public void lackOfLatencyTest() {
		// With zero latency, appended activities all execute immediately
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market);
		market.submitOrder(agent, SELL, new Price(100), 1, TimeStamp.ZERO);

		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals("Didn't update Ask price", new Price(100), quote.getAskPrice());
		assertEquals("Didn't update Ask quantity", 1, quote.getAskQuantity());
		assertEquals("Changed Bid price unnecessarily", null, quote.getBidPrice());
		assertEquals("Changed Bid quantity unnecessarily", 0, quote.getBidQuantity());
	}

	@Test
	public void latencyTest() {
		Quote quote;
		MockMarket market = new MockMarket(exec, sip, TimeStamp.create(100));

		// Test that before Time 100 nothing has been updated
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market);
		exec.scheduleActivity(TimeStamp.ZERO, new SubmitOrder(agent, market, SELL, new Price(100), 1));
		exec.executeUntil(TimeStamp.create(99));

		quote = market.getQuoteProcessor().getQuote();
		assertEquals("Updated Ask price too early", null, quote.getAskPrice());
		assertEquals("Updated Ask quantity too early", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid price initialization", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity initialization", 0, quote.getBidQuantity());

		// Test that after 100 they did get updated
		exec.executeUntil(TimeStamp.create(100));

		quote = market.getQuoteProcessor().getQuote();
		assertEquals("Didn't update Ask price", new Price(100), quote.getAskPrice());
		assertEquals("Didn't update Ask quantity", 1, quote.getAskQuantity());
		assertEquals("Changed Bid price unnecessarily", null, quote.getBidPrice());
		assertEquals("Changed Bid quantity unnecessarily", 0, quote.getBidQuantity());
	}

	@Test
	public void marketTime() {
		// verify that market time is always unique, despite number of submit/withdraw orders
		Set<Long> marketTimes = new TreeSet<Long>();

		TimeStamp time0 = TimeStamp.ZERO;

		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		market.submitOrder(agent1, SELL, new Price(100), 1, time0);
		assertFalse(marketTimes.contains(market.marketTime));
		marketTimes.add(market.marketTime);
		market.clear(time0);

		// Withdraw order
		Collection<Order> orders = agent1.getOrders();
		Order toWithdraw = orders.iterator().next(); // get first (& only) order
		market.withdrawOrder(toWithdraw, time0);
		market.clear(time0);

		// No transaction, because agent1 withdrew its order
		market.submitOrder(agent2, BUY, new Price(125), 1, time0);
		market.clear(time0);
		assertEquals( 0, market.getTransactions().size() );
		assertFalse(marketTimes.contains(market.marketTime));
		marketTimes.add(market.marketTime);

		// Submit order, which should transact
		market.submitOrder(agent1, SELL, new Price(100), 1, time0);
		market.clear(time0);
		assertEquals( 1, market.getTransactions().size() );
		assertFalse(marketTimes.contains(market.marketTime));
		marketTimes.add(market.marketTime);

		market.submitOrder(agent2, BUY, new Price(115), 1, time0);
		market.clear(time0);
		assertFalse(marketTimes.contains(market.marketTime));
		marketTimes.add(market.marketTime);

		// Submit order, which should transact
		market.submitOrder(agent1, SELL, new Price(105), 1, time0);
		market.clear(time0);
		assertFalse(marketTimes.contains(market.marketTime));
		marketTimes.add(market.marketTime);
		assertEquals( 2, market.getTransactions().size() );

		// One market time added for each submit order
		assertEquals(5, marketTimes.size());
	}

	@Test
	public void updateQPNoDelay() {
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market);
		market.submitOrder(agent, BUY, new Price(100), 1, TimeStamp.ZERO);

		QuoteProcessor qp = market.getQuoteProcessor();
		Quote q = market.getQuoteProcessor().getQuote();

		// Update QP
		q = new Quote(market, new Price(80), 1, new Price(100), 1, TimeStamp.ZERO);
		exec.executeActivity(new SendToQP(market, q, qp));
		// Verify quote
		q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", new Price(100), q.ask );
		assertEquals("Incorrect BID", new Price(80), q.bid);
		assertEquals("Incorrect ASK quantity",  1,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );
	}

	@Test
	public void updateQPDelay() {
		Quote q;
		MockMarket market = new MockMarket(exec, sip, TimeStamp.create(100));
		QuoteProcessor qp = market.getQuoteProcessor();

		// Test that before Time 100 nothing has been updated
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market);
		exec.executeActivity(new SubmitOrder(agent, market, SELL, new Price(100), 1));
		exec.executeUntil(TimeStamp.create(99));

		q = market.getQuoteProcessor().getQuote();
		assertEquals("Updated Ask price too early", null, q.getAskPrice());
		assertEquals("Updated Ask quantity too early", 0, q.getAskQuantity());
		assertEquals("Incorrect Bid price initialization", null, q.getBidPrice());
		assertEquals("Incorrect Bid quantity initialization", 0, q.getBidQuantity());

		// Update QP
		exec.executeUntil(TimeStamp.create(100));
		q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", new Price(100), q.ask );
		assertEquals("Incorrect BID", null, q.bid);
		assertEquals("Incorrect ASK quantity",  1,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
		
		// Add new quote
		q = new Quote(market, new Price(80), 1, new Price(100), 1, TimeStamp.create(1));
		exec.executeActivity(new SendToQP(market, q, qp)); // This happens at time 100
		
		// Update QP
		// Test that after 101 new quote did get updated
		exec.executeUntil(TimeStamp.create(200));
		q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", new Price(100), q.ask );
		assertEquals("Incorrect BID", new Price(80), q.bid);
		assertEquals("Incorrect ASK quantity",  1,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );

	}

	@Test
	public void updateTPNoDelay() {
		TransactionProcessor tp = market.getTransactionProcessor();

		// Creating dummy agents & transaction list
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(150), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(140), 1));
		exec.executeActivity(new Clear(market));

		// Verify that transactions have updated
		tp.processTransactions(market, ImmutableList.<Transaction> of(), ZERO);
		List<Transaction> trans = tp.getTransactions();
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());

		// Update TP
		exec.executeActivity(new SendToTP(market, trans, tp));
		// Verify that transactions have updated
		trans = tp.getTransactions();
		assertEquals("Incorrect number of transactions", 2, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(1).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(1).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(1).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(1).getSeller());
	}

	@Test
	public void updateTPDelay() {
		Market market = new MockMarket(exec, sip, TimeStamp.create(100));
		TransactionProcessor tp = market.getTransactionProcessor();
		
		// Creating dummy agents & transaction list
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		market.submitOrder(agent1, BUY, new Price(150), 1, ZERO);
		market.submitOrder(agent2, SELL, new Price(140), 1, ZERO);
		market.clear(ZERO);

		// Verify that transactions have not updated yet
		List<Transaction> trans = market.getTransactions();
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals(0, tp.getTransactions().size());
		
		// Test that after 100 new transaction did get updated
		exec.executeUntil(TimeStamp.create(100));
		trans = tp.getTransactions();
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());
	}

}
