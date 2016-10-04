package json.summary;

import java.util.ArrayList;
import java.util.List;

import json.Observations;
import json.Role;

public class SummaryObservations {
	
	String name;
	List<Role> roles;
	String simulator_fullname;
	List<SummaryProfile> profiles;
	
	public SummaryObservations(String name, List<Role> roles, String simulator_fullname,
			List<SummaryProfile> profiles) {
		this.name = name;
		this.roles = roles;
		this.simulator_fullname = simulator_fullname;
		this.profiles = profiles;
	}
	
	public String getName() {
		return name;
	}
	
	public List<Role> getRoles() {
		return roles;
	}
	
	public String getSimulatorFullname() {
		return simulator_fullname;
	}
	
	public List<SummaryProfile> getProfiles() {
		return profiles;
	}
	
	public SummaryObservations(Observations aggObs, List<SummaryProfile> profiles) {
		this(aggObs.getName(), aggObs.getRoles(), aggObs.getSimulatorFullName(), profiles);
	}
	
	public SummaryObservations(Observations aggObs) {
		this(aggObs.getName(), aggObs.getRoles(), aggObs.getSimulatorFullName(), 
				new ArrayList<SummaryProfile>());
	}
	
	public void addSummaryProfile(SummaryProfile profile) {
		profiles.add(profile);
	}
	
	public SummaryProfile findProfileByID(int id) {
		for (SummaryProfile profile : profiles) {
			if (profile.getID() == id) {
				return profile;
			}
		}
		return null;
	}
}