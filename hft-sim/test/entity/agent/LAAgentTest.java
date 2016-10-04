package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
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

import systemmanager.Consts;
import systemmanager.Executor;

import com.google.common.collect.ImmutableList;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

public class LAAgentTest {

	// TODO Here or in HFT Test make sure it gets notified about transactions appropriately
	
	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market1, market2;
	private Agent agent1, agent2;
	private SIP sip;

	@BeforeClass
	public static void setupClass() throws IOException {
		
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "LAAgentTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market1 = new CDAMarket(exec, sip, new Random(), EntityProperties.empty());
		market2 = new CDAMarket(exec, sip, new Random(), EntityProperties.empty());
		agent1 = new MockAgent(exec, fundamental, sip);
		agent2 = new MockAgent(exec, fundamental, sip);
	}
	
	/*
	 * Bug in LA that occurred in very particular circumstances. Imagine market1
	 * has BUY @ 30 and BUY @ 50. A SELL @ 10 arrives in market2. The LA submits
	 * a SELL @ 30 -> market1 and schedules a BUY @ 30 for market2. After the
	 * SELL clears, market1 has a BUY @ 30 left. There is still an arbitrage opp,
	 * and the LA acts again for before its second order goes into market2. So
	 * it submits a SELL @ 20 -> market1, and schedules a BUY @ 20 for market2.
	 * This first SELL clears, and results in no arbitrage opportunities, so
	 * then the first BUY @ 30 makes it market2 where it transacts. Finally the
	 * BUY @ 20 makes it to market2, but there are no more orders, and so this
	 * order sits while the LA holds a position.
	 */
	@Test
	public void oneSidedArbitrageTest() {
		/*
		 * This test doesn't seem very "unit," however it needs the market to
		 * clear and update the LAIP accordingly. This means we need execute all
		 * of the immediate events, and we need to use CDAMarkets instead of
		 * MockMarkets because we need them to clear after the original
		 * LAStrategy in order to trigger the next LAStrategy before the first
		 * has finished executing.
		 */
		LAAgent la = new LAAgent(exec, fundamental, sip, ImmutableList.of(market1, market2),
				new Random(), TimeStamp.IMMEDIATE, 1, 0.001);
		market1.submitOrder(agent1, BUY, new Price(5), 1, TimeStamp.ZERO);
		market1.submitOrder(agent1, BUY, new Price(7), 1, TimeStamp.ZERO);
		market2.submitOrder(agent2, SELL, new Price(1), 1, TimeStamp.ZERO);
		// LA Strategy get's called implicitly

		assertEquals(0, la.positionBalance);
		assertTrue(la.profit > 0);
	}
	
	@Test
	public void multiOneSidedArbitrageTest() {
		for (int i = 0; i < 1000; ++i) {
			setup();
			oneSidedArbitrageTest();
		}
	}
	
	@Test
	public void laProfitTest() {
		LAAgent la = new LAAgent(exec, fundamental, sip, ImmutableList.of(market1, market2),
				new Random(), TimeStamp.IMMEDIATE, 1, 0.001);
		market1.submitOrder(agent1, BUY, new Price(5), 1, TimeStamp.ZERO);
		market2.submitOrder(agent2, SELL, new Price(1), 1, TimeStamp.ZERO);
		// LA Strategy gets called implicitly 
		
		assertEquals(0, la.positionBalance);
		assertEquals(4, la.profit);
	}
	
	@Test
	/**
	 * This test verifies that an LA with latency doesn't submit new orders until it's sure it's old orders are reflected in its quote.
	 */
	public void laLatencyNoRepeatOrdersTest() {
		TimeStamp latency = TimeStamp.create(10);
		LAAgent la = new LAAgent(exec, fundamental, sip, ImmutableList.of(market1, market2),
				new Random(), latency, 1, 0.001);
		
		market1.submitOrder(agent1, BUY, new Price(5), 1, TimeStamp.ZERO);
		market2.submitOrder(agent2, SELL, new Price(1), 1, TimeStamp.ZERO);
		market1.submitOrder(agent1, BUY, new Price(2), 1, TimeStamp.create(5)); // Used to cause LA to submit extra orders
		exec.executeUntil(latency); // LA has submitted orders (and they've transacted)
		// LA Strategy gets called implicitly 
		
		assertEquals(0, la.positionBalance);
		assertEquals(0, la.profit);
		assertEquals(2, la.activeOrders.size());
		
		exec.executeUntil(latency.plus(latency)); // Takes this long for the LA to find out about it
		assertEquals(0, la.positionBalance);
		assertEquals(4, la.profit);
		assertTrue(la.activeOrders.isEmpty());
	}
	
	@Test
	/**
	 * This test makes sure the fix for the previous test doesn't effect infinitely fast LAs.
	 */
	public void laNoLatencySeveralOrders() {
		LAAgent la = new LAAgent(exec, fundamental, sip, ImmutableList.of(market1, market2),
				new Random(), TimeStamp.IMMEDIATE, 1, 0.001);
		
		market1.submitOrder(agent1, BUY, new Price(5), 1, TimeStamp.ZERO);
		market2.submitOrder(agent2, SELL, new Price(1), 1, TimeStamp.ZERO);
		// LA Strategy gets called implicitly
		
		assertEquals(0, la.positionBalance);
		assertEquals(4, la.profit);
		assertTrue(la.activeOrders.isEmpty());
		
		market1.submitOrder(agent1, BUY, new Price(10), 1, TimeStamp.ZERO);
		market2.submitOrder(agent2, SELL, new Price(6), 1, TimeStamp.ZERO);
		// LA Strategy gets called implicitly 
		// The fix might have stopped the LA from acting a second time at TimeStamp 0

		assertEquals(0, la.positionBalance);
		assertEquals(8, la.profit);
		assertTrue(la.activeOrders.isEmpty());
	}

}
