package systemmanager;

import java.io.Serializable;

import utils.Maps2;

import com.google.common.collect.ImmutableMap;

/**
 * Stores ALL hard-coded defaults for simulation spec (environment) parameters 
 * and agent strategy parameters.
 *  
 * @author ewah
 *
 */
public class Defaults implements Serializable {

	private static final long serialVersionUID = -2159139939831086051L;
	
	// TODO reduce size of strategy strings (they're all way too long, provide some 
	// better guidelines on usage
	
	public static String get(String key) {
		return defaults.get(key);
	}

	public final static ImmutableMap<String, String> defaults = Maps2.fromPairs(
		// General
		Keys.NUM,					0,
		
		Keys.TICK_SIZE,				1,
		
		// Simulation spec (general)
		Keys.SIMULATION_LENGTH, 	60000,
		Keys.FUNDAMENTAL_MEAN, 		100000,
		Keys.FUNDAMENTAL_KAPPA,		0.05,
		Keys.FUNDAMENTAL_SHOCK_VAR, 1000000,
		Keys.FUNDAMENTAL_SHOCK_PROB, 1.0,
		Keys.RAND_SEED,				System.currentTimeMillis(),
		Keys.NUM_SIMULATIONS,		1,
		Keys.NBBO_LATENCY,			-1,
		Keys.MARKET_LATENCY,		-1,
		
		// Market
		Keys.PRICING_POLICY,		0.5,
		Keys.CLEAR_INTERVAL,		1000,
		
		
		// Agent-level defaults
		Keys.REENTRY_RATE,			0.005,
		
		// Agent Types by Role
		// HFT Agents
		Keys.LA_LATENCY, 			-1,
		Keys.ALPHA, 				0.001,

		// Background Agents		
		Keys.PRIVATE_VALUE_VAR,		1000000,
		Keys.MAX_POSITION, 			10,
		Keys.BID_RANGE_MIN, 		0,
		Keys.BID_RANGE_MAX, 		5000, 
		Keys.WINDOW_LENGTH, 		5000,
		
		Keys.ACCEPTABLE_PROFIT_THRESHOLD, 0.8, // For ZIRPs
		Keys.WITHDRAW_ORDERS, 		true,	// for ZIRs
	
		// AA Agent
		Keys.AGGRESSION, 			0,
		Keys.THETA, 				-4,
		Keys.THETA_MIN, 			-8,
		Keys.THETA_MAX, 			2,
		Keys.ETA, 					3,
		Keys.LAMBDA_R, 				0.05,
		Keys.LAMBDA_A, 				0.02,	// 0.02 in paper 
		Keys.GAMMA, 				2,
		Keys.BETA_R, 				0.4, 	// or U[0.2, 0.6] 
		Keys.BETA_T, 				0.4, 	// or U[0.2, 0.6] 
		Keys.BUYER_STATUS, 			true,
		Keys.DEBUG, 				false,
	
		// ZIP Agent
		Keys.MARGIN_MIN, 			0.05,
		Keys.MARGIN_MAX, 			0.35,
		Keys.GAMMA_MIN, 			0,
		Keys.GAMMA_MAX, 			0.1,
		Keys.BETA_MIN, 				0.1,
		Keys.BETA_MAX, 				0.5,
		Keys.COEFF_A, 				0.05, 
		Keys.COEFF_R, 				0.05,
		
		// FUNDA, of FundamentalAgent
		Keys.NOISE_STDEV,           5,
		Keys.PROB_FUND_AGENT,       0.7,
		
		// BayesMarketMaker
		Keys.BMM_SHADE_TICKS,       0,
		Keys.BMM_INVENTORY_FACTOR,  0,
	
		// Market Maker
		Keys.NUM_RUNGS, 			100,
		Keys.RUNG_SIZE, 			100,
		Keys.TRUNCATE_LADDER, 		true,
		Keys.TICK_IMPROVEMENT, 		true,
		Keys.TICK_OUTSIDE, 			false,
		Keys.INITIAL_LADDER_RANGE, 	1000,
		// Keys.INITIAL_LADDER_MEAN should be set to fundamental mean in agent strategies that use it
		
		// MAMM
		Keys.NUM_HISTORICAL, 		5,
		
		// WMAMM
		Keys.WEIGHT_FACTOR, 		0,
		
		// AdaptiveMM
		Keys.STRATS, 				new int[] {500,1000,2500,5000},
		Keys.USE_MEDIAN_SPREAD, 	false,
		Keys.FAST_LEARNING, 		true,
		Keys.USE_LAST_PRICE, 		true,
		
		// Fundamental MM
		Keys.FUNDAMENTAL_ESTIMATE,	-1,
		Keys.SPREAD, 				-1,		// for backwards compatibility
		Keys.UPDATE,				false
	);
}
