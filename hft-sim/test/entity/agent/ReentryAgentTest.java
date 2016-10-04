package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import activity.AgentStrategy;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import event.TimeStamp;
import event.TimedActivity;

public class ReentryAgentTest {

	private Executor exec;
	private Market market;
	private SIP sip;
	
	@BeforeClass
	public static void setupClass() throws IOException {
		
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "ReentryAgentTest.log"));
	}
	
	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
	}
	
	@Test
	public void reentryTest() {
		TimeStamp time = TimeStamp.create(100);
		FundamentalValue fundamental = new MockFundamental(100000);
		
		MockReentryAgent agent = new MockReentryAgent(exec, fundamental, sip, market, new Random(), 0.1, 1);
		
		// Test reentries
		Iterator<TimeStamp> reentries = agent.getReentryTimes(); 
		assertTrue(reentries.hasNext());
		TimeStamp next = reentries.next();
		assertTrue(next.getInTicks() >= 0);
		
		// Test agent strategy
		agent.agentStrategy(time);
		TimedActivity act = exec.peek();
		assertTrue( act.getActivity() instanceof AgentStrategy );
		assertTrue( act.getTime().getInTicks() >= time.getInTicks());
	}
	
	@Test
	public void reentryRateZeroTest() {
		TimeStamp time = TimeStamp.create(100);
		FundamentalValue fundamental = new MockFundamental(100000);
		
		// Test reentries - note should never iterate past INFINITE b/c it 
		// will never execute
		MockReentryAgent agent = new MockReentryAgent(exec, fundamental, sip, market, new Random(), 0, 1);
		Iterator<TimeStamp> reentries = agent.getReentryTimes(); 
		assertFalse(reentries.hasNext());

		// Now test for agent, which should arrive at time 0
		MockReentryAgent agent2 = new MockReentryAgent(exec, fundamental, sip, market, new Random(), 0, 1);
		agent2.agentStrategy(time);
		assertTrue(exec.isEmpty());
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i <= 100; i++) {
			setup();
			reentryTest();
		}
	}
}
