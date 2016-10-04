package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;

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
import activity.AgentStrategy;
import activity.SubmitNMSOrder;
import activity.SubmitOrder;

import com.google.common.collect.Iterables;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.BestBidAsk;
import entity.infoproc.SIP;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Only testing order withdrawal, everything else is just same as ZIAgent.
 * 
 * @author ewah
 *
 */
public class ZIRAgentTest {

	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;
	private static Random rand;
	private static EntityProperties agentProperties = EntityProperties.fromPairs(
			Keys.REENTRY_RATE, 0,
			Keys.MAX_POSITION, 2,
			Keys.PRIVATE_VALUE_VAR, 0);

	@BeforeClass
	public static void setUpClass() throws IOException{
		
		
		// Setting up the log file
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "ZIRAgentTest.log"));

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

	public ZIRAgent createAgent(Object... parameters) {
		return createAgent(fundamental, market, rand, parameters);
	}
	
	public ZIRAgent createAgent(FundamentalValue fundamental, Market market, Random rand, Object... parameters) {
		return new ZIRAgent(exec, TimeStamp.ZERO, fundamental, sip, market,
				rand, EntityProperties.copyFromPairs(agentProperties,
						parameters));
	}

	@Test
	public void withdrawTest() {
		// verify that orders are correctly withdrawn at each re-entry
		ZIRAgent agent = createAgent(
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.TICK_SIZE, 1,
				Keys.MAX_POSITION, 2,
				Keys.BID_RANGE_MIN, 0,
				Keys.BID_RANGE_MAX, 5000,
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

	/**
	 * Specific scenario where an order withdrawal changes the market to which 
	 * an order is routed (market quote latency is immediate)
	 */
	@Test
	public void withdrawQuoteUpdateTest() {
		TimeStamp time0 = TimeStamp.ZERO;
		TimeStamp time50 = TimeStamp.create(50);
		SIP sip = new SIP(exec, TimeStamp.create(50));

		// Set up CDA markets (market quote latency = 0) and their quotes
		Market nasdaq = new CDAMarket(exec, sip, rand, time0, 1);
		Market nyse = new CDAMarket(exec, sip, rand, time0, 1);
		MockBackgroundAgent background1 = new MockBackgroundAgent(exec, fundamental, sip, nyse);
		MockBackgroundAgent background2 = new MockBackgroundAgent(exec, fundamental, sip, nasdaq);
		exec.executeActivity(new SubmitOrder(background1, nyse, SELL, new Price(111000), 1));
		exec.executeActivity(new SubmitOrder(background1, nasdaq, BUY, new Price(104000), 1));
		exec.executeActivity(new SubmitOrder(background2, nasdaq, SELL, new Price(108000), 1));
		exec.executeActivity(new SubmitOrder(background2, nyse, BUY, new Price(102000), 1));
		exec.executeUntil(time50);
		// Verify that NBBO quote is (104, 108) at time 50 (after quotes have been processed by SIP)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(108000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(104000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nasdaq, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nasdaq, nbbo.getBestBidMarket());

		///////////////
		// Creating ZIR agent that WILL withdraw its orders; & submit buy order
		FundamentalValue fundamental2 = new MockFundamental(110000);
		ZIRAgent agent1 = createAgent(fundamental2, nasdaq, new Random(4), // rand seed selected to insert BUY
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.TICK_SIZE, 1,
				Keys.MAX_POSITION, 1,
				Keys.BID_RANGE_MIN, 1000,
				Keys.BID_RANGE_MAX, 1000,
				Keys.WITHDRAW_ORDERS, true);

		// ZIR submits sell at 105 (is routed to nyse)
		// Verify that NBBO quote is (104, 105) at time 100
		exec.executeActivity(new SubmitNMSOrder(agent1, nyse, SELL, new Price(105000), 1));
		exec.executeUntil(TimeStamp.create(100));
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(105000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(104000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nyse, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nasdaq, nbbo.getBestBidMarket());

		// Execute ZIR agent strategy
		// ZIR agent should withdraw its order and submit a buy @ 109 to nasdaq
		exec.executeActivity(new AgentStrategy(agent1));

		// Verify that order submitted with correct price (109)
		// Order is routed to NYSE because of out of date SIP (104, 105) even
		// though it is really (104, 111) because the SELL @ 105 was withdrawn
		assertEquals(1, agent1.activeOrders.size());
		Order o = Iterables.getOnlyElement(agent1.activeOrders);
		assertEquals(new Price(109000), o.getPrice());
		assertEquals(BUY, o.getOrderType());
		assertEquals(nyse, o.getMarket());

		// Verify NBBO quote is (104, 111)
		// Notice that the BID/ASK cross; if the SIP had been immediate, then
		// the BUY order at 109 would have been routed to Nasdaq and it
		// would have transacted immediately
		exec.executeUntil(TimeStamp.create(150));
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(108000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(109000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nasdaq, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nyse, nbbo.getBestBidMarket());
	}
	
	/**
	 * Specific scenario where not withdrawing order means that the order will
	 * be routed and will transact immediately.
	 */
	@Test
	public void noWithdrawQuoteUpdateTest() {
		SIP sip = new SIP(exec, TimeStamp.create(50));

		// Set up CDA markets (market quote latency is immediate) and their quotes
		Market nasdaq = new CDAMarket(exec, sip, rand, TimeStamp.IMMEDIATE, 1);
		Market nyse = new CDAMarket(exec, sip, rand, TimeStamp.IMMEDIATE, 1);
		MockBackgroundAgent background1 = new MockBackgroundAgent(exec, fundamental, sip, nyse);
		MockBackgroundAgent background2 = new MockBackgroundAgent(exec, fundamental, sip, nasdaq);
		exec.executeActivity(new SubmitOrder(background1, nyse, SELL, new Price(111000), 1));
		exec.executeActivity(new SubmitOrder(background1, nasdaq, BUY, new Price(104000), 1));
		exec.executeActivity(new SubmitOrder(background2, nasdaq, SELL, new Price(108000), 1));
		exec.executeActivity(new SubmitOrder(background2, nyse, BUY, new Price(102000), 1));
		exec.executeUntil(TimeStamp.create(50));
		// Verify that NBBO quote is (104, 108) at time 50 (after quotes have been processed by SIP)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(108000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(104000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nasdaq, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nasdaq, nbbo.getBestBidMarket());

		///////////////
		// Creating ZIR agent that WILL NOT withdraw its orders; & submit buy order
		FundamentalValue fundamental2 = new MockFundamental(110000);
		ZIRAgent agent1 = createAgent(fundamental2, nasdaq, new Random(4), // Submits buy
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.TICK_SIZE, 1,
				Keys.MAX_POSITION, 1,
				Keys.BID_RANGE_MIN, 1000,
				Keys.BID_RANGE_MAX, 1000,
				Keys.WITHDRAW_ORDERS, false);
		
		// ZIR submits sell at 105 (is routed to nyse)
		// Verify that NBBO quote is (104, 105) at time 100
		exec.executeActivity(new SubmitNMSOrder(agent1, nyse, SELL, new Price(105000), 1));
		exec.executeUntil(TimeStamp.create(100));
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(105000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(104000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nyse, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nasdaq, nbbo.getBestBidMarket());

		// Execute ZIR agent strategy
		// ZIR agent should withdraw its order and submit a sell @ 109
		exec.executeActivity(new AgentStrategy(agent1));

		// Verify that order submitted with correct price (109)
		// Order is routed to NYSE but can't trade with itself
		assertEquals(0, agent1.activeOrders.size());

		// Verify that order transacts in Nasdaq
		// for testing purposes, have agent trade with itself (possible when
		// it doesn't withdraw its order at each reentry)
		assertEquals(0, nasdaq.getTransactions().size());
		assertEquals(1, nyse.getTransactions().size());
		Transaction t = Iterables.getFirst(nyse.getTransactions(), null);
		assertEquals(new Price(105000), t.getPrice());
		assertEquals(agent1, t.getSeller());
		assertEquals(agent1, t.getBuyer());
		
		// Verify NBBO quote is (104, 111)
		exec.executeUntil(TimeStamp.create(150));
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(108000), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(104000), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", nasdaq, nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", nasdaq, nbbo.getBestBidMarket());
	}
}
