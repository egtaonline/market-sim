package systemmanager;

import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import logger.Log;
import logger.Log.Prefix;

import com.google.common.base.Objects;

import data.EntityProperties;
import data.MultiSimulationObservations;
import entity.agent.Agent;
import entity.agent.ZIRPAgent;
import entity.infoproc.ProcessorIDs;
import entity.market.Market;

/**
 * This class serves the purpose of the Client in the Command pattern, in that
 * it instantiates the Activity objects and provides the methods to execute them
 * later.
 * 
 * Usage: java -cp "$(ls lib/*.jar | tr '\n' :)dist/hft.jar" systemmanager.SystemManager [simulation folder name] [sample #]
 * 
 * @author ewah
 */
public class SystemManager {

    /**
     * Two input arguments: first is simulation folder, second is sample number.
     *
     * @param args
     */
    public static void main(String... args) {        
        File simulationFolder = new File(".");
        int observationNumber = 1;
        switch (args.length) {
        default:
            observationNumber = Integer.parseInt(args[1]);
            //$FALL-THROUGH$
        case 1:
            simulationFolder = new File(args[0]);
            //$FALL-THROUGH$
        case 0:
        }

        try {
            SystemManager manager =
                new SystemManager(simulationFolder, observationNumber);
            manager.executeSimulations();
            manager.writeResults();
            log.closeLogger();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private final File simulationFolder;
    private final int observationNumber,
        totalSimulations, simulationLength, logLevel;
    private final boolean outputConfig;
    private final long baseRandomSeed;
    private final SimulationSpec specification;
    private final MultiSimulationObservations observations;

    /**
     * Constructor reads everything in and sets appropriate variables.
     */
    public SystemManager(File simFolder, int obsNum) throws IOException {
        this.simulationFolder = simFolder;
        this.observationNumber = obsNum;
        this.specification = new SimulationSpec(new File(simFolder, Consts.SIM_SPEC_FILE));

        EntityProperties simProps = specification.getSimulationProps();
        this.totalSimulations = simProps.getAsInt(Keys.NUM_SIMULATIONS);
        this.baseRandomSeed = simProps.getAsLong(Keys.RAND_SEED);
        this.simulationLength = simProps.getAsInt(Keys.SIMULATION_LENGTH);

        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(new File(Consts.CONFIG_DIR, Consts.CONFIG_FILE));
        props.load(fis);
        fis.close();
        this.logLevel = Integer.parseInt(props.getProperty("logLevel", "0"));
        this.outputConfig = Boolean.parseBoolean(props.getProperty("outputConfig", "false"));
        this.observations = new MultiSimulationObservations(outputConfig, totalSimulations);
    }

    public SystemManager(File simFolder, int obsNum, String fileName) throws IOException {
        this.simulationFolder = simFolder;
        this.observationNumber = obsNum;
        this.specification = new SimulationSpec(new File(simFolder, fileName));
        EntityProperties simProps = specification.getSimulationProps();
        this.totalSimulations = simProps.getAsInt(Keys.NUM_SIMULATIONS);
        this.baseRandomSeed = simProps.getAsLong(Keys.RAND_SEED);
        this.simulationLength = simProps.getAsInt(Keys.SIMULATION_LENGTH);
        Properties props = new Properties();
        FileInputStream fis =
            new FileInputStream(new File(Consts.CONFIG_DIR, Consts.CONFIG_FILE));
        props.load(fis);
        fis.close();
        this.logLevel = Integer.parseInt(props.getProperty("logLevel", "0"));
        this.outputConfig = Boolean.parseBoolean(props.getProperty("outputConfig", "false"));
        this.observations = new MultiSimulationObservations(outputConfig, totalSimulations);
    }

    /**
     * Runs all of the simulations
     */
    public void executeSimulations() throws IOException {
        // for testing run time
        // final long startTime = System.nanoTime();
        Random rand = new Random();
        for (int i = 0; i < totalSimulations; i++) {
            Market.nextID = Agent.nextID = ProcessorIDs.nextID = 1; // Reset ids
            rand.setSeed(Objects.hashCode(baseRandomSeed, observationNumber * totalSimulations + i));
            Simulation sim = new Simulation(specification, rand);

            initializeLogger(logLevel, simulationFolder, observationNumber, i, sim, simulationLength);
            log.log(INFO, "Random Seed: %d", baseRandomSeed);
            log.log(INFO, "Configuration: %s", specification);

            sim.executeEvents();
            observations.addObservation(sim.getObservations());
        }

        // for testing run time
        /*
        final long endTime = System.nanoTime();
        final long duration = (endTime - startTime);
        final long nanosPerSec = 1000000000l;
        System.out.println(duration / nanosPerSec);
        */
    }

    /**
     * Must be done after "envProps" exists
     */
    protected static void initializeLogger(int logLevel, File simulationFolder,
            int observationNumber, int simulationNumber, final Simulation simulation,
            int simulationLength) throws IOException {
        if (logLevel == 0) { // No logging
            log = Log.nullLogger();
            return;
        }

        StringBuilder logFileName = new StringBuilder(
            new File(".").toURI().relativize(simulationFolder.toURI()).getPath().replace('/', '_'));
        logFileName.append(observationNumber).append('_');
        logFileName.append(simulationNumber).append('_');
        DateFormat localDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        logFileName.append(localDateFormat.format(new Date())).append(".txt");

        File logDir = new File(simulationFolder, Consts.LOG_DIR);
        logDir.mkdirs();

        File logFile = new File(logDir, logFileName.toString());
        final int digits = Integer.toString(simulationLength).length();

        // Create log file
        log = Log.create(Log.Level.values()[logLevel], logFile, new Prefix() {
            @Override
            public String getPrefix() {
                return String.format("%" + digits + "d| ", simulation.getCurrentTime().getInTicks());
            }
        });
    }

    public void writeResults() throws IOException {
        File results = new File(simulationFolder, Consts.OBS_FILE_PREFIX + observationNumber + ".json");
        observations.writeToFile(results);
    }

    public static double mean(final List<Double> list) {
        if (list.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Double item: list) {
            sum += item;
        }
        return 1.0 * sum / list.size();
    }

    public static double sampleVar(List<Double> list) {
        final double mean = mean(list);
        double sumDiffsSquared = 0.0;
        for (Double value : list) {
            double diff = value - mean;
            diff *= diff;
            sumDiffsSquared += diff;
        }

        return sumDiffsSquared  / (list.size() - 1);
     }

    public static List<List<Result>> flip(final List<List<Result>> input) {
        List<List<Result>> result = new ArrayList<List<Result>>();
        for (int i = 0; i < input.get(0).size(); i++) {
            List<Result> current = new ArrayList<Result>();
            for (int j = 0; j < input.size(); j++) {
                current.add(input.get(j).get(i));
            }
            result.add(current);
        }
        return result;
    }

    public static List<Result> getMeanResultList(
        final int index, final int trials
    ) throws Exception {
        List<List<Result>> resultLists = new ArrayList<List<Result>>();
        while (resultLists.size() < trials) {
            resultLists.add(getResultList(index));
        }
        resultLists = flip(resultLists);

        List<Result> result = new ArrayList<Result>();
        for (List<Result> resultList: resultLists) {
            double meanOfMeans = 0.0;
            double meanOfVars = 0.0;
            for (Result cur: resultList) {
                meanOfMeans += cur.mean;
                meanOfVars += cur.variance;
            }
            meanOfMeans /= resultList.size();
            meanOfVars /= resultList.size();
            result.add(new Result(meanOfMeans, meanOfVars));
        }

        return result;
    }

    public static List<Result> getResultList(
        final int index
    ) throws Exception {
        File simulationFolder = new File("simulations/test/");
        int observationNumber = 1;
        String fileName = "simulation_spec" + index + ".json";
        try {
            SystemManager manager =
                new SystemManager(
                    simulationFolder, observationNumber, fileName
                );
            Collection<Agent> agents = manager.getAgents();
            List<Integer> rMaxes = new ArrayList<Integer>();
            for (Agent agent: agents) {
                if (agent instanceof ZIRPAgent) {
                    ZIRPAgent zirp = (ZIRPAgent) agent;
                    if (!rMaxes.contains(zirp.bidRangeMax)) {
                        rMaxes.add(zirp.bidRangeMax);
                    }
                }
            }
            List<Result> results = new ArrayList<Result>();
            for (int rMax: rMaxes) {
                List<Double> times = new ArrayList<Double>();

                for (Agent agent: agents) {
                    if (agent instanceof ZIRPAgent) {
                        ZIRPAgent zirp = (ZIRPAgent) agent;
                        if (zirp.bidRangeMax == rMax) {
                            times.add((double)
                                agent.getArrivalTime().getInTicks());
                        }
                    }
                }
                final double mean = mean(times);
                final double var = sampleVar(times);
                results.add(new Result(mean, var));
            }

            return results;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class Result {
        public final double mean;
        public final double variance;
        public Result(final double aMean, final double aVariance) {
            this.mean = aMean;
            this.variance = aVariance;
        }

        @Override
        public String toString() {
            return "Result [mean=" + mean + ", variance=" + variance + "]";
        }
    }

    public Collection<Agent> getAgents() {
           Random rand = new Random();
            Market.nextID = Agent.nextID = ProcessorIDs.nextID = 1; // Reset ids
            Simulation sim = new Simulation(specification, rand);
            return sim.agents;
    }
}
