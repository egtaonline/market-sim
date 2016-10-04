package controlvariates;

import java.io.File;

/**
 * Class responsible for variance reduction given fields for control variates
 * adjustment.
 * 
 * Example arguments:
 * 
 * path=foldername ( MUST BE FIRST ARGUMENT )
 * 
 * outcoeff=coeffs-output.json
 * incoeff=coeffs.json
 * summ=113-summary.json
 * obs=113-observations.json
 * feat=113-features.json (from FeatureParser)
 * diff=113-output-var-diff.json
 * adj=113-output-adj-summary.json
 * 
 * @author ewah
 */
public class VarianceReduction {

	public enum Granularity { STRATEGY, ROLE }; // Add PROFILE?

	public final static String[] roles = { ControlVariates.BACKGROUND, ControlVariates.MARKETMAKER };

	// For parsing arguments
	public final static String INPUT_COEFF_KEY = "incoeff";
	public final static String OUTPUT_COEFF_KEY = "outcoeff";
	public final static String SUMMARY_KEY = "summ";
	public final static String OBS_KEY = "obs";
	public final static String ADJUSTED_SUMMARY_KEY = "adj";
	public final static String VAR_DIFF_KEY = "diff";
	public final static String FEATURES_KEY = "feat";
	public final static String GRANULARITY_KEY = "type";
	public final static String PATH_KEY = "path";

	public final static String OUTPUT_COEFF_DEFAULT = "coeffs-output-default.json"; // default
	public static String outputCoeffFilename = null;

	public final static String[] params = { PATH_KEY, OUTPUT_COEFF_KEY, INPUT_COEFF_KEY, 
		SUMMARY_KEY, OBS_KEY, ADJUSTED_SUMMARY_KEY, VAR_DIFF_KEY, FEATURES_KEY, 
		GRANULARITY_KEY };

	// inputs
	File observationFile = null; 	// either obs or features required
	File featuresFile = null;
	File summaryFile = null;		// required
	File inputCoeffsFile = null;
	// outputs
	File outputFile = null;
	File varDiffFile = null;
	String path = "";

	Granularity type = Granularity.ROLE;	// default is coarsest grained

	/**
	 * Argument is location of json file (aggregate observations) to read
	 * and location to write the summary obs.
	 * 
	 * @param argsI think what's going on
	 */
	public static void main(String... args) {

		VarianceReduction var = new VarianceReduction();
		for (String arg : args) {
			var.parseArguments(arg);
		}
		// if neither in nor out coeffs set, then set as default
		if (var.inputCoeffsFile == null && outputCoeffFilename == null) {
			outputCoeffFilename = var.path + OUTPUT_COEFF_DEFAULT;
		}
		if (var.varDiffFile == null) {
			var.varDiffFile = new File(var.path + "var-diff-default.json");
		}
		try {
			var.adjustVariance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void adjustVariance() throws Exception {

		if (summaryFile == null && (observationFile == null || featuresFile == null)) {
			System.err.println("Summary file + either observation or feature files are required");
			System.exit(1);
		}
		ControlVariates cv = new ControlVariates(featuresFile != null, featuresFile, observationFile);

		// Adjust variance
		if (inputCoeffsFile != null) cv.readCoefficients(inputCoeffsFile);

		if (type == Granularity.STRATEGY)
			cv.applyCVByStrategyToAggegatedObservations();
		else if (type == Granularity.ROLE)
			if (featuresFile == null)
				cv.applyCVByRoleOverGame();
			else
				cv.applyCVByRoleOverGameWithFeatures();

		// Generate outputs
		if (outputFile != null)	cv.writeAdjustedToJson(outputFile);

		if (summaryFile != null) {
			cv.readSummaryObservations(summaryFile);
			cv.compareVarianceReduction();
			if (varDiffFile != null) cv.writeDiffToJson(varDiffFile);
		}
	}

	private void parseArguments(String arg) {
		String[] parts = arg.split("=");

		if (parts[0].equals(PATH_KEY)) {
			path = parts[1] + "/";
		} else if (parts[0].equals(OBS_KEY)) {
			observationFile = new File(path + parts[1]); 
		} else if (parts[0].equals(INPUT_COEFF_KEY)) {
			inputCoeffsFile = new File(path + parts[1]);
		} else if (parts[0].equals(SUMMARY_KEY)) {
			summaryFile = new File(path + parts[1]);
		} else if (parts[0].equals(ADJUSTED_SUMMARY_KEY)) {
			outputFile = new File(path + parts[1]);
		} else if (parts[0].equals(VAR_DIFF_KEY)) {
			varDiffFile = new File(path + parts[1]);
		} else if (parts[0].equals(FEATURES_KEY)) {
			featuresFile = new File(path + parts[1]);
		} else if (parts[0].equals(GRANULARITY_KEY)) {
			type = Granularity.valueOf(parts[1]);
		} else if (parts[0].equals(OUTPUT_COEFF_KEY)) {
			outputCoeffFilename = path + parts[1];
		}
	}

}
