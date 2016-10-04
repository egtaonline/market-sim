package entity.market;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import systemmanager.Keys;
import activity.Clear;
import activity.SubmitOrder;

import com.google.common.collect.Iterables;

import data.FundamentalValue;
import data.MarketProperties;
import data.MockFundamental;
import entity.agent.MockBackgroundAgent;
import entity.infoproc.SIP;
import event.TimeStamp;

/**
 * Note that CallMarket initial Clear activities are inserted by the
 * SystemManager.executeEvents method.
 * 
 * @author ewah
 */
public class CallMarketTest {

	private static final TimeStamp one = TimeStamp.create(1);
	
	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private SIP sip;
	private Market market1;
	private Market market2;
	private TimeStamp clearFreq100 = TimeStamp.create(100);

	@BeforeClass
	public static void setupClass() throws IOException {
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "CallMarketTest.log"));
	}
	
	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		// no delay from SIP + clears every 100
		market1 = new CallMarket(exec, sip, new Random(), TimeStamp.IMMEDIATE, 1, 0.5,
				clearFreq100);
		// no delay from SIP + clears every 100 with pricing policy=1
		market2 = new CallMarket(exec, sip, new Random(), TimeStamp.IMMEDIATE, 1, 1, 
				clearFreq100);		
	}

	@Test
	public void addBid() {
		TimeStamp time = TimeStamp.create(0);

		// Creating the agent
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market1);

		// Creating and adding the bid
		market1.submitOrder(agent, BUY, new Price(1), 1, time);

		Collection<Order> orders = market1.orders;
		assertFalse(orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(new Price(1), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertEquals(BUY, order.getOrderType());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market1, order.getMarket());
		
		// Check if market quote correct
		market1.updateQuote(time);
		Quote q = market1.quote;
		assertEquals("Incorrect ASK",  null,  q.ask );
		assertEquals("Incorrect BID", new Price(1),  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );
	}

	@Test
	public void addAsk() {
		TimeStamp time = TimeStamp.create(0);
		
		//Creating the agent
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		// Creating and adding the ask
		market1.submitOrder(agent, SELL, new Price(1), 1, time);

		Collection<Order> orders = market1.orders;
		assertFalse(orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(new Price(1), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertEquals(SELL, order.getOrderType());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market1, order.getMarket());
		
		// Check if market quote correct
		market1.updateQuote(time);
		Quote q = market1.quote;
		assertEquals("Incorrect ASK", new Price(1), q.ask);
		assertEquals("Incorrect BID", null, q.bid );
		assertEquals("Incorrect ASK quantity",  1,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
	}

	
	@Test
	public void basicEqualClear() {
		TimeStamp time = TimeStamp.create(0);
		
		//Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		// Creating and adding bids
		market1.submitOrder(agent1, BUY, new Price(100), 1, time);
		market1.submitOrder(agent2, SELL, new Price(100), 1, time);

		// Testing the market for the correct transaction
		market1.clear(time);
		assertEquals( 1, market1.getTransactions().size() );
		for (Transaction tr : market1.getTransactions()) {
			assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
			assertEquals("Incorrect Seller", agent2, tr.getSeller());
			assertEquals("Incorrect Price", new Price(100), tr.getPrice());
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		}
		Quote quote = market1.quote;
		assertEquals(null, quote.getAskPrice());
		assertEquals(null, quote.getBidPrice());
	}

	@Test
	public void basicOverlapClear() {
		TimeStamp time = TimeStamp.create(0);
		TimeStamp time2 = TimeStamp.create(1);
		
		//Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		// Creating and adding bids
		market1.submitOrder(agent1, BUY, new Price(200), 1, time);
		market1.submitOrder(agent2, SELL, new Price(100), 1, time2);
		market2.submitOrder(agent1, BUY, new Price(200), 1, time);
		market2.submitOrder(agent2, SELL, new Price(100), 1, time2);
		
		// Testing market1 for the correct transaction
		market1.clear(time2);
		assertEquals( 1, market1.getTransactions().size() );
		for (Transaction tr : market1.getTransactions()) {
			assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
			assertEquals("Incorrect Seller", agent2, tr.getSeller());
			assertEquals("Incorrect Price", new Price(150), tr.getPrice());
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		}
		
		// Testing market2 for the correct transaction (@buyer price)
		market2.clear(time2);
		assertEquals( 1, market2.getTransactions().size() );
		for (Transaction tr : market2.getTransactions()) {
			assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
			assertEquals("Incorrect Seller", agent2, tr.getSeller());
			assertEquals("Incorrect Price", new Price(200), tr.getPrice());
			assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		}
		Quote quote = market1.quote;
		assertEquals(null, quote.getAskPrice());
		assertEquals(null, quote.getBidPrice());
	}

	@Test
	public void multiBidSingleClear() {
		TimeStamp time = TimeStamp.ZERO;
		
		//Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent3 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent4 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		// Creating and adding bids
		market1.submitOrder(agent1, BUY, new Price(150), 1, time);
		market1.submitOrder(agent2, BUY, new Price(100), 1, time);
		market1.submitOrder(agent3, SELL, new Price(180), 1, time);
		market1.submitOrder(agent4, SELL, new Price(120), 1, time);
		market1.clear(time);
		market2.submitOrder(agent1, BUY, new Price(150), 1, time);
		market2.submitOrder(agent2, BUY, new Price(100), 1, time);
		market2.submitOrder(agent3, SELL, new Price(180), 1, time);
		market2.submitOrder(agent4, SELL, new Price(120), 1, time);
		market2.clear(time);
		
		// Testing the market for the correct transactions
		assertEquals( 1, market1.getTransactions().size() );
		Transaction tr = market1.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(135), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Creating and adding bids (existing orders at buy@100, sell@175)
		market1.submitOrder(agent1, BUY, new Price(150), 1, time.plus(TimeStamp.create(1)));
		market1.submitOrder(agent4, SELL, new Price(50), 1, time.plus(TimeStamp.create(2)));
		market1.clear(time.plus(TimeStamp.create(2)));
		market2.submitOrder(agent1, BUY, new Price(150), 1, time.plus(TimeStamp.create(1)));
		market2.submitOrder(agent4, SELL, new Price(50), 1, time.plus(TimeStamp.create(2)));
		market2.clear(time.plus(TimeStamp.create(2)));
		
		// Testing the market for the correct transactions
		// agent 1 and 4 still trade even though buy@100 also crosses sell@75 
		assertEquals( 2, market1.getTransactions().size() );
		tr = market1.getTransactions().get(1);
		assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(100), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		// Testing the second market (different pricing policy)
		assertEquals( 2, market2.getTransactions().size() );
		for (Transaction t : market2.getTransactions()) {
			assertEquals("Incorrect Buyer", agent1, t.getBuyer());
			assertEquals("Incorrect Seller", agent4, t.getSeller());
			assertEquals("Incorrect Price", new Price(150), t.getPrice());
			assertEquals("Incorrect Quantity", 1, t.getQuantity());
		}
	}
	
	
	@Test
	public void multiOverlapClear() {
		TimeStamp time = TimeStamp.ZERO;
		
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent3 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent4 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		// Creating and adding bids (clears are not returned by submitOrder)
		market1.submitOrder(agent1, BUY, new Price(150), 1, time);
		market1.submitOrder(agent2, SELL, new Price(100), 1, time);
		market1.submitOrder(agent3, BUY, new Price(200), 1, time);
		market1.submitOrder(agent4, SELL, new Price(130), 1, time);
		assertEquals(0, market1.getTransactions().size());
		
		// Testing the market for the correct transactions (uniform price=140)
		market1.clear(time);
		assertEquals( 2, market1.getTransactions().size() );
		Transaction tr = market1.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent3, tr.getBuyer());
		assertEquals("Incorrect Seller", agent2, tr.getSeller());
		assertEquals("Incorrect Price", new Price(140), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market1.getTransactions().get(1);
		assertEquals("Incorrect Buyer", agent1, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(140), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
	}
	
	
	/**
	 * Scenario with two possible matches, but only one pair transacts at the
	 * uniform price.
	 */
	@Test
	public void partialOverlapClear() {
		TimeStamp time = TimeStamp.ZERO;
		
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent3 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent4 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		// Creating and adding bids
		market1.submitOrder(agent3, BUY, new Price(200), 1, time);
		market1.submitOrder(agent4, SELL, new Price(130), 1, time);
		market1.submitOrder(agent1, BUY, new Price(110), 1, time);
		market1.submitOrder(agent2, SELL, new Price(100), 1, time);
		assertEquals(0, market1.getTransactions().size());
		
		// Testing the market for the correct transactions (uniform price=150)
		market1.clear(time);
		assertEquals( 1, market1.getTransactions().size() );
		Transaction tr = market1.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent3, tr.getBuyer());
		assertEquals("Incorrect Seller", agent2, tr.getSeller());
		assertEquals("Incorrect Price", new Price(150), tr.getPrice());
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
			partialOverlapClear();
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
		TimeStamp time2 = TimeStamp.create(1);
		
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		market1.submitOrder(agent1, SELL, new Price(100), 2, time);
		market1.submitOrder(agent2, BUY, new Price(150), 5, time2);
		market1.clear(time2);
		
		// Check that two units transact
		assertEquals( 1, market1.getTransactions().size() );
		Transaction tr = market1.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent1, tr.getSeller());
		assertEquals("Incorrect Price", new Price(125), tr.getPrice());
		assertEquals("Incorrect Quantity", 2, tr.getQuantity());

		// Check that post-trade BID is correct (3 buy units at 150)
		market1.updateQuote(time2);
		Quote q = market1.quote;
		assertEquals("Incorrect ASK", null, q.ask);
		assertEquals("Incorrect BID", new Price(150), q.bid);
		assertEquals("Incorrect ASK quantity", 0, q.askQuantity);
		assertEquals("Incorrect BID quantity", 3, q.bidQuantity);
	}
	
	@Test
	public void multiQuantity() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);

		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);

		market1.submitOrder(agent1, SELL, new Price(150), 1, time0);
		market1.submitOrder(agent1, SELL, new Price(140), 1, time0);
		market1.clear(time0);

		// Both agents' sell orders should transact b/c partial quantity withdrawn
		market1.submitOrder(agent2, BUY, new Price(160), 2, time1);
		market1.clear(time1);
		assertEquals( 2, market1.getTransactions().size() );
		Transaction tr = market1.getTransactions().get(0);
		assertEquals("Incorrect Price", new Price(155), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market1.getTransactions().get(1);
		assertEquals("Incorrect Price", new Price(155), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
	}
	
	
	@Test
	public void basicWithdraw() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);
		TimeStamp time2 = TimeStamp.create(2);

		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);

		market1.submitOrder(agent1, SELL, new Price(100), 1, time0);
		
		// Check that quotes are correct (no bid, no ask)
		Quote q = market1.quote;
		assertEquals("Incorrect ASK", null,  q.ask );
		assertEquals("Incorrect BID", null,  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
		
		Collection<Order> orders = agent1.getOrders();
		Order toWithdraw = orders.iterator().next(); // get first (& only) order
		// Test that withdraw create SendToIP activities (updates quotes)
		market1.withdrawOrder(toWithdraw, time0);
		
		// Check that quotes are correct (no bid, no ask)
		q = market1.quote;
		assertEquals("Incorrect ASK", null,  q.ask );
		assertEquals("Incorrect BID", null,  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
		
		// Check that no transaction, because agent1 withdrew its order
		market1.submitOrder(agent2, BUY, new Price(125), 1, time1);
		assertEquals( 0, market1.getTransactions().size() );

		market1.submitOrder(agent2, BUY, new Price(115), 1, time1);
		orders = agent2.getOrders();
		toWithdraw = null;
		for (Order o : orders)
			if (o.getPrice().equals(new Price(125))) toWithdraw = o;
		market1.withdrawOrder(toWithdraw, time1);
		market1.clear(time1);

		// Check that it transacts at 110 with order (@115) that was not withdrawn
		market1.submitOrder(agent1, SELL, new Price(105), 1, time2);
		market1.clear(time2);
		assertEquals( 1, market1.getTransactions().size() );
		Transaction tr = market1.getTransactions().get(0);
		assertEquals("Incorrect Price", new Price(110), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		q = market1.quote;
		assertEquals("Incorrect ASK", null,  q.ask );
		assertEquals("Incorrect BID", null,  q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
	}
	
	@Test
	public void multiQuantityWithdraw() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);

		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		market1.submitOrder(agent1, SELL, new Price(150), 1, time0);
		market1.submitOrder(agent1, SELL, new Price(140), 2, time0);
		Collection<Order> orders = agent1.getOrders();
		Order toWithdraw = null;
		for (Order o : orders)
			if (o.getPrice().equals(new Price(140))) toWithdraw = o;
		market1.withdrawOrder(toWithdraw, 1, time0);
		market1.clear(time0);

		// Check that quotes are correct (ask @140 at qty=2, no bid)
		Quote q = market1.quote;
		assertEquals("Incorrect ASK", new Price(140), q.ask);
		assertEquals("Incorrect BID", null, q.bid);
		assertEquals("Incorrect ASK quantity",  1,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
		
		// Both agents' sell orders should transact b/c partial quantity withdrawn
		market1.submitOrder(agent2, BUY, new Price(160), 1, time1);
		market1.submitOrder(agent2, BUY, new Price(160), 2, time1);
		market1.clear(time1);
		assertEquals( 2, market1.getTransactions().size() );
		Transaction tr = market1.getTransactions().get(0);
		// Clearing price should be based on pricing policy=0.5 between 150 & 160
		assertEquals("Incorrect Price", new Price(155), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market1.getTransactions().get(1);
		assertEquals("Incorrect Price", new Price(155), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		q = market1.quote;
		assertEquals("Incorrect ASK", null, q.ask );
		assertEquals("Incorrect BID", new Price(160), q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );
		
		///////////////////////
		// Same test with market w/ different pricing policy
		market2.submitOrder(agent1, SELL, new Price(150), 1, time0);
		market2.submitOrder(agent1, SELL, new Price(140), 2, time0);
		orders = market2.orders; // Agent orders aren't updated due to delayed order processing. Need to use market instead
		toWithdraw = null;
		for (Order o : orders) if (o.getPrice().equals(new Price(140))) toWithdraw = o;
		market2.withdrawOrder(toWithdraw, 1, time0);
		market2.clear(time0);
		
		// Check that quotes are correct (ask @140 at qty=2, no bid)
		q = market2.quote;
		assertEquals("Incorrect ASK", new Price(140), q.ask);
		assertEquals("Incorrect BID", null, q.bid);
		assertEquals("Incorrect ASK quantity",  1,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  0,  q.bidQuantity );
		
		// Both agents' sell orders should transact b/c partial quantity withdrawn
		market2.submitOrder(agent2, BUY, new Price(160), 1, time1);
		market2.submitOrder(agent2, BUY, new Price(160), 2, time1);
		market2.clear(time1);
		assertEquals( 2, market2.getTransactions().size() );
		tr = market2.getTransactions().get(0);
		
		// Clearing price should be based on pricing policy=1 between 150 & 160
		assertEquals("Incorrect Price", new Price(160), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market2.getTransactions().get(1);
		assertEquals("Incorrect Price", new Price(160), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		q = market2.quote;
		assertEquals("Incorrect ASK", null, q.ask );
		assertEquals("Incorrect BID", new Price(160), q.bid);
		assertEquals("Incorrect ASK quantity",  0,  q.askQuantity );
		assertEquals("Incorrect BID quantity",  1,  q.bidQuantity );
	}
	
	
	/**
	 * Test clearing when there are ties in price. Should match at uniform price.
	 * Also checks tie-breaking by time.
	 */
	@Test
	public void priceTimeTest() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);
		TimeStamp time2 = TimeStamp.create(2);

		MockBackgroundAgent agent0 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent3 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent4 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		
		market1.submitOrder(agent1, SELL, new Price(100), 1, time0);
		market1.submitOrder(agent2, SELL, new Price(100), 1, time1);
		market1.submitOrder(agent3, BUY, new Price(150), 1, time1);
		market1.clear(time1);
		
		// Check that earlier agent (agent1) is trading with agent3
		// Testing the market for the correct transactions
		assertEquals(1, market1.getTransactions().size());
		Transaction tr = market1.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent3, tr.getBuyer());
		assertEquals("Incorrect Seller", agent1, tr.getSeller());
		assertEquals("Incorrect Price", new Price(125), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());

		market1.submitOrder(agent1, SELL, new Price(100), 1, time1);
		market1.submitOrder(agent3, SELL, new Price(100), 1, time2);
		market1.submitOrder(agent4, SELL, new Price(100), 1, time2);
		market1.clear(time2); // would be inserted onto Q, but hard-coded here
		market1.submitOrder(agent0, BUY, new Price(130), 1, time2);
		market1.clear(time2);
		
		// Check that the first submitted -1@100 transacts (from agent2)
		assertEquals( 2, market1.getTransactions().size() );
		tr = market1.getTransactions().get(1);
		assertEquals("Incorrect Buyer", agent0,  tr.getBuyer());
		assertEquals("Incorrect Seller", agent2,  tr.getSeller());
		assertEquals("Incorrect Price", new Price(115), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Let's try populating the market with random orders 
		// agent 1's order -1@100 at time1 remains
		// agent 3's order -1@100 at time2 remains
		// agent 4's order -1@100 at time2 remains
		market1.submitOrder(agent0, SELL, new Price(90), 1, time2);
		market1.submitOrder(agent0, SELL, new Price(100), 1, time2);
		market1.submitOrder(agent0, SELL, new Price(110), 1, time2);
		market1.submitOrder(agent0, SELL, new Price(120), 1, time2);
		market1.submitOrder(agent0, BUY, new Price(80), 1, time2);
		market1.submitOrder(agent0, BUY, new Price(70), 1, time2);
		market1.submitOrder(agent0, BUY, new Price(60), 1, time2);
		market1.clear(time2);
		assertEquals(2, market1.getTransactions().size()); // no change

		// Check basic overlap - between agent0 (@90) and agent2
		market1.submitOrder(agent2, BUY, new Price(130), 1, time2);
		market1.clear(time2);
		assertEquals(3, market1.getTransactions().size());
		tr = market1.getTransactions().get(2);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent0, tr.getSeller());
		assertEquals("Incorrect Price", new Price(110), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
		// Check additional overlapping orders
		// Transactions between:
		// - agent 2 and agent 1
		// - agent 2 and agent 3
		// - agent 2 and agent 4
		// - agent 2 and agent 0
		market1.submitOrder(agent2, BUY, new Price(110), 1, time2);
		market1.submitOrder(agent2, BUY, new Price(110), 1, time2);
		market1.submitOrder(agent2, BUY, new Price(110), 1, time2);
		market1.submitOrder(agent2, BUY, new Price(110), 1, time2);
		market1.clear(time2);
		assertEquals(7, market1.getTransactions().size());
		tr = market1.getTransactions().get(3);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent1, tr.getSeller());
		assertEquals("Incorrect Price", new Price(105), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market1.getTransactions().get(4);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent3, tr.getSeller());
		assertEquals("Incorrect Price", new Price(105), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market1.getTransactions().get(5);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent4, tr.getSeller());
		assertEquals("Incorrect Price", new Price(105), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		tr = market1.getTransactions().get(6);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent0, tr.getSeller());
		assertEquals("Incorrect Price", new Price(105), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
		
	}
	
	/**
	 * Test insertion of Clear activities. Initialize first clear manually.
	 */
	@Test
	public void clearActivityInsertion() {
		TimeStamp time = TimeStamp.create(1);
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market1);
		Quote quote;
			
		// Test that before time 100 quotes do not change
		exec.executeActivity(new Clear(market1)); // initialize
		quote = market1.getQuoteProcessor().getQuote();
		assertEquals("Incorrect Ask", null, quote.getAskPrice());
		assertEquals("Incorrect Ask quantity", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity", 0, quote.getBidQuantity());
		
		// Quote still undefined before clear
		exec.scheduleActivity(time, new SubmitOrder(agent1, market1, BUY, new Price(100),  1));
		exec.scheduleActivity(time, new SubmitOrder(agent1, market1, SELL, new Price(110), 1));
		exec.executeUntil(clearFreq100.minus(one));
		quote = market1.getQuoteProcessor().getQuote();
		assertEquals("Incorrect Ask", null, quote.getAskPrice());
		assertEquals("Incorrect Ask quantity", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity", 0, quote.getBidQuantity());
		
		// Now quote should be updated
		exec.executeUntil(clearFreq100);
		quote = market1.getQuoteProcessor().getQuote();
		assertEquals("Incorrect Ask", new Price(110), quote.getAskPrice());
		assertEquals("Incorrect Ask quantity", 1, quote.getAskQuantity());
		assertEquals("Incorrect Bid", new Price(100), quote.getBidPrice());
		assertEquals("Incorrect Bid quantity", 1, quote.getBidQuantity());
		
		// Now check that transactions are correct as well as quotes
		exec.executeActivity(new SubmitOrder(agent2, market1, SELL, new Price(150), 1));
		exec.executeActivity(new SubmitOrder(agent2, market1, BUY, new Price(120), 1));
		// Before second clear interval ends, quote remains the same
		exec.executeUntil(clearFreq100.plus(clearFreq100).minus(one));
		quote = market1.getQuoteProcessor().getQuote();
		assertEquals("Incorrect Ask", new Price(110), quote.getAskPrice());
		assertEquals("Incorrect Ask quantity", 1, quote.getAskQuantity());
		assertEquals("Incorrect Bid", new Price(100), quote.getBidPrice());
		assertEquals("Incorrect Bid quantity", 1, quote.getBidQuantity());
		// Once clear interval ends, orders match and clear, and the quote updates
		exec.executeUntil(clearFreq100.plus(clearFreq100));
		quote = market1.getQuoteProcessor().getQuote();
		assertEquals("Incorrect Ask", new Price(150), quote.getAskPrice());
		assertEquals("Incorrect Ask quantity", 1, quote.getAskQuantity());
		assertEquals("Incorrect Bid", new Price(100), quote.getBidPrice());
		assertEquals("Incorrect Bid quantity", 1, quote.getBidQuantity());
		assertEquals(1, market1.getTransactions().size());
		Transaction tr = market1.getTransactions().get(0);
		assertEquals("Incorrect Buyer", agent2, tr.getBuyer());
		assertEquals("Incorrect Seller", agent1, tr.getSeller());
		assertEquals("Incorrect Price", new Price(115), tr.getPrice());
		assertEquals("Incorrect Quantity", 1, tr.getQuantity());
	}
	
	/**
	 * Test that zero latency "call" market == CDA market
	 */
	@Test
	public void testMarketTypeForLatency() {
		MarketFactory mf = new MarketFactory(exec, sip, new Random());
		MarketProperties props = MarketProperties.empty(Consts.MarketType.CALL);
		props.put(Keys.CLEAR_INTERVAL, -1);
		Market market = mf.createMarket(props);
		assertTrue("Incorrect market type at zero latency", market instanceof CDAMarket);
		
		props.put(Keys.CLEAR_INTERVAL, 100);
		market = mf.createMarket(props);
		assertTrue("Incorrect market type at nonzero latency", market instanceof CallMarket);
	}
	
	/**
	 * Test that quotes are delayed for market with clears every 100 and zero 
	 * information latency.
	 */
	@Test
	public void quoteNoLatency() {		
		Quote quote;
		exec.executeActivity(new Clear(market1)); // initialize clear manually

		// Test that before Time 100 nothing has been updated
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market1);
		exec.scheduleActivity(TimeStamp.ZERO, new SubmitOrder(agent, market1, SELL, new Price(100), 1));
		exec.executeUntil(TimeStamp.create(99));
		
		quote = market1.getQuoteProcessor().getQuote();
		assertEquals("Updated Ask price too early", null, quote.getAskPrice());
		assertEquals("Updated Ask quantity too early", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid price initialization", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity initialization", 0, quote.getBidQuantity());
		
		// Test that after clear at 100 quotes are updated
		exec.executeUntil(TimeStamp.create(100));
		quote = market1.getQuoteProcessor().getQuote();
		assertEquals("Didn't update Ask price", new Price(100), quote.getAskPrice());
		assertEquals("Didn't update Ask quantity", 1, quote.getAskQuantity());
		assertEquals("Changed Bid price unnecessarily", null, quote.getBidPrice());
		assertEquals("Changed Bid quantity unnecessarily", 0, quote.getBidQuantity());
		
		// Test that no change in quotes given matched orders
		exec.scheduleActivity(TimeStamp.create(150), new SubmitOrder(agent, market1, BUY, new Price(150), 1));
		exec.executeUntil(TimeStamp.create(100));
		quote = market1.getQuoteProcessor().getQuote();
		assertEquals("Changed Ask price unnecessarily", new Price(100), quote.getAskPrice());
		assertEquals("Changed Ask quantity unnecessarily", 1, quote.getAskQuantity());
		assertEquals("Changed Bid price unnecessarily", null, quote.getBidPrice());
		assertEquals("Changed Bid quantity unnecessarily", 0, quote.getBidQuantity());
		
		// Now post-clear, orders are matched and removed and quote is updated
		exec.executeUntil(TimeStamp.create(200));
		quote = market1.getQuoteProcessor().getQuote();
		assertEquals("Didn't update Ask price", null, quote.getAskPrice());
		assertEquals("Didn't update Ask quantity", 0, quote.getAskQuantity());
		assertEquals("Changed Bid price unnecessarily", null, quote.getBidPrice());
		assertEquals("Changed Bid quantity unnecessarily", 0, quote.getBidQuantity());	
	}
	
	/**
	 * Test markets with nonzero clearing intervals and nonzero information latencies.
	 */
	@Test
	public void quoteLatency() {		
		Quote quote;
		
		// delayed info by 50 + clears every 100
		CallMarket market3 = new CallMarket(exec, sip, new Random(), TimeStamp.create(50), 1, 0.5,
				clearFreq100);
		// delayed info by 150 + clears every 100
		CallMarket market4 = new CallMarket(exec, sip, new Random(), TimeStamp.create(150), 1, 0.5,
				clearFreq100);
		// Initialize first clears manually
		exec.executeActivity(new Clear(market3));
		exec.executeActivity(new Clear(market4));
		
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market3);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market4);
		
		exec.executeActivity(new SubmitOrder(agent1, market3, SELL, new Price(100), 1));
		exec.executeActivity(new SubmitOrder(agent2, market4, BUY, new Price(100), 1));
		
		// Test that before Time 100 nothing has been updated for either market
		exec.executeUntil(TimeStamp.create(99));
		quote = market3.getQuoteProcessor().getQuote();
		assertEquals("Updated Ask price too early", null, quote.getAskPrice());
		assertEquals("Updated Ask quantity too early", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid price initialization", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity initialization", 0, quote.getBidQuantity());
		quote = market4.getQuoteProcessor().getQuote();
		assertEquals("Updated Ask price too early", null, quote.getAskPrice());
		assertEquals("Updated Ask quantity too early", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid price initialization", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity initialization", 0, quote.getBidQuantity());
		
		// Test that after clear at 100 quotes are still not updated
		exec.executeUntil(TimeStamp.create(100));
		assertEquals("Updated Ask price too early", null, quote.getAskPrice());
		assertEquals("Updated Ask quantity too early", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid price initialization", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity initialization", 0, quote.getBidQuantity());
		quote = market4.getQuoteProcessor().getQuote();
		assertEquals("Updated Ask price too early", null, quote.getAskPrice());
		assertEquals("Updated Ask quantity too early", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid price initialization", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity initialization", 0, quote.getBidQuantity());
		
		// Test that market3's quotes have now updated (but not market4's)
		exec.executeUntil(TimeStamp.create(150));
		quote = market3.getQuoteProcessor().getQuote();
		assertEquals("Didn't update Ask price", new Price(100), quote.getAskPrice());
		assertEquals("Didn't update Ask quantity", 1, quote.getAskQuantity());
		assertEquals("Changed Bid price unnecessarily", null, quote.getBidPrice());
		assertEquals("Changed Bid quantity unnecessarily", 0, quote.getBidQuantity());
		quote = market4.getQuoteProcessor().getQuote();
		assertEquals("Updated Ask price too early", null, quote.getAskPrice());
		assertEquals("Updated Ask quantity too early", 0, quote.getAskQuantity());
		assertEquals("Incorrect Bid price initialization", null, quote.getBidPrice());
		assertEquals("Incorrect Bid quantity initialization", 0, quote.getBidQuantity());
		
		// Test that market4's quotes have now updated (quote from first clear)
		exec.executeUntil(TimeStamp.create(250));
		quote = market4.getQuoteProcessor().getQuote();
		assertEquals("Didn't update Bid price", new Price(100), quote.getBidPrice());
		assertEquals("Didn't update Bid quantity", 1, quote.getBidQuantity());
		assertEquals("Changed Ask price unnecessarily", null, quote.getAskPrice());
		assertEquals("Changed Ask quantity unnecessarily", 0, quote.getAskQuantity());
	}
}
