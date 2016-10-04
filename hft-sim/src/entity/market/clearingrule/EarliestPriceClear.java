package entity.market.clearingrule;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import entity.market.MarketTime;
import entity.market.Order;
import entity.market.Price;
import fourheap.MatchedOrders;

public class EarliestPriceClear implements ClearingRule {

	private static final long serialVersionUID = -6417178198266057261L;

	protected final int tickSize;
	
	public EarliestPriceClear(int tickSize) {
		this.tickSize = tickSize;
	}
	
	@Override
	public Map<MatchedOrders<Price, MarketTime, Order>, Price> pricing(
			Iterable<MatchedOrders<Price, MarketTime, Order>> matchedOrders) {
		Builder<MatchedOrders<Price, MarketTime, Order>, Price> prices = ImmutableMap.builder();
		for (MatchedOrders<Price, MarketTime, Order> match : matchedOrders)
			prices.put(match, match.getBuy().getSubmitTime().compareTo(match.getSell().getSubmitTime()) < 0
					? match.getBuy().getPrice().quantize(tickSize)
					: match.getSell().getPrice().quantize(tickSize));
		return prices.build();
	}

}
