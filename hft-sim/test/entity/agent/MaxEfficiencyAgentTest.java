package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import systemmanager.Consts;
import systemmanager.Executor;
import systemmanager.Keys;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.CallMarket;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import event.TimeStamp;

public class MaxEfficiencyAgentTest {
	
	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;
	private static Random rand;
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@BeforeClass
	public static void setUpClass() throws IOException{
		// Setting up the log file
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "MaxEfficiencyAgentTest.log"));
		rand = new Random();
	}
	
	@Before
	public void setup(){
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new CallMarket(exec, sip, rand, EntityProperties.fromPairs(
				Keys.CLEAR_INTERVAL, 10));
	}
	
	@Test
	public void callMarket() {
		market = new MockMarket(exec, sip);
		
		Agent agent = new MaxEfficiencyAgent(exec, fundamental, sip, market, rand, 
				EntityProperties.fromPairs(
						Keys.MAX_POSITION, 2,
						Keys.PRIVATE_VALUE_VAR, 1000));
	
		// must be a call market
		exception.expect(IllegalArgumentException.class);
		agent.agentStrategy(TimeStamp.ZERO);
	}
	
	@Test
	public void numOrdersTest() {
		Agent agent = new MaxEfficiencyAgent(exec, fundamental, sip, market, rand, 
				EntityProperties.fromPairs(
						Keys.MAX_POSITION, 10,
						Keys.PRIVATE_VALUE_VAR, 1000));
		
		agent.agentStrategy(TimeStamp.ZERO);
		
		assertEquals(20, agent.activeOrders.size());
		int cnt = 0;
		for (Order o : agent.activeOrders) {
			if (o.getOrderType().equals(BUY)) cnt++;
			else cnt--;
		}
		assertEquals(0, cnt); // equal number of buys and sells
		
	}
	
	@Test
	public void basicTest() {
		Agent agent = new MaxEfficiencyAgent(exec, fundamental, sip, market, rand, 
				EntityProperties.fromPairs(
						Keys.MAX_POSITION, 1,
						Keys.PRIVATE_VALUE_VAR, 10000000));
		
		agent.agentStrategy(TimeStamp.ZERO);
		
		assertEquals(2, agent.activeOrders.size());
		int buyPrice = 0, sellPrice = 0;
		for (Order o : agent.activeOrders) {
			if (o.getOrderType().equals(BUY))
				buyPrice = o.getPrice().intValue();
			else 
				sellPrice = o.getPrice().intValue();
		}
		assertTrue(sellPrice >= buyPrice);	
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			basicTest();
			setup();
			numOrdersTest();
		}
	}
	
}
