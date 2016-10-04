package activity;

import static com.google.common.base.Preconditions.checkNotNull;
import entity.agent.MockBackgroundAgent;
import event.TimeStamp;

public class MockAgentActivity extends Activity {

	protected final MockBackgroundAgent agent;
	
	public MockAgentActivity(MockBackgroundAgent agent) {
		this.agent = checkNotNull(agent, "Agent");
	}

	@Override
	public void execute(TimeStamp currentTime) {
		this.agent.addMockActivity(currentTime);
	}
}
