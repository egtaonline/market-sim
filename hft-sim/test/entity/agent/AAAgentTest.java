package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
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
import utils.Rands;

import com.google.common.collect.Iterables;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.agent.AAAgent.Aggression;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class AAAgentTest {

	private static Random rand;
	private static EntityProperties agentProperties;

	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;

	@BeforeClass
	public static void setupClass() throws IOException {
		// Setting up the log file
		Log.log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "AAAgentTest.log"));

		// Creating the setup properties
		rand = new Random();

		// Setting up default agentProperties
		agentProperties = EntityProperties.fromPairs(
				Keys.REENTRY_RATE, 0,
				Keys.MAX_POSITION, 10,
				Keys.ETA, 3,
				Keys.WITHDRAW_ORDERS, false,
				Keys.WINDOW_LENGTH, 5000,
				Keys.AGGRESSION, 0,
				Keys.THETA, 0,
				Keys.THETA_MIN, -4,
				Keys.THETA_MAX, 4,
				Keys.NUM_HISTORICAL, 5,
				Keys.LAMBDA_R, 0.05,
				Keys.LAMBDA_A, 0.02,	// x ticks/$ for Eq 10/11 
				Keys.GAMMA, 2,
				Keys.BETA_R, 0.4,
				Keys.BETA_T, 0.4,
				Keys.DEBUG, true);
	}

	@Before
	public void setupTest() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
	}

	@Test
	public void tauChange() {
		double r = 0.5;
		AAAgent agent = addAgent(BUY);
		agent.theta = 2;
		assertEquals(0.268, agent.tauChange(r), 0.001);
	}

	/**
	 * Computation of moving average for estimating the equilibrium price.
	 */
	@Test
	public void estimateEquilibrium() {
		TimeStamp time = TimeStamp.ZERO;
		
		// Creating a buyer
		AAAgent agent = addAgent(BUY, EntityProperties.copyFromPairs(agentProperties,
				Keys.NUM_HISTORICAL, 3,
				Keys.PRIVATE_VALUE_VAR, 5E7));
		
		assertNull(agent.estimateEquilibrium(agent.getWindowTransactions(time)));
		
		// Adding Transactions and Bids
		addTransaction(75000, 1, 20);
		addTransaction(90000, 1, 25);

		// not enough transactions, need to use only 2
		double[] weights = {1/1.9, 0.9/1.9};
		assertEquals(Math.round(weights[0]*75000 + weights[1]*90000),
				agent.estimateEquilibrium(agent.getWindowTransactions(time)).intValue());
		
		// sufficient transactions
		addTransaction(100000, 1, 25);
		double total = 2.71;
		double[] weights2 = {1/total, 0.9/total, 0.81/total};
		assertEquals(Math.round(weights2[0]*75000 + weights2[1]*90000 + weights2[2]*100000),
				agent.estimateEquilibrium(agent.getWindowTransactions(time)).intValue());
	}
	
	@Test
	public void computeRShoutBuyer() {
		AAAgent buyer = addAgent(BUY, EntityProperties.fromPairs(Keys.THETA, -2));
		
		Price limit = new Price(110000);
		Price last = new Price(105000);
		Price equil = new Price(105000);
		
		// Intramarginal (limit > equil)
		assertEquals(0, buyer.computeRShout(limit, last, equil), 0.001);
		last = new Price(100000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(buyer.computeRShout(limit, last, equil)), 0.001);
		last = new Price(109000);	// more aggressive (lower margin)
		assertEquals(1, Math.signum(buyer.computeRShout(limit, last, equil)), 0.001);
		
		// Extramarginal
		equil = new Price(111000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(buyer.computeRShout(limit, last, equil)), 0.001);
		last = limit;
		assertEquals(0, buyer.computeRShout(limit, last, equil), 0.001);
	}
	
	@Test
	public void computeRShoutSeller() {
		AAAgent seller = addAgent(SELL, EntityProperties.fromPairs(Keys.THETA, -2));

		Price limit = new Price(105000);
		Price last = new Price(110000);
		Price equil = new Price(110000);
		
		// Intramarginal (limit < equil)
		assertEquals(0, seller.computeRShout(limit, last, equil), 0.001);
		last = new Price(111000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(seller.computeRShout(limit, last, equil)), 0.001);
		last = new Price(109000);	// more aggressive (lower margin)
		assertEquals(1, Math.signum(seller.computeRShout(limit, last, equil)), 0.001);

		// Extramarginal
		equil = new Price(104000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(seller.computeRShout(limit, last, equil)), 0.001);
		last = limit;
		assertEquals(0, seller.computeRShout(limit, last, equil), 0.001);
	}

	@Test
	public void determineTargetPriceBuyer() {
		AAAgent buyer = addAgent(BUY, EntityProperties.fromPairs(Keys.THETA, 2));
		
		Price limit = new Price(110000);
		Price equil = new Price(105000);
		
		// Intramarginal (limit > equil)
		buyer.aggression = -1;		// passive
		assertEquals(Price.ZERO, buyer.determineTargetPrice(limit, equil));
		buyer.aggression = -0.5;	// less aggressive, so lower target than equil
		assertTrue(buyer.determineTargetPrice(limit, equil).lessThan(equil));
		buyer.aggression = 0;		// active
		assertEquals(equil, buyer.determineTargetPrice(limit, equil));
		buyer.aggression = 0.5;		// aggressive, so target exceeds equil
		assertTrue(buyer.determineTargetPrice(limit, equil).greaterThan(equil));
		buyer.aggression = 1;		// most aggressive
		assertEquals(limit, buyer.determineTargetPrice(limit, equil));
		
		// Extramarginal
		limit = new Price(104000);
		buyer.aggression = -1;		// passive
		assertEquals(Price.ZERO, buyer.determineTargetPrice(limit, equil));
		buyer.aggression = -0.5;	// less aggressive, so lower target than equil
		assertTrue(buyer.determineTargetPrice(limit, equil).lessThan(equil));
		buyer.aggression = 0.5;		// aggressiveness capped at 0
		assertEquals(limit, buyer.determineTargetPrice(limit, equil));
	}
	
	@Test
	public void determineTargetPriceSeller() {
		AAAgent seller = addAgent(SELL, EntityProperties.fromPairs(Keys.THETA, 2));
		
		Price limit = new Price(105000);
		Price equil = new Price(110000);
		
		// Intramarginal (limit < equil)
		seller.aggression = -1;		// passive
		assertEquals(Price.INF, seller.determineTargetPrice(limit, equil));
		seller.aggression = -0.5;	// less aggressive, so target exceeds equil
		assertTrue(seller.determineTargetPrice(limit, equil).greaterThan(equil));
		seller.aggression = 0;		// active
		assertEquals(equil, seller.determineTargetPrice(limit, equil));
		seller.aggression = 0.5;	// aggressive, so target less than equil
		assertTrue(seller.determineTargetPrice(limit, equil).lessThan(equil));
		seller.aggression = 1;		// most aggressive
		assertEquals(limit, seller.determineTargetPrice(limit, equil));
		
		// Extramarginal
		limit = new Price(111000);
		seller.aggression = -1;		// passive
		assertEquals(Price.INF, seller.determineTargetPrice(limit, equil));
		seller.aggression = -0.5;	// less aggressive, so lower target than equil
		assertTrue(seller.determineTargetPrice(limit, equil).greaterThan(equil));
		seller.aggression = 0.5;	// aggressiveness capped at 0
		assertEquals(limit, seller.determineTargetPrice(limit, equil));
	}
	
	@Test
	public void biddingLayerNoTarget() {
		TimeStamp time = TimeStamp.ZERO;
		Price limit = new Price(145000);
		
		Quote q1 = new Quote(market, new Price(Rands.nextUniform(rand, 75000, 80000)), 1, 
				new Price(Rands.nextUniform(rand, 81000, 100000)), 1, time);
		sip.processQuote(market, q1, time);
		
		AAAgent buyer = addAgent(BUY);
		buyer.biddingLayer(limit, null, 1, time);
		assertEquals(1, buyer.activeOrders.size());
		assertCorrectBid(buyer, 75000, 100000, 1);
		
		AAAgent seller = addAgent(SELL);
		seller.biddingLayer(limit, null, 1, time);
		assertEquals(1, buyer.activeOrders.size());
		assertCorrectBid(buyer, 75000, 100000, 1);
	}
	
	@Test
	public void biddingLayerBuyer() {
		TimeStamp time = TimeStamp.ZERO;
		Price limit = new Price(145000);
		Price target = new Price(175000);
		
		AAAgent buyer = addAgent(BUY);
		
		buyer.biddingLayer(limit, target, 1, time);
		assertEquals(1, buyer.activeOrders.size());
		buyer.withdrawAllOrders();
		
		addOrder(BUY, 150000, 1, 10);
		addOrder(SELL, 200000, 1, 10);
		
		buyer.positionBalance = buyer.privateValue.getMaxAbsPosition() + 1;
		buyer.biddingLayer(limit, target, 1, TimeStamp.create(20));
		assertTrue(buyer.activeOrders.isEmpty()); // would exceed max position
		
		buyer.positionBalance = 0;
		buyer.biddingLayer(limit, target, 1, TimeStamp.create(20));
		assertTrue(buyer.activeOrders.isEmpty()); // limit price < bid
		
		limit = new Price(211000);
		buyer.biddingLayer(limit, null, 1, TimeStamp.create(20));
		exec.executeUntil(TimeStamp.create(20));
		assertCorrectBid(buyer, 170007, 1);
		
		limit = new Price(210000);
		target = new Price(180000);
		buyer.withdrawAllOrders();
		buyer.biddingLayer(limit, target, 1, TimeStamp.create(20));
		exec.executeUntil(TimeStamp.create(20));
		assertCorrectBid(buyer, 160000, 1);
	}
	
	@Test
	public void biddingLayerSeller() {
		TimeStamp time = TimeStamp.ZERO;
		Price limit = new Price(210000);
		Price target = new Price(175000);
		
		AAAgent seller = addAgent(SELL);
		
		seller.biddingLayer(limit, target, 1, time);
		assertEquals(1, seller.activeOrders.size()); // ZI strat
		seller.withdrawAllOrders();
		
		addOrder(BUY, 150000, 1, 10);
		addOrder(SELL, 200000, 1, 10);
		
		seller.positionBalance = seller.privateValue.getMaxAbsPosition() + 1;
		seller.biddingLayer(limit, target, 1, TimeStamp.create(20));
		assertTrue(seller.activeOrders.isEmpty()); // would exceed max position
		
		seller.positionBalance = 0;
		seller.biddingLayer(limit, target, 1, TimeStamp.create(20));
		assertTrue(seller.activeOrders.isEmpty()); // limit price > ask
		
		limit = new Price(170000);
		seller.biddingLayer(limit, null, 1, TimeStamp.create(20));
		exec.executeUntil(TimeStamp.create(20));
		assertCorrectBid(seller, 190000, 1);
		
		limit = new Price(165000);
		target = new Price(170000);
		seller.withdrawAllOrders();
		seller.biddingLayer(limit, target, 1, TimeStamp.create(20));
		exec.executeUntil(TimeStamp.create(20));
		assertCorrectBid(seller, 190000, 1);
	}
	
	@Test
	public void updateTheta() {
		TimeStamp time = TimeStamp.ZERO;
		AAAgent agent = addAgent(SELL, EntityProperties.fromPairs(
				Keys.NUM_HISTORICAL, 5,
				Keys.THETA_MAX, 8,
				Keys.THETA_MIN, -8,
				Keys.BETA_T, 0.25,
				Keys.GAMMA, 2,
				Keys.THETA, -4));
		
		agent.updateTheta(null, agent.getWindowTransactions(time));
		assertEquals(-4, agent.theta, 0.001);
		
		addTransaction(105000, 1, 20);
		addTransaction(100000, 1, 25);
		addTransaction(100000, 1, 30);
		
		Price equil = new Price(100000);
		// haven't used 5 transactions yet, so keep theta fixed
		agent.updateTheta(equil, agent.getWindowTransactions(TimeStamp.create(20)));
		assertEquals(-4, agent.theta, 0.001);
		
		addTransaction(110000, 1, 35);
		addTransaction(111000, 1, 40);
		agent.updateTheta(equil, agent.getWindowTransactions(TimeStamp.create(40)));
		assertTrue(agent.theta < -4);	// decreases because high price vol
		assertEquals(-5, agent.theta, 0.001); 
		
		addTransaction(106000, 1, 45);
		addTransaction(107000, 1, 50);
		addTransaction(108000, 1, 50);
		equil = new Price(108000);
		agent.updateTheta(equil, agent.getWindowTransactions(TimeStamp.create(50)));
		assertTrue(agent.theta > -5);	// increases because lower price vol
	}
	
	@Test
	public void testAggressionDataStructure() {
		Aggression agg = new Aggression();
		assertEquals(0, agg.getMaxAbsPosition());
		assertEquals(0, agg.values.size());
		
		agg = new Aggression(1, 0.5);
		assertEquals(2, agg.values.size());
		assertEquals(1, agg.getMaxAbsPosition());
		assertEquals(0.5, agg.values.get(0), 0.001);
		assertEquals(0.5, agg.values.get(1), 0.001);
		
		agg = new Aggression(2, 0.75);
		assertEquals(0.75, agg.getValue(0, BUY), 0.001);
		assertEquals(0.75, agg.getValue(-1, SELL), 0.001);
		assertEquals(new Double(0), agg.getValue(-2, SELL));
		assertEquals(new Double(0), agg.getValue(2, BUY));
		
		agg.setValue(0, BUY, 0.5);
		assertEquals(0.5, agg.getValue(0, BUY), 0.001);
		
		agg.setValue(-2, SELL, -0.5);
		// still 0 since outside max position
		assertEquals(0, agg.getValue(-2, SELL), 0.001);
	}
	
	@Test
	public void initialBuyer() {
		log.log(DEBUG, "\nTesting buyer on empty market: Result should be price=0");
		// Creating a buyer
		AAAgent agent = addAgent(BUY);
		assertTrue(agent.type.equals(BUY));
		// Testing against an empty market
		agent.agentStrategy(TimeStamp.create(100));

		assertCorrectBidQuantity(agent, 1);
	}

	@Test
	public void initialSeller() {
		log.log(DEBUG, "\nTesting seller on empty market: Result should be price=%s", Price.INF);
		// Creating a seller
		AAAgent agent = addAgent(SELL);
		assertTrue(agent.type.equals(SELL));
		// Testing against an empty market
		agent.agentStrategy(TimeStamp.create(100));

		assertCorrectBidQuantity(agent, 1);
	}

	@Test
	public void noTransactionsBuyer() {
		log.log(DEBUG, "\nTesting buyer on market with bids/asks but no transactions");
		log.log(DEBUG, "50000 < Bid price < 100000");

		// Setting up the bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 200000, 1, 10);

		// Testing against a market with initial bids but no transaction history
		AAAgent agent = addAgent(OrderType.BUY, EntityProperties.copyFromPairs(agentProperties, Keys.ETA, 4));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct (based on EQ 10/11)
		assertCorrectBid(agent, 62500, 1);
	}

	@Test
	public void noTransactionsSeller() {
		log.log(DEBUG, "\nTesting seller on market with bids/asks but no transactions");
		log.log(DEBUG, "100000 < Ask price < 200000");

		// Adding setup bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 200000, 1, 10);

		// Testing against a market with initial bids but no transaction history
		AAAgent agent = addAgent(OrderType.SELL, EntityProperties.copyFromPairs(agentProperties, Keys.ETA, 4));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct (based on EQ 10/11)
		assertCorrectBid(agent, 175000, 1);
	}

	@Test
	public void IntraBuyerPassive() {
		log.log(DEBUG, "\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(BUY, EntityProperties.fromPairs(Keys.AGGRESSION, -1));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 30000, 35000, 1);
	}

	@Test
	public void IntraBuyerNegative() {
		log.log(DEBUG, "\nTesting r = -0.5 buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY, EntityProperties.copyFromPairs(agentProperties,
				Keys.THETA, -3,
				Keys.AGGRESSION, -0.5));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 36000, 41000, 1);
	}

	/**
	 * Currently fails due to incorrect market behavior,
	 * but AAAgent acts correctly based on the information it receives
	 */
	@Test
	public void IntraBuyerActive() {
		log.log(DEBUG, "\nTesting active buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 15);
		addTransaction(75000, 1, 20);

		AAAgent agent = addAgent(BUY, EntityProperties.fromPairs(Keys.AGGRESSION, 0));
		log.log(DEBUG, "Price ~= 58333");
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 57000, 59000, 1);
	}

	/**
	 * Currently fails due to incorrect market behavior,
	 * but AAAgent acts correctly based on the information it receives
	 */
	@Test
	public void IntraBuyerPositive() {
		log.log(DEBUG, "\nTesting r = -0.5 buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY, EntityProperties.copyFromPairs(agentProperties,
				Keys.THETA, -3,
				Keys.AGGRESSION, 0.5));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 64000, 66000, 1);
	}

	@Test
	public void IntraBuyerAggressive() {
		log.log(DEBUG, "");
		log.log(DEBUG, "Testing aggressive buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 20);

		AAAgent agent = addAgent(BUY, EntityProperties.fromPairs(Keys.AGGRESSION, 1));
		log.log(DEBUG, "Price ~= 66667");
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 65000, 68000, 1);
	}

	@Test
	public void IntraSellerPassive() { // Check Aggression
		log.log(DEBUG, "");
		log.log(DEBUG, "Testing passive seller on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 20);

		// Testing the Agent
		AAAgent agent = addAgent(SELL, EntityProperties.fromPairs(Keys.AGGRESSION, -1));
		log.log(DEBUG, "Price ~= %d", 150000 + (Price.INF.intValue() - 150000) / 3);
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		int low = 150000 + (Price.INF.intValue() - 150000) / 3 - 1000;
		int high = 150000 + (Price.INF.intValue() - 150000) / 3 + 1000;
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void IntraSellerActive() {
		log.log(DEBUG, "\nTesting active seller on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 15);
		addTransaction(125000, 1, 20);

		AAAgent agent = addAgent(SELL, EntityProperties.fromPairs(Keys.AGGRESSION, 0));
		log.log(DEBUG, "Price ~= 141667");
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 138000, 144000, 1);
	}

	@Test
	public void IntraSellerAggressive() { // Check Aggression
		log.log(DEBUG, "");
		log.log(DEBUG, "Testing aggressive seller on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 20);

		AAAgent agent = addAgent(SELL, EntityProperties.fromPairs(Keys.AGGRESSION, 1));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 130000, 135000, 1);
	}


	@Test
	public void ExtraBuyerPassive() {
		log.log(DEBUG, "\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(BUY, EntityProperties.fromPairs(Keys.AGGRESSION, -1));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 30000, 35000, 1);
	}

	@Test
	public void ExtraBuyerNegative() {
		log.log(DEBUG, "\nTesting r = -0.5 buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY, EntityProperties.copyFromPairs(agentProperties,
				Keys.THETA, -3,
				Keys.AGGRESSION, -0.5));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 38000, 41000, 1);
	}

	@Test
	public void ExtraBuyerActive() {
		log.log(DEBUG, "\nTesting r = 0 buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY, EntityProperties.copyFromPairs(agentProperties,
				Keys.THETA, -3,
				Keys.AGGRESSION, 0));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 65000, 70000, 1);
	}

	@Test
	public void ExtraBuyerPositive() {
		log.log(DEBUG, "\nTesting r = 0 buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY, EntityProperties.copyFromPairs(agentProperties,
				Keys.THETA, -3,
				Keys.AGGRESSION, 0.5));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 65000, 70000, 1);
	}

	@Test
	public void ExtraSellerPassive() {
		log.log(DEBUG, "\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(SELL, EntityProperties.fromPairs(Keys.AGGRESSION, -1));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		int low = 95000 + (int) (Price.INF.doubleValue() / 3);
		int high = 105000 + (int) (Price.INF.doubleValue() / 3);
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void ExtraSellerActive() {
		log.log(DEBUG, "\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(SELL, EntityProperties.fromPairs(Keys.AGGRESSION, 0));
		agent.agentStrategy(TimeStamp.create(100));

		// Asserting the bid is correct
		assertCorrectBid(agent, 132000, 135000, 1);
	}

	@Test
	public void BuyerAggressionIncrease() {
		log.log(DEBUG, "\nTesting aggression learning");

		// Adding Bids and Transactions
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(95000, 1, 20);
		addTransaction(100000, 1, 25);
		addTransaction(100000, 1, 30);
		addTransaction(100000, 1, 35);
		addTransaction(105000, 1, 40);

		AAAgent agent = addAgent(BUY);
		agent.agentStrategy(TimeStamp.create(100));
		assertTrue(agent.aggression > 0);
		assertCorrectBid(agent, 50000, 100000, 1);
	}

	@Test
	public void SellerAggressionIncrease() {
		log.log(DEBUG, "\nTesting aggression learning");

		// Adding Bids and Transactions
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(105000, 1, 20);
		addTransaction(100000, 1, 25);
		addTransaction(100000, 1, 30);
		addTransaction(100000, 1, 35);
		addTransaction(95000, 1, 40);

		AAAgent agent = addAgent(SELL);
		agent.agentStrategy(TimeStamp.create(100));
		assertTrue(agent.aggression > 0);
		assertCorrectBid(agent, 100000, 150000, 1);
	}

	/**
	 * Test short-term learning (EQ 7)
	 */
	@Test
	public void updateAggressionBuyer() {
		log.log(DEBUG, "\nTesting aggression update (buyer)");

		// Adding Bids and Transactions
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(105000, 1, 20);
		addTransaction(100000, 1, 35);
		addTransaction(95000, 1, 40);

		// Setting up the agent
		double oldAggression = 0.5;
		AAAgent agent = addAgent(BUY, EntityProperties.copyFromPairs(agentProperties,
				Keys.THETA, -3,
				Keys.AGGRESSION, oldAggression));
		agent.agentStrategy(TimeStamp.create(100));

		checkAggressionUpdate(BUY, agent.lastTransactionPrice, agent.targetPrice,
				oldAggression, agent.aggression);
	}

	/**
	 * Test short-term learning (EQ 7)
	 * Note that the value of theta affects whether or not this test will pass
	 * (e.g. -3 causes a NaN in computeRShout)
	 */
	@Test
	public void updateAggressionSeller() {
		log.log(DEBUG, "\nTesting aggression update (seller)");

		// Adding Bids and Transactions
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(105000, 1, 20);
		addTransaction(100000, 1, 25);
		addTransaction(110000, 1, 30);
		addTransaction(100000, 1, 35);
		addTransaction(95000, 1, 40);

		// Setting up the agent
		double oldAggression = 0.2;
		AAAgent agent = addAgent(SELL, EntityProperties.copyFromPairs(agentProperties,
				Keys.THETA, 2,
				Keys.AGGRESSION, oldAggression));
		agent.agentStrategy(TimeStamp.create(100));

		checkAggressionUpdate(SELL, agent.lastTransactionPrice, agent.targetPrice,
				oldAggression, agent.aggression);
	}

	/**
	 * Note that for extramarginal buyer, must have last transaction price less
	 * than the limit otherwise rShout gets set to 0.
	 */
	@Test
	public void randomizedUpdateAggressionBuyer() {
		log.log(DEBUG, "\nTesting aggression update (buyer)");

		// Adding Bids and Transactions
		addOrder(BUY, (int) Rands.nextUniform(rand, 25000, 75000), 1, 10);
		addOrder(SELL, (int) Rands.nextUniform(rand, 125000, 175000), 1, 10);
		addTransaction((int) Rands.nextUniform(rand, 100000, 110000), 1, 20);
		addTransaction((int) Rands.nextUniform(rand, 50000, 150000), 1, 25);
		addTransaction((int) Rands.nextUniform(rand, 100000, 120000), 1, 30);
		addTransaction((int) Rands.nextUniform(rand, 750000, 150000), 1, 35);
		addTransaction((int) Rands.nextUniform(rand, 80000, 100000), 1, 40);

		// Setting up the agent
		double oldAggression = 0.5;
		AAAgent agent = addAgent(BUY, EntityProperties.copyFromPairs(agentProperties,
				Keys.THETA, -2,
				Keys.PRIVATE_VALUE_VAR, 5E7,
				Keys.AGGRESSION, oldAggression));
		agent.agentStrategy(TimeStamp.create(100));

		checkAggressionUpdate(BUY, agent.lastTransactionPrice, agent.targetPrice,
				oldAggression, agent.aggression);
	}

	/**
	 * For intramarginal seller, want limit price less than equilibrium,
	 * otherwise rShout clipped at 0.
	 */
	@Test
	public void randomizedUpdateAggressionSeller() {
		log.log(DEBUG, "\nTesting aggression update (seller)");

		// Adding Bids and Transactions
		addOrder(BUY, (int) Rands.nextUniform(rand, 25000, 75000), 1, 10);
		addOrder(SELL, (int) Rands.nextUniform(rand, 125000, 175000), 1, 10);
		addTransaction((int) Rands.nextUniform(rand, 100000, 110000), 1, 20);
		addTransaction((int) Rands.nextUniform(rand, 500000, 150000), 1, 25);
		addTransaction((int) Rands.nextUniform(rand, 500000, 120000), 1, 30);
		addTransaction((int) Rands.nextUniform(rand, 100000, 150000), 1, 35);
		addTransaction((int) Rands.nextUniform(rand, 100000, 110000), 1, 40);

		// Setting up the agent
		double oldAggression = 0.2;
		AAAgent agent = addAgent(SELL, EntityProperties.copyFromPairs(agentProperties,
				Keys.THETA, -3,
				Keys.PRIVATE_VALUE_VAR, 5E7,
				Keys.AGGRESSION, oldAggression));
		agent.agentStrategy(TimeStamp.create(100));

		checkAggressionUpdate(SELL, agent.lastTransactionPrice, agent.targetPrice,
				oldAggression, agent.aggression);
	}

	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setupTest();
			randomizedUpdateAggressionBuyer();
			setupTest();
			randomizedUpdateAggressionSeller();
			setupTest();
			biddingLayerNoTarget();
		}
	}


	// Helper methods

	/**
	 * Check aggression updating
	 */
	private static void checkAggressionUpdate(OrderType type, Price lastTransactionPrice,
			Price targetPrice, double oldAggression, double aggression) {

		// Asserting that aggression updated correctly
		if (type.equals(BUY)) {
			if (lastTransactionPrice.compareTo(targetPrice) < 0) 
				assertTrue("r_old " + oldAggression + " less than " + aggression,
						oldAggression >= aggression); // less aggressive
			else
				assertTrue("r_old " + oldAggression + " greater than " + aggression,
						oldAggression <= aggression); // more aggressive
		} else {
			if (lastTransactionPrice.compareTo(targetPrice) > 0)
				assertTrue("r_old " + oldAggression + " less than " + aggression,
						oldAggression >= aggression); // less aggressive
			else
				assertTrue("r_old " + oldAggression + " greater than " + aggression,
						oldAggression <= aggression); // more aggressive
		}
	}

	private AAAgent addAgent(OrderType type, EntityProperties testProps) {
		testProps.put(Keys.BUYER_STATUS, type == OrderType.BUY);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);	// private values all 0
		testProps.put(Keys.DEBUG, true);

		return new AAAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
					testProps);
	}

	private AAAgent addAgent(OrderType type) {
		return addAgent(type, EntityProperties.copy(agentProperties));
	}

	private void addOrder(OrderType type, int price, int quantity, int time) {
		TimeStamp currentTime = TimeStamp.create(time);
		// creating a dummy agent
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market);
		// Having the agent submit a bid to the market
		market.submitOrder(agent, type, new Price(price), quantity, currentTime);
	}

	private void addTransaction(int p, int q, int time) {
		addOrder(BUY, p, q, time);
		addOrder(SELL, p, q, time);
		market.clear(TimeStamp.create(time));
	}

	/**
	 * Note this method only works if there's only one order. Verifies that
	 * order price is equal to the specified price.
	 * 
	 * @param agent
	 * @param price
	 * @param quantity
	 */
	private static void assertCorrectBid(Agent agent, int price, int quantity) {
		Collection<Order> orders = agent.activeOrders;
		// Asserting the bid is correct
		assertNotEquals("Num orders is incorrect", 0, orders.size());
		Order order = Iterables.getFirst(orders, null);

		assertNotEquals("Order agent is null", null, order.getAgent());
		assertEquals("Order agent is incorrect", agent, order.getAgent());

		Price bidPrice = order.getPrice();
		assertEquals("Price is incorrect", new Price(price), bidPrice);
		assertEquals("Quantity is incorrect", quantity, order.getQuantity());
	}

	/**
	 * Only works if there's only one order. Verifies that order price is
	 * between the range specified.
	 * 
	 * @param agent
	 * @param low
	 * @param high
	 * @param quantity
	 */
	private static void assertCorrectBid(Agent agent, int low, int high,
			int quantity) {
		Collection<Order> orders = agent.activeOrders;
		// Asserting the bid is correct
		assertNotEquals("Num orders is incorrect", 0, orders.size());
		Order order = Iterables.getFirst(orders, null);

		assertNotEquals("Order agent is null", null, order.getAgent());
		assertEquals("Order agent is incorrect", agent, order.getAgent());

		Price bidPrice = order.getPrice();
		assertTrue("Order price (" + bidPrice + ") less than " + new Price(low),
				bidPrice.greaterThan(new Price(low)));
		assertTrue("Order price (" + bidPrice + ") greater than " + new Price(high),
				bidPrice.lessThan(new Price(high)));

		assertEquals("Quantity is incorrect", quantity, order.getQuantity());
	}

	private static void assertCorrectBidQuantity(Agent agent, int quantity) {
		assertCorrectBid(agent, 0, Integer.MAX_VALUE, quantity);
	}
	
}
