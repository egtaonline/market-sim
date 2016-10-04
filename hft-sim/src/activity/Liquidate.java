package activity;

import static com.google.common.base.Preconditions.checkNotNull;
import entity.agent.Agent;
import entity.market.Price;
import event.TimeStamp;

/**
 * Class for Activity to liquidate an agent's position, based on some given price.
 * This price may be based on the value of the global fundamental.
 * 
 * @author ewah
 */
public class Liquidate extends Activity {

	protected final Agent agent;
	protected final Price price;

	public Liquidate(Agent agent, Price price) {
		this.agent = checkNotNull(agent, "Agent");
		this.price = checkNotNull(price, "Price");
	}

	@Override
	public void execute(TimeStamp currentTime) {
		this.agent.liquidateAtPrice(this.price, currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + agent + " @" + price;
	}
	
}
