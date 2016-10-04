package json.aggregated;

import java.util.List;

import json.Observations;
import json.Role;

public class AggregatedObservations extends Observations {
	
	String name;
	List<Role> roles;
	String simulator_fullname;
	List<AggregatedObservationProfile> profiles;
	
	
	public AggregatedObservations(String name, List<Role> roles, 
			String simulator_fullname, List<AggregatedObservationProfile> profiles) {
		this.name = name;
		this.roles = roles;
		this.simulator_fullname = simulator_fullname;
		this.profiles = profiles;
	}
	
	public List<AggregatedObservationProfile> getProfiles() {
		return profiles;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public List<Role> getRoles() {
		return roles;
	}
	
	@Override
	public String getSimulatorFullName() {
		return simulator_fullname;
	}
	
}