package entity.infoproc;

import static event.TimeStamp.ZERO;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import activity.ProcessQuote;
import activity.SendToQP;

import com.google.common.collect.ImmutableList;

import data.FundamentalValue;
import data.MockFundamental;
import entity.agent.MockHFTAgent;
import entity.market.DummyMarketTime;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.MockMarket;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import event.TimedActivity;

/**
 * Note that it's necessary to create an HFT with latency in order to ensure
 * that the agent's quotes and transactions are delayed.
 * 
 * @author ewah
 *
 */
public class HFTQuoteProcessorTest {
	
	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market1;
	private Market market2;
	private SIP sip;
	private MockHFTAgent hft;	// necessary to access QP/TPs
	private HFTQuoteProcessor mktip1;
	private HFTQuoteProcessor mktip2;

	@BeforeClass
	public static void setupClass() throws IOException {
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "HFTQuoteProcessorTest.log"));
	}
	
	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		// market that updates immediately
		market1 = new MockMarket(exec, sip);
		// market with latency 100
		market2 = new MockMarket(exec, sip, TimeStamp.create(100));
		
		hft = new MockHFTAgent(exec, TimeStamp.IMMEDIATE, fundamental, sip, 
				ImmutableList.of(market1, market2));
		mktip1 = hft.getHFTQuoteProcessor(market1);
		mktip2 = hft.getHFTQuoteProcessor(market2);
	}
	
	
	@Test
	public void basicProcessQuote() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		
		// Check HFT IP latencies
		assertEquals(TimeStamp.IMMEDIATE, mktip1.latency);
		assertEquals(TimeStamp.IMMEDIATE, mktip2.latency);
		
		// Check initial quote is null for both markets
		Quote q = hft.getQuote(market1);
		assertEquals("Incorrect quote time", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		q = hft.getQuote(market2);
		assertEquals("Incorrect last quote time", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Test on undelayed market's HFTQuoteProcessor
		q = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime);
		mktip1.processQuote(market1, q, time);
		
		// Check updated quote after process quote
		q = hft.getQuote(market1);
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());

		// Now test for delayed market's SMIP
		q = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime);
		mktip2.processQuote(market2, q, time);
		// agent strategy method added

		// Check second market correct
		q = hft.getQuote(market2);
		assertEquals("Incorrect last quote time", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	@Test
	public void basicNoDelay() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);

		assertEquals(TimeStamp.IMMEDIATE, mktip1.getLatency());
		
		// Check initial quote is null for both markets
		q = hft.getQuote(market1);
		assertEquals("Incorrect last quote time", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());

		// Add new quote
		q = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime);
		mktip1.sendToQuoteProcessor(market1, q, time);

		// Check updated quote after process quote
		q = hft.getQuote(market1);
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	@Test
	public void basicDelay() {
		Quote q;
		MarketTime mktTime = new DummyMarketTime(ZERO, 1);
		
		// set up delayed HFT agent (so QP, TP have non-immediate latency)
		MockHFTAgent hft = new MockHFTAgent(exec, TimeStamp.create(100), fundamental, sip, 
				ImmutableList.of(market1, market2));
		HFTQuoteProcessor mktip2 = hft.getHFTQuoteProcessor(market2);
		assertEquals(TimeStamp.create(100), mktip2.getLatency());
		
		// Check initial quote is null
		q = mktip2.quote;
		assertEquals("Incorrect last quote time", TimeStamp.ZERO, q.getQuoteTime());
		assertEquals("Incorrect ASK", null, q.getAskPrice());
		assertEquals("Incorrect BID", null, q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 0, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 0, q.getBidQuantity());
		
		// Check that process quote activity scheduled correctly
		q = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime);
		mktip2.sendToQuoteProcessor(market2, q, ZERO);
		assertFalse(exec.isEmpty());
		TimedActivity act = exec.peek();
		assertEquals("Incorrect scheduled activity time", TimeStamp.create(100),
				act.getTime());
		assertTrue("Incorrect activity type scheduled",
				act.getActivity() instanceof ProcessQuote);
		exec.executeUntil(TimeStamp.create(100));

		// Check updated quote after process quote (specific to SMIP)
		q = mktip2.quote;
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	/**
	 * Test creation of additional HFTQuoteProcessor with a different latency.
	 */
	@Test
	public void alternateDelayHFTQuoteProcessor() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		Quote q = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime);
		
		MockHFTAgent hft2 = new MockHFTAgent(exec, TimeStamp.IMMEDIATE, fundamental, sip, 
				ImmutableList.of(market1, market2));
		HFTQuoteProcessor hftip = hft2.getHFTQuoteProcessor(market2);
		assertEquals(TimeStamp.IMMEDIATE, hftip.latency);
		
		// Send quotes to appropriate IPs
		exec.executeActivity(new SendToQP(market2, q, hftip));
		exec.executeActivity(new SendToQP(market2, q, hftip));
		
		// Check HFTQuoteProcessor, which should be immediate
		q = hftip.quote;
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	/**
	 * Testing latency with EventManager.
	 */
	@Test
	public void eventManagerLatencyTest() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime);
		Quote q2 = new Quote(market2, new Price(75), 1, new Price(95), 1, mktTime);
		
		// Send quotes to appropriate IPs
		exec.executeActivity(new SendToQP(market1, q1, mktip1));
		exec.executeActivity(new SendToQP(market2, q2, mktip2));
		
		// Check HFT IP for market 1 updated
		q = mktip1.quote;
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		// Check HFT IP for market 2 not updated
		q = mktip2.quote;
		assertEquals("Last quote time not updated", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	@Test
	public void multiQuoteUpdates() {
		Quote q;
		TimeStamp time2 = TimeStamp.create(50);
		MarketTime mktTime1 = new DummyMarketTime(ZERO, 1);
		MarketTime mktTime2 = new DummyMarketTime(ZERO, 2);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime1);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime2);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, mktTime2);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, mktTime1);
		
		exec.executeActivity(new SendToQP(market1, q1, mktip1));
		exec.executeActivity(new SendToQP(market2, q4, mktip2));
		// Send updated quotes at time2
		exec.scheduleActivity(time2, new SendToQP(market1, q3, mktip1));
		exec.scheduleActivity(time2, new SendToQP(market2, q2, mktip2));
		
		// Check that both HFT IPs have updated after time2=50
		exec.executeUntil(time2);
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	/**
	 * Test markets updating twice in the same TimeStamp.
	 */
	@Test
	public void multiQuoteUpdatesAtSameTime() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime1 = new DummyMarketTime(time, 1);
		MarketTime mktTime2 = new DummyMarketTime(time, 2);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime1);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime2);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, mktTime2);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, mktTime1);
		
		exec.executeActivity(new SendToQP(market1, q1, mktip1));
		exec.executeActivity(new SendToQP(market2, q4, mktip2));
		// Send updated quotes (also at time 0)
		exec.executeActivity(new SendToQP(market1, q3, mktip1));
		exec.executeActivity(new SendToQP(market2, q2, mktip2));
		
		// Check market1's SMIP has updated but not market2's after time 0
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	/**
	 * Test handling of stale quotes
	 */
	@Test
	public void staleQuotes() {
		Quote q;
		MarketTime mktTime1 = new DummyMarketTime(ZERO, 1);
		MarketTime mktTime2 = new DummyMarketTime(ZERO, 2);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime2);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime1);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, mktTime1);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, mktTime2);
		
		exec.executeActivity(new SendToQP(market1, q1, mktip1));
		exec.executeActivity(new SendToQP(market2, q4, mktip2));
		
		// Check market1's HFT IP has updated but not market2's after time 0
		exec.executeUntil(ZERO);
		
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
				
		// Send stale quotes to HFT IPs
		exec.executeActivity(new SendToQP(market1, q3, mktip1));
		exec.executeActivity(new SendToQP(market2, q2, mktip2));
		
		// Check that market1's HFT IP quote doesn't change
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Incorrect last quote time", mktTime2, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());		
	}
	
	/**
	 * Test scenario where marketTime of two quotes is the same. Only works for
	 * markets with latency IMMEDIATE (otherwise will be nondeterministic).
	 * 
	 * It is an invariant that every quote in a market will have a unique MarketTime.
	 */
	@Test
	public void sameMarketTime() {
		Quote q;
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime);
		Quote q2 = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime);
		Quote q3 = new Quote(market1, new Price(75), 1, new Price(95), 1, mktTime);
		Quote q4 = new Quote(market2, new Price(75), 1, new Price(95), 1, mktTime);
		
		exec.executeActivity(new SendToQP(market1, q1, mktip1));
		exec.executeActivity(new SendToQP(market2, q4, mktip2));
		
		// Check market1's SMIP has updated but not market2's after time 0
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Updated SMIP 2 too early", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		
		// Send stale quotes to SMIPs
		exec.executeActivity(new SendToQP(market1, q3, mktip1));
		exec.executeActivity(new SendToQP(market2, q2, mktip2));
		
		// Check that market1's SMIP quote updates to most recent quote (of same market time)
		q = mktip1.quote;
		assertEquals("Incorrect last quote time", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(95), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(75), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
		q = mktip2.quote;
		assertEquals("Updated SMIP 2 too early", mktTime, q.getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), q.getAskPrice());
		assertEquals("Incorrect BID", new Price(80), q.getBidPrice());
		assertEquals("Incorrect ASK quantity", 1, q.getAskQuantity());
		assertEquals("Incorrect BID quantity", 1, q.getBidQuantity());
	}
	
	@Test
	public void extraTest() {
		for(int i=0; i < 100; i++) {
			setup();
			staleQuotes();
			setup();
			sameMarketTime();
		}
	}
}
