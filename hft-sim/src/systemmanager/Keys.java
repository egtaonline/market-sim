package systemmanager;

/**
 * All of the keys for use in the simulation spec. Parameters must be camelCase.
 * OK for observation file descriptors to not be camel case.
 * 
 * @author erik
 * 
 */
public interface Keys {

	public final static String PRESETS = 			"presets";
	
	public final static String RAND_SEED = 			"randomSeed";
	public final static String NUM_SIMULATIONS = 	"numSims";
	
	public final static String REENTRY_RATE = 		"reentryRate";
	public final static String BACKGROUND_REENTRY_RATE = "backgroundReentryRate";
	public final static String MARKETMAKER_REENTRY_RATE = "marketmakerReentryRate";
	
	public final static String MAX_POSITION = 		"maxPosition";
	
	public final static String TICK_SIZE = 			"tickSize";
	public final static String MARKET_TICK_SIZE = 	"marketTickSize";
	public final static String AGENT_TICK_SIZE = 	"agentTickSize";
	
	public final static String PRIVATE_VALUE_VAR = 	"privateValueVar";
	public final static String SIMULATION_LENGTH = 	"simLength";
	public final static String FUNDAMENTAL_MEAN = 	"meanValue";
	public final static String FUNDAMENTAL_KAPPA = 	"kappa";
	public final static String FUNDAMENTAL_SHOCK_VAR = "shockVar";
	// used for jump process; 1.0 if not a jump process
	public final static String FUNDAMENTAL_SHOCK_PROB =  "shockProbability";
	
	public final static String NUM = 				"num";
	
	// SimulationSpec Keys
	public final static String ASSIGN = 			"assignment";
	public final static String CONFIG = 			"configuration";

	// Observation Keys (control variates)
	public final static String PV_BUY1 = 			"pv_buy1";
	public final static String PV_SELL1 = 			"pv_sell1";
	public final static String PV_POSITION1_MAX_ABS = "pv_position_max_abs1";

	
	// Latency
	public final static String QUOTE_LATENCY = 		"quoteLatency";
	public final static String TRANSACTION_LATENCY = "transLatency";

	public final static String NBBO_LATENCY = 		"nbboLatency";
	public final static String MARKET_LATENCY = 	"mktLatency";
	public final static String LA_LATENCY = 		"laLatency";
	
	// Call Market
	public final static String CLEAR_INTERVAL =		"clearInterval";
	public final static String PRICING_POLICY = 	"pricingPolicy";

	
	// Agents
	public final static String WITHDRAW_ORDERS = 	"withdraw";
	public final static String BID_RANGE_MAX = 		"Rmax";
	public final static String BID_RANGE_MIN = 		"Rmin";
	public final static String WINDOW_LENGTH = 		"window";
	
	public final static String ALPHA = 				"alpha";
	
	// AAAgent
	// FIXME change AA agent params to all be set under a preset
	public final static String ETA = 				"eta";
	public final static String LAMBDA_R = 			"lambdaR";
	public final static String LAMBDA_A = 			"lambdaA";
	public final static String GAMMA = 				"gamma";
	public final static String BETA_R = 			"betaR";
	public final static String BETA_T = 			"betaT";
	public final static String AGGRESSION = 		"aggression";
	public final static String THETA = 				"theta";
	public final static String THETA_MAX = 			"thetaMax";
	public final static String THETA_MIN = 			"thetaMin";
	public final static String DEBUG = 				"debug";
	public final static String BUYER_STATUS = 		"buyer";
	
	// ZIPAgent
	public final static String MARGIN_MIN = 		"marginMin";
	public final static String MARGIN_MAX = 		"marginMax";
	public final static String GAMMA_MIN = 			"gammaMin";
	public final static String GAMMA_MAX = 			"gammaMax";
	public final static String BETA_MIN = 			"betaMin";
	public final static String BETA_MAX = 			"betaMax";
	public final static String COEFF_R = 			"rangeR";
	public final static String COEFF_A = 			"rangeA";
	
	// ZIRPAgent
	public final static String ACCEPTABLE_PROFIT_THRESHOLD = "thresh";
	
	// FundamentalAgent
	public final static String NOISE_STDEV = "noiseStdev";
	public final static String PROB_FUND_AGENT = "fundAgentProb"; // probability that arriving agent
	                                                               // is FundamentalAgent, rather than
	                                                               // ZIMOAgent
	
	// BayesMarketMaker
	public final static String BMM_SHADE_TICKS = "bmmShadeTicks";
	public final static String BMM_INVENTORY_FACTOR = "bmmInventoryFactor";

	// Market Data Agent
	public final static String FILENAME = 			"fileName";
	
	// Market Makers
	public final static String NUM_HISTORICAL = 	"N";
	
	public final static String NUM_RUNGS = 			"K";
	public final static String RUNG_SIZE = 			"size";
	public final static String TRUNCATE_LADDER = 	"trunc";
	public final static String TICK_IMPROVEMENT = 	"tickImprove";
	public final static String TICK_OUTSIDE = 		"tickOutside";
	public final static String INITIAL_LADDER_MEAN = "initLadderMean";
	public final static String INITIAL_LADDER_RANGE = "initLadderRange";
	
	public final static String WEIGHT_FACTOR = 		"w";
	public final static String STRATS = 			"strats";
	public final static String USE_MEDIAN_SPREAD = 	"median";
	public final static String FAST_LEARNING = 		"fastLearn";
	public final static String USE_LAST_PRICE = 	"lastPrice";
	
	public final static String FUNDAMENTAL_ESTIMATE = "fundEstimate";
	public final static String SPREAD = 			"spread";

	public final static String UPDATE = 			"update";		// XXX temporary fix
}
