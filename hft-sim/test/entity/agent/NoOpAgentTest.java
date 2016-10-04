package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
import activity.AgentArrival;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.MockMarket;
import entity.market.Order;
import event.TimeStamp;

public class NoOpAgentTest {

	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private SIP sip;
	private static Random rand = new Random();

	@BeforeClass
	public static void setupClass() throws IOException {
		
		// Setting up the log file
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "NoOpAgentTest.log"));
	}

	@Before
	public void setupTest() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
	}
	
	@Test
	public void strategyTest() {
		MockMarket market = new MockMarket(exec, sip);
		Agent noop = new NoOpAgent(exec, fundamental, sip, rand, EntityProperties.empty());
		Agent zir  = new ZIRAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand, EntityProperties.empty());
		exec.executeActivity(new AgentArrival(zir));
		exec.executeActivity(new AgentArrival(noop));
		// NoOp agent doesn't know about the market, and should never have agent strategy called...
		
		for (int i = 0; i < 6000; ++i) {
			exec.executeUntil(TimeStamp.create(i));
			assertEquals(0, noop.getPayoff(), 0);		// no profit
			assertTrue(noop.activeOrders.isEmpty());	// no orders
			assertTrue(noop.transactions.isEmpty());	// no transactions
			for (Order o : market.getOrders())
				assertNotEquals(noop, o.getAgent());	// no orders
		}
		
	}
	
}
