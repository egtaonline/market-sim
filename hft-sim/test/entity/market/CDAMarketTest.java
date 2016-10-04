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
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import activity.SubmitOrder;
import activity.WithdrawOrder;

import com.google.common.collect.Iterables;

import data.FundamentalValue;
import data.MockFundamental;
import entity.agent.MockBackgroundAgent;
import entity.infoproc.SIP;
import event.TimeStamp;

public class CDAMarketTest {

	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private SIP sip;
	private Market market;

	@BeforeClass
	public static void setupClass() throws IOException {
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "CDAMarketTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new CDAMarket(exec, sip, new Random(), TimeStamp.IMMEDIATE, 1);
	}

	@Test
	public void addBid() {
		TimeStamp time = TimeStamp.create(0);

		// Creating the agent
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding the bid
		market.submitOrder(agent, BUY, new Price(1), 1, time);

		Collection<Order> orders = market.orders;
		assertFalse(orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(new Price(1), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertEquals(BUY, order.getOrderType());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market, order.getMarket());
		
		// Check if market quote correct
		market.updateQuote(time);
		Quote q = market.quote;
		assertEquals("Incorrect ASK",  null,  q.ask );
		assertEquals("Incorrect BID", new Price(1),  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );
	}

	@Test
	public void addAsk() {
		TimeStamp time = TimeStamp.create(0);
		
		//Creating the agent
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market);
		
		// Creating and adding the bid
		market.submitOrder(agent, SELL, new Price(1), 1, time);

		Collection<Order> orders = market.orders;
		assertFalse(orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(new Price(1), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertEquals(SELL, order.getOrderType());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market, order.getMarket());
		
		// Check if market quote correct
		market.updateQuote(time);
		Quote q = market.quote;
		assertEquals("Incorrect ASK", new Price(1),  q.ask);
		assertEquals("Incorrect BID",  null,  q.bid );
		assertEquals("Incorrect ASK quantity",  1,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
	}
	
	
//	public void selfTrade() {
//		// Verify that agent can't trade with itself
//		TimeStamp time0 = TimeStamp.ZERO;
//		
//		MockAgent agent1 = new MockAgent(exec, fundamental, sip, market);
//		
//		market.submitOrder(agent1, new Price(100),-1, time0);
//		market.submitOrder(agent1, new Price(150), 1, time0);
//		market.clear(time0);
//		
//		assertEquals( 0, market.getTransactions().size() );
//	}

	@Test
	public void basicEqualClear() {
		TimeStamp time = TimeStamp.create(0);
		
		//Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		market.submitOrder(agent1, BUY, new Price(100), 1, time);
		market.submitOrder(agent2, SELL, new Price(100), 1, time);

		// Testing the market for the correct transaction
		market.clear(time);
		assertEquals( 1, market.getTransactions().size() );
		for (Transaction tr : market.getTransactions()) {
			assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
			assertEquals("Incorrect Seller", agent2, tr.getSeller());
			assertEquals("Incorrect Price", new Price(100), tr.getPrice());
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		}
		Quote quote = market.quote;
		assertEquals(null, quote.getAskPrice());
		assertEquals(null, quote.getBidPrice());
	}

	@Test
	public void basicOverlapClear() {
		TimeStamp time = TimeStamp.create(0);
		TimeStamp time2 = TimeStamp.create(1);
		
		//Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		
		// Creating and adding bids
		market.submitOrder(agent1, BUY, new Price(200), 1, time);
		market.submitOrder(agent2, SELL, new Price(50), 1, time2);

		// Testing the market for the correct transaction
		market.clear(time2);
		assertEquals( 1, market.getTransactions().size() );
		for (Transaction tr : market.getTransactions()) {
			assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
			assertEquals("Incorrect Seller", agent2, tr.getSeller());
			assertEquals("Incorrect Price", new Price(200), tr.getPrice());
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
		// Also check that submitOrder returns a single immediate Clear activity
		market.submitOrder(agent1, BUY, new Price(150), 1, time);
		market.submitOrder(agent2, BUY, new Price(100), 1, time);
		market.submitOrder(agent3, SELL, new Price(175), 1, time);
		market.submitOrder(agent4, SELL, new Price(125), 1, time);
		market.clear(time);

		// Testing the market for the correct transactions
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(150), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Creating and adding bids (existing orders at buy@100, sell@175)
		market.submitOrder(agent1, BUY, new Price(150), 1, time.plus(TimeStamp.create(1)));
		market.submitOrder(agent4, SELL, new Price(75), 1, time.plus(TimeStamp.create(2)));
		market.clear(time.plus(TimeStamp.create(2)));
		
		// Testing the market for the correct transactions
		// agent 1 and 4 still trade even though buy@100 also crosses sell@75 
		assertEquals( 2, market.getTransactions().size() );
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(150), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
	}
	
	@Test
	public void multiOverlapClear() {
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent3 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent4 = new MockBackgroundAgent(exec, fundamental, sip, market);
		
		// Creating and adding bids
		// Added for-loop so market clear will happen appropriately
		// Note: A clear should be inserted after EVERY order submitted
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(150), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(100), 1));
		exec.executeActivity(new SubmitOrder(agent3, market, BUY, new Price(175), 1));
		exec.executeActivity(new SubmitOrder(agent4, market, SELL, new Price(125), 1));
		
		// Testing the market for the correct transactions
		assertEquals( 2, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
		assertEquals("Incorrect Seller", agent2, tr.getSeller());
		assertEquals("Incorrect Price", new Price(150), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Buyer", agent3, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(175), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
	}
	
	
	@Test
	public void extraTest() {
		for(int i=0; i < 100; i++) {
			setup();
			multiBidSingleClear();
			setup();
			multiOverlapClear();
			setup();
			priceTimeTest();
		}
	}
	
	
	/**
	 * Test quantities of partially transacted orders. 
	 */
	@Test
	public void partialQuantity() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);
		
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		
		market.submitOrder(agent1, SELL, new Price(100), 2, time);
		market.submitOrder(agent2, BUY, new Price(150), 5, time1);
		market.clear(time1);
		
		// Check that two units transact
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent1, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 2, tr.getQuantity());

		// Check that post-trade BID is correct (3 buy units at 150)
		market.updateQuote(time1);
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
		market.submitOrder(agent2, BUY, new Price(155), 2, time1);
		market.clear(time1);
		assertEquals( 2, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Price", new Price(140), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Price", new Price(150), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
	}
	
	
	/**
	 * Tests order withdrawals. Via market withdrawals.
	 */
	@Test
	public void basicWithdraw() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);

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
		// Test that withdraw create SendToIP activities (updates quotes)
		market.withdrawOrder(toWithdraw, time1);
		
		// Check that quotes are correct (no bid, no ask)
		q = market.quote;
		assertEquals("Incorrect ASK", null,  q.ask );
		assertEquals("Incorrect BID", null,  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
		
		// Check that no transaction, because agent1 withdrew its order
		market.submitOrder(agent2, BUY, new Price(125), 1, time1);
		market.clear(time1);
		assertEquals( 0, market.getTransactions().size() );

		// Check that quotes are correct (bid @125)
		q = market.quote;
		assertEquals("Incorrect ASK", null,  q.ask );
		assertEquals("Incorrect BID", new Price(125),  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );
	}
	
	/**
	 * Also tests that CDAMarket.withdrawOrder will update quotes.
	 */
	@Test
	public void basicWithdrawClear() {
		TimeStamp time1 = TimeStamp.create(1);
		TimeStamp time2 = TimeStamp.create(2);

		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		
		market.submitOrder(agent2, BUY, new Price(105), 1, time1);
		market.submitOrder(agent2, BUY, new Price(110), 1, time1);
		market.clear(time1);
		
		// Check that quotes are correct (bid @110, no ask)
		Quote q = market.quote;
		assertEquals("Incorrect ASK", null,  q.ask );
		assertEquals("Incorrect BID", new Price(110),  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );
				
		Collection<Order> orders = agent2.getOrders();
		Order toWithdraw = null;
		for (Order o : orders)
			if (o.getPrice().equals(new Price(105))) toWithdraw = o;
		market.withdrawOrder(toWithdraw, time1);
		
		// Check that quotes are correct (only a buy order @110)
		q = market.quote;
		assertEquals("Incorrect ASK", null,  q.ask );
		assertEquals("Incorrect BID", new Price(110),  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );
		
		// Check that it transacts at 110, price of order that was not withdrawn
		market.submitOrder(agent1, SELL, new Price(100), 1, time2);
		market.clear(time2);
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Price", new Price(110), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		q = market.quote;
		assertEquals("Incorrect ASK", null,  q.ask );
		assertEquals("Incorrect BID", null,  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
	}
	
	
	/**
	 * Uses EventManager for testing.
	 */
	@Test
	public void multiQuantityWithdraw() {
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		
		exec.executeActivity(new SubmitOrder(agent1, market, SELL, new Price(150), 1));
		exec.executeActivity(new SubmitOrder(agent1,market, SELL, new Price(140), 2));
		
		// Check that quotes are correct (ask @140 at qty=2, no bid)
		Quote q = market.quote;
		assertEquals("Incorrect ASK", new Price(140), q.ask);
		assertEquals("Incorrect BID", null, q.bid);
		assertEquals("Incorrect ASK quantity",  2,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
		
		Collection<Order> orders = agent1.getOrders();
		Order toWithdraw = null;
		for (Order o : orders)
			if (o.getPrice().equals(new Price(140))) toWithdraw = o;
		exec.executeActivity(new WithdrawOrder(toWithdraw, 1));
		
		// Check that quotes are correct (ask @140 at qty=1, no bid)
		q = market.quote;
		assertEquals("Incorrect ASK", new Price(140), q.ask);
		assertEquals("Incorrect BID", null, q.bid);
		assertEquals("Incorrect ASK quantity",  1,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );

		exec.executeUntil(TimeStamp.create(1));
		// Both agents' sell orders should transact b/c partial quantity withdrawn
		exec.executeActivity(new SubmitOrder(agent2, market, BUY, new Price(155), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, BUY, new Price(155), 2));
		assertEquals( 2, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Price", new Price(140), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Price", new Price(150), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		q = market.quote;
		assertEquals("Incorrect ASK", null, q.ask );
		assertEquals("Incorrect BID", new Price(155), q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );
	}
	
	
	/**
	 * Test clearing when there are ties in price. Should always match the 
	 * order that arrived first if there is a tie in price.
	 */
	@Test
	public void priceTimeTest() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);
		TimeStamp time2 = TimeStamp.create(2);

		MockBackgroundAgent agent0 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent3 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent4 = new MockBackgroundAgent(exec, fundamental, sip, market);
		
		market.submitOrder(agent1, SELL, new Price(100), 1, time0);
		market.submitOrder(agent2, SELL, new Price(100), 1, time1);
		market.submitOrder(agent3, BUY, new Price(150), 1, time1);
		market.clear(time1);
		
		// Check that earlier agent (agent1) is trading with agent3
		// Testing the market for the correct transactions
		assertEquals(1, market.getTransactions().size());
		Transaction tr = market.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent3, tr.getBuyer());
		assertEquals("Incorrect Seller", agent1, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());

		market.submitOrder(agent1, SELL, new Price(100), 1, time1);
		market.submitOrder(agent3, SELL, new Price(100), 1, time2);
		market.submitOrder(agent4, SELL, new Price(100), 1, time2);
		market.clear(time2); // would be inserted onto Q, but hard-coded here
		market.submitOrder(agent0, BUY, new Price(125), 1, time2);
		market.clear(time2);
		
		// Check that the first submitted -1@100 transacts (from agent2)
		assertEquals( 2, market.getTransactions().size() );
		tr = market.getTransactions().get(1);
		assertEquals("Incorrect Buyer", agent0,  tr.getBuyer());
		assertEquals("Incorrect Seller", agent2,  tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Let's try populating the market with random orders 
		// agent 1's order -1@100 at time1 remains
		// agent 3's order -1@100 at time2 remains
		// agent 4's order -1@100 at time2 remains
		market.submitOrder(agent0, SELL, new Price(95), 1, time2);
		market.submitOrder(agent0, SELL, new Price(100), 1, time2);
		market.submitOrder(agent0, SELL, new Price(110), 1, time2);
		market.submitOrder(agent0, SELL, new Price(115), 1, time2);
		market.submitOrder(agent0, BUY, new Price(90), 1, time2);
		market.submitOrder(agent0, BUY, new Price(85), 1, time2);
		market.submitOrder(agent0, BUY, new Price(80), 1, time2);
		market.clear(time2);
		assertEquals(2, market.getTransactions().size()); // no change

		// Check basic overlap - between agent0 and agent2
		market.submitOrder(agent2, BUY, new Price(125), 1, time2);
		market.clear(time2);
		assertEquals(3, market.getTransactions().size());
		tr = market.getTransactions().get(2);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent0, tr.getSeller());
		assertEquals("Incorrect Price", new Price(95), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Check that earliest time (agent1) at price 100 trades
		// Check that the transaction was between agent1 (earliest @100) and agent2
		market.submitOrder(agent2, BUY, new Price(105), 1, time2);
		market.clear(time2);
		assertEquals(4, market.getTransactions().size());
		tr = market.getTransactions().get(3);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent1, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Check that next earliest (agent3) although same price & time trades (vs. agent4 or 0)
		// Check that the transaction was between agent3 (first submitted) and agent2
		market.submitOrder(agent2, BUY, new Price(105), 1, time2);
		market.clear(time2);
		assertEquals(5, market.getTransactions().size());
		tr = market.getTransactions().get(4);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent3, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Check that next earliest (agent4) although same price & time trades (vs. agent0)
		market.submitOrder(agent2, BUY, new Price(105), 1, time2);
		market.clear(time2);
		assertEquals(6, market.getTransactions().size());
		tr = market.getTransactions().get(5);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Check that final order (agent0) at -1@100 trades with agent2
		market.submitOrder(agent2, BUY, new Price(105), 1, time2);
		market.clear(time2);
		assertEquals(7, market.getTransactions().size());
		tr = market.getTransactions().get(6);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent0, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
	}
	
	@Test
	public void lackOfLatencyTest() {
		Quote quote;

		// Submit Order doesn't happen
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market);
		exec.scheduleActivities(TimeStamp.ZERO, new SubmitOrder(agent, market, SELL, new Price(100), 1));
		
		quote = market.getQuoteProcessor().getQuote();
		assertEquals("Updated Ask price too early", null, quote.getAskPrice());
		assertEquals("Updated Ask quantity too early", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid price initialization", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity initialization", 0, quote.getBidQuantity());
		
		// This will execute all of the remaining activities
		exec.executeUntil(ZERO);
		
		quote = market.getQuoteProcessor().getQuote();
		assertEquals("Didn't update Ask price", new Price(100), quote.getAskPrice());
		assertEquals("Didn't update Ask quantity", 1, quote.getAskQuantity());
		assertEquals("Changed Bid price unnecessarily", null, quote.getBidPrice());
		assertEquals("Changed Bid quantity unnecessarily", 0, quote.getBidQuantity());
	}
	
	@Test
	public void latencyTest() {
		Quote quote;
		CDAMarket market = new CDAMarket(exec, sip, new Random(), TimeStamp.create(100), 1);

		// Test that before Time 100 nothing has been updated
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market);
		exec.executeActivity(new SubmitOrder(agent, market, SELL, new Price(100), 1));
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
}
