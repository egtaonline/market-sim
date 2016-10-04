package activity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import entity.agent.Agent;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * Class for Activity of submitting a National Market System (NMS) bid (price + quantity) to a
 * market. Will submit the bid to the best market of those available.
 * 
 * @author ewah
 */
public class SubmitNMSOrder extends Activity {

	protected final Agent agent;
	protected final Price price;
	protected final int quantity;
	protected final OrderType type;
	protected final Market primaryMarket;
	protected final TimeStamp duration;

	public SubmitNMSOrder(Agent agent, Market primaryMarket, OrderType type,
			Price price, int quantity, TimeStamp duration) {
		checkArgument(quantity > 0, "Quantity must be positive");
		this.agent = checkNotNull(agent, "Agent");
		this.price = checkNotNull(price, "Price");
		this.quantity = quantity;
		this.type = type;
		this.primaryMarket = checkNotNull(primaryMarket, "Primary Market");
		this.duration = checkNotNull(duration, "Duration");
	}

	public SubmitNMSOrder(Agent agent, Market primaryMarket, OrderType type,
			Price price, int quantity) {
		this(agent, primaryMarket, type, price, quantity, TimeStamp.IMMEDIATE);
	}

	@Override
	public void execute(TimeStamp currentTime) {
		primaryMarket.submitNMSOrder(agent, type, price, quantity, currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + agent + " " + type + "(" +
				+ quantity + "@" + price + ") -> " + primaryMarket;
	}
	
}
