package activity;

import static com.google.common.base.Preconditions.checkNotNull;
import entity.agent.Agent;
import event.TimeStamp;

/**
 * Class for activity of agents arriving in a market or market(s).
 * 
 * @author ewah
 */
// XXX: To Elaine: Is this activity necessary?
public class AgentArrival extends Activity {

	protected final Agent agent;

	public AgentArrival(Agent agent) {
		this.agent = checkNotNull(agent, "Agent");
	}

	@Override
	public void execute(TimeStamp currentTime) {
		agent.agentArrival();
	}

	@Override
	public String toString() {
		return super.toString() + agent;
	}

}