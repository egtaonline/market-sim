package json;

public class ProfileSymmetryGroup {
	
	int id;
	int count;
	String role;
	String strategy;

	public ProfileSymmetryGroup(int count, String role, int id, String strategy) {
		this.id = id;
		this.count = count;
		this.role = role;
		this.strategy = strategy;
	}
	
	public int getID() {
		return id;
	}
	
	public int getCount() {
		return count;
	}
	
	public String getRole() {
		return role;
	}
	
	public String getStrategy() {
		return strategy;
	}
}
