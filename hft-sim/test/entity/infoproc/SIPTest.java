package entity.infoproc;

import static event.TimeStamp.ZERO;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import activity.ProcessQuote;
import activity.SendToQP;
import activity.SubmitNMSOrder;
import activity.SubmitOrder;

import com.google.common.collect.Iterables;

import data.FundamentalValue;
import data.MockFundamental;
import entity.agent.Agent;
import entity.agent.MockAgent;
import entity.agent.MockBackgroundAgent;
import entity.market.CDAMarket;
import entity.market.DummyMarketTime;
import entity.market.Market;
import entity.market.MarketTime;
import entity.market.MockMarket;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;
import event.TimedActivity;

public class SIPTest {

	/*
	 * TODO Undefined order between markets? Transactions for a market should be
	 * in proper order, but if several markets had a transaction at the same
	 * time, the order will be undefined / random
	 */
	private Executor exec;
	private Market market1;
	private Market market2;
	private SIP sip;
	private SIP sip2;
	private FundamentalValue fundamental;
	
	@BeforeClass
	public static void setupClass() throws IOException {
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "SIPTest.log"));
	}
	
	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		sip2 = new SIP(exec, TimeStamp.create(50));
		// market that updates immediately
		market1 = new MockMarket(exec, sip);
		// market with latency 100
		market2 = new MockMarket(exec, sip, TimeStamp.create(100));
		fundamental = new MockFundamental(10000);
	}
	
	@Test
	public void basicQuote() {
		TimeStamp time = TimeStamp.ZERO;
		
		// Test initial NBBO quote
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals(null, nbbo.bestAsk);
		assertEquals(null, nbbo.bestBid);
		assertEquals(0, nbbo.bestAskQuantity);
		assertEquals(0, nbbo.bestBidQuantity);
		assertEquals(null, nbbo.bestAskMarket);
		assertEquals(null, nbbo.bestBidMarket);
		assertEquals(0, sip.marketQuotes.size());
		
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		sip.processQuote(market1, q1, time);
		
		// Test that NBBO quote is correct
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(1, sip.marketQuotes.size());
	}
	
	@Test
	public void multiQuote() {
		TimeStamp time = TimeStamp.ZERO;
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		sip.processQuote(market1, q1, time);
		
		Quote q2 = new Quote(market1, new Price(70), 1, new Price(90), 1, time);
		sip.processQuote(market1, q2, time);
		
		// Test that NBBO quote is correct (completely replaces old quote of [80, 100])
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(90), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(70), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(1, sip.marketQuotes.size());
	}
	
	@Test
	public void staleQuote() {
		TimeStamp time = TimeStamp.create(10);
		
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, new DummyMarketTime(time, 2));
		sip.processQuote(market1, q1, time);
		
		// Test that NBBO quote is correct
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(new DummyMarketTime(time, 2), sip.marketQuotes.get(market1).getQuoteTime());
		
		// Note that staleness is based solely on MarketTime (not timestamp)
		Quote q2 = new Quote(market1, new Price(70), 1, new Price(90), 1, TimeStamp.ZERO);
		sip.processQuote(market1, q2, time);
		
		// Test that NBBO quote is correct (ignores stale quote q2)
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(new DummyMarketTime(time, 2), sip.marketQuotes.get(market1).getQuoteTime());
	}
	
	@Test
	public void twoMarketQuote() {
		TimeStamp time = TimeStamp.ZERO;
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		sip.processQuote(market1, q1, time);
		
		Quote q2 = new Quote(market2, new Price(70), 1, new Price(90), 1, time);
		sip.processQuote(market2, q2, time);
		
		// Test that NBBO quote is correct (computes best quote between both markets)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(90), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market2, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(2, sip.marketQuotes.size());
	}
	
	@Test
	public void twoMarketMultiQuote() {
		TimeStamp time = TimeStamp.ZERO;
		Quote q1 = new Quote(market1, new Price(85), 1, new Price(100), 1, new DummyMarketTime(time, 1));
		Quote q2 = new Quote(market2, new Price(75), 1, new Price(95), 1, new DummyMarketTime(time, 2));
		Quote q3 = new Quote(market1, new Price(65), 1, new Price(90), 1, new DummyMarketTime(time, 3));
		
		sip.processQuote(market1, q1, time);
		sip.processQuote(market2, q2, time);
		sip.processQuote(market1, q3, time);
				
		// Test that NBBO quote is correct & that market 1's quote was replaced
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(90), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(75), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market2, nbbo.bestBidMarket);
		assertEquals(new DummyMarketTime(time, 3), sip.marketQuotes.get(market1).getQuoteTime());
		assertEquals(new DummyMarketTime(time, 2), sip.marketQuotes.get(market2).getQuoteTime());
		
		Quote q4 = new Quote(market2, new Price(60), 1, new Price(91), 1, new DummyMarketTime(time, 4));
		sip.processQuote(market2, q4, time);
		
		// Test that NBBO quote is correct & that market 2's quote was replaced
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(90), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(65), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		assertEquals(new DummyMarketTime(time, 3), sip.marketQuotes.get(market1).getQuoteTime());
		assertEquals(new DummyMarketTime(time, 4), sip.marketQuotes.get(market2).getQuoteTime());

		/*
		 * XXX NOTE: if tie in price, nondeterminism in which market has best
		 * price.
		 */
	}
	
	@Test
	public void basicNoDelay() {
		TimeStamp time = TimeStamp.ZERO;
		MarketTime mktTime = new DummyMarketTime(time, 1);

		// Add new quote
		Quote q = new Quote(market1, new Price(80), 1, new Price(100), 2, mktTime);
		sip.sendToQuoteProcessor(market1, q, time);
		// Verify correct process quote activity inserted right after
		assertTrue(exec.isEmpty());

		// Test that NBBO quote is correct
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 2, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
	}

	@Test
	public void basicDelay() {
		MarketTime mktTime = new DummyMarketTime(ZERO, 1);
		
		// Check that process quote activity scheduled correctly
		Quote q = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime);
		sip2.sendToQuoteProcessor(market1, q, ZERO);
		// Verify correct process quote activity added to execute at time 50
		exec.executeUntil(TimeStamp.create(49));
		TimedActivity act = exec.peek();
		assertTrue(act.getActivity() instanceof ProcessQuote);
		assertEquals(TimeStamp.create(50), act.getTime());
		exec.executeUntil(TimeStamp.create(50));

		// Test that NBBO quote is correct
		BestBidAsk nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
	}
	
	@Test
	public void basicZeroDelay() {
		// SIP with zero not immediate latency
		SIP sip3 = new SIP(exec, ZERO);
		// Check that process quote activity scheduled correctly
		Quote q = new Quote(market1, new Price(80), 1, new Price(100), 1, ZERO);
		exec.executeActivity(new SendToQP(market1, q, sip3));
		BestBidAsk nbbo = sip3.getNBBO();
		assertEquals("Incorrect ASK", null, nbbo.bestAsk);
		assertEquals("Incorrect BID", null, nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 0, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 0, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", null, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", null, nbbo.bestBidMarket);
		
		// Test that NBBO quote is correct after time 0
		exec.executeUntil(ZERO);
		nbbo = sip3.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(80), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
	}
	
	@Test
	public void alternateDelay() {
		MarketTime mktTime = new DummyMarketTime(ZERO, 1);
		Quote q = new Quote(market2, new Price(80), 1, new Price(100), 1, mktTime);
		
		// Send quotes to SIP. Market 2 has a delay of 100, SIP2 has delay of 50
		exec.executeActivity(new SendToQP(market2, q, sip2));
		exec.executeActivity(new SendToQP(market2, q, sip));
		
		// Check immediate SIP
		assertEquals("Last quote time not updated", mktTime, sip.marketQuotes.get(market2).getQuoteTime());
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", q.getAskPrice(), nbbo.bestAsk);
		assertEquals("Incorrect BID", q.getBidPrice(), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market2, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market2, nbbo.bestBidMarket);
		// Check delayed SIP not updated
		assertEquals("Updated delayed SIP too early", null, sip2.marketQuotes.get(market2));
		assertEquals("Incorrect ASK", null, sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", null, sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 0, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 0, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", null, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", null, sip2.getNBBO().bestBidMarket);
		assertEquals(0, sip2.marketQuotes.size());
		
		exec.executeUntil(sip2.getLatency());
		// Check delayed SIP has been updated
		assertEquals("Last quote time not updated", mktTime, sip2.marketQuotes.get(market2).getQuoteTime());
		assertEquals("Incorrect ASK", new Price(100), sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", new Price(80), sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 1, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", market2, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", market2, sip2.getNBBO().bestBidMarket);
		assertEquals(1, sip2.marketQuotes.size());
	}
	
	@Test
	public void eventManagerLatencyTest() {
		MarketTime mktTime = new DummyMarketTime(ZERO, 1);
		MarketTime mktTime2 = new DummyMarketTime(TimeStamp.create(30), 2);
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, mktTime);
		Quote q2 = new Quote(market2, new Price(75), 1, new Price(95), 2, mktTime2);
		
		// Send quotes to both IPs
		exec.executeActivity(new SendToQP(market1, q1, sip));
		exec.executeActivity(new SendToQP(market1, q1, sip2));
		
		// Check that no quotes have updated in slow sip yet (fast sip should update)
		assertEquals("Updated delayed SIP too early", null, sip2.marketQuotes.get(market2));
		assertEquals("Incorrect ASK", null, sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", null, sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 0, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 0, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", null, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", null, sip2.getNBBO().bestBidMarket);
		
		
		// Check immediate SIP updated with quote 1
		assertEquals("Last quote time not updated", mktTime, sip.marketQuotes.get(market1).getQuoteTime());
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", q1.getAskPrice(), nbbo.bestAsk);
		assertEquals("Incorrect BID", q1.getBidPrice(), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		// Check delayed SIP not updated
		assertEquals("Updated delayed SIP too early", null, sip2.marketQuotes.get(market1));
		assertEquals("Incorrect ASK", null, sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", null, sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 0, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 0, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", null, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", null, sip2.getNBBO().bestBidMarket);
		
		// Send more quotes to SIPs but only execute up to SIP2 latency of 100
		// so only first quote of [80, 100] should reach the delayed SIP
		exec.executeUntil(TimeStamp.create(30));
		exec.executeActivity(new SendToQP(market2, q2, sip));
		exec.executeActivity(new SendToQP(market2, q2, sip2));
		exec.executeUntil(sip2.getLatency());
		// Check immediate SIP updated with quote 2
		assertEquals("Last quote time not updated", mktTime, sip.marketQuotes.get(market1).getQuoteTime());
		assertEquals("Last quote time not updated", mktTime2, sip.marketQuotes.get(market2).getQuoteTime());
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", q2.getAskPrice(), nbbo.bestAsk);
		assertEquals("Incorrect BID", q1.getBidPrice(), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 2, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", market2, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market1, nbbo.bestBidMarket);
		// Check delayed SIP updated only with quote 1
		assertEquals("Last quote time not updated", mktTime, sip2.marketQuotes.get(market1).getQuoteTime());
		assertEquals("Updated delayed SIP too early", null, sip2.marketQuotes.get(market2));
		assertEquals("Incorrect ASK", q1.getAskPrice(), sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", q1.getBidPrice(), sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 1, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", market1, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", market1, sip2.getNBBO().bestBidMarket);
		
		// Delayed SIP won't update until after 100 time steps after the second 
		// quote was submitted (which was at time 30), so need to execute up to time 130
		exec.executeUntil(sip2.getLatency().plus(TimeStamp.create(30)));
		// Check delayed SIP updated finally with quote 2
		assertEquals("Last quote time not updated", mktTime, sip2.marketQuotes.get(market1).getQuoteTime());
		assertEquals("Last quote time not updated", mktTime2, sip2.marketQuotes.get(market2).getQuoteTime());
		assertEquals("Incorrect ASK", q2.getAskPrice(), sip2.getNBBO().bestAsk);
		assertEquals("Incorrect BID", q1.getBidPrice(), sip2.getNBBO().bestBid);
		assertEquals("Incorrect ASK quantity", 2, sip2.getNBBO().bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, sip2.getNBBO().bestBidQuantity);
		assertEquals("Incorrect ASK market", market2, sip2.getNBBO().bestAskMarket);
		assertEquals("Incorrect BID market", market1, sip2.getNBBO().bestBidMarket);
	}
	
	@Test
	public void transactionsInSIP() {
		FundamentalValue fundamental = new MockFundamental(100000);
		Market market = new CDAMarket(exec, sip, new Random(), TimeStamp.IMMEDIATE, 1);
		
		//Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(150), 2));
		// should execute clear since CDA
		
		// Verify that NBBO quote has updated
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", null, nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(150), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 0, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 2, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", null, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market, nbbo.bestBidMarket);
		
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(140), 1));
		// should execute Clear-->SendToSIP-->processInformations
		
		// Verify that transactions has updated as well as NBBO
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", null, nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(150), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 0, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", null, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market, nbbo.bestBidMarket);
		List<Transaction> trans = sip.getTransactions();
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());
	}
	
	@Test
	public void transactionsInDelayedSIP() {
		FundamentalValue fundamental = new MockFundamental(100000);
		SIP sip = new SIP(exec, TimeStamp.create(100));
		Market market = new CDAMarket(exec, sip, new Random(), TimeStamp.IMMEDIATE, 1);
		
		//Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(150), 2));
		// should execute clear since CDA
		
		// Verify that no NBBO quote yet
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals(null, nbbo.bestAsk);
		assertEquals(null, nbbo.bestBid);
		assertEquals(0, nbbo.bestAskQuantity);
		assertEquals(0, nbbo.bestBidQuantity);
		assertEquals(null, nbbo.bestAskMarket);
		assertEquals(null, nbbo.bestBidMarket);
		assertEquals(0, sip.marketQuotes.size());
			
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(140), 1));
		// should execute Clear-->SendToSIP-->ProcessQuotes
		
		// Verify that transactions has updated as well as NBBO
		exec.executeUntil(sip.getLatency()); // because of SIP latency
		nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", null, nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(150), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 0, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", null, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", market, nbbo.bestBidMarket);
		List<Transaction> trans = sip.getTransactions();
		assertEquals("Incorrect number of transactions", 1, trans.size());
		assertEquals("Incorrect transaction price", new Price(150), trans.get(0).getPrice());
		assertEquals("Incorrect transaction quantity", 1, trans.get(0).getQuantity());
		assertEquals("Incorrect buyer", agent1, trans.get(0).getBuyer());
		assertEquals("Incorrect buyer", agent2, trans.get(0).getSeller());
	}
	
	@Test
	public void basicOrderRoutingNMS() {
		FundamentalValue fundamental = new MockFundamental(100000);
		TimeStamp time = TimeStamp.create(50);
		
		// Set up CDA markets and their quotes
		Market nasdaq = new CDAMarket(exec, sip2, new Random(), time, 1);
		Market nyse = new CDAMarket(exec, sip2, new Random(), time, 1);
		MockBackgroundAgent background1 = new MockBackgroundAgent(exec, fundamental, sip2, nyse);
		MockBackgroundAgent background2 = new MockBackgroundAgent(exec, fundamental, sip2, nasdaq);
		exec.executeActivity(new SubmitOrder(background1, nyse, SELL, new Price(111), 1));
		exec.executeActivity(new SubmitOrder(background1, nasdaq, BUY, new Price(104), 1));
		exec.executeActivity(new SubmitOrder(background2, nasdaq, SELL, new Price(110), 1));
		exec.executeActivity(new SubmitOrder(background2, nyse, BUY, new Price(102), 1));
		exec.executeUntil(time);

		// Verify that NBBO quote is (104, 110) at time 50 (after quotes have been processed by SIP)
		BestBidAsk nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(110), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(104), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nasdaq, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
		
		///////////////
		// Creating dummy agent & submit sell order
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip2, nyse);
		exec.executeActivity(new SubmitNMSOrder(agent1, nyse, SELL, new Price(105), 1, time));
		exec.executeUntil(TimeStamp.create(99));
		nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(110), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(104), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nasdaq, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
		
		// Verify that NBBO quote is (104, 105) at time 100 (after quotes have been processed by SIP)
		exec.executeUntil(TimeStamp.create(100));
		nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(105), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(104), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nyse, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
		
		// Another agent submits a buy order
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip2, nasdaq);
		exec.executeActivity(new SubmitNMSOrder(agent2, nasdaq, BUY, new Price(109), 1));
		
		// Verify that order is routed to nyse and transacts immediately w/ agent1's order
		assertEquals(1, nyse.getTransactions().size());
		assertEquals(0, nasdaq.getTransactions().size());
		Transaction t = Iterables.getFirst(nyse.getTransactions(), null);
		assertEquals(nyse, t.getMarket());
		assertEquals(new Price(105), t.getPrice());
		assertEquals(agent1, t.getSeller());
		assertEquals(agent2, t.getBuyer());
	}

	@Test
	public void latencyArbRoutingNMS() {
		FundamentalValue fundamental = new MockFundamental(100000);
		TimeStamp time = TimeStamp.create(50);
		
		// Set up markets and their quotes
		// both markets are undelayed (not true, but irrelevant), although SIP is delayed by 50
		Market nasdaq = new CDAMarket(exec, sip2, new Random(), time, 1);
		Market nyse = new CDAMarket(exec, sip2, new Random(), time, 1);
		MockBackgroundAgent background1 = new MockBackgroundAgent(exec, fundamental, sip2, nyse);
		MockBackgroundAgent background2 = new MockBackgroundAgent(exec, fundamental, sip2, nasdaq);
		exec.executeActivity(new SubmitOrder(background1, nyse, SELL, new Price(111), 1));
		exec.executeActivity(new SubmitOrder(background1, nasdaq, BUY, new Price(104), 1));
		exec.executeActivity(new SubmitOrder(background2, nasdaq, SELL, new Price(110), 1));
		exec.executeActivity(new SubmitOrder(background2, nyse, BUY, new Price(102), 1));
		exec.executeUntil(time);
		
		///////////////
		// Creating dummy agent & submit sell order
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip2, nyse);
		exec.executeActivity(new SubmitNMSOrder(agent1, nyse, SELL, new Price(105), 1));
		
		// Verify that NBBO quote is still (104, 110) (hasn't updated yet)
		BestBidAsk nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(110), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(104), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nasdaq, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
		
		// Another agent submits a buy order
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip2, nasdaq);
		exec.executeActivity(new SubmitNMSOrder(agent2, nasdaq, BUY, new Price(109), 1));
		exec.executeUntil(TimeStamp.create(99));
		// Verify that NBBO quote is still (104, 110) (hasn't updated yet)
		nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(110), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(104), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nasdaq, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
		
		// The buy order was still routed to Nasdaq, so the NBBO will cross and 
		// there is a latency arbitrage opportunity
		exec.executeUntil(TimeStamp.create(100));
		// Verify that NBBO quote is now (109, 105)
		nbbo = sip2.getNBBO();
		assertEquals("Incorrect ASK", new Price(105), nbbo.bestAsk);
		assertEquals("Incorrect BID", new Price(109), nbbo.bestBid);
		assertEquals("Incorrect ASK quantity", 1, nbbo.bestAskQuantity);
		assertEquals("Incorrect BID quantity", 1, nbbo.bestBidQuantity);
		assertEquals("Incorrect ASK market", nyse, nbbo.bestAskMarket);
		assertEquals("Incorrect BID market", nasdaq, nbbo.bestBidMarket);
	}
	
	@Test
	public void transactionOrderingTest() {
		SIP sip = new SIP(exec, ZERO); // ZERO not IMMEDIATE
		Market market = new CDAMarket(exec, sip, new Random(), TimeStamp.IMMEDIATE, 
				TimeStamp.IMMEDIATE, 1);
		
		Agent one = new MockAgent(exec, fundamental, sip);
		Agent two = new MockAgent(exec, fundamental, sip);
		
		// First order
		market.submitOrder(one, BUY, new Price(100), 1, ZERO);
		market.submitOrder(two, SELL, new Price(100), 1, ZERO);
		
		// Second order
		market.submitOrder(two, BUY, new Price(200), 2, ZERO);
		market.submitOrder(one, SELL, new Price(200), 2, ZERO);
		
		// Hasn't propogated to transaction processor yet
		assertTrue(sip.getTransactions().isEmpty());
		
		// Execute until proper time
		exec.executeUntil(sip.getLatency());
		// Now both transactions should show up
		assertEquals(2, sip.getTransactions().size());
		
		// The real test is to check that they're in the proper order
		Transaction first = sip.getTransactions().get(0);
		assertEquals(1, first.getQuantity());
		assertEquals(new Price(100), first.getPrice());
		assertEquals(one, first.getBuyer());
		assertEquals(two, first.getSeller());
		
		Transaction second = sip.getTransactions().get(1);
		assertEquals(2, second.getQuantity());
		assertEquals(new Price(200), second.getPrice());
		assertEquals(two, second.getBuyer());
		assertEquals(one, second.getSeller());
	}
	
	@Test
	public void repeatTransactionOrderingTest() {
		for (int i = 0; i < 1000; ++i) {
			setup();
			transactionOrderingTest();
		}
	}
	
	@Test
	public void transactionOrderingOtherMarketTest() {
		SIP sip = new SIP(exec, ZERO); // ZERO not IMMEDIATE
		Market market = new CDAMarket(exec, sip, new Random(), TimeStamp.IMMEDIATE, 
				TimeStamp.IMMEDIATE, 1);
		Market other = new CDAMarket(exec, sip, new Random(), TimeStamp.IMMEDIATE, 
				TimeStamp.IMMEDIATE, 1);
		
		Agent one = new MockAgent(exec, fundamental, sip);
		Agent two = new MockAgent(exec, fundamental, sip);
		
		// First order
		market.submitOrder(one, BUY, new Price(100), 1, ZERO);
		market.submitOrder(two, SELL, new Price(100), 1, ZERO);
		
		// Second order
		market.submitOrder(two, BUY, new Price(200), 2, ZERO);
		market.submitOrder(one, SELL, new Price(200), 2, ZERO);

		// Other
		other.submitOrder(two, BUY, new Price(300), 3, ZERO);
		other.submitOrder(one, SELL, new Price(300), 3, ZERO);

		// Hasn't propogated to transaction processor yet
		assertTrue(sip.getTransactions().isEmpty());
		
		// Execute until proper time
		exec.executeUntil(sip.getLatency());
		// Now both transactions should show up
		assertEquals(3, sip.getTransactions().size());
		
		int index = 0, firstIndex = Integer.MAX_VALUE, secondIndex = Integer.MIN_VALUE;
		for (Transaction trans : sip.getTransactions()) {
			++index;
			switch (trans.getQuantity()) {
			case 1:
				assertEquals(new Price(100), trans.getPrice());
				assertEquals(one, trans.getBuyer());
				assertEquals(two, trans.getSeller());
				firstIndex = index;
				break;
			case 2:
				assertEquals(new Price(200), trans.getPrice());
				assertEquals(two, trans.getBuyer());
				assertEquals(one, trans.getSeller());
				secondIndex = index;
				break;
			case 3:
				assertEquals(new Price(300), trans.getPrice());
				assertEquals(two, trans.getBuyer());
				assertEquals(one, trans.getSeller());
				break;
			default:
				fail();
			}
		}
		
		// Assert that the first order came in first. Don't care about the actual order
		assertTrue(firstIndex < secondIndex);
		// XXX This should be 1 2, 1 3, 2 3, but for some reason, I don't see 1 2. Not sure why
	}
	
	/*
	 * XXX SIP has uniform latency - would we ever want different latency from
	 * certain markets? Probably. This change shouldn't be too difficult.
	 */
}
