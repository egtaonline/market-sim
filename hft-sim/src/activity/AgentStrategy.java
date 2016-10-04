package activity;

import static com.google.common.base.Preconditions.checkNotNull;
import entity.agent.Agent;
import event.TimeStamp;

/**
 * Class for executing agent strategies.
 * 
 * @author ewah
 */
public class AgentStrategy extends Activity {

	protected final Agent agent;

	public AgentStrategy(Agent agent) {
		this.agent = checkNotNull(agent, "Agent");
	}

	@Override
	public void execute(TimeStamp currentTime) {
		agent.agentStrategy(currentTime);
	}

	@Override
	public String toString() {
		return super.toString() + agent;
	}

}
