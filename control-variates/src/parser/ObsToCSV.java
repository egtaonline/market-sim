package parser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import controlvariates.ControlVariates;
import json.Role;
import json.summary.SummaryObservations;

/**
 * Class for parsing results of running on EGTA. Main method takes as input 
 * the name of the JSON file downloaded from EGTA (which contains results from
 * all samples).
 * 
 * Usage:
 * 		[summary json file - no mm]
 * 		[summary json file - with mm]
 * 		[output csv file]
 * 
 * @author ewah
 *
 */
public class ObsToCSV {

	protected static final Gson gson = new Gson();

	static SummaryObservations noMMObs;
	static SummaryObservations mmObs;
	FileWriter writer;
	SurplusComparison surplus;

	public static void main(String... args) {

		if (args.length != 3) System.exit(1);
		ObsToCSV obs = new ObsToCSV(args);
		obs.surplus.processNoMMProfiles(noMMObs, mmObs);

		obs.createWriter(args[2]);
		obs.addFirstLine();
		obs.fillInColumns();
		obs.endDocument();
		
	}

	/**
	 * Constructor
	 */
	public ObsToCSV(String[] args) {
		try {
			noMMObs = gson.fromJson(new FileReader(args[0]), SummaryObservations.class);
			mmObs = gson.fromJson(new FileReader(args[1]), SummaryObservations.class);

			List<String> mmStrategies = Lists.newArrayList();
			for (Role role : mmObs.getRoles()) {
				if (role.getName().equals(ControlVariates.MARKETMAKER))
					mmStrategies.addAll(role.getStrategies());
			}
			surplus = new SurplusComparison(mmStrategies);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createWriter(String filename) {
		try {
			File file = new File(filename);
			if (!file.exists()) {
				file.createNewFile();
			}
			writer = new FileWriter(file);	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public void fillInColumns() {
		// have to make sure pulling out references to the MM strats in the 
		// correct order for each profile
		// first column is going to be the strategies
		addValue("BACKGROUND_ONLY");
		for (SurplusComparisonProfile profile : surplus.profiles) {
			addValue(profile.getBackgroundSurplus()); // background only
			addValue(0);	// zero profit from MM
		}
		endLine();
		
		for (String strat : surplus.mmStrategies) {
			addValue(strat);
			for (SurplusComparisonProfile profile : surplus.profiles) {
				addValue(profile.getSurplusByStrategy(strat));
				addValue(profile.getProfitByStrategy(strat));
			}
			endLine();
		}
	}
	
	
	public void addFirstLine() {
		// iterate through surplus comparison object, just store the
		addValue("strategies");
		for (SurplusComparisonProfile profile : surplus.profiles) {
			addValue(profile.getID() + "_surplus");
			addValue(profile.getID() + "_mm_profit");
		}
		endLine();
	}
	

	public void addValue(Object s) {
		try {
			if (s != null) {
				writer.append(s.toString());
				writer.append(',');
			} else {
				writer.append("null");
				writer.append(',');
			}
		} catch(IOException e) {
			e.printStackTrace();
		} 
	}

	public void endLine() {
		try {
			writer.append('\n');
		} catch(IOException e) {
			e.printStackTrace();
		} 
	}

	public void endDocument() {
		try {
			writer.flush();
			writer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
