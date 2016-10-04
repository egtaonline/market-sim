package systemmanager;

/**
 * System-wide constants
 * 
 * @author ewah
 */
public interface Consts {
	
	public enum DiscountFactor {
		NO_DISC(0), MEDIUM(0.0006);
		// SMALL(0.0003), LARGE(0.0009)

		public final double discount;

	    DiscountFactor(double discount) {
	        this.discount = discount;
	    }
	    
	    @Override
	    public String toString() {
	    	if (discount == 0)
	    		return "no_disc";
	    	return "disc_" + discount;
	    }
	}
	
	public final static int[] PERIODS = { 1, 250 };
	
	// **********************************************************
	// Agent, market, and model types
	// XXX UPDATE WHEN ADD NEW AGENT, MARKET, OR MODEL
	// TODO Use the class name instead of this strange enum? This could be a Collection<Class<? extends Agent>>
	public static enum AgentType { NOOP, AA, ZI, ZIP, ZIR, ZIRP, ZIMO, FUNDA, SCHEDA, MAXEFFICIENCY,
				    FUNDAMENTALMM, ADAPTIVEMM, BASICMM, CONSTMM, MAMM, WMAMM, BAYESMM, LA, ODA, MARKETDATA };
	public static enum MarketType { CDA, CALL };
	
	public static enum Presets { NONE, TWOMARKET, TWOMARKETLA, CENTRALCDA, CENTRALCALL, MAXEFF };
	
	// **********************************************************
	// FILENAMES
	
	// Directories
	public final static String TEST_OUTPUT_DIR = "simulations/unit_testing/";
	public final static String CONFIG_DIR = "config/";
	public final static String LOG_DIR = "logs/";
	
	// Config/spec file names
	public final static String SIM_SPEC_FILE = "simulation_spec.json";
	public final static String CONFIG_FILE = "env.properties";
	public final static String OBS_FILE_PREFIX = "observation";
	public final static String OBJS_FILE_PREFIX = "objects";

	// Constants in simulation_spec file
	public final static String SETUP_SUFFIX = "_setup";
	
	// Exchanges
	public final static String NYSE = "nyse";
	public final static String NASDAQ = "nasdaq";

}
