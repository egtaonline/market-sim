package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import systemmanager.Keys;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import event.TimeStamp;

public class BayesMarketMakerTest {

    private Executor exec;
    private FundamentalValue fundamental = new MockFundamental(100000);
    private Market market;
    private SIP sip;
    private static Random rand;
    private static final int simLength = 100;
    private static final int fundamentalMean = 100000;
    private static final int shockVar = 1000000;
    private static final int shockStdev = 1000;
    private static final int shadeTicks = 5;
    private static final double inventoryFactor = 0.5;
    private static final EntityProperties bayesMMProps = 
        EntityProperties.fromPairs(
            Keys.REENTRY_RATE, 0,
            Keys.TICK_SIZE, 1,
            Keys.TRUNCATE_LADDER, true,
            Keys.TICK_IMPROVEMENT, true,
            Keys.TICK_OUTSIDE, true,
            Keys.INITIAL_LADDER_MEAN, 0,
            Keys.INITIAL_LADDER_RANGE, 0,
            Keys.SIMULATION_LENGTH, simLength,
            Keys.FUNDAMENTAL_KAPPA, 0.05,
            Keys.FUNDAMENTAL_MEAN, fundamentalMean,
            Keys.SPREAD, -1,
            Keys.FUNDAMENTAL_ESTIMATE, -1,
            Keys.PROB_FUND_AGENT, 0.7,
            Keys.NOISE_STDEV, 5,
            Keys.FUNDAMENTAL_SHOCK_VAR, shockVar,
            Keys.BMM_SHADE_TICKS, shadeTicks,
            Keys.BMM_INVENTORY_FACTOR, inventoryFactor
        );
    
    private static final EntityProperties bayesMMPropsZeroKZeroGamma = 
        EntityProperties.fromPairs(
            Keys.REENTRY_RATE, 0,
            Keys.TICK_SIZE, 1,
            Keys.TRUNCATE_LADDER, true,
            Keys.TICK_IMPROVEMENT, true,
            Keys.TICK_OUTSIDE, true,
            Keys.INITIAL_LADDER_MEAN, 0,
            Keys.INITIAL_LADDER_RANGE, 0,
            Keys.SIMULATION_LENGTH, simLength,
            Keys.FUNDAMENTAL_KAPPA, 0.05,
            Keys.FUNDAMENTAL_MEAN, fundamentalMean,
            Keys.SPREAD, -1,
            Keys.FUNDAMENTAL_ESTIMATE, -1,
            Keys.PROB_FUND_AGENT, 0.7,
            Keys.NOISE_STDEV, 5,
            Keys.FUNDAMENTAL_SHOCK_VAR, shockVar,
            Keys.BMM_SHADE_TICKS, 0,
            Keys.BMM_INVENTORY_FACTOR, 0.0
        );
    
    private static final EntityProperties bayesMMPropsZeroGamma = 
        EntityProperties.fromPairs(
            Keys.REENTRY_RATE, 0,
            Keys.TICK_SIZE, 1,
            Keys.TRUNCATE_LADDER, true,
            Keys.TICK_IMPROVEMENT, true,
            Keys.TICK_OUTSIDE, true,
            Keys.INITIAL_LADDER_MEAN, 0,
            Keys.INITIAL_LADDER_RANGE, 0,
            Keys.SIMULATION_LENGTH, simLength,
            Keys.FUNDAMENTAL_KAPPA, 0.05,
            Keys.FUNDAMENTAL_MEAN, fundamentalMean,
            Keys.SPREAD, -1,
            Keys.FUNDAMENTAL_ESTIMATE, -1,
            Keys.PROB_FUND_AGENT, 0.7,
            Keys.NOISE_STDEV, 5,
            Keys.FUNDAMENTAL_SHOCK_VAR, shockVar,
            Keys.BMM_SHADE_TICKS, shadeTicks,
            Keys.BMM_INVENTORY_FACTOR, 0.0
        );
    
    private static final EntityProperties bayesMMNoShock = 
        EntityProperties.fromPairs(
            Keys.REENTRY_RATE, 0,
            Keys.TICK_SIZE, 1,
            Keys.TRUNCATE_LADDER, true,
            Keys.TICK_IMPROVEMENT, true,
            Keys.TICK_OUTSIDE, true,
            Keys.INITIAL_LADDER_MEAN, 0,
            Keys.INITIAL_LADDER_RANGE, 0,
            Keys.SIMULATION_LENGTH, simLength,
            Keys.FUNDAMENTAL_KAPPA, 0.05,
            Keys.FUNDAMENTAL_MEAN, fundamentalMean,
            Keys.SPREAD, -1,
            Keys.FUNDAMENTAL_ESTIMATE, -1,
            Keys.PROB_FUND_AGENT, 0.7,
            Keys.NOISE_STDEV, 5,
            Keys.FUNDAMENTAL_SHOCK_VAR, 0.0,
            Keys.BMM_SHADE_TICKS, shadeTicks,
            Keys.BMM_INVENTORY_FACTOR, inventoryFactor
        );
    
    private static final EntityProperties bayesMMPropsAllZIMO = 
        EntityProperties.fromPairs(
            Keys.REENTRY_RATE, 0,
            Keys.TICK_SIZE, 1,
            Keys.TRUNCATE_LADDER, true,
            Keys.TICK_IMPROVEMENT, true,
            Keys.TICK_OUTSIDE, true,
            Keys.INITIAL_LADDER_MEAN, 0,
            Keys.INITIAL_LADDER_RANGE, 0,
            Keys.SIMULATION_LENGTH, simLength,
            Keys.FUNDAMENTAL_KAPPA, 0.05,
            Keys.FUNDAMENTAL_MEAN, fundamentalMean,
            Keys.SPREAD, -1,
            Keys.FUNDAMENTAL_ESTIMATE, -1,
            Keys.PROB_FUND_AGENT, 0.0,
            Keys.NOISE_STDEV, 5,
            Keys.FUNDAMENTAL_SHOCK_VAR, shockVar,
            Keys.BMM_SHADE_TICKS, shadeTicks,
            Keys.BMM_INVENTORY_FACTOR, inventoryFactor
        );
    
    private static final EntityProperties bayesMMPropsAllFundNoNoise = 
        EntityProperties.fromPairs(
            Keys.REENTRY_RATE, 0,
            Keys.TICK_SIZE, 1,
            Keys.TRUNCATE_LADDER, true,
            Keys.TICK_IMPROVEMENT, true,
            Keys.TICK_OUTSIDE, true,
            Keys.INITIAL_LADDER_MEAN, 0,
            Keys.INITIAL_LADDER_RANGE, 0,
            Keys.SIMULATION_LENGTH, simLength,
            Keys.FUNDAMENTAL_KAPPA, 0.05,
            Keys.FUNDAMENTAL_MEAN, fundamentalMean,
            Keys.SPREAD, -1,
            Keys.FUNDAMENTAL_ESTIMATE, -1,
            Keys.PROB_FUND_AGENT, 1.0,
            Keys.NOISE_STDEV, 0,
            Keys.FUNDAMENTAL_SHOCK_VAR, shockVar,
            Keys.BMM_SHADE_TICKS, shadeTicks,
            Keys.BMM_INVENTORY_FACTOR, inventoryFactor
        );

    private static final double delta = 0.001;
    private static final double weakerDelta = 1.0;
    
    @BeforeClass
    public static void setUpClass() throws IOException{
        // Setting up the log file
        log = Log.create(DEBUG, new File(
            Consts.TEST_OUTPUT_DIR + "BayesMarketMakerTest.log")
        );

        // Creating the setup properties
        rand = new Random();        
    }
    
    @Before
    public void setup(){
        exec = new Executor();
        sip = new SIP(exec, TimeStamp.IMMEDIATE);
        // Creating the MockMarket
        market = new MockMarket(exec, sip);
        

    }
    
    // initBelief:
    // size of belief vector is (shockStdev * 8 + 1)
    // all values are 0, except middle value, which is 1
    @Test
    public void initBeliefTest() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        bayesMM.initBelief();
        assertEquals(bayesMM.belief.size(), shockStdev * 8 + 1);
        for (int i = 0; i < bayesMM.belief.size(); i++) {
            if (i == shockStdev * 4) {
                assertEquals(bayesMM.belief.get(i), 1.0, delta);
            } else {
                assertEquals(bayesMM.belief.get(i), 0.0, delta);
            }
        }
    }
    
    // resetBelief:
    // size of belief vector is (shockStdev * 8 + 1),
    //     as long as (current - shockStdDev) * 4 > 0
    // sum of belief vector is 1
    // all belief entries are >= 0
    // expected value of belief vector is current
    // standard deviation of belief is approximately shockStdev
    // strict maximum of belief vector is in middle entry
    // with 0 shockVar:
    //     all values are 0, except middle value, which is 1
    @Test
    public void resetBeliefTest() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        bayesMM.resetBelief(fundamentalMean);
        assertEquals(
            bayesMM.belief.size(), 
            shockStdev * 8 + 1
        );
        assertEquals(
            beliefSum(bayesMM.belief), 
            1.0, 
            delta
        );
        assertTrue(isNonNegative(bayesMM.belief));
        assertEquals(
            expectedValue(bayesMM.belief, bayesMM.vMin), 
            fundamentalMean, 
            weakerDelta
        );        
        assertEquals(
            stdDev(bayesMM.belief),
            shockStdev,
            weakerDelta
        );
        assertTrue(middleIsStrictMax(bayesMM.belief));
        
        final BayesMarketMaker bayesMMNoShock = 
            getBayesMarketMakerNoShock();
        bayesMMNoShock.resetBelief(fundamentalMean);
        assertEquals(bayesMMNoShock.belief.get(0), 1.0, delta);
    }
    
    // sellOccurred:
    // size of belief vector is (shockStdev * 8 + 1),
    //     as long as (current - shockStdDev) * 4 > 0
    // sum of belief vector is 1
    // all belief entries are >= 0
    // expected value after, is less than expected value before
    @Test
    public void testSellOccurred() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        bayesMM.resetBelief(fundamentalMean);
        
        final int repeats = 5;
        double previousExpectedValue = 
            expectedValue(bayesMM.belief, bayesMM.vMin);
        for (int i = 0; i < repeats; i++) {
            setBidAndAsk(bayesMM);
            bayesMM.sellOccurred();
            
            assertEquals(
                bayesMM.belief.size(), 
                shockStdev * 8 + 1
            );
            assertEquals(
                beliefSum(bayesMM.belief), 
                1.0, 
                delta
            );
            assertTrue(isNonNegative(bayesMM.belief));
            
            double currentExpectedValue = 
                expectedValue(bayesMM.belief, bayesMM.vMin);
            assertTrue(currentExpectedValue < previousExpectedValue);
            previousExpectedValue = currentExpectedValue;
        }
    }
    
    // buyOccurred:
    // size of belief vector is (shockStdev * 8 + 1),
    //     as long as (current - shockStdDev) * 4 > 0
    // sum of belief vector is 1
    // all belief entries are >= 0
    // expected value after, is greater than expected value before
    @Test
    public void testBuyOccurred() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        bayesMM.resetBelief(fundamentalMean);
        
        final int repeats = 5;
        double previousExpectedValue = 
            expectedValue(bayesMM.belief, bayesMM.vMin);
        for (int i = 0; i < repeats; i++) {
            setBidAndAsk(bayesMM);
            bayesMM.buyOccurred();
            
            assertEquals(
                bayesMM.belief.size(), 
                shockStdev * 8 + 1
            );
            assertEquals(
                beliefSum(bayesMM.belief), 
                1.0, 
                delta
            );
            assertTrue(isNonNegative(bayesMM.belief));
            
            double currentExpectedValue = 
                expectedValue(bayesMM.belief, bayesMM.vMin);
            assertTrue(currentExpectedValue > previousExpectedValue);
            previousExpectedValue = currentExpectedValue;
        }
    }
    
    // noTradeOccurred:
    // size of belief vector is (shockStdev * 8 + 1),
    //     as long as (current - shockStdDev) * 4 > 0
    // sum of belief vector is 1
    // all belief entries are >= 0
    // expected value after, equals expected value before
    @Test
    public void testNoTradeOccurred() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        bayesMM.resetBelief(fundamentalMean);
        
        final int repeats = 5;
        double previousExpectedValue = 
            expectedValue(bayesMM.belief, bayesMM.vMin);
        for (int i = 0; i < repeats; i++) {
            setBidAndAsk(bayesMM);
            bayesMM.noTradeOccurred();
            
            assertEquals(
                bayesMM.belief.size(), 
                shockStdev * 8 + 1
            );
            assertEquals(
                beliefSum(bayesMM.belief), 
                1.0, 
                delta
            );
            assertTrue(isNonNegative(bayesMM.belief));
            
            double currentExpectedValue = 
                expectedValue(bayesMM.belief, bayesMM.vMin);
            assertEquals(
                previousExpectedValue, 
                currentExpectedValue, 
                weakerDelta
            );
            previousExpectedValue = currentExpectedValue;
        }
    }
    
    // probSell:
    // in [0, 1]
    // in typical conditions, increases with increasing bidPrice
    // with all noise traders, always 0.5
    // with all informed traders and 0 noiseStandardDev,
    //     and initial belief vector:
    //     1 if bidPrice > fundamentalMean
    //     0 if bidPrice < fundamentalMean
    @Test
    public void testProbSell() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        bayesMM.resetBelief(fundamentalMean);
        
        final int repeats = 5;
        final int offset = 3;
        
        int bidPrice = fundamentalMean;
        double previousProbSell = bayesMM.probSell(bidPrice);
        for (int i = 0; i < repeats; i++) {
            bidPrice += offset;
            double currentProbSell = bayesMM.probSell(bidPrice);
            assertTrue(currentProbSell > 0.0);
            assertTrue(currentProbSell < 1.0);
            assertTrue(currentProbSell > previousProbSell);
            previousProbSell = currentProbSell;
        }

        final BayesMarketMaker bayesMMAllZIMO = getBayesMarketMakerAllZIMO();
        bayesMMAllZIMO.resetBelief(fundamentalMean);
        bidPrice = fundamentalMean;
        for (int i = 0; i < repeats; i++) {
            bidPrice += offset;
            double currentProbSell = bayesMMAllZIMO.probSell(bidPrice);
            assertEquals(currentProbSell, 0.5, delta);
        }
        
        final BayesMarketMaker bayesMMAllFundNoNoise = 
            getBayesMarketMakerAllFundNoNoise();
        bayesMMAllFundNoNoise.initBelief();
        bidPrice = fundamentalMean + offset;
        double currentProbSell = bayesMMAllFundNoNoise.probSell(bidPrice);
        assertEquals(currentProbSell, 1.0, delta);
        bidPrice = fundamentalMean - offset;
        currentProbSell = bayesMMAllFundNoNoise.probSell(bidPrice);
        assertEquals(currentProbSell, 0.0, delta);
    }
    
    // probBuy:
    // in [0, 1]
    // in typical conditions, increases with decreasing askPrice
    // with all noise traders, always 0.5
    // with all informed traders and 0 noiseStandardDev,
    //     and initial belief vector:
    //     1 if askPrice < fundamentalMean
    //     0 if askPrice > fundamentalMean
    @Test
    public void testProbBuy() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        bayesMM.resetBelief(fundamentalMean);
        
        final int repeats = 5;
        final int offset = 3;
        
        int askPrice = fundamentalMean;
        double previousProbBuy = bayesMM.probBuy(askPrice);
        for (int i = 0; i < repeats; i++) {
            askPrice -= offset;
            double currentProbBuy = bayesMM.probBuy(askPrice);
            assertTrue(currentProbBuy > 0.0);
            assertTrue(currentProbBuy < 1.0);
            assertTrue(currentProbBuy > previousProbBuy);
            previousProbBuy = currentProbBuy;
        }
        
        final BayesMarketMaker bayesMMAllZIMO = getBayesMarketMakerAllZIMO();
        bayesMMAllZIMO.resetBelief(fundamentalMean);
        askPrice = fundamentalMean;
        for (int i = 0; i < repeats; i++) {
            askPrice -= offset;
            double currentProbBuy = bayesMMAllZIMO.probBuy(askPrice);
            assertEquals(currentProbBuy, 0.5, delta);
        }
        
        final BayesMarketMaker bayesMMAllFundNoNoise = 
            getBayesMarketMakerAllFundNoNoise();
        bayesMMAllFundNoNoise.initBelief();
        askPrice = fundamentalMean - offset;
        double currentProbBuy = bayesMMAllFundNoNoise.probBuy(askPrice);
        assertEquals(currentProbBuy, 1.0, delta);
        askPrice = fundamentalMean + offset;
        currentProbBuy = bayesMMAllFundNoNoise.probBuy(askPrice);
        assertEquals(currentProbBuy, 0.0, delta);
    }
    
    // probNoTrade:
    // in [0, 1]
    // plus probBuy plus probSell == 1
    // in typical conditions:
    //     increases with decreasing bidPrice
    //     increases with increasing askPrice
    //     never more than probFundAgent [because ZIMOAgent always trades]
    // with all noise traders, always 0.0
    // with all informed traders and 0 noiseStandardDev,
    //     and initial belief vector:
    //     1 if askPrice > fundamentalMean && bidPrice < fundamentalMean
    //     0 if askPrice < fundamentalMean
    //     0 if bidPrice > fundamentalMean
    @Test
    public void testProbNoTrade() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        bayesMM.resetBelief(fundamentalMean);
        
        final int repeats = 5;
        final int offset = 3;
        
        int bidPrice = fundamentalMean;
        int askPrice = fundamentalMean;
        double previousProbNoTrade = bayesMM.probNoTrade(bidPrice, askPrice);
        for (int i = 0; i < repeats; i++) {
            askPrice += offset;
            double currentProbNoTrade = bayesMM.probNoTrade(bidPrice, askPrice);
            assertTrue(currentProbNoTrade > 0.0);
            assertTrue(currentProbNoTrade < 1.0);
            assertTrue(currentProbNoTrade <= bayesMM.probFundAgent);
            assertTrue(currentProbNoTrade > previousProbNoTrade);
            double currentProbBuy = bayesMM.probBuy(askPrice);
            double currentProbSell = bayesMM.probSell(bidPrice);
            assertEquals(
                currentProbNoTrade + currentProbBuy + currentProbSell, 
                1.0, 
                delta
            );
            previousProbNoTrade = currentProbNoTrade;
        }
        
        bidPrice = fundamentalMean;
        askPrice = fundamentalMean;
        previousProbNoTrade = bayesMM.probNoTrade(bidPrice, askPrice);
        for (int i = 0; i < repeats; i++) {
            bidPrice -= offset;
            double currentProbNoTrade = bayesMM.probNoTrade(bidPrice, askPrice);
            assertTrue(currentProbNoTrade > 0.0);
            assertTrue(currentProbNoTrade < 1.0);
            assertTrue(currentProbNoTrade <= bayesMM.probFundAgent);
            assertTrue(currentProbNoTrade > previousProbNoTrade);
            double currentProbBuy = bayesMM.probBuy(askPrice);
            double currentProbSell = bayesMM.probSell(bidPrice);
            assertEquals(
                currentProbNoTrade + currentProbBuy + currentProbSell, 
                1.0, 
                delta
            );
            previousProbNoTrade = currentProbNoTrade;
        }
        
        final BayesMarketMaker bayesMMAllZIMO = getBayesMarketMakerAllZIMO();
        bayesMMAllZIMO.resetBelief(fundamentalMean);
        bidPrice = fundamentalMean - offset * repeats;
        askPrice = fundamentalMean + offset * repeats;
        for (int i = 0; i < repeats; i++) {
            askPrice -= offset;
            double currentProbNoTrade = bayesMMAllZIMO.probNoTrade(bidPrice, askPrice);
            assertEquals(currentProbNoTrade, 0.0, delta);
            double currentProbBuy = bayesMMAllZIMO.probBuy(askPrice);
            double currentProbSell = bayesMMAllZIMO.probSell(bidPrice);
            assertEquals(
                currentProbNoTrade + currentProbBuy + currentProbSell, 
                1.0, 
                delta
            );
        }
        
        final BayesMarketMaker bayesMMAllFundNoNoise = 
            getBayesMarketMakerAllFundNoNoise();
        bayesMMAllFundNoNoise.initBelief();
        bidPrice = fundamentalMean - 2 * offset;
        askPrice = fundamentalMean + 2 * offset;
        double currentProbNoTrade = bayesMMAllFundNoNoise.probNoTrade(bidPrice, askPrice);
        assertEquals(currentProbNoTrade, 1.0, delta);
        askPrice = fundamentalMean - offset;
        currentProbNoTrade = bayesMMAllFundNoNoise.probNoTrade(bidPrice, askPrice);
        assertEquals(currentProbNoTrade, 0.0, delta);
        bidPrice = fundamentalMean + offset;
        askPrice = fundamentalMean + 2 * offset;
        currentProbNoTrade = bayesMMAllFundNoNoise.probNoTrade(bidPrice, askPrice);
        assertEquals(currentProbNoTrade, 0.0, delta);
        double currentProbBuy = bayesMMAllFundNoNoise.probBuy(askPrice);
        double currentProbSell = bayesMMAllFundNoNoise.probSell(bidPrice);
        assertEquals(
            currentProbNoTrade + currentProbBuy + currentProbSell, 
            1.0, 
            delta
        );
    }
    
    // probSellGivenV:
    // in [0, 1]
    // in typical conditions:
    //     increases with increasing bidPrice
    //     decreases with increasing v
    // with all noise traders, always 0.5
    // with all informed traders and 0 noiseStandardDev:
    //     1 if bidPrice > v
    //     0 if bidPrice < v
    @Test
    public void testProbSellGivenV() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        
        final int repeats = 5;
        final int offset = 3;

        int v= fundamentalMean;
        int bidPrice = fundamentalMean;
        double previousProbSell = bayesMM.probSellGivenV(v, bidPrice);
        for (int i = 0; i < repeats; i++) {
            bidPrice += offset;
            double currentProbSell = bayesMM.probSellGivenV(v, bidPrice);
            assertTrue(currentProbSell > 0.0);
            assertTrue(currentProbSell < 1.0);
            assertTrue(currentProbSell > previousProbSell);
            previousProbSell = currentProbSell;
        }
        
        v= fundamentalMean;
        bidPrice = fundamentalMean;
        previousProbSell = bayesMM.probSellGivenV(v, bidPrice);
        for (int i = 0; i < repeats; i++) {
            v += offset;
            double currentProbSell = bayesMM.probSellGivenV(v, bidPrice);
            assertTrue(currentProbSell > 0.0);
            assertTrue(currentProbSell < 1.0);
            assertTrue(currentProbSell < previousProbSell);
            previousProbSell = currentProbSell;
        }
        
        final BayesMarketMaker bayesMMAllZIMO = getBayesMarketMakerAllZIMO();
        bayesMMAllZIMO.resetBelief(fundamentalMean);
        bidPrice = fundamentalMean;
        v= fundamentalMean;
        for (int i = 0; i < repeats; i++) {
            bidPrice += offset;
            double currentProbSell = bayesMMAllZIMO.probSellGivenV(v, bidPrice);
            assertEquals(currentProbSell, 0.5, delta);
        }
        
        final BayesMarketMaker bayesMMAllFundNoNoise = 
            getBayesMarketMakerAllFundNoNoise();
        v = fundamentalMean + offset;
        bidPrice = v + offset;
        double currentProbSell = bayesMMAllFundNoNoise.probSellGivenV(v, bidPrice);
        assertEquals(currentProbSell, 1.0, delta);
        bidPrice = v - offset;
        currentProbSell = bayesMMAllFundNoNoise.probSellGivenV(v, bidPrice);
        assertEquals(currentProbSell, 0.0, delta);
    }
    
    // probBuyGivenV:
    // in [0, 1]
    // in typical conditions:
    //     increases with decreasing askPrice
    //     increases with increasing v   
    // with all noise traders, always 0.5
    // with all informed traders and 0 noiseStandardDev:
    //     1 if askPrice < v
    //     0 if askPrice > v
    @Test
    public void testProbBuyGivenV() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        
        final int repeats = 5;
        final int offset = 3;

        int v= fundamentalMean;
        int askPrice = fundamentalMean;
        double previousProbBuy = bayesMM.probBuyGivenV(v, askPrice);
        for (int i = 0; i < repeats; i++) {
            askPrice -= offset;
            double currentProbBuy = bayesMM.probBuyGivenV(v, askPrice);
            assertTrue(currentProbBuy > 0.0);
            assertTrue(currentProbBuy < 1.0);
            assertTrue(currentProbBuy > previousProbBuy);
            previousProbBuy = currentProbBuy;
        }
        
        v= fundamentalMean;
        askPrice = fundamentalMean;
        previousProbBuy = bayesMM.probBuyGivenV(v, askPrice);
        for (int i = 0; i < repeats; i++) {
            v += offset;
            double currentProbBuy = bayesMM.probBuyGivenV(v, askPrice);
            assertTrue(currentProbBuy > 0.0);
            assertTrue(currentProbBuy < 1.0);
            assertTrue(currentProbBuy > previousProbBuy);
            previousProbBuy = currentProbBuy;
        }
        
        final BayesMarketMaker bayesMMAllZIMO = getBayesMarketMakerAllZIMO();
        bayesMMAllZIMO.resetBelief(fundamentalMean);
        askPrice = fundamentalMean;
        v= fundamentalMean;
        for (int i = 0; i < repeats; i++) {
            askPrice -= offset;
            double currentProbBuy = bayesMMAllZIMO.probBuyGivenV(v, askPrice);
            assertEquals(currentProbBuy, 0.5, delta);
        }
        
        final BayesMarketMaker bayesMMAllFundNoNoise = 
            getBayesMarketMakerAllFundNoNoise();
        v = fundamentalMean + offset;
        askPrice = v - offset;
        double currentProbBuy = bayesMMAllFundNoNoise.probBuyGivenV(v, askPrice);
        assertEquals(currentProbBuy, 1.0, delta);
        askPrice = v + offset;
        currentProbBuy = bayesMMAllFundNoNoise.probBuyGivenV(v, askPrice);
        assertEquals(currentProbBuy, 0.0, delta);
    }
    
    // probNoTradeGivenV:
    // in [0, 1]
    // in typical conditions:
    //     increases with decreasing bidPrice
    //     increases with increasing askPrice
    // with all noise traders, always 0.0
    // with all informed traders and 0 noiseStandardDev:
    //     1 if askPrice > v && bidPrice < v
    //     0 if askPrice < v
    //     0 if bidPrice > v
    // in all cases:
    //     probSellGivenV + probBuyGivenV + probNoTradeGivenV = 1.0
    @Test
    public void testProbNoTradeGivenV() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        
        final int repeats = 5;
        final int offset = 3;

        int v= fundamentalMean;
        int bidPrice = fundamentalMean;
        int askPrice = fundamentalMean;
        double previousProbNoTrade = bayesMM.probNoTradeGivenV(v, bidPrice, askPrice);
        for (int i = 0; i < repeats; i++) {
            bidPrice -= offset;
            double currentProbNoTrade = bayesMM.probNoTradeGivenV(v, bidPrice, askPrice);
            assertTrue(currentProbNoTrade > 0.0);
            assertTrue(currentProbNoTrade < 1.0);
            assertTrue(currentProbNoTrade > previousProbNoTrade);
            
            double currentProbBuy = bayesMM.probBuyGivenV(v, askPrice);
            double currentProbSell = bayesMM.probSellGivenV(v, bidPrice);
            assertEquals(
                currentProbNoTrade + currentProbBuy + currentProbSell, 
                1.0, 
                delta
            );
            previousProbNoTrade = currentProbNoTrade;
        }
        
        bidPrice = fundamentalMean;
        askPrice = fundamentalMean;
        previousProbNoTrade = bayesMM.probNoTradeGivenV(v, bidPrice, askPrice);
        for (int i = 0; i < repeats; i++) {
            askPrice += offset;
            double currentProbNoTrade = bayesMM.probNoTradeGivenV(v, bidPrice, askPrice);
            assertTrue(currentProbNoTrade > 0.0);
            assertTrue(currentProbNoTrade < 1.0);
            assertTrue(currentProbNoTrade > previousProbNoTrade);
            
            double currentProbBuy = bayesMM.probBuyGivenV(v, askPrice);
            double currentProbSell = bayesMM.probSellGivenV(v, bidPrice);
            assertEquals(
                currentProbNoTrade + currentProbBuy + currentProbSell, 
                1.0, 
                delta
            );
            
            previousProbNoTrade = currentProbNoTrade;
        }
        
        final BayesMarketMaker bayesMMAllZIMO = getBayesMarketMakerAllZIMO();
        bayesMMAllZIMO.resetBelief(fundamentalMean);
        askPrice = fundamentalMean;
        v= fundamentalMean;
        for (int i = 0; i < repeats; i++) {
            askPrice += offset;
            double currentProbNoTrade = bayesMMAllZIMO.probNoTradeGivenV(v, bidPrice, askPrice);
            assertEquals(currentProbNoTrade, 0.0, delta);
            
            double currentProbBuy = bayesMMAllZIMO.probBuyGivenV(v, askPrice);
            double currentProbSell = bayesMMAllZIMO.probSellGivenV(v, bidPrice);
            assertEquals(
                currentProbNoTrade + currentProbBuy + currentProbSell, 
                1.0, 
                delta
            );
        }
        
        final BayesMarketMaker bayesMMAllFundNoNoise = 
            getBayesMarketMakerAllFundNoNoise();
        v = fundamentalMean;
        bidPrice = v - offset;
        askPrice = v + offset;
        double currentProbNoTrade = 
            bayesMMAllFundNoNoise.probNoTradeGivenV(v, bidPrice, askPrice);
        assertEquals(currentProbNoTrade, 1.0, delta);
        double currentProbBuy = bayesMMAllFundNoNoise.probBuyGivenV(v, askPrice);
        double currentProbSell = bayesMMAllFundNoNoise.probSellGivenV(v, bidPrice);
        assertEquals(
            currentProbNoTrade + currentProbBuy + currentProbSell, 
            1.0, 
            delta
        );
        
        bidPrice = v - 2 * offset;
        askPrice = v - offset;
        currentProbNoTrade = 
            bayesMMAllFundNoNoise.probNoTradeGivenV(v, bidPrice, askPrice);
        assertEquals(currentProbNoTrade, 0.0, delta);
        currentProbBuy = bayesMMAllFundNoNoise.probBuyGivenV(v, askPrice);
        currentProbSell = bayesMMAllFundNoNoise.probSellGivenV(v, bidPrice);
        assertEquals(
            currentProbNoTrade + currentProbBuy + currentProbSell, 
            1.0, 
            delta
        );
        
        bidPrice = v + offset;
        askPrice = v + 2 * offset;
        currentProbNoTrade = 
            bayesMMAllFundNoNoise.probNoTradeGivenV(v, bidPrice, askPrice);
        assertEquals(currentProbNoTrade, 0.0, delta);
        currentProbBuy = bayesMMAllFundNoNoise.probBuyGivenV(v, askPrice);
        currentProbSell = bayesMMAllFundNoNoise.probSellGivenV(v, bidPrice);
        assertEquals(
            currentProbNoTrade + currentProbBuy + currentProbSell, 
            1.0, 
            delta
        );
    }
    
    // expectedValue:
    // after resetBelief:
    //     equals fundamentalMean
    @Test
    public void testExpectedValue() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        final int repeats = 5;
        final int offset = 50;
        for (int i = 0; i < repeats; i++) {
            final int newMean = fundamentalMean + offset * i;
            bayesMM.resetBelief(newMean);
            final double actualMean = expectedValue(bayesMM.belief, bayesMM.vMin);
            assertEquals(actualMean, newMean, weakerDelta);
        }
    }
    
    // normalizeBelief:
    // all values in [0, 1]
    // sum of values is 1, to within tolerance = 0.001
    @Test
    public void testNormalizeBelief() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        final int repeats = 5;
        final int offset = 50;
        for (int i = 0; i < repeats; i++) {
            bayesMM.resetBelief(fundamentalMean + offset * i);
            for (int j = 0; j < bayesMM.belief.size(); j++) {
                final double newValue = bayesMM.belief.get(j) + Math.random() / bayesMM.belief.size();
                bayesMM.belief.set(j, newValue);
            }
            
            bayesMM.normalizeBelief();
            assertEquals(beliefSum(bayesMM.belief), 1.0, delta);
            assertTrue(isNonNegative(bayesMM.belief));
        }
    }
    
    // findBidWithShadeAndInv:
    // with k = 0 and \gamma = 0:
    //    same as findBidPriceNoProfit()
    // with k > 0, \gamma = 0:
    //     findBidPriceNoProfit() - k
    // with \gamma > 0, inventory > 0:
    //     Math.round(findBidPriceNoProfit() - k - \gamma * inventory)
    // with \gamma > 0, inventory < 0:
    //     Math.round(findBidPriceNoProfit() - k - \gamma * inventory)
    @Test
    public void testFindBidWithShadeAndInv() {
        final BayesMarketMaker bayesMMZeroKZeroGamma = 
            getBayesMarketMakerZeroKZeroGamma();
        
        final int repeats = 5;
        int inventory = 5;
        final int offset = 3;
        bayesMMZeroKZeroGamma.positionBalance = inventory;
        for (int i = 0; i < repeats; i++) {
            bayesMMZeroKZeroGamma.resetBelief(fundamentalMean + offset * i);
            assertEquals(
                bayesMMZeroKZeroGamma.findBidWithShadeAndInv(),
                bayesMMZeroKZeroGamma.findBidPriceNoProfit(),
                delta
            );
        }

        final BayesMarketMaker bayesMMZeroGamma = 
            getBayesMarketMakerZeroGamma();
        bayesMMZeroGamma.positionBalance = inventory;
        for (int i = 0; i < repeats; i++) {
            bayesMMZeroGamma.resetBelief(fundamentalMean + offset * i);
            assertEquals(
                bayesMMZeroGamma.findBidWithShadeAndInv(),
                bayesMMZeroGamma.findBidPriceNoProfit() - shadeTicks,
                delta
            );
        }  
        
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        bayesMM.positionBalance = inventory;
        for (int i = 0; i < repeats; i++) {
            bayesMM.resetBelief(fundamentalMean + offset * i);
            assertEquals(
                bayesMM.findBidWithShadeAndInv(),
                Math.round(
                    bayesMM.findBidPriceNoProfit() 
                    - shadeTicks - inventoryFactor * inventory
                ),
                delta
            );
        }  
        
        inventory = -5;
        bayesMM.positionBalance = inventory;
        for (int i = 0; i < repeats; i++) {
            bayesMM.resetBelief(fundamentalMean + offset * i);
            assertEquals(
                bayesMM.findBidWithShadeAndInv(),
                Math.round(
                    bayesMM.findBidPriceNoProfit() 
                    - shadeTicks - inventoryFactor * inventory
                ),
                delta
            );
        }  
    }
    
    // findAskWithShadeAndInv:
    // with k = 0 and \gamma = 0:
    //    same as findAskPriceNoProfit()
    // with k > 0, \gamma = 0:
    //     findAskPriceNoProfit() + k
    // with \gamma > 0, inventory > 0:
    //     Math.round(findAskPriceNoProfit() + k - \gamma * inventory)
    // with \gamma > 0, inventory < 0:
    //     Math.round(findAskPriceNoProfit() + k - \gamma * inventory)
    @Test
    public void testFindAskWithShadeAndInv() {
        final BayesMarketMaker bayesMMZeroKZeroGamma = 
            getBayesMarketMakerZeroKZeroGamma();
        
        final int repeats = 5;
        int inventory = 5;
        final int offset = 3;
        bayesMMZeroKZeroGamma.positionBalance = inventory;
        for (int i = 0; i < repeats; i++) {
            bayesMMZeroKZeroGamma.resetBelief(fundamentalMean + offset * i);
            assertEquals(
                bayesMMZeroKZeroGamma.findAskWithShadeAndInv(),
                bayesMMZeroKZeroGamma.findAskPriceNoProfit(),
                delta
            );
        }
        
        final BayesMarketMaker bayesMMZeroGamma = 
            getBayesMarketMakerZeroGamma();
        bayesMMZeroGamma.positionBalance = inventory;
        for (int i = 0; i < repeats; i++) {
            bayesMMZeroGamma.resetBelief(fundamentalMean + offset * i);
            assertEquals(
                bayesMMZeroGamma.findAskWithShadeAndInv(),
                bayesMMZeroGamma.findAskPriceNoProfit() + shadeTicks,
                delta
            );
        }  
        
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        bayesMM.positionBalance = inventory;
        for (int i = 0; i < repeats; i++) {
            bayesMM.resetBelief(fundamentalMean + offset * i);
            assertEquals(
                bayesMM.findAskWithShadeAndInv(),
                Math.round(
                    bayesMM.findAskPriceNoProfit() 
                    + shadeTicks - inventoryFactor * inventory
                ),
                delta
            );
        }  
        
        inventory = -5;
        bayesMM.positionBalance = inventory;
        for (int i = 0; i < repeats; i++) {
            bayesMM.resetBelief(fundamentalMean + offset * i);
            assertEquals(
                bayesMM.findAskWithShadeAndInv(),
                Math.round(
                    bayesMM.findAskPriceNoProfit() 
                    + shadeTicks - inventoryFactor * inventory
                ),
                delta
            );
        }   
    }
    
    // findBidPriceNoProfit:
    //     result is <= expectedValueGivenSaleOverSaleProb(result)
    //     (result + 1) is > expectedValueGivenSaleOverSaleProb(result + 1)
    //     -- because you find this bid price by iteratively reducing the bid
    //        by 1, until the inequality is satisfied
    @Test
    public void testFindBidPriceNoProfit() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        final int repeats = 5;
        final int offset = 3;
        for (int i = 0; i < repeats; i++) {
            bayesMM.resetBelief(fundamentalMean + offset * i);
            final int bidPrice = bayesMM.findBidPriceNoProfit();
            assertTrue(
                bidPrice
                <= bayesMM.expectedValueGivenSaleOverSaleProb(bidPrice)
            );
            assertTrue(
                bidPrice + 1
                > bayesMM.expectedValueGivenSaleOverSaleProb(bidPrice + 1)
            );
        }   
    }
    
    // findAskPriceNoProfit:
    //     result is >= expectedValueGivenBuyOverBuyProb(result)
    //     (result - 1) is < expectedValueGivenBuyOverBuyProb(result - 1)
    //     -- because you find this ask price by iteratively increasing the ask
    //        by 1, until the inequality is satisfied
    @Test
    public void testFindAskPriceNoProfit() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        final int repeats = 5;
        final int offset = 3;
        for (int i = 0; i < repeats; i++) {
            bayesMM.resetBelief(fundamentalMean + offset * i);
            final int askPrice = bayesMM.findAskPriceNoProfit();
            assertTrue(
                askPrice
                >= bayesMM.expectedValueGivenBuyOverBuyProb(askPrice)
            );
            assertTrue(
                askPrice - 1
                > bayesMM.expectedValueGivenSaleOverSaleProb(askPrice - 1)
            );
        }   
    }
    
    // expectedValueGivenBuyOverBuyProb:
    // result in typical conditions is:
    //     expectedValueOnBuy() / probBuy()
    @Test
    public void testExpectedValueGivenBuyOverBuyProb() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        final int repeats = 5;
        final int offset = 3;
        final int askPrice = fundamentalMean;
        for (int i = 0; i < repeats; i++) {
            bayesMM.resetBelief(fundamentalMean + offset * i);
            final double expectedValue = 
                bayesMM.expectedValueGivenBuyOverBuyProb(askPrice);
            assertEquals(
                expectedValue,
                bayesMM.expectedValueOnBuy(askPrice) / bayesMM.probBuy(askPrice),
                delta
            );
        } 
    }
    
    // expectedValueGivenSaleOverSaleProb:
    // result in typical conditions is:
    //     expectedValueOnSale() / probSell()
    @Test
    public void expectedValueGivenSaleOverSaleProb() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        final int repeats = 5;
        final int offset = 3;
        final int bidPrice = fundamentalMean;
        for (int i = 0; i < repeats; i++) {
            bayesMM.resetBelief(fundamentalMean + offset * i);
            final double expectedValue = 
                bayesMM.expectedValueGivenSaleOverSaleProb(bidPrice);
            assertEquals(
                expectedValue,
                bayesMM.expectedValueOnSale(bidPrice) / bayesMM.probSell(bidPrice),
                delta
            );
        } 
    }
    
    // expectedValueOnBuy:
    // in typical conditions:
    //     greater than 0
    //     no greater than vMax
    //     increases with decreasing askPrice
    // with all noise traders:
    //     same regardless of askPrice
    @Test
    public void testExpectedValueOnBuy() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        final int repeats = 5;
        final int offset = 3;
        int askPrice = fundamentalMean;
        double previousExpectedValue = bayesMM.expectedValueOnBuy(askPrice);
        for (int i = 0; i < repeats; i++) {
            askPrice -= offset;
            double currentExpectedValue = bayesMM.expectedValueOnBuy(askPrice);
            assertTrue(currentExpectedValue > 0);
            assertTrue(currentExpectedValue < bayesMM.vMin + bayesMM.belief.size());
            assertTrue(currentExpectedValue > previousExpectedValue);
            previousExpectedValue = currentExpectedValue;
        } 
        
        final BayesMarketMaker bayesMMAllZIMO = getBayesMarketMakerAllZIMO();
        bayesMMAllZIMO.resetBelief(fundamentalMean);
        askPrice = fundamentalMean;
        previousExpectedValue = bayesMMAllZIMO.expectedValueOnBuy(askPrice);
        for (int i = 0; i < repeats; i++) {
            askPrice -= offset;
            double currentExpectedValue = bayesMMAllZIMO.expectedValueOnBuy(askPrice);
            assertTrue(currentExpectedValue > 0);
            assertTrue(currentExpectedValue < bayesMMAllZIMO.vMin + bayesMMAllZIMO.belief.size());
            assertEquals(currentExpectedValue, previousExpectedValue, delta);
            previousExpectedValue = currentExpectedValue;
        }
    }
    
    // expectedValueOnSale: 
    // in typical conditions:
    //     greater than 0
    //     no greater than vMax
    //     increases with increasing bidPrice
    // with all noise traders:
    //     same regardless of bidPrice
    @Test
    public void testExpectedValueOnSale() {
        final BayesMarketMaker bayesMM = getBayesMarketMaker();
        final int repeats = 5;
        final int offset = 3;
        int bidPrice = fundamentalMean;
        double previousExpectedValue = bayesMM.expectedValueOnSale(bidPrice);
        for (int i = 0; i < repeats; i++) {
            bidPrice += offset;
            double currentExpectedValue = bayesMM.expectedValueOnSale(bidPrice);
            assertTrue(currentExpectedValue > 0);
            assertTrue(currentExpectedValue < bayesMM.vMin + bayesMM.belief.size());
            assertTrue(currentExpectedValue > previousExpectedValue);
            previousExpectedValue = currentExpectedValue;
        } 
        
        final BayesMarketMaker bayesMMAllZIMO = getBayesMarketMakerAllZIMO();
        bayesMMAllZIMO.resetBelief(fundamentalMean);
        bidPrice = fundamentalMean;
        previousExpectedValue = bayesMMAllZIMO.expectedValueOnSale(bidPrice);
        for (int i = 0; i < repeats; i++) {
            bidPrice += offset;
            double currentExpectedValue = bayesMMAllZIMO.expectedValueOnSale(bidPrice);
            assertTrue(currentExpectedValue > 0);
            assertTrue(currentExpectedValue < bayesMMAllZIMO.vMin + bayesMMAllZIMO.belief.size());
            assertEquals(currentExpectedValue, previousExpectedValue, delta);
            previousExpectedValue = currentExpectedValue;
        }
    }
    
    private static void setBidAndAsk(final BayesMarketMaker bayesMM) {
        final double expectedValue = expectedValue(bayesMM.belief, bayesMM.vMin);
        bayesMM.myBidPrice = (int) expectedValue - bayesMM.shadeTicks;
        bayesMM.myAskPrice = (int) expectedValue + bayesMM.shadeTicks;
    }
    
    private static boolean middleIsStrictMax(final List<Double> belief) {
        final double middleEntry = belief.get(belief.size() / 2);
        for (int i = 0; i < belief.size(); i++) {
            if (i < belief.size() / 2 - 1 || i > belief.size() / 2 + 1) {
                if (belief.get(i) >= middleEntry) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 
     * @param belief
     * @return square root of:
     * expected value of squared difference from mean
     * index in the belief vector
     */
    private static double stdDev(final List<Double> belief) {
        double meanIndex = 0.0;
        for (int i = 0; i < belief.size(); i++) {
            meanIndex += i * belief.get(i);
        }
        
        double variance = 0.0;
        for (int i = 0; i < belief.size(); i++) {
            variance += (meanIndex - i) * (meanIndex - i) * belief.get(i);
        }
        
        return Math.sqrt(variance);
    }
    
    private static double expectedValue(
        final List<Double> belief,
        final int vMin
    ) {
        double result = 0.0;
        for (int i = 0; i < belief.size(); i++) {
            result += i * belief.get(i);
        }
        
        return vMin + result;
    }
    
    private static boolean isNonNegative(final List<Double> belief) {
        for (final Double current: belief) {
            if (current < 0) {
                return false;
            }
        }
        
        return true;
    }
    
    private static double beliefSum(final List<Double> belief) {
        double result = 0.0;
        for (final Double current: belief) {
            result += current;
        }
        
        return result;
    }
    
    private BayesMarketMaker getBayesMarketMaker() {
        return new BayesMarketMaker(
            exec, fundamental, sip, market,
            new Random(rand.nextLong()), 
            bayesMMProps
        );
    }
    
    private BayesMarketMaker getBayesMarketMakerZeroKZeroGamma() {
        return new BayesMarketMaker(
            exec, fundamental, sip, market,
            new Random(rand.nextLong()), 
            bayesMMPropsZeroKZeroGamma
        );
    }
    
    private BayesMarketMaker getBayesMarketMakerZeroGamma() {
        return new BayesMarketMaker(
            exec, fundamental, sip, market,
            new Random(rand.nextLong()), 
            bayesMMPropsZeroGamma
        );
    }
    
    private BayesMarketMaker getBayesMarketMakerAllZIMO() {
        return new BayesMarketMaker(
            exec, fundamental, sip, market,
            new Random(rand.nextLong()), 
            bayesMMPropsAllZIMO
        );
    }
    
    private BayesMarketMaker getBayesMarketMakerAllFundNoNoise() {
        return new BayesMarketMaker(
            exec, fundamental, sip, market,
            new Random(rand.nextLong()), 
            bayesMMPropsAllFundNoNoise
        );
    }
    
    private BayesMarketMaker getBayesMarketMakerNoShock() {
        return new BayesMarketMaker(
            exec, fundamental, sip, market,
            new Random(rand.nextLong()), 
            bayesMMNoShock
        );
    }
}
