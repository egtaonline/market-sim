package entity.market.clearingrule;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Lists;

import entity.market.DummyMarketTime;
import entity.market.MarketTime;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;
import fourheap.MatchedOrders;

public class EarliestPriceClearTest {

	@Test
	public void Basic() {
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = Lists.newArrayList();
		list.add(createOrderPair(new Price(110), 1, TimeStamp.create(100), 
								 new Price(100), 1, TimeStamp.create(105)));
		
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price,MarketTime, Order>,Price> result = cr.pricing(list);
		
		Set<MatchedOrders<Price,MarketTime, Order>> keySet = result.keySet();
		for(MatchedOrders<Price,MarketTime, Order> key : keySet) {
			// Verify clears at the earlier price
			assertEquals(new Price(110), result.get(key));
		}
	}
	
	@Test
	public void TimeMatch(){
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = Lists.newArrayList();
		list.add(createOrderPair(new Price(110), 1, TimeStamp.create(100), 
								 new Price(100), 1, TimeStamp.create(100)));
		
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price, MarketTime, Order>, Price> result = cr.pricing(list);
		
		Set<MatchedOrders<Price, MarketTime, Order>> keySet = result.keySet();
		for(MatchedOrders<Price, MarketTime, Order> key : keySet) {
			// Verify for tie at time, it clears at the earlier price (because of MarketTime)
			assertEquals(new Price(110), result.get(key));
		}
	}
	
	@Test
	public void Multi() {
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = new ArrayList<MatchedOrders<Price, MarketTime, Order>>();
		
		MatchedOrders<Price, MarketTime, Order> match1 = createOrderPair(
				new Price(110), 1, TimeStamp.create(100), 
				new Price(100), 1, TimeStamp.create(105));
		list.add(match1);
		
		MatchedOrders<Price, MarketTime, Order> match2 = createOrderPair(
				new Price(110), 1, TimeStamp.create(105), 
				new Price(100), 1, TimeStamp.create(100));
		list.add(match2);
				
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price, MarketTime, Order>, Price> result = cr.pricing(list);

		// Verify always clears at the earlier price (no time ties here)
		assertEquals(new Price(110), result.get(match1));
		assertEquals(new Price(100), result.get(match2));
	}
	
	/**
	 * Create matched order pair (buy, sell)
	 * @param p1
	 * @param q1
	 * @param t1
	 * @param p2
	 * @param q2
	 * @param t2
	 * @return
	 */
	public MatchedOrders<Price, MarketTime, Order> createOrderPair(Price p1, int q1, 
			TimeStamp t1, Price p2, int q2, TimeStamp t2){
		// NOTE: the same MarketTime will never be created for two orders
		// So if t1 == t2, t2 will be created at an incremented MarketTime
		MarketTime mt1 = new DummyMarketTime(t1, t1.getInTicks());
		MarketTime mt2 = new DummyMarketTime(t2, t2.plus(TimeStamp.create(t2.equals(t1) ? 1 : 0)).getInTicks());
		Order a = Order.create(BUY, null, null, p1, q1, mt1);
		Order b = Order.create(SELL, null, null, p2, q2, mt2);
		// Generic for compartability with 1.6 compiler / non eclipse
		return MatchedOrders.<Price, MarketTime, Order> create(a, b, Math.min(q1, q2));
	}
}
