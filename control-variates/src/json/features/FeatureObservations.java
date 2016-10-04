package json.features;

import java.util.List;
import java.util.Set;

import json.Observations;
import json.Role;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class FeatureObservations extends Observations {
	
	String name;
	List<FeatureObservationProfile> profiles;
	String simulator_fullname;
	List<Role> roles;
	
	public FeatureObservations(String name, List<Role> roles,
			String simulator_fullname, List<FeatureObservationProfile> profiles) {
		this.name = name;
		this.roles = roles;
		this.simulator_fullname = simulator_fullname;
		this.profiles = profiles;
	}
	
	public FeatureObservations(String name, String simulator_fullname) {
		this.name = name;
		this.roles = Lists.newArrayList();
		this.simulator_fullname = simulator_fullname;
		this.profiles = Lists.newArrayList();
	}
	
	public List<FeatureObservationProfile> getProfiles() {
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
	
	/**
	 * Merge another FeatureObservation object with this one.
	 * 
	 * @param obs
	 */
	public void mergeObservations(FeatureObservations obs) {
		// check that name, simulator name, match
		// if (obs.name.equals(this.name) && obs.simulator_fullname.equals(this.simulator_fullname)) {
		// only check match with name (because may be from a different game)
		if (obs.simulator_fullname.equals(this.simulator_fullname)) {
			
			// add new strategies (if do not exist)
			for (Role role : roles) {
				for (Role obsRole : obs.roles) {
					if (obsRole.getName().equals(role.getName())) {
						Set<String> uniqueStrats = Sets.newHashSet(role.getStrategies());
						uniqueStrats.addAll(obsRole.getStrategies());
						role.setStrategies(Lists.newArrayList(uniqueStrats));
					}
				}
			}
			// now add observations
			profiles.addAll(obs.getProfiles());
		}	
	}
}