package entity.agent;

import java.util.Random;

/**
 * Mock private value for mock background agents.
 * 
 * @author ewah
 *
 */
public class MockPrivateValue extends PrivateValue {

	private static final long serialVersionUID = 1L;
	
	public MockPrivateValue() {
		super(10, 0, new Random());
	}
	
	public MockPrivateValue(int absMaxPosition) {
		super(absMaxPosition, 0, new Random());
	}
}
