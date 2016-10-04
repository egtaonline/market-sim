package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import activity.SubmitOrder;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * @author yngchen, ewah
 *
 */
public class WindowAgentTest {

	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;
	private static Random rand;
	
	@BeforeClass
	public static void setUpClass() throws IOException{
		
		// Setting up the log file
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "WindowAgentTest.log"));
	}

	@Before
	public void setup(){
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new CDAMarket(exec, sip, new Random(), TimeStamp.IMMEDIATE, 1);
		
		rand = new Random(1);
	}
	
	@Test
	public void basicWindowTest() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = TimeStamp.create(1);
		TimeStamp time10 = TimeStamp.create(10);
		
		WindowAgent agent = new MockWindowAgent(exec, fundamental, sip, market, 10);
		
		assertEquals("Incorrect initial transactions in window", 0, 
				agent.getWindowTransactions(time).size());
		
		// populate market with a transaction
		MockBackgroundAgent background1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent background2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		exec.executeActivity(new SubmitOrder(background1, market, BUY, new Price(111), 1));
		exec.executeActivity(new SubmitOrder(background2, market, SELL, new Price(110), 1));
		
		// basic window check
		Transaction trans = market.getTransactions().get(0);
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions(time1).size());
		assertEquals("Incorrect window transactions", trans, agent.getWindowTransactions(time1).get(0));
		trans = sip.getTransactions().get(0);
		assertEquals("Incorrect window transactions", trans, agent.getWindowTransactions(time1).get(0));
	
		// checking just inside window
		trans = market.getTransactions().get(0);
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions(time10.minus(time1)).size());
		assertEquals("Incorrect window transactions", trans, agent.getWindowTransactions(time10.minus(time1)).get(0));
		// check outside window
		assertEquals("Incorrect # transactions", 0, agent.getWindowTransactions(time10).size());
	}
	
	@Test
	public void delayedTransactionProcessorTest() {
		TimeStamp time1 = TimeStamp.create(1);
		TimeStamp time5 = TimeStamp.create(5);
		TimeStamp time10 = TimeStamp.create(10);
		Market market = new CDAMarket(exec, sip, new Random(), TimeStamp.create(5), 1);
		
		WindowAgent agent = new MockWindowAgent(exec, fundamental, sip, market, 10);
		
		// populate market with a transaction
		MockBackgroundAgent background1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent background2 = new MockBackgroundAgent(exec, fundamental, sip, market);
		exec.executeActivity(new SubmitOrder(background1, market, BUY, new Price(111), 1));
		exec.executeActivity(new SubmitOrder(background2, market, SELL, new Price(110), 1));
		exec.executeUntil(time1);
		exec.executeActivity(new SubmitOrder(background1, market, BUY, new Price(104), 1));
		exec.executeActivity(new SubmitOrder(agent, market, SELL, new Price(102), 1));
		
		// test getting transactions at time 5 - should be missing the second transaction
		exec.executeUntil(time5);
		assertEquals("Incorrect # transactions", 1, agent.getWindowTransactions(time5).size());
		List<Transaction> trans = agent.getWindowTransactions(time5);
		assertTrue("Transaction incorrect", trans.contains(market.getTransactions().get(0)));
		
		// check time 10
		exec.executeUntil(time10);
		trans = agent.getWindowTransactions(time10);
		assertEquals("Incorrect # transactions", 1, trans.size());
		assertEquals("Transaction price incorrect", new Price(104), trans.get(0).getPrice());
		assertEquals("Transaction seller incorrect", agent, trans.get(0).getSeller());
		assertEquals("Transaction buyer incorrect", background1, trans.get(0).getBuyer());
		
		// check outside window (at time 11)
		exec.executeUntil(TimeStamp.create(11));
		trans = agent.getWindowTransactions(time10.plus(time1));
		assertEquals("Incorrect # transactions", 0, trans.size());
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			randMultipleTransactionWindow();
		}
	}
	
	@Test 
	public void addTransactionTest() {
		addTransaction(market, 10, 1, 10);
		List<Transaction> transactions = market.getTransactionProcessor().getTransactions();
		assertEquals("Number of transactions is incorrect", 1, transactions.size());
	}
	
	@Test
	public void singleTransactionWindow() {
		//Instantiate a WindowAgent
		WindowAgent myAgent = new MockWindowAgent(exec, fundamental, sip, market, 10);
		assertEquals("WindowAgent Window Length is incorrect", TimeStamp.create(10), myAgent.windowLength);
		//Add a transaction to the market
		addTransaction(market, 20, 1, 10);
		//Retrieve transactions via the WindowAgent's getWindowTransactions
		List<Transaction> windowTransactions = myAgent.getWindowTransactions(TimeStamp.create(15));
		//Test size of returned transactions
		assertEquals("Number of transactions is not correct", 1, windowTransactions.size());
		//Test qualities of returned transaction
		assertEquals("Quantity of transaction is not correct", 1, windowTransactions.get(0).getQuantity());
		assertEquals("Price of transaction is not correct", new Price(20), windowTransactions.get(0).getPrice());
		
		
	}
	
	@Test
	public void multipleTransactionWindow() {
		WindowAgent myAgent = new MockWindowAgent(exec, fundamental, sip, market, 10);
		assertEquals("WindowAgent Window Length is incorrect", TimeStamp.create(10), myAgent.getWindowLength());
		addTransaction(market, 40, 1, 2);  //Not Included
		addTransaction(market, 30, 1, 3);  //Not Included
		addTransaction(market, 60, 1, 5);  //Not Included
		addTransaction(market, 10, 1, 10); //Included
		addTransaction(market, 20, 1, 11); //Included
		addTransaction(market, 50, 1, 14); //Included

		List<Transaction> windowTransactions = myAgent.getWindowTransactions(TimeStamp.create(15)); //Window is from (5,15]
		//Test size of returned transactions
		assertEquals("Number of transactions is not correct", 3, windowTransactions.size());
		//Test qualities of returned transactions
		assertEquals("Quantity of transaction one is not correct", 1, windowTransactions.get(0).getQuantity());
		assertEquals("Price of transaction one is not correct", new Price(10), windowTransactions.get(0).getPrice());
		assertEquals("Quantity of transaction two is not correct", 1, windowTransactions.get(1).getQuantity());
		assertEquals("Price of transaction two is not correct", new Price(20), windowTransactions.get(1).getPrice());
		
	}
	
	@Test
	public void multipleTransactionWindowLatency(){
		Market market = new CDAMarket(exec, sip, new Random(), TimeStamp.create(100), 1);
		WindowAgent myAgent = new MockWindowAgent(exec, fundamental, sip, market, 160);
		List<Transaction> windowTransactions;
		assertEquals("WindowAgent Window Length is incorrect", TimeStamp.create(160), myAgent.getWindowLength());
		
		//Create mock background agents to transact
		MockBackgroundAgent agent_S = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent_B = new MockBackgroundAgent(exec, fundamental, sip, market);
		
		//Timestamps for the first transaction and the time to execute up to
		TimeStamp t_50 = TimeStamp.create(50);
		//Timestamps for the second transaction and the time to execute up to
		TimeStamp t_100 = TimeStamp.create(100);
		//Timestamps for important intervals to check the window
		TimeStamp t_150 = TimeStamp.create(150);
		TimeStamp t_200 = TimeStamp.create(200);
		TimeStamp t_250 = TimeStamp.create(250);
		TimeStamp t_300 = TimeStamp.create(300);

		//Execute first transaction
		exec.executeUntil(t_50);
		exec.executeActivity(new SubmitOrder(agent_S, market, SELL, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent_B, market, BUY, new Price(40), 1));
		//Assert that the agent can't see the transaction due to latency
		windowTransactions = myAgent.getWindowTransactions(t_50);
		assertTrue("Window Transactions should be empty", windowTransactions.isEmpty());
		
		//Execute second transaction
		exec.executeUntil(t_100);
		exec.executeActivity(new SubmitOrder(agent_S, market, SELL, new Price(60), 1));
		exec.executeActivity(new SubmitOrder(agent_B, market, BUY, new Price(60), 1));
		//Assert that the agent can't see the transaction due to latency
		windowTransactions = myAgent.getWindowTransactions(t_100);
		assertTrue("Window Transactions should be empty", windowTransactions.isEmpty());
		
		//Execute up to when the first transaction comes into the window
		exec.executeUntil(t_150);
		//Assert that the window returns one transaction
		windowTransactions = myAgent.getWindowTransactions(t_150);
		assertEquals("Window Transactions should be size 1", 1, windowTransactions.size());
		
		//Execute up to when the second transaction comes into the window
		exec.executeUntil(t_200);
		//Assert that the window returns two transactions
		windowTransactions = myAgent.getWindowTransactions(t_200);
		assertEquals("Window Transactions should be size 2", 2, windowTransactions.size());
		
		//Execute up to when the first transaction leaves the window
		exec.executeUntil(t_250);
		//Assert that the window returns one transaction
		windowTransactions = myAgent.getWindowTransactions(t_250);
		assertEquals("Window Transactions should be size 1", 1, windowTransactions.size());
		
		//Execute up to when all transactions leave the window
		exec.executeUntil(t_300);
		//Assert that the window returns no transactions
		windowTransactions = myAgent.getWindowTransactions(t_300);
		assertTrue("Window Transactions should be empty", windowTransactions.isEmpty());
	}
	
	
	@Test
	public void randMultipleTransactionWindow(){
		//Window length
		int windowLength = rand.nextInt(100);     
		//Re-entry Time must be greater than window length
		int reentryTime = windowLength + rand.nextInt(100);
		//Instantiate WindowAgent
		WindowAgent myAgent = new MockWindowAgent(exec, fundamental, sip, market, windowLength);
		assertEquals("WindowAgent Window Length is incorrect", TimeStamp.create(windowLength), myAgent.getWindowLength());
		
		//Keep track of how many transactions should be in the window
		int numWindow = 0;             
		int[] transactionTimes = new int[10];
		for(int j = 0; j < 10; j++){
			//Transaction times must be before re entry time
			transactionTimes[j] = rand.nextInt(reentryTime);       
			if(transactionTimes[j] > reentryTime - windowLength){
				//If transaction time falls in the window, increment
				numWindow++;           
			}
		}
		//Sort transaction times into ascending order so we can add transactions in the proper order
		Arrays.sort(transactionTimes);
		for(int t : transactionTimes)
			addTransaction(market, 100, 1, t);
		
		log.log(DEBUG, "Transaction times: %s Window Length: %d Re entry Time %d", Arrays.toString(transactionTimes), windowLength, reentryTime);

		List<Transaction> windowTransactions = myAgent.getWindowTransactions(TimeStamp.create(reentryTime));
		assertEquals("Number of transactions is not correct", numWindow, windowTransactions.size());
		assertEquals("Price of transactions is not correct", new Price(100), 
				windowTransactions.get(rand.nextInt(numWindow)).getPrice());
	}
	
	//Testing methods==============================================================================

	private void addTransaction(Market m, int p, int q, int time) {
		TimeStamp t = TimeStamp.create(time);
		MockBackgroundAgent agent_S = new MockBackgroundAgent(exec, fundamental, sip, m);
		MockBackgroundAgent agent_B = new MockBackgroundAgent(exec, fundamental, sip, m);

		exec.executeUntil(t);
		exec.executeActivity(new SubmitOrder(agent_S, m, SELL, new Price(p), q));
		exec.executeActivity(new SubmitOrder(agent_B, m, BUY, new Price(p), q));
	}
	
}
