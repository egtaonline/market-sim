package controlvariates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import json.Observations;
import json.ProfileSymmetryGroup;
import json.aggregated.AggregatedObservationProfile;
import json.aggregated.AggregatedObservations;
import json.features.FeatureObservationProfile;
import json.features.FeatureObservations;
import json.summary.SummaryObservations;
import json.summary.SummaryProfile;
import json.summary.SummarySymmetryGroup;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import Jama.Matrix;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * 
 * After compute the variance adjustment, update the payoffs
 * And then save into the summary format
 *
 * @author ewah
 *
 */
public class ControlVariates {

	protected static final Gson gson = new Gson();

	protected AggregatedObservations aggregatedObs;
	protected FeatureObservations aggregatedFeatures;
	protected Observations generalObs;
	protected SummaryObservations adjustedSummaryObs;
	protected SummaryObservations summaryObs;
	protected SummaryObservations varReductionSummary;

	protected SummaryStatistics varReductionPercent;

	public final static String BACKGROUND = "BACKGROUND";
	public final static String MARKETMAKER = "MARKETMAKER";

	// within features, the control variables to pull out
	public final static String CONTROL_MEAN_FUND = "control_mean_fund";
	public final static String CONTROL_MEAN_PRIVATE = "control_mean_private";
	public final static String CONTROL_VAR_FUND = "control_var_fund";
	public final static String[] control_variables =
		//{CONTROL_MEAN_PRIVATE, CONTROL_VAR_FUND};
			// {CONTROL_MEAN_FUND,	
		{ CONTROL_MEAN_PRIVATE }; //, CONTROL_VAR_FUND };

	public final static String PV_BUY1 = "pv_buy1";
	public final static String PV_SELL1 = "pv_sell1";
	public final static String PV_POSITION_MAX_ABS1 = "pv_position_max_abs1";
	public final static String[] pv_control_variables =
		{PV_BUY1};
		//{ PV_BUY1, PV_SELL1 }; //, PV_POSITION_MAX_ABS1 };
		//{ PV_BUY1, PV_SELL1, PV_POSITION_MAX_ABS1 };

	Map<String, Matrix> coefficients = Maps.newHashMap();


	/**
	 * @param features
	 * @param featuresFile
	 * @param obsFile
	 * @throws FileNotFoundException
	 */
	public ControlVariates(boolean features, File featuresFile, File obsFile) throws FileNotFoundException {
		this(new FileReader(features ? featuresFile : obsFile), features);
	}

	/**
	 * @param reader
	 */
	public ControlVariates(Reader reader, boolean features) {
		//		aggregatedObs = gson.fromJson(reader, AggregatedObservations.class);
		if (features) {
			aggregatedFeatures = gson.fromJson(reader, FeatureObservations.class);
			adjustedSummaryObs = new SummaryObservations(aggregatedFeatures);
			varReductionSummary = new SummaryObservations(aggregatedFeatures);

		} else {
			aggregatedObs = gson.fromJson(reader, AggregatedObservations.class);
			adjustedSummaryObs = new SummaryObservations(aggregatedObs);
			varReductionSummary = new SummaryObservations(aggregatedObs);
		}
		varReductionPercent = new SummaryStatistics();
	}


	/**
	 * Apply control variates by profile / symmetry group.
	 */
	public void applyCVByStrategyToAggegatedObservations() {
		for (AggregatedObservationProfile profile : aggregatedObs.getProfiles()) {
			double[][] X_t = profile.getControlVariableMatrix(control_variables);
			SummaryProfile summaryProfile = SummaryProfile.create(profile);

			for (ProfileSymmetryGroup profileGroup : profile.getSymmetryGroups()) {
				double[] Y = profile.getObservationPayoffs(profileGroup);

				double[] coeffs = computeRegressionCoefficients(X_t, Y);
				EmpiricalValue val = applyControlVariates(X_t, Y, coeffs);

				SummarySymmetryGroup summaryGroup = SummarySymmetryGroup.create(profileGroup, val);
				summaryProfile.addGroup(summaryGroup);
			}
			adjustedSummaryObs.addSummaryProfile(summaryProfile);
		}
	}

	/**
	 * Apply control variates to the game (coarsest-grained variance adjustment)
	 * 
	 */
	public void applyCVByRoleOverGame() throws Exception {
		if (coefficients.isEmpty()) {
			JsonWriter writer = new JsonWriter(new FileWriter(VarianceReduction.outputCoeffFilename));
			writer.beginArray();
			for (String role : VarianceReduction.roles) {
				System.out.println(role + ":");
				double[][] allX_t = null;
				double[] allY = null;

				for (AggregatedObservationProfile profile : aggregatedObs.getProfiles()) {
					double[][] X_t = profile.getControlVariables(control_variables);

					for (ProfileSymmetryGroup profileGroup : profile.getSymmetryGroups()) {
						if (profileGroup.getRole().equals(role)) {
							double[] Y = profile.getObservationPayoffs(profileGroup);
							allY = concatenateVectors(allY, Y);
							allX_t = concatenateVertMatrices(allX_t, X_t); 
						}
					}
				}
				allX_t = subtractColumnMeans(allX_t); /// TODO TEST THIS
				double[] coeffs = computeRegressionCoefficients(allX_t, allY);
				coefficients.put(role, new Matrix(coeffs, coeffs.length));
				String s = "coeffs: ";
				for (double c : coeffs) s += "   " + c;
				System.out.println(s);

				writeCoeffsToJson(writer, role, coeffs, control_variables);
			}
			writer.endArray();
			writer.close();
		} 

		// now apply computed coeffs in turn to all groups and profiles
		for (AggregatedObservationProfile profile : aggregatedObs.getProfiles()) {
			double[][] X_t = profile.getControlVariables(control_variables);
			SummaryProfile summaryProfile = SummaryProfile.create(profile);

			for (ProfileSymmetryGroup profileGroup : profile.getSymmetryGroups()) {
				String role = profileGroup.getRole();
				double[] coeffs = coefficients.get(role).getRowPackedCopy();
				double[] Y = profile.getObservationPayoffs(profileGroup);

				EmpiricalValue val = applyControlVariates(X_t, Y, coeffs);

				SummarySymmetryGroup summaryGroup = SummarySymmetryGroup.create(profileGroup, val);
				summaryProfile.addGroup(summaryGroup);
			}
			adjustedSummaryObs.addSummaryProfile(summaryProfile);
		}
	}

	/**
	 * Apply control variates to the game (coarsest-grained variance adjustment)
	 * 
	 * Still by role, but now matrix contains
	 * the control features, then the player features
	 */
	public void applyCVByRoleOverGameWithFeatures() throws Exception {
		if (coefficients.isEmpty()) {
			JsonWriter writer = new JsonWriter(new FileWriter(VarianceReduction.outputCoeffFilename));
			writer.beginArray();
			for (String role : VarianceReduction.roles) {
				System.out.println(role + ":");

				double[][] allX_t = null;
				double[] allY = null;

				for (FeatureObservationProfile profile : aggregatedFeatures.getProfiles()) {

					double[][] X_t = profile.getControlVariables(control_variables);

					for (ProfileSymmetryGroup profileGroup : profile.getSymmetryGroups()) {
						if (profileGroup.getRole().equals(role)) {
							double[] Y = profile.getObservationPayoffs(profileGroup);
							allY = concatenateVectors(allY, Y);
							if (role.equals(BACKGROUND)) {
								double[][] pvs = profile.getFeatureControlVariables(pv_control_variables, profileGroup);
								allX_t = concatenateVertMatrices(allX_t, concatenateHorizMatrices(X_t, pvs));
							} else
								allX_t = concatenateVertMatrices(allX_t, X_t);
						}
					}
				}
				allX_t = subtractColumnMeans(allX_t);
				double[] coeffs = computeRegressionCoefficients(allX_t, allY);

				coefficients.put(role, new Matrix(coeffs, coeffs.length));
				String s = "coeffs: ";
				for (double c : coeffs) s += "   " + c;
				System.out.println(s);
				System.out.print("           const_term");
				for (String cv : control_variables) System.out.print("          " + cv);
				if (role.equals(BACKGROUND)) {
					for (String cv : pv_control_variables) System.out.print("          " + cv);
				}
				System.out.println("");

				if (role.equals(BACKGROUND)) {
					writeCoeffsToJson(writer, role, coeffs, 
							concatenateStringVectors(control_variables, pv_control_variables));
				} else { 
					writeCoeffsToJson(writer, role, coeffs, control_variables);
				}
			}
			writer.endArray();
			writer.close();
		}

		// now apply computed coeffs in turn to all groups and profiles
		for (FeatureObservationProfile profile : aggregatedFeatures.getProfiles()) {
			double[][] X_t = profile.getControlVariables(control_variables);		// XXX fix the mean subtraction for this as well
			SummaryProfile summaryProfile = SummaryProfile.create(profile);

			for (ProfileSymmetryGroup profileGroup : profile.getSymmetryGroups()) {
				String role = profileGroup.getRole();
				double[] coeffs = coefficients.get(role).getRowPackedCopy();
				double[] Y = profile.getObservationPayoffs(profileGroup);

				double[][] allX_t = X_t;
				if (role.equals(BACKGROUND)) {
					double[][] pvs = profile.getFeatureControlVariables(pv_control_variables, profileGroup);
					allX_t = concatenateHorizMatrices(X_t, pvs);
				}
				allX_t = subtractColumnMeans(allX_t); /// TODO TEST THIS

				EmpiricalValue val = applyControlVariates(allX_t, Y, coeffs);

				SummarySymmetryGroup summaryGroup = SummarySymmetryGroup.create(profileGroup, val);
				summaryProfile.addGroup(summaryGroup);
			}
			adjustedSummaryObs.addSummaryProfile(summaryProfile);
		}
	}


	/**
	 * Note this does not include that column of 1s.
	 * @param cv
	 * @return
	 */
	public double[][] subtractColumnMeans(double[][] cv) {
		int length = cv[0].length;
		double[] controlVarMeans = new double[length];

		// should be consistent between symmetry_group & payoff b/c same iteration order
		for (int j = 0; j < controlVarMeans.length; j++) {
			for (int i = 0; i < cv.length; i++) {
				controlVarMeans[j] += cv[i][j];
			}
			controlVarMeans[j] /= cv.length;
		}

		double[][] cvMatrix = new double[cv.length][length];
		for (int i = 0; i < cv.length; i++) {
			for (int j = 0; j < controlVarMeans.length; j++) {
				cvMatrix[i][j] = cv[i][j] -	controlVarMeans[j];
			}
		}
		return cvMatrix;
	}


	/**
	 * Generates an EmpiricalValue holding the variance-adjusted estimated mean
	 * and standard deviation.
	 * 
	 * @param X_t 		control variable matrix, EQ (A6)
	 * @param Y 		observed payoffs
	 * @return
	 */
	public double[] computeRegressionCoefficients(double[][] X_t, double[] Y) {
		double Ymean = 0;
		for (int i = 0; i < Y.length; i++) Ymean += Y[i];
		Ymean /= Y.length;
		
		//if (!ArrayUtils.contains(Y, 0.0)) {
		if (Ymean != 0) {
			OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
			regression.newSampleData(Y, X_t);

			// Q+1 is number of rows in regression coeffs, gamma contains both mu and beta
			double[] mu_and_beta = regression.estimateRegressionParameters();
			double[] coeff_stderr = regression.estimateRegressionParametersStandardErrors();

			double[] signif = new double[coeff_stderr.length];
			for (int i = 0; i < coeff_stderr.length; i++) {
				// compute T-values 
				signif[i] = mu_and_beta[i] / coeff_stderr[i];
			}
			String s = "t-values: ";
			for (double t : signif) s += "   " + t;
			System.out.println(s);

			return mu_and_beta;
		}
		return new double[X_t[0].length + 1];
	}

	/**
	 * Given the regression coefficients and the matrices with payoffs and
	 * control variates, apply to generate an estimate & stddev value.
	 * 
	 * @param X_t (no column of 1's at the beginning)
	 * @param Y
	 * @param mu_and_beta
	 * @return
	 */
	public EmpiricalValue applyControlVariates(double[][] X_t, double[] Y, 
			double[] mu_and_beta) {

		double Ymean = 0;
		for (int i = 0; i < Y.length; i++) Ymean += Y[i];
		Ymean /= Y.length;
		
		if (Ymean != 0) {
			int K = X_t.length;		 	// number of simulation runs (observations) (already transposed)
			int Q = X_t[0].length;		// number of control variables

			Matrix gamma = new Matrix(mu_and_beta, Q+1);	// EQ (A5)

			// Generate estimate for expected value of payoffs
			double estimate = 0;
			for(int i = 0; i < K; i++){
				estimate += Y[i];
				for(int j = 0; j < Q; j++){
					estimate -= gamma.get(j+1,0) * X_t[i][j];
				}
			}
			estimate /= (double) K;	// estimated expected value of Y

			// get submatrix rows 1-Q (leave off coeff at index 0)
			double mu_hat = gamma.get(0,0);
			Matrix beta_hat = gamma.getMatrix(1, Q, 0, 0);
			double[][] X_t_1 = this.addFirstColumnOnes(X_t);

			double estimate2 = 0;
			for (int k = 0; k < K; k++) {
				Matrix xMatrix_k = new Matrix(X_t[k], Q);
				System.out.println(beta_hat.transpose().times(xMatrix_k).get(0,0));
				estimate2 -= beta_hat.transpose().times(xMatrix_k).get(0,0);
				if (k == 50) System.out.println("at k=50, est is " + (estimate2/50.0) + " vs " + Ymean);
				if (k == 100) System.out.println("at k=100, est is " + (estimate2/100.0) + " vs " + Ymean);
				if (k == 200) System.out.println("at k=200, est is " + (estimate2/200.0) + " vs " + Ymean);
			}
			estimate2 /= (double) K;

			Matrix mX_t = new Matrix(X_t_1, X_t_1.length, X_t_1[0].length); // with column of 1's
//			double s = mX_t.transpose().times(mX_t).inverse().get(0, 0); // Eq (A7)

			SummaryStatistics stat = new SummaryStatistics();
			double sigma_hat2 = 0;
			for(int k = 0; k < K; k++){	// Eq (A8) - modified
				// Matrix X_k = mX_t.getMatrix(k, k, 1, Q); // (c_k - mu_c) matrix
				//sigma_hat2 += Math.pow(Y[k] - (mu_hat + beta_hat.times(X_k).get(0, 0)), 2);	// XXX
				stat.addValue(Y[k]);
				sigma_hat2 += Math.pow(Y[k] - estimate, 2);
			}
			//sigma_hat2 /= (K-Q-1);
			//double variance = s*sigma_hat2;	// XXX
			double variance = sigma_hat2 / (K-1);	// sample variance
			//System.out.println("via SS: " + stat.getStandardDeviation() + ", via est " + Math.sqrt(variance));

			return new EmpiricalValue(estimate, Math.sqrt(variance), K);
		}
		return new EmpiricalValue(0, 0, Y.length);
	}


	/**
	 * Adds a first column of ones to the control variable matrix.
	 * @param X_t
	 * @return
	 */
	private double[][] addFirstColumnOnes(double[][] X_t) {
		double[][] X_t_1 = new double[X_t.length][X_t[0].length+1];
		for (int i = 0; i < X_t_1.length; i++) {
			X_t_1[i][0] = 1;
			for (int j = 1; j < X_t_1[0].length; j++) {
				X_t_1[i][j] = X_t[i][j-1];
			}
		}
		return X_t_1;
	}

	/**
	 * @param file
	 */
	public void readCoefficients(File file) {
		try {
			for (String role : VarianceReduction.roles) {
				double[] coeffs = readCoeffsFromJson(role, file);
				coefficients.put(role, new Matrix(coeffs, coeffs.length));
				String s = "coeffs: ";
				for (double c : coeffs) s += "   " + c;
				System.out.println(s);
				System.out.print("           const_term");
				for (String cv : control_variables) System.out.print("          " + cv);
				if (role.equals(BACKGROUND)) {
					for (String cv : pv_control_variables) System.out.print("          " + cv);
				}
				System.out.println("");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes coefficients for a given role.
	 * 
	 * @param role
	 * @param coeffs
	 * @param labels
	 */
	protected void writeCoeffsToJson(JsonWriter writer, String role, 
			double[] coeffs, String[] labels) throws IOException {

		writer.beginObject();
		writer.name("role").value(role);
		writer.name("coeffs").beginObject();
		writer.name("const_term").value(coeffs[0]);
		for (int i = 1; i <= labels.length; i++) {
			writer.name(labels[i-1]).value(coeffs[i]);
		}
		writer.endObject();
		writer.endObject();

		//		coefficients.put(role, new Matrix(coeffs, coeffs.length));
		//		String s = "coeffs: ";
		//		for (double c : coeffs) s += "   " + c;
		//		System.out.println(s);
		//		System.out.print("           const_term");
		//		for (String cv : control_variables) System.out.print("          " + cv);
		//		if (role.equals(BACKGROUND)) {
		//			for (String cv : pv_control_variables) System.out.print("          " + cv);
		//		}
		//		System.out.println("");
	}

	/**
	 * @param role
	 * @return
	 */
	protected double[] readCoeffsFromJson(String role, File file) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		double[] coeffs = new double[control_variables.length + 1];

		reader.beginArray();
		while (reader.hasNext()) {
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("role")) {
					if (reader.nextString().equals(role)) {
						// XXX unsafe but because of way coeffs are written out,
						// the next name will always be coeffs
						reader.nextName(); // coeffs
						coeffs = readCoeffs(reader, role);
					}
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
		}
		reader.endArray();
		reader.close();
		return coeffs;
	}

	/**
	 * @param reader
	 * @param role TODO
	 * @return
	 * @throws IOException
	 */
	protected double[] readCoeffs(JsonReader reader, String role) throws IOException {
		String[] labels = control_variables;
		if (role.equals(BACKGROUND))
			labels = concatenateStringVectors(labels, pv_control_variables);

		double[] coeffs = new double[labels.length + 1];

		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("const_term")) {
				coeffs[0] = reader.nextDouble();
			} else {
				for (int i = 0; i < labels.length; i++) {
					if (name.equals(labels[i])) {
						coeffs[i+1] = reader.nextDouble();
					}
				}
			}
		}
		reader.endObject();
		return coeffs;
	}

	/**
	 * Read in summary observations file (unadjusted for variance)
	 * 
	 * @param file
	 */
	public void readSummaryObservations(File file) {
		try {
			Reader reader = new FileReader(file);
			summaryObs = gson.fromJson(reader, SummaryObservations.class);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Determine the amount by which the variance has been reduced for each
	 * profile.
	 * 
	 * Iterate through all the summary observations downloaded, then compute
	 * the difference between corresponding payoffs and variance.
	 * Saves results in varReductionSummary.
	 */
	public void compareVarianceReduction() {
		int worseVariance = 0;

		for (SummaryProfile adjProfile : adjustedSummaryObs.getProfiles()) {
			int id = adjProfile.getID();
			SummaryProfile diffSummaryProfile = SummaryProfile.create(adjProfile);
			SummaryProfile origSummaryProfile = summaryObs.findProfileByID(id);

			// have to compute the difference for each symmetry group
			for (SummarySymmetryGroup group : adjProfile.getGroups()) {
				// original minus adjusted (so all diffs should be hopefully positive)
				int groupID = group.getID();
				double sd_diff = origSummaryProfile.findGroupByID(groupID).getPayoffSD() - 
						group.getPayoffSD();
				double payoff_diff = origSummaryProfile.findGroupByID(groupID).getPayoff() -
						group.getPayoff();

				SummarySymmetryGroup diffGroup = SummarySymmetryGroup.create(group,
						payoff_diff, sd_diff);
				diffSummaryProfile.addGroup(diffGroup);

				// if original stddev was 0, then can't divide (plus it prob had no payoff then)
				if (origSummaryProfile.findGroupByID(groupID).getPayoffSD() != 0 &&
						group.getPayoffSD() != 0) {
					varReductionPercent.addValue( sd_diff / 
							origSummaryProfile.findGroupByID(groupID).getPayoffSD() );
					if (sd_diff < 0)
						worseVariance++;
				}
			}
			varReductionSummary.addSummaryProfile(diffSummaryProfile);
		}
		System.out.println("stddev reduction worse: " + worseVariance + " / " 
				+ (varReductionPercent.getN()+worseVariance));
		System.out.println("stddev reduction % mean=" + varReductionPercent.getMean() * 100);
		System.out.println("stddev reduction % max=" + varReductionPercent.getMax() * 100);
		System.out.println("stddev reduction % min=" + varReductionPercent.getMin() * 100);
		System.out.println("stddev reduction N=" +varReductionPercent.getN());
	}


	/**
	 * @param summaryFile
	 */
	public void writeAdjustedToJson(File summaryFile) {
		String json = gson.toJson(adjustedSummaryObs);
		try {
			FileWriter writer = new FileWriter(summaryFile);
			writer.write(json);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param summaryFile
	 */
	public void writeDiffToJson(File summaryFile) {
		String json = gson.toJson(varReductionSummary);
		FileWriter writer;
		try {
			writer = new FileWriter(summaryFile);
			writer.write(json);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Note that this assumes that original & arrayToAdd have the same number
	 * of columns (it does perform a check to verify this; if this condition
	 * is not satisfied, it just returns the original array).
	 * 
	 * @param original
	 * @param arrayToAdd
	 * @return
	 */
	protected double[][] concatenateVertMatrices(double[][] original, double[][] arrayToAdd) {
		if (original == null) return arrayToAdd;

		if (original[0].length != arrayToAdd[0].length) {
			return original;
		}
		double[][] expandedArray = new double[original.length + arrayToAdd.length][original[0].length];
		for (int i = 0; i < original.length; i++) {
			for (int j = 0; j < original[0].length; j++) {
				expandedArray[i][j] = original[i][j];
			}
		}
		for (int i = original.length; i < expandedArray.length; i++) {
			for (int j = 0; j < original[0].length; j++) {
				expandedArray[i][j] = arrayToAdd[i-original.length][j];
			}
		}
		return expandedArray;
	}

	/**
	 * @param original
	 * @param arrayToAdd
	 * @return
	 */
	protected double[][] concatenateHorizMatrices(double[][] original, double[][] arrayToAdd) {
		if (original == null) return arrayToAdd;

		if (original.length != arrayToAdd.length) {
			return original;
		}
		double[][] expandedArray = new double[original.length][original[0].length + arrayToAdd[0].length];
		for (int i = 0; i < original.length; i++) {
			for (int j = 0; j < original[0].length; j++) {
				expandedArray[i][j] = original[i][j];
			}
		}
		for (int i = 0; i < original.length; i++) {
			for (int j = original[0].length; j < expandedArray[0].length; j++) {
				expandedArray[i][j] = arrayToAdd[i][j-original[0].length];
			}
		}
		return expandedArray;
	}

	/**
	 * @param original
	 * @param vectorToAdd
	 * @return
	 */
	protected double[] concatenateVectors(double[] original, double[] vectorToAdd) {
		if (original == null) return vectorToAdd;

		double[] expandedVector = new double[original.length + vectorToAdd.length];
		for (int i = 0; i < original.length; i++) {
			expandedVector[i] = original[i];
		}
		for (int i = original.length; i < expandedVector.length; i++) {
			expandedVector[i] = vectorToAdd[i-original.length];
		}
		return expandedVector;
	}

	protected String[] concatenateStringVectors(String[] original, String[] vectorToAdd) {
		if (original == null) return vectorToAdd;

		String[] expandedVector = new String[original.length + vectorToAdd.length];
		for (int i = 0; i < original.length; i++) {
			expandedVector[i] = original[i];
		}
		for (int i = original.length; i < expandedVector.length; i++) {
			expandedVector[i] = vectorToAdd[i-original.length];
		}
		return expandedVector;
	}
}
