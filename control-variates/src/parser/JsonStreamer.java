package parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import json.ProfileSymmetryGroup;
import json.Role;
import json.features.Feature;
import json.features.FeatureObservation;
import json.features.FeatureObservationProfile;
import json.features.FeatureObservations;
import json.features.FeatureSymmetryGroup;
import json.features.PlayerFeatureSummary;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.stream.JsonReader;
import com.google.gson.Gson;

import controlvariates.ControlVariates;

/**
 * Parses the full game data to pull out aggregated player-level features.
 * 
 * @author ewah
 *
 */
public class JsonStreamer {

	protected static final Gson gson = new Gson();
	protected FeatureObservations aggregatedFeatures = null;

	public void readJsonStream(InputStream in) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
		try {
			FeatureObservations obs = readObservations(reader);
			if (aggregatedFeatures == null) // if undefined
				aggregatedFeatures = obs;
			else {
				aggregatedFeatures.mergeObservations(obs);
			}
		} finally {
			reader.close();
		}
	}

	public void writeFeaturesToJson(File file) {
		String json = gson.toJson(aggregatedFeatures);
		try {
			FileWriter writer = new FileWriter(file);
			writer.write(json);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public FeatureObservations readObservations(JsonReader reader) throws IOException {
		String name = null;
		String simulator_fullname = null;
		List<Role> roles = Lists.newArrayList();
		List<FeatureObservationProfile> profiles = Lists.newArrayList();

		reader.beginObject();
		while (reader.hasNext()) {
			String key = reader.nextName();
			if (key.equals("name")) {
				name = reader.nextString();
			} else if (key.equals("roles")) {
				roles = readRoles(reader);
			} else if (key.equals("simulator_fullname")) {
				simulator_fullname = reader.nextString();
			} else if (key.equals("profiles")) {
				profiles = readProfiles(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		return new FeatureObservations(name, roles, simulator_fullname, profiles);
	}

	public List<Role> readRoles(JsonReader reader) throws IOException {
		List<Role> roles = Lists.newArrayList();

		reader.beginArray();
		while (reader.hasNext()) {
			roles.add(readRole(reader));
		}
		reader.endArray();
		return roles;
	}
	
	public Role readRole(JsonReader reader) throws IOException {
		int count = -1;
		List<String> strategies = Lists.newArrayList();
		String name = null;
		
		reader.beginObject();
		while (reader.hasNext()) {
			String key = reader.nextName();
			if (key.equals("count")) {
				count = reader.nextInt();
			} else if (key.equals("name")) {
				name = reader.nextString();
			} else if (key.equals("strategies")) {
				strategies = readRoleStrategies(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		return new Role(count, strategies, name);
	}
	
	public List<String> readRoleStrategies(JsonReader reader) throws IOException {
		List<String> strategies = Lists.newArrayList();

		reader.beginArray();
		while (reader.hasNext()) {
			strategies.add(reader.nextString());
		}
		reader.endArray();
		return strategies;
	}
	
	public List<FeatureObservationProfile> readProfiles(JsonReader reader) throws IOException {
		List<FeatureObservationProfile> profiles = Lists.newArrayList();

		reader.beginArray();
		while (reader.hasNext()) {
			profiles.add(readProfile(reader));
		}
		reader.endArray();
		return profiles;
	}

	public FeatureObservationProfile readProfile(JsonReader reader) throws IOException {
		List<ProfileSymmetryGroup> symmetry_groups = Lists.newArrayList();
		int id = -1;
		List<FeatureObservation> observations = Lists.newArrayList();

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("id")) {
				id = reader.nextInt();
			} else if (name.equals("symmetry_groups")) {
				symmetry_groups = readSymmetryGroups(reader);
			} else if (name.equals("observations")) {
				observations = readFeatureObservations(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		return new FeatureObservationProfile(id, symmetry_groups, observations);
	}

	public List<ProfileSymmetryGroup> readSymmetryGroups(JsonReader reader) throws IOException {
		List<ProfileSymmetryGroup> symmetry_groups = Lists.newArrayList();
		reader.beginArray();
		while (reader.hasNext()) {
			symmetry_groups.add(readSymmetryGroup(reader));
		}
		reader.endArray();
		return symmetry_groups;
	}

	public ProfileSymmetryGroup readSymmetryGroup(JsonReader reader) throws IOException {
		int id = -1;
		int count = 0;
		String role = null;
		String strategy = null;

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("id")) {
				id = reader.nextInt();
			} else if (name.equals("count")) {
				count = reader.nextInt();
			} else if (name.equals("strategy")) {
				strategy = reader.nextString();
			} else if (name.equals("role")) {
				role = reader.nextString();
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		return new ProfileSymmetryGroup(count, role, id, strategy);
	}

	public List<FeatureObservation> readFeatureObservations(JsonReader reader) throws IOException {
		List<FeatureObservation> observations = Lists.newArrayList();
		reader.beginArray();
		while (reader.hasNext()) {
			observations.add(readFeatureObservation(reader));
		}
		reader.endArray();
		return observations;
	}

	public FeatureObservation readFeatureObservation(JsonReader reader) throws IOException {
		List<FeatureSymmetryGroup> symmetry_groups = Lists.newArrayList();
		Feature features = null;
		
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("players")) {
				symmetry_groups = readPlayers(reader);
			} else if (name.equals("features")) {
				features = readFeatures(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		return new FeatureObservation(features, symmetry_groups);
	}

	public Feature readFeatures(JsonReader reader) throws IOException {
		double control_mean_fund = 0;
		double control_var_fund = 0;
		double control_mean_private = 0;
		
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals(ControlVariates.CONTROL_MEAN_FUND)) {
				control_mean_fund = reader.nextDouble();
			} else if (name.equals(ControlVariates.CONTROL_VAR_FUND)) {
				control_var_fund = reader.nextDouble();
			} else if (name.equals(ControlVariates.CONTROL_MEAN_PRIVATE)) {
				control_mean_private = reader.nextDouble();
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		return new Feature(control_mean_fund, control_var_fund, control_mean_private);
	}
	
	
	public List<FeatureSymmetryGroup> readPlayers(JsonReader reader) throws IOException {
		Map<Integer, PlayerFeatureSummary> playerFeatures = Maps.newHashMap();

		reader.beginArray();
		while (reader.hasNext()) {
			// within this step, perform the aggregation
			readPlayer(reader, playerFeatures);
		}
		reader.endArray();

		// Parse the map and create the symmetry groups
		return FeatureSymmetryGroup.createGroups(playerFeatures);
	}


	public void readPlayer(JsonReader reader, Map<Integer, PlayerFeatureSummary> playerFeatures) 
			throws IOException {

		int sid = -1;
		int p = 0;
		Map<String, Double> f = Maps.newHashMap();

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("p")) {
				p = reader.nextInt();
			} else if (name.equals("sid")) {
				sid = reader.nextInt();
			} else if (name.equals("f")) {
				f = readPlayerFeatures(reader);
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();

		// now add to container storing the aggregated info for each strategy
		if (!playerFeatures.containsKey(sid))
			playerFeatures.put(sid, new PlayerFeatureSummary());

		playerFeatures.get(sid).addPayoff(p);
		if (!f.isEmpty())
			for (String key : ControlVariates.pv_control_variables) {
				playerFeatures.get(sid).addFeature(key, f.get(key));
			}
	}

	public Map<String, Double> readPlayerFeatures(JsonReader reader) throws IOException {
		Map<String, Double> pvVars = Maps.newHashMap();

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals(ControlVariates.PV_BUY1)) {
				pvVars.put(name, reader.nextDouble());
			} else if (name.equals(ControlVariates.PV_SELL1)) {
				pvVars.put(name, reader.nextDouble());	
			} else if (name.equals(ControlVariates.PV_POSITION_MAX_ABS1)) {
				pvVars.put(name, reader.nextDouble());
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		return pvVars;
	}
}
