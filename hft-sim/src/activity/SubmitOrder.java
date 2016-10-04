package activity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import entity.agent.Agent;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * Class for Activity of submitting a bid (price + quantity) to a market.
 * 
 * @author ewah
 */
public class SubmitOrder extends Activity {

	protected final Agent agent;
	protected final Market market;
	protected final Price price;
	protected final int quantity;
	protected final OrderType type;

	public SubmitOrder(Agent agent, Market market, OrderType type, Price price,
			int quantity) {
		checkArgument(quantity > 0, "Quantity must be positive");
		this.agent = checkNotNull(agent, "Agent");
		this.market = checkNotNull(market, "Market");
		this.price = checkNotNull(price, "Price");
		this.quantity = quantity;
		this.type = type;
	}

	@Override
	public void execute(TimeStamp currentTime) {
		market.submitOrder(agent, type, price, quantity, currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + agent + " " + type + "(" +
				+ quantity + "@" + price + ") -> " + market;
	}
	
}
