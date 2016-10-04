package entity.agent;

import static event.TimeStamp.ZERO;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import activity.Clear;
import activity.LiquidateAtFundamental;
import activity.SubmitOrder;
import activity.WithdrawOrder;

import com.google.common.collect.Iterables;

import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

public class AgentTest {
	
	private static final TimeStamp time1 = TimeStamp.create(1);

	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private Agent agent;
	private SIP sip;

	@BeforeClass
	public static void setupClass() throws IOException {
		
		Log.log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "AgentTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
		agent = new MockAgent(exec, fundamental, sip);
	}

	@Test
	public void basicOrdersTest() {
		TimeStamp time = TimeStamp.ZERO;
		
		// test adding and removing orders
		market.submitOrder(agent, BUY, new Price(100), 1, time);
		market.submitOrder(agent, SELL, new Price(50), 2, time.plus(TimeStamp.create(1)));
		Collection<Order> orders = agent.activeOrders;
		Order order = Iterables.getFirst(orders, null);
		// Note: nondeterministic which order is "first" so need to check
		boolean buyRemoved = true;
		if (order.getSubmitTime().equals(time)) {
			assertEquals(BUY, order.getOrderType());
			assertEquals(new Price(100), order.getPrice());
			assertEquals(1, order.getQuantity());
		} else if (order.getSubmitTime().equals(TimeStamp.create(1))) {
			assertEquals(SELL, order.getOrderType());
			assertEquals(new Price(50), order.getPrice());
			assertEquals(2, order.getQuantity());
			buyRemoved = false;
		} else {
			fail("Should never get here");
		}
		assertEquals(2, orders.size());
		assertTrue("Agent does not know about buy order", agent.activeOrders.contains(order));
		
		// Test that remove works correctly
		market.withdrawOrder(order, TimeStamp.create(1));
		orders = agent.activeOrders;
		assertEquals(1, orders.size());
		assertTrue("Order was not removed", !agent.activeOrders.contains(order));
		order = Iterables.getFirst(orders, null);
		if (buyRemoved) {
			assertEquals(SELL, order.getOrderType());
			assertEquals(new Price(50), order.getPrice());
			assertEquals(2, order.getQuantity());
		} else {
			assertEquals(BUY, order.getOrderType());
			assertEquals(new Price(100), order.getPrice());
			assertEquals(1, order.getQuantity());
		}
	}
	
	@Test
	public void withdrawOrder() {
		exec.scheduleActivity(ZERO, new SubmitOrder(agent, market, BUY, new Price(100), 1));
		exec.executeUntil(time1);
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(1, orders.size());
		Order orderToWithdraw = Iterables.getOnlyElement(orders);
		assertEquals(new Price(100), orderToWithdraw.getPrice());
		assertEquals(ZERO, orderToWithdraw.getSubmitTime());
		assertNotNull(orderToWithdraw);
		
		Quote q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(100),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// Withdraw order
		exec.executeActivity(new WithdrawOrder(orderToWithdraw));
		
		orders = agent.activeOrders;
		assertEquals("Order was not withdrawn", 0, orders.size());
		q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", null,  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  0,  q.getBidQuantity() );
	}
	
	@Test
	public void withdrawOrderDelayed() {
		// withdraw order when quotes are delayed
		TimeStamp time10 = TimeStamp.create(10);
		
		MockMarket market2 = new MockMarket(exec, sip, time10);
		exec.scheduleActivity(ZERO, new SubmitOrder(agent, market2, BUY, new Price(100), 1));
		exec.executeUntil(ZERO);
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(1, orders.size());
		Order orderToWithdraw = null;
		for (Order o : orders) {
			orderToWithdraw = o;
			assertEquals(new Price(100), o.getPrice());
			assertEquals(ZERO, o.getSubmitTime());
		}
		assertNotNull(orderToWithdraw);
		
		Quote q = market2.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", null,  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  0,  q.getBidQuantity() );
		
		// After quotes have updated
		exec.executeUntil(time10);
		q = market2.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(100),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// Withdraw order
		exec.executeActivity(new WithdrawOrder(orderToWithdraw));
		
		orders = agent.activeOrders;
		assertEquals("Order was not withdrawn", 0, orders.size());
		assertTrue("Order was not removed", !agent.activeOrders.contains(orderToWithdraw));
		// Verify that quote is now stale
		q = market2.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(100),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// After quotes have updated
		exec.executeUntil(TimeStamp.create(20));
		q = market2.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", null,  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  0,  q.getBidQuantity() );
	}
	
	@Test
	public void withdrawNewestOrder() {
		exec.scheduleActivity(ZERO, new SubmitOrder(agent, market, BUY, new Price(50), 1));
		exec.scheduleActivity(time1, new SubmitOrder(agent, market, SELL, new Price(100), 1));
		exec.executeUntil(time1);
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(2, orders.size());
		Order orderToWithdraw = null;
		for (Order o : orders) {
			if (o.getOrderType() == BUY) {
				assertEquals(new Price(50), o.getPrice());
				assertEquals(ZERO, o.getSubmitTime());
			}
			if (o.getOrderType() == SELL) {
				orderToWithdraw = o;
				assertEquals(new Price(100), o.getPrice());
				assertEquals(time1, o.getSubmitTime());
			}
		}
		assertNotNull(orderToWithdraw);
		Quote q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", new Price(100),  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(50),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  1,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// Withdraw newest order (sell)
		exec.executeActivity(new WithdrawOrder(orderToWithdraw));
		
		orders = agent.activeOrders;
		assertEquals(1, orders.size());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(BUY, order.getOrderType());
		assertEquals(new Price(50), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertTrue("Order was not withdrawn", !agent.activeOrders.contains(orderToWithdraw));
		q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(50),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
	}
	
	@Test
	public void withdrawOldestOrder() {
		exec.scheduleActivity(ZERO, new SubmitOrder(agent, market, BUY, new Price(50), 1));
		exec.scheduleActivity(time1, new SubmitOrder(agent, market, SELL, new Price(100), 1));
		exec.executeUntil(time1);
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(2, orders.size());
		Order orderToWithdraw = null;
		for (Order o : orders) {
			if (o.getOrderType() == BUY) {
				orderToWithdraw = o;
				assertEquals(new Price(50), o.getPrice());
				assertEquals(ZERO, o.getSubmitTime());
			}
			if (o.getOrderType() == SELL) {
				assertEquals(new Price(100), o.getPrice());
				assertEquals(time1, o.getSubmitTime());
			}
		}
		assertNotNull(orderToWithdraw);
		// Verify quote correct
		Quote q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", new Price(100),  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(50),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  1,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// Withdraw oldest order (buy)
		agent.withdrawOldestOrder();
		
		orders = agent.activeOrders;
		assertEquals(1, orders.size());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(SELL, order.getOrderType());
		assertEquals(new Price(100), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertTrue("Order was not withdrawn", !agent.activeOrders.contains(orderToWithdraw));
		// Verify quote correct
		q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", new Price(100),  q.getAskPrice() );
		assertEquals("Incorrect BID", null,  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  1,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  0,  q.getBidQuantity() );
	}
	
	@Test
	public void withdrawAllOrders() {
		exec.scheduleActivity(ZERO, new SubmitOrder(agent, market, BUY, new Price(50), 1));
		exec.scheduleActivity(time1, new SubmitOrder(agent, market, SELL, new Price(100), 1));
		exec.executeUntil(time1);
		
		// Verify orders added correctly
		Collection<Order> orders = agent.activeOrders;
		assertEquals(2, orders.size());
		for (Order o : orders) {
			if (o.getOrderType() == BUY) {
				assertEquals(new Price(50), o.getPrice());
				assertEquals(ZERO, o.getSubmitTime());
			}
			if (o.getOrderType() == SELL) {
				assertEquals(new Price(100), o.getPrice());
				assertEquals(time1, o.getSubmitTime());
			}
		}
		// Verify quote correct
		Quote q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", new Price(100),  q.getAskPrice() );
		assertEquals("Incorrect BID", new Price(50),  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  1,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  1,  q.getBidQuantity() );
		
		// Withdraw all orders
		agent.withdrawAllOrders();
		
		assertEquals(0, agent.activeOrders.size());	
		// Verify quote correct
		q = market.getQuoteProcessor().getQuote();
		assertEquals("Incorrect ASK", null,  q.getAskPrice() );
		assertEquals("Incorrect BID", null,  q.getBidPrice() );
		assertEquals("Incorrect ASK quantity",  0,  q.getAskQuantity() );
		assertEquals("Incorrect BID quantity",  0,  q.getBidQuantity() );
	}
	
	@Test
	public void processTransaction() {
		TimeStamp time = TimeStamp.ZERO;
		MockAgent agent2 = new MockAgent(exec, fundamental, sip);
		
		assertEquals( 0, agent.transactions.size());
		
		// Creating and adding bids
		market.submitOrder(agent, BUY, new Price(110000), 1, time);
		market.submitOrder(agent2, SELL, new Price(100000), 1, time);
		assertEquals(0, market.getTransactions().size());
		assertEquals(0, agent.positionBalance);
		assertEquals(0, agent2.positionBalance);
		assertEquals(0, agent.cashBalance);
        assertEquals(0, agent2.cashBalance);
        assertEquals(0, agent.positioningProfit);
        assertEquals(0, agent2.positioningProfit);
        assertEquals(0, agent.spreadProfit);
        assertEquals(0, agent2.spreadProfit);
		
		// Testing the market for the correct transactions
		exec.executeActivity(new Clear(market));	// will call processTransaction
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals( 1, agent.transactions.size());
		assertEquals(tr, agent.transactions.get(0));
		assertEquals( 1, agent.positionBalance );
		assertEquals( -110000, agent.profit );
		assertEquals( -1, agent2.positionBalance );
		assertEquals( 110000, agent2.profit );
		assertEquals(-110000, agent.cashBalance);
        assertEquals(110000, agent2.cashBalance);
        assertEquals(0, agent.positioningProfit);
        assertEquals(0, agent2.positioningProfit);
        assertEquals(-10000, agent.spreadProfit);
        assertEquals(10000, agent2.spreadProfit);
        
        exec.executeActivity(new LiquidateAtFundamental(agent));
        exec.executeActivity(new LiquidateAtFundamental(agent2));
        assertEquals( -10000, agent.profit );
        assertEquals( 10000, agent2.profit );
        assertEquals(0, agent.positioningProfit);
        assertEquals(0, agent2.positioningProfit);
        assertEquals(-10000, agent.spreadProfit);
        assertEquals(10000, agent2.spreadProfit);
	}
	
	@Test
	public void processTransactionMultiQuantity() {
		TimeStamp time = TimeStamp.ZERO;
		MockAgent agent2 = new MockAgent(exec, fundamental, sip);
		
		assertEquals( 0, agent.transactions.size());
		
		// Creating and adding bids
		market.submitOrder(agent, BUY, new Price(110), 3, time);
		market.submitOrder(agent2, SELL, new Price(100), 2, time);
		assertEquals(0, market.getTransactions().size());
		assertEquals(0, agent.positionBalance);
		assertEquals(0, agent2.positionBalance);
		
		// Testing the market for the correct transactions
		exec.executeActivity(new Clear(market));	// will call processTransaction
		assertEquals( 1, market.getTransactions().size() );
		Transaction tr = market.getTransactions().get(0);
		assertEquals( 1, agent.transactions.size());
		assertEquals(tr, agent.transactions.get(0));
		assertEquals( 2, agent.positionBalance );
		assertEquals( -220, agent.profit );
		assertEquals( -2, agent2.positionBalance );
		assertEquals( 220, agent2.profit );
	}
	
	@Test
	public void liquidation() {
		TimeStamp time = TimeStamp.create(100);
		agent.profit = 5000;
		
		// Check that no change if position 0
		agent.positionBalance = 0;
		final int priceValue = 100000;
		agent.liquidateAtPrice(new Price(priceValue), TimeStamp.create(100));
		assertEquals(5000, agent.getPostLiquidationProfit());
		assertEquals(agent.positionBalance * priceValue, agent.cashBalance);
		
		// Check liquidation when position > 0 (sell 1 unit)
		agent.profit = 5000;
		agent.positionBalance = 1;
		agent.cashBalance = 0;
		agent.liquidateAtPrice(new Price(priceValue), TimeStamp.create(100));
		assertEquals(105000, agent.getPostLiquidationProfit());
		assertEquals(agent.positionBalance * priceValue, agent.cashBalance);
		
		// Check liquidation when position < 0 (buy 2 units)
		agent.profit = 5000;
		agent.positionBalance = -2;
	    agent.cashBalance = 0;
		agent.liquidateAtPrice(new Price(priceValue), TimeStamp.create(100));
		assertEquals(-195000, agent.getPostLiquidationProfit());
        assertEquals(agent.positionBalance * priceValue, agent.cashBalance);
		
		// Check liquidation at fundamental
		agent.profit = 5000;
		agent.positionBalance = 1;
        agent.cashBalance = 0;
		agent.liquidateAtFundamental(time);
		assertEquals(fundamental.getValueAt(time).longValue() + 5000, agent.getPostLiquidationProfit());
        assertEquals(agent.positionBalance * priceValue, agent.cashBalance);
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			basicOrdersTest();
			estimatedFundamental();
		}
	}
	
	@Test
	public void estimatedFundamental() {
		double kappa = 0.05;
		int meanVal = 100000;
		double var = 1E6;
		FundamentalValue fund = FundamentalValue.create(kappa, meanVal, var, 1.0, new Random());
		TimeStamp time = TimeStamp.create(25);
		
		Agent agent = new MockAgent(exec, fund, sip);
		
		// high margin of error here because of rounding issues
		assertEquals(0.005920529*fund.getValueAt(time).intValue() + 99407.9, 
				agent.getEstimatedFundamental(time, 125, kappa, meanVal).doubleValue(), 0.75);
		
	}
}
