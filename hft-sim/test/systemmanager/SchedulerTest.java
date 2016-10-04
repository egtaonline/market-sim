package systemmanager;

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

import activity.MockActivity;
import activity.MockAgentActivity;
import data.FundamentalValue;
import data.MockFundamental;
import entity.agent.MockBackgroundAgent;
import entity.infoproc.SIP;
import entity.market.MockMarket;
import event.TimeStamp;

public class SchedulerTest {

	private FundamentalValue fundamental = new MockFundamental(100000);
	private Scheduler scheduler;
	private MockMarket market;
	private SIP sip;
	
	@BeforeClass
	public static void setupClass() throws IOException {
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "EventManagerTest.log"));
	}

	@Before
	public void setup() {
		scheduler = new Scheduler(new Random());
		sip = new SIP(scheduler, TimeStamp.IMMEDIATE);
		market = new MockMarket(scheduler, sip, TimeStamp.IMMEDIATE);
	}
	
	@Test
	public void basicExecution() {
		TimeStamp time = TimeStamp.create(10);
		
		// Initially empty, time 0
		assertEquals(0, scheduler.eventQueue.size());
		assertEquals(TimeStamp.ZERO, scheduler.getCurrentTime());
		
		scheduler.scheduleActivity(time, new MockActivity());
		assertEquals(1, scheduler.eventQueue.size());
		assertTrue(scheduler.eventQueue.peek().getActivity() instanceof MockActivity);
		assertEquals(time, scheduler.eventQueue.peek().getTime());
		// Verify that current time is 0 until first activity is executed
		assertEquals(TimeStamp.ZERO, scheduler.getCurrentTime());
		
		scheduler.executeNext();
		// Check that activity did in fact execute
		assertEquals(0, scheduler.eventQueue.size());
		assertEquals(time, scheduler.getCurrentTime());
	}
	
	@Test
	public void executeUntilTest() {
		TimeStamp time = TimeStamp.create(100);
		scheduler.scheduleActivity(time, new MockActivity());
		scheduler.scheduleActivity(time.plus(time), new MockActivity());
		
		assertEquals(2, scheduler.eventQueue.size());
		assertEquals(TimeStamp.ZERO, scheduler.getCurrentTime());
		
		// Check that second activity hasn't executed yet
		scheduler.executeUntil(time);
		assertEquals(1, scheduler.eventQueue.size());
		assertEquals(time, scheduler.getCurrentTime());
		assertEquals(time.plus(time), scheduler.eventQueue.peek().getTime());
		
		scheduler.executeNext();
		// Check that activity did in fact execute
		assertEquals(0, scheduler.eventQueue.size());
		assertEquals(time.plus(time), scheduler.getCurrentTime());	
		assertEquals(null, scheduler.eventQueue.peek());
	}
	
	
	@Test
	public void chainingActivityExecution() {
		// testing activities that insert further activities
		// MockAgentActivity will insert a MockActivity
		TimeStamp time = TimeStamp.create(10);
		
		MockBackgroundAgent agent = new MockBackgroundAgent(scheduler, fundamental, sip, market);
		assertEquals(0, scheduler.eventQueue.size());
		assertEquals(TimeStamp.ZERO, scheduler.getCurrentTime());
		
		scheduler.scheduleActivity(time, new MockAgentActivity(agent));
		// Verify that new activity has been added correctly 
		// and that current time is 0 until first activity is executed
		assertEquals(1, scheduler.eventQueue.size());
		assertTrue(scheduler.eventQueue.peek().getActivity() instanceof MockAgentActivity);
		assertEquals(time, scheduler.eventQueue.peek().getTime());
		assertEquals(TimeStamp.ZERO, scheduler.getCurrentTime());
		
		scheduler.executeNext();
		// Check that MockAgentActivity did in fact execute
		assertEquals(1, scheduler.eventQueue.size());
		assertTrue(scheduler.eventQueue.peek().getActivity() instanceof MockActivity);
		assertEquals(time, scheduler.eventQueue.peek().getTime());
		assertEquals(time, scheduler.getCurrentTime());

		scheduler.executeNext();
		// Check that MockActivity did in fact execute
		assertEquals(0, scheduler.eventQueue.size());
		assertEquals(time, scheduler.getCurrentTime());
		assertEquals(null, scheduler.eventQueue.peek());
	}
}
