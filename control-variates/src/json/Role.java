package json;

import java.util.List;

public class Role {
	int count;
	List<String> strategies;
	String name;
	
	public Role(int count, List<String> strategies, String name) {
		this.count = count;
		this.strategies = strategies;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public int getCount() {
		return count;
	}
	
	public List<String> getStrategies() {
		return strategies;
	}
	
	public void setStrategies(List<String> strategies) {
		this.strategies = strategies;
	}
}
