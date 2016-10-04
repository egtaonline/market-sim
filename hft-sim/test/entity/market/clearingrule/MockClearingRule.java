package entity.market.clearingrule;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;

import entity.market.MarketTime;
import entity.market.Order;
import entity.market.Price;
import fourheap.MatchedOrders;

/**
 * Always clears at the buy price. This was changed so that the affect of
 * different clearing prices on the summary statistics could be tested.
 * 
 * @author erik
 * 
 */
public class MockClearingRule implements ClearingRule {

	private static final long serialVersionUID = 1L;
	
	@Override
	public Map<MatchedOrders<Price, MarketTime, Order>, Price> pricing(
			Iterable<MatchedOrders<Price, MarketTime, Order>> transactions) {
		if (Iterables.isEmpty(transactions)) return ImmutableMap.of();
		
		Builder<MatchedOrders<Price, MarketTime, Order>, Price> prices = ImmutableMap.builder();
		for (MatchedOrders<Price, MarketTime, Order> trans : transactions)
			prices.put(trans, trans.getBuy().getPrice());
		return prices.build();
	}

}
