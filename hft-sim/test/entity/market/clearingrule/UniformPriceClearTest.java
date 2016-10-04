package entity.market.clearingrule;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Test;

import entity.market.DummyMarketTime;
import entity.market.MarketTime;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;
import fourheap.MatchedOrders;

public class UniformPriceClearTest {

	@Test
	public void UniformPriceBasic() {
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = new ArrayList<MatchedOrders<Price, MarketTime, Order>>();
		ClearingRule cr = new UniformPriceClear(0.5, 1);
		
		MatchedOrders<Price, MarketTime, Order> match1 = createOrderPair(
				new Price(110), 1, TimeStamp.create(100), 
				new Price(100), 1, TimeStamp.create(105));
		list.add(match1);

		Map<MatchedOrders<Price, MarketTime, Order>, Price> result = cr.pricing(list);
		
		// Verify clearing at midpoint of the two orders (policy=0.5)
		assertEquals(new Price(105), result.get(match1));
	}
	
	@Test
	public void UniformPriceRatio() {
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = new ArrayList<MatchedOrders<Price, MarketTime, Order>>();
		ClearingRule cr = new UniformPriceClear(1, 1);
		
		MatchedOrders<Price, MarketTime, Order> match1 = createOrderPair(
				new Price(110), 1, TimeStamp.create(100), 
				new Price(100), 1, TimeStamp.create(105));
		list.add(match1);

		Map<MatchedOrders<Price, MarketTime, Order>,Price> result = cr.pricing(list);
		// Verify clearing at the higher price of the two orders (policy=1)
		assertEquals(new Price(110), result.get(match1));
		
		cr = new UniformPriceClear(0, 1);
		result = cr.pricing(list);
		// Verify clearing at the lower price of the two orders (policy=0)
		assertEquals(new Price(100), result.get(match1));
	}
	
	@Test
	public void UniformPriceMulti() {
		ClearingRule cr = new UniformPriceClear(0.5, 1);

		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = new ArrayList<MatchedOrders<Price, MarketTime, Order>>();
		
		MatchedOrders<Price, MarketTime, Order> match1 = createOrderPair(
				new Price(110), 1, TimeStamp.create(100), 
				new Price(100), 1, TimeStamp.create(105));
		list.add(match1);
		
		MatchedOrders<Price, MarketTime, Order> match2 = createOrderPair(
				new Price(110), 1, TimeStamp.create(105), 
				new Price(100), 1, TimeStamp.create(100));
		list.add(match2);
				
		Map<MatchedOrders<Price, MarketTime, Order>,Price> result = cr.pricing(list);
		// Verify that for multiple orders, clears at correct midpoint price (policy=0.5)
		assertEquals(new Price(105), result.get(match1));
		assertEquals(new Price(105), result.get(match2));
		
		// Testing second set of prices
		list.clear();
		match1 = createOrderPair(new Price(110), 1, TimeStamp.create(100), 
								 new Price(105), 1, TimeStamp.create(105));
		list.add(match1);
		match2 = createOrderPair(new Price(104), 1, TimeStamp.create(101), 
								 new Price(108), 1, TimeStamp.create(102));
		list.add(match2);
		result = cr.pricing(list);
		// Verify that for multiple orders, clears at correct midpoint price (policy=0.5)
		// midpoint between BID=max(matched sells), ASK=min(matched buys)
		assertEquals(new Price(106), result.get(match1));
		assertEquals(new Price(106), result.get(match2));
	}
	
	
	/**
	 * Create matched order pair (buy, then sell)
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
		MarketTime mt1 = new DummyMarketTime(t1, t1.getInTicks());
		MarketTime mt2 = new DummyMarketTime(t2, t2.getInTicks());
		Order a = Order.create(BUY, null, null, p1, q1, mt1);
		Order b = Order.create(SELL, null, null, p2, q2, mt2);
		// Generic for compartability with 1.6 compiler / non eclipse
		return MatchedOrders.<Price, MarketTime, Order> create(a, b, Math.min(q1, q2));
	}

}
