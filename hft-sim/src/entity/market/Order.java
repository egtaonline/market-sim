package entity.market;

import entity.agent.Agent;

/**
 * Contains array of Points which can be evaluated independently by
 * the market (i.e., the bid is divisible). Bids are contained in
 * an array sorted by price and quantity, both descending.
 * 
 * @author ewah
 */
public class Order extends fourheap.Order<Price, MarketTime> {

	private static final long serialVersionUID = 4020465194816241014L;
	
	protected final Agent agent;
	protected final Market market;

	public Order(OrderType type, Agent agent, Market market, Price price,
			int quantity, MarketTime time) {
		super(type, price, quantity, time);
		this.agent = agent;
		this.market = market;
	}

	public static Order create(OrderType type, Agent agent, Market market,
			Price price, int quantity, MarketTime time) {
		return new Order(type, agent, market, price, quantity, time);
	}
	
	public Agent getAgent() {
		return agent;
	}

	public Market getMarket() {
		return market;
	}
	
	@Override
	public String toString() {
		return agent + " " + type + ' ' + getQuantity() + " @ " + price + " in " + market;
	}
	
}
