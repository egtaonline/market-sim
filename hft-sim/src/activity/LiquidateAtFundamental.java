package activity;

import static com.google.common.base.Preconditions.checkNotNull;
import entity.agent.Agent;
import event.TimeStamp;

/**
 * Class for Activity to liquidate an agent's position, based on some given price.
 * This price may be based on the value of the global fundamental.
 * 
 * @author ewah
 */
public class LiquidateAtFundamental extends Activity {

	protected final Agent agent;

	public LiquidateAtFundamental(Agent agent) {
		this.agent = checkNotNull(agent, "Agent");
	}

	@Override
	public void execute(TimeStamp currentTime) {
		this.agent.liquidateAtFundamental(currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + agent;
	}
	
}
