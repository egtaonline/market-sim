package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.INFO;


import java.io.File;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;


import java.text.DecimalFormat;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import data.Observations;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

/**
 * Based on the market making agent in:
 * 
 * Das, S. (2005). A learning market-maker in the Glosten-Milgrom model. 
 * Quantitative Finance, 5(2), 169-180.
 * 
 * @author masonwright
 *
 */
public class BayesMarketMaker extends MarketMaker {
    
    private static final long serialVersionUID = 1915370366149096714L;
    
    // probability that an arriving background trader
    // will be a FundamentalAgent, instead of a ZIMOAgent.
    // FundamentalAgent corresponds to the "informed" traders in (Das 2005)
    // referred to as \alpha in (Das 2005)
    final double probFundAgent;
    
    // standard deviation of noise for FundamentalAgent background traders.
    // referred to as \sigma_w in (Das 2005)
    private final double noiseStandardDev;
    // \sigma_w^2 in (Das 2005)
    @SuppressWarnings("unused")
    private final double noiseVar;
    
    // initial fundamental value
    // referred to as V_0 in (Das 2005)
    private final double fundamentalMean;
    
    // variance of shocks in fundamental value
    // referred to as \sigma^2 in (Das 2005)
    private final double shockVar;
    // \sigma in (Das 2005)
    private final double shockStdev;
    
    // number of ticks above and below expected value to shade order prices
    // referred to as k in (Das 2005)
    final int shadeTicks;
    
    // factor used for inventory control
    // referred to as \gamma in (Das 2005)
    private final double inventoryFactor;
    
    // probability distribution over current FundamentalValue.
    // first entry is Pr(FundamentalValue == vMin).
    // next entry is Pr(FundamentalValue == vMin + 1), in ticks.
    // and so on through the last entry.
    // that is, covers a range of:
    // {vMin, vMin + 1, . . ., vMin + belief.size() - 1}.
    final List<Double> belief;
    
    // fundamental value corresponding to the first entry in the
    // "belief" probability vector. that is, belief.get(0) returns
    // the probability that the fundamental value equals vMin.
    int vMin;
    
    // this market maker's bid price from the previous time step
    int myBidPrice;
    
    // this market maker's ask price from the previous time step
    int myAskPrice;
    
    private static final double delta = 0.0001;
    
    // don't "truncate" the order ladder to avoid crossing existing prices
    private static final boolean truncateLadder = false;
    
    // only post orders to buy 1 unit and sell 1 unit
    private static final int numRungs = 1;
    
    private static final boolean debugging = false;

    // NB: inventory is stored in the "positionBalance" member variable
    // of the superclass Agent.
    
    // only for testing of line search efficiency
    // private static final List<Integer> stepsList = new ArrayList<Integer>();
    
    
    // only for testing
    private final List<Integer> bidsList = new ArrayList<Integer>();
    
    // only for testing
    private final List<Integer> asksList = new ArrayList<Integer>();
    
    // only for testing
    private final List<Integer> fundamentalsList = new ArrayList<Integer>();
    
    // only for testing
    private final List<List<Double>> beliefsList = new ArrayList<List<Double>>();
    
    // only for testing. used to account for changes in vMin,
    // which plotting must use to plot belief vectors consistently.
    private final List<Integer> beliefsListOffsets = new ArrayList<Integer>();
    
    // only for testing. used to track offsets to vMin since the start of a run,
    // for plotting beliefsList.
    private int originalVMin;
    
    
    public BayesMarketMaker(
        final Scheduler scheduler, 
        final FundamentalValue fundamental,
        final SIP sip, 
        final Market market, 
        final Random rand, 
        final double reentryRate,
        final int tickSize, 
        final int rungSize,
        final boolean tickImprovement, 
        final boolean tickOutside, 
        final int initLadderMean,
        final int initLadderRange,
        final double aProbFundAgent,
        final double aNoiseStandardDev,
        final double aFundamentalMean,
        final double aShockVar,
        final int aShadeTicks,
        final double aInventoryFactor
    ) {
        super(scheduler, fundamental, sip, market, rand, reentryRate, tickSize, numRungs,
            rungSize, truncateLadder, tickImprovement, tickOutside, initLadderMean,
            initLadderRange);

        if (aProbFundAgent < 0 || aProbFundAgent > 1) {
            throw new IllegalArgumentException();
        }
        this.probFundAgent = aProbFundAgent;
        
        if (aNoiseStandardDev < 0) {
            throw new IllegalArgumentException();
        }
        this.noiseStandardDev = aNoiseStandardDev;
        this.noiseVar = this.noiseStandardDev * this.noiseStandardDev;
        
        if (aFundamentalMean < 0) {
            throw new IllegalArgumentException();
        }
        this.fundamentalMean = aFundamentalMean;
        
        if (aShockVar < 0) {
            throw new IllegalArgumentException();
        }
        this.shockVar = aShockVar;
        this.shockStdev = Math.sqrt(this.shockVar);
        
        if (aShadeTicks < 0) {
            throw new IllegalArgumentException();
        }
        this.shadeTicks = aShadeTicks;
        
        if (aInventoryFactor < 0) {
            throw new IllegalArgumentException();
        }
        this.inventoryFactor = aInventoryFactor;
        
        this.belief = new ArrayList<Double>();
        initBelief();
    }
    
    public BayesMarketMaker(
        final Scheduler scheduler, 
        final FundamentalValue fundamental,
        final SIP sip, 
        final Market market, 
        final Random rand, 
        final EntityProperties props
    ) {
        this(
            scheduler, 
            fundamental, 
            sip, 
            market, 
            rand,
            props.getAsDouble(Keys.MARKETMAKER_REENTRY_RATE, Keys.REENTRY_RATE),
            props.getAsInt(Keys.AGENT_TICK_SIZE, Keys.TICK_SIZE),
            props.getAsInt(Keys.RUNG_SIZE), 
            props.getAsBoolean(Keys.TICK_IMPROVEMENT), 
            props.getAsBoolean(Keys.TICK_OUTSIDE),
            props.getAsInt(Keys.INITIAL_LADDER_MEAN, Keys.FUNDAMENTAL_MEAN),
            props.getAsInt(Keys.INITIAL_LADDER_RANGE),
            props.getAsDouble(Keys.PROB_FUND_AGENT),
            props.getAsDouble(Keys.NOISE_STDEV),
            props.getAsDouble(Keys.FUNDAMENTAL_MEAN),
            props.getAsDouble(Keys.FUNDAMENTAL_SHOCK_VAR),
            (int) props.getAsDouble(Keys.BMM_SHADE_TICKS),
            props.getAsDouble(Keys.BMM_INVENTORY_FACTOR)
        );
    }
    
    /**
     * The fundamental value v must be within the range of
     * values represented by "belief". This range is:
     * {vMin, vMin + 1, . . ., vMin + belief.size() - 1}.
     * 
     * @param v a fundamental value
     * @return the probability that the true fundamental value
     * is equal to the given value
     */
    @SuppressWarnings("unused")
    private double getBeliefAt(final double v) {
        final int intV = (int) v;
        
        // else too low for belief vector range
        assert intV >= vMin;
        
        final int index = intV - vMin;
        // else too high for belief vector range
        assert index <= belief.size() - 1;
        return belief.get(index);
    }
    
    /**
     * Initialize the belief probability distribution. Assumes that the
     * initial fundamental value is known to be at fundamentalMean.
     * 
     * Beliefs are stored as a vector of probabilities, for discrete fundamental
     * values in {fundamentalMean - 4 \sigma, fundamentalMean - 4 \sigma + 1, . . ., 
     * fundamentalMean + 4 \sigma}.
     * 
     * The probability of each element in the vector is initialized as:
     * 1 if v = fundamentalMean
     * 0 otherwise.
     */
    void initBelief() {
        belief.clear();
        
        // 4 is value in (Das 2005)
        final double sigmaRange = 4.0;
        
        // minimum fundamental value belief is greater of (0, V_t - sigmaRange * \sigma)
        vMin = Math.max(0, (int) (fundamentalMean - sigmaRange * shockStdev));
        // maximum fundamental value belief is (V_t + sigmaRange * \sigma)
        final int vMax = (int) (fundamentalMean + sigmaRange * shockStdev); 
        
        for (int v = vMin; v <= vMax; v++) {
            if (v == (int) fundamentalMean) {
                // you know the initial value is fundamentalMean
                belief.add(1.0);
            } else {
                // any other value has probability 0 initially
                belief.add(0.0);
            }
        }
    }
    
    /**
     * Reset the belief probability distribution after a jump in fundamental value,
     * about an expected value of "current," the previous expected value.
     * 
     * Beliefs are stored as a vector of probabilities, for discrete fundamental
     * values in {current - 4 \sigma, current - 4 \sigma + 1, . . ., 
     * current + 4 \sigma}.
     * 
     * The probability of each element v in the vector is initialized as:
     * The integral of the pdf ~ N(current, \sigma), over [v, v + 1].
     * 
     * The result will be like a discretized normal curve, with expected
     * value equal to current.
     */
    void resetBelief(final double current) {
        assert !belief.isEmpty();
        belief.clear();
                
        // 4 is value in (Das 2005)
        final double sigmaRange = 4.0;
        
        // minimum fundamental value belief is greater of (0, V_t - sigmaRange * \sigma)
        vMin = Math.max(0, (int) (current - sigmaRange * shockStdev));
        // maximum fundamental value belief is (V_t + sigmaRange * \sigma)
        final int vMax = (int) (current + sigmaRange * shockStdev); 
        
        if (Math.abs(shockStdev) < delta) {
            // can't create a NormalDistribution object with 0 standard deviation.
            for (int v = vMin; v <= vMax; v++) {
                if (v == (int) current) {
                    // you know the initial value is current
                    belief.add(1.0);
                } else {
                    // any other value has probability 0
                    belief.add(0.0);
                }
            }
            
            return;
        }
        
        final NormalDistribution norm = 
            new NormalDistribution(
                current, // mean
                shockStdev // standard deviation
            );
        for (int v = vMin; v <= vMax; v++) {
            final double probV = norm.cumulativeProbability(v, v + 1);
            belief.add(probV);
        }
        
        normalizeBelief();
    }
    
    /**
     * Reset the belief as a discretized Gaussian pdf about the previous expected value,
     * because a Gaussian jump occurred from the previous value.
     * 
     * Usage:
     * Call this when there is a jump in fundamental value.
     * Calls to jumpOccurred(), sellOccurred(), buyOccurred(), and noTradeOccurred()
     * should be made in the order of the events that cause them.
     */
    public void jumpOccurred() {
        assert !belief.isEmpty();
        if (debugging) {
            System.out.println("jump occurred");
        }
        resetBelief(expectedValue());
    }
    
    /**
     * Update the belief, given that a background trader sold 1 share to this
     * market maker in the previous time step.
     * 
     * Use Bayes' rule:
     * 
     * Pr(V | SELL) = Pr(SELL | V) Pr(V) / Pr(SELL)
     * where Pr(V | SELL) is the updated belief vector, and Pr(V) is the prior belief 
     * vector.
     */
    // call this when there a background trader sells 1 share to this BayesMarketMaker.
    // 
    // calls to jumpOccurred(), sellOccurred(), buyOccurred(), and noTradeOccurred()
    // should be made in the order of the events that cause them.
    public void sellOccurred() {
        assert !belief.isEmpty();
        final List<Double> newBelief = new ArrayList<Double>();
        
        if (debugging) {
            System.out.println("sell occurred");
        }
                
        final double probSell = probSell(myBidPrice);
        for (int i = 0; i < belief.size(); i++) {
            final int currentValue = vMin + i;
            final double probCurrentValue = belief.get(i);
            final double probSellGivenValue = 
                probSellGivenV(currentValue, myBidPrice);
            final double probVGivenSell = 
                probSellGivenValue * probCurrentValue / probSell;
            newBelief.add(probVGivenSell);
        }
        
        belief.clear();
        belief.addAll(newBelief);
        normalizeBelief();
    }
    
    /**
     * Update the belief, given that a background trader bought 1 share from this
     * market maker in the previous time step.
     * 
     * Use Bayes' rule:
     * 
     * Pr(V | BUY) = Pr(BUY | V) Pr(V) / Pr(BUY)
     * where Pr(V | BUY) is the updated belief vector, and Pr(V) is the prior belief 
     * vector.
     */
    // call this when there a background trader buys 1 share from this BayesMarketMaker.
    // 
    // calls to jumpOccurred(), sellOccurred(), buyOccurred(), and noTradeOccurred()
    // should be made in the order of the events that cause them.
    public void buyOccurred() {
        assert !belief.isEmpty();
        final List<Double> newBelief = new ArrayList<Double>();
        
        if (debugging) {
            System.out.println("buy occurred");
        }
                
        final double probBuy = probBuy(myAskPrice);
        for (int i = 0; i < belief.size(); i++) {
            final int currentValue = vMin + i;
            final double probCurrentValue = belief.get(i);
            final double probBuyGivenValue = 
                probBuyGivenV(currentValue, myAskPrice);
            final double probVGivenBuy = 
                probBuyGivenValue * probCurrentValue / probBuy;
            newBelief.add(probVGivenBuy);
        }
        
        belief.clear();
        belief.addAll(newBelief);
        normalizeBelief();
    }
    
    /**
     * Pr(SELL) = \sum_{v = vMin}^{vMax} Pr(V) Pr(SELL | V)
     * where vMax = vMin + belief.size() - 1, and Pr(V) is the present belief vector.
     * 
     * @param bidPrice the market maker's bid price in the given time step
     * 
     * @return the probability that a background trader will sell 1 share
     * to this market maker in a time step, given "belief" vector
     */
    double probSell(
        final int bidPrice
    ) {
        assert bidPrice != 0;
        assert !belief.isEmpty();
        double result = 0.0;
        for (int i = 0; i < belief.size(); i++) {
            final int currentValue = vMin + i;
            final double probCurrentValue = belief.get(i);
            final double probSellGivenValue = 
                probSellGivenV(currentValue, bidPrice);
            result += probCurrentValue * probSellGivenValue;
        }
        
        return result;
    }
    
    /**
     * Pr(BUY) = \sum_{v = vMin}^{vMax} Pr(V) Pr(BUY | V)
     * where vMax = vMin + belief.size() - 1, and Pr(V) is the present belief vector.
     * 
     * @param askPrice the market maker's ask price in the given time step
     * 
     * @return the probability that a background trader will buy 1 share
     * from this market maker in a time step, given "belief" vector
     */
    double probBuy(
        final int askPrice
    ) {
        assert !belief.isEmpty();
        assert askPrice != 0;
        double result = 0.0;
        for (int i = 0; i < belief.size(); i++) {
            final int currentValue = vMin + i;
            final double probCurrentValue = belief.get(i);
            final double probBuyGivenValue = 
                probBuyGivenV(currentValue, askPrice);
            result += probCurrentValue * probBuyGivenValue;
        }
        
        return result;
    }
    
    /**
     * Pr(SELL | V) = 
     *     (1 - probFundAgent) * 0.5 
     *     + probFundAgent * Pr(N~(0, noiseStandardDev) < bidPrice - v)
     *     -- because ZIMOAgents sell half the time, while FundamentalAgents sell
     *         only if they observe an apparent value less than the bidPrice.
     *     -- the probability that an FundamentalAgent that arrives observes an
     *         apparent value greater than askPrice is the probability of a draw
     *         from the noise distribution greater than (askPrice - v).
     *     
     * @param v the assumed fundamental value
     * @param askPrice the market maker's ask price in the given time step
     * @return the probability that a background trader will buy 1 share from this market maker 
     * in a certain time step, given that the fundamental equals v.
     */
    double probSellGivenV(
        final int v,
        final int bidPrice
    ) {
        assert !belief.isEmpty();
        assert bidPrice != 0;        
        if (Math.abs(noiseStandardDev) < delta) {
            // can't create a NormalDistribution object with 0 standard deviation.
            if (bidPrice < v) {
                // FundamentalAgent never sells
                return (1 - probFundAgent) * 0.5;
            }
            
            // FundamentalAgent always sells
            return (1 - probFundAgent) * 0.5 + probFundAgent;
        }
        
        final NormalDistribution noiseDist = 
            new NormalDistribution(
                0, // mean of noise
                noiseStandardDev // standard deviation of noise
            );
        
        // test if N is <= (bidPrice - v)
        final double probNoiseLowEnough = 
            noiseDist.cumulativeProbability(bidPrice - v);

        return (1 - probFundAgent) * 0.5 + probFundAgent * probNoiseLowEnough;
    }
    
    /**
     * Pr(BUY | V) = 
     *     (1 - probFundAgent) * 0.5 
     *     + probFundAgent * Pr(N~(0, noiseStandardDev) > askPrice - v)
     *     -- because ZIMOAgents buy half the time, while FundamentalAgents buy
     *         only if they observe an apparent value greater than the askPrice.
     *     -- the probability that an FundamentalAgent that arrives observes an
     *         apparent value greater than askPrice is the probability of a draw
     *         from the noise distribution greater than (askPrice - v).
     *     
     * @param v the assumed fundamental value
     * @param askPrice the market maker's ask price in the given time step
     * @return the probability that a background trader will buy 1 share from this market maker 
     * in a certain time step, given that the fundamental equals v.
     */
    double probBuyGivenV(
        final int v,
        final int askPrice
    ) {
        assert !belief.isEmpty();
        assert askPrice != 0;
        
        if (Math.abs(noiseStandardDev) < delta) {
            // can't create a NormalDistribution object with 0 standard deviation.
            if (askPrice > v) {
                // FundamentalAgent never buys
                return (1 - probFundAgent) * 0.5;
            }
            
            // FundamentalAgent always buys
            return (1 - probFundAgent) * 0.5 + probFundAgent;
        }
        
        final NormalDistribution noiseDist = 
            new NormalDistribution(
                0, // mean of noise
                noiseStandardDev // standard deviation of noise
            );
        
        // instead of testing if N is > (askPrice - v),
        // test if N is <= (v - askPrice), which is equivalent.
        final double probNoiseHighEnough = 
            noiseDist.cumulativeProbability(v - askPrice);

        return (1 - probFundAgent) * 0.5 + probFundAgent * probNoiseHighEnough;
    }
    
    /**
     * Update the belief, given that no trade occurred in the previous time step.
     * 
     * Use Bayes' rule:
     * Pr(V | NT) = Pr(NT | V) Pr(V) / Pr(NT)
     * where Pr(V | NT) is the updated belief vector, and Pr(V) the prior belief vector.
     */
    // call this when in the current time step, no background trader bought or
    // sold a share from (to) this BayesMarketMaker.
    // 
    // calls to jumpOccurred(), sellOccurred(), buyOccurred(), and noTradeOccurred()
    // should be made in the order of the events that cause them.
    public void noTradeOccurred() {
        assert !belief.isEmpty();
        final List<Double> newBelief = new ArrayList<Double>();
        
        if (debugging) {
            System.out.println("no trade occurred");
        }
                
        // handle case where this is called before the market maker has set
        // myBidPrice and myAskPrice, so they have default values of 0
        if (myBidPrice == 0 && myAskPrice == 0) {
            // no orders have yet been placed by this market maker,
            // so we don't get any new information from no trade occurring.
            // take no action.
            return;
        }
        
        final double probNoTrade = probNoTrade(myBidPrice, myAskPrice);
        for (int i = 0; i < belief.size(); i++) {
            final int currentValue = vMin + i;
            final double probCurrentValue = belief.get(i);
            final double probNoTradeGivenValue = 
                probNoTradeGivenV(currentValue, myBidPrice, myAskPrice);
            final double probVGivenNoTrade = 
                probNoTradeGivenValue * probCurrentValue / probNoTrade;
            newBelief.add(probVGivenNoTrade);
        }
        
        belief.clear();
        belief.addAll(newBelief);
        normalizeBelief();
    }
    
    /**
     * Pr(NT) = \sum_{v = vMin}^{v = vMax} Pr(V) * Pr(NT | V)
     * where vMax = vMin + belief.size() - 1, and Pr(V) is the present belief vector.
     * 
     * @param bidPrice the market maker's bid price in the given time step
     * @param askPrice the market maker's ask price in the given time step
     * 
     * @return the probability that no trade will occur in a time step,
     * given "belief" vector
     */
    double probNoTrade(
        final int bidPrice,
        final int askPrice
    ) {
        assert !belief.isEmpty();
        assert bidPrice != 0 && askPrice != 0;
        assert bidPrice <= askPrice;

        double result = 0.0;
        for (int i = 0; i < belief.size(); i++) {
            final int currentValue = vMin + i;
            final double probCurrentValue = belief.get(i);
            final double probNTGivenValue = 
                probNoTradeGivenV(currentValue, bidPrice, askPrice);
            result += probCurrentValue * probNTGivenValue;
        }
        
        return result;
    }
    
    /**
     * Pr(NT | V) = probFundAgent * Pr(N~(0, noiseStandardDev) in (bidPrice - v, askPrice - v))
     * -- because ZIMOAgents always trade, while FundamentalAgents trade only if they
     *        observe a good deal.
     * -- the probability that a FundamentalAgent that arrives does not observe a good deal
     *        is the probability its noise term is too high to make buying seem good,
     *        but too low to make selling seem good.
     * 
     * @param v the assumed fundamental value
     * @param bidPrice the market maker's bid price in the given time step
     * @param askPrice the market maker's ask price in the given time step
     * @return the probability that no trade will occur in a certain time step,
     * given that the fundamental equals v.
     */
    double probNoTradeGivenV(
        final int v,
        final int bidPrice,
        final int askPrice
    ) {
        assert !belief.isEmpty();
        assert bidPrice != 0 && askPrice != 0;
        assert bidPrice <= askPrice;

        if (Math.abs(noiseStandardDev) < delta) {
            // can't create a NormalDistribution object with 0 standard deviation.
            if (bidPrice < v && askPrice > v) {
                // FundamentalAgent never trades
                return probFundAgent;
            }
            
            // FundamentalAgent always trades
            return 0.0;
        }
        
        final NormalDistribution noiseDist = 
            new NormalDistribution(
                0, // mean of noise
                noiseStandardDev // standard deviation of noise
            );
        final double probNoiseInRange = 
            noiseDist.cumulativeProbability(bidPrice - v, askPrice - v);

        return probFundAgent * probNoiseInRange;
    }
    
    /**
     * @return the expected value of the fundamental, based on probabilities in
     * the "belief" vector, starting from vMin.
     */
    double expectedValue() {
        assert !belief.isEmpty();
        // meanIndex will hold the expected value of the offset from
        // index 0 in the belief vector.
        double meanIndex = 0.0;
        for (int i = 0; i < belief.size(); i++) {
            meanIndex += belief.get(i) * i;
        }
        
        // add the value of the first index (vMin) to the expected offset
        // from that index
        return vMin + meanIndex;
    }
    
    /**
     * If the vector "belief" does not sum to 1,
     * within a tolerance of "tolerance,"
     * divide each element by the sum, so the sum will
     * equal 1.
     */
    void normalizeBelief() {
        assert !belief.isEmpty();
        final double tolerance = 0.001;
        
        double total = 0.0;
        for (final double current: belief) {
            assert current >= 0;
            total += current;
        }
        
        if (Math.abs(total - 1) < tolerance) {
            // already normalized
            return;
        }
        
        for (int i = 0; i < belief.size(); i++) {
            belief.set(i, belief.get(i) / total);
        }
    }
    
    /**
     * bid = no-profit bid - k - \gamma I
     * 
     * @return no-expected-profit bid, minus the shading parameter, minus
     * the current inventory times the inventory control parameter.
     * round to nearest whole number tick.
     * must not be negative.
     */
    int findBidWithShadeAndInv() {
        assert !belief.isEmpty();
        final int noProfitBid = findBidPriceNoProfitClosestValue();
        int result = (int) Math.round(noProfitBid - shadeTicks - inventoryFactor * positionBalance);
        if (result < 0) {
            result = 0;
        }
        return result;
    }
    
    /**
     * ask = no-profit ask + k - \gamma I
     * 
     * @return no-expected-profit ask, plus the shading parameter, minus
     * the current inventory times the inventory control parameter.
     * round to nearest whole number tick.
     * must not be negative.
     */
    int findAskWithShadeAndInv() {
        assert !belief.isEmpty();
        final int noProfitAsk = findAskPriceNoProfitClosestValue();
        int result = (int) Math.round(noProfitAsk + shadeTicks - inventoryFactor * positionBalance);
        if (result < 0) {
            result = 0;
        }
        return result;
    }
    
    /*
     * NB: This implementation returns the integer bid price that
     * minimizes the absolute value of the difference between 
     * the bid and the expected value of the fundamental in case of 
     * a sale by a background trader.
     * 
     * The returned value may be greater than the conditional expected value of
     * the fundamental.
     */
    int findBidPriceNoProfitClosestValue() {
        assert !belief.isEmpty();
        // start from a slightly higher bid, before reducing the bid
        int currentBid = (int) Math.ceil(expectedValue());
        double currentExpValue = expectedValueGivenSaleOverSaleProb(currentBid);
        
        // difference between current bid and conditional expected value
        double currentDifference = currentExpValue - currentBid;
        double previousDifference = currentDifference;
        while (currentBid > currentExpValue) {
            currentBid--;
        
            // update expected value and difference
            currentExpValue = expectedValueGivenSaleOverSaleProb(currentBid);
            previousDifference = currentDifference;
            currentDifference = currentExpValue - currentBid;
            if (currentBid < 0) {
                // bid should never be negative
                throw new IllegalStateException();
            }
        }

        assert currentBid >= 0;
        final double tolerance = 0.000001;
        if (
            Math.abs(previousDifference) + tolerance
                < Math.abs(currentDifference)
        ) {
            // previous bid produced smaller difference absolute value, from the
            // conditional expected value. thus, return previous bid.
            // use tolerance because values are floating point
            return currentBid + 1;
        }
        
        return currentBid;
    }
    
    /*
     * NB: This implementation returns the integer ask price that
     * minimizes the absolute value of the difference between
     * the ask and the expected value of the fundamental in case of
     * a buy by a background trader.
     * 
     * The returned value may be less than the conditional expected value of
     * the fundamental.
     */
    int findAskPriceNoProfitClosestValue() {
        assert !belief.isEmpty();
        int currentAsk = (int) expectedValue();
        double currentExpValue = expectedValueGivenBuyOverBuyProb(currentAsk);
        
        // difference between current ask and conditional expected value
        double currentDifference = currentExpValue - currentAsk;
        double previousDifference = currentDifference;
        while (currentAsk < currentExpValue) {
            currentAsk++;
        
            // update expected value and difference
            currentExpValue = expectedValueGivenBuyOverBuyProb(currentAsk);
            previousDifference = currentDifference;
            currentDifference = currentExpValue - currentAsk;
            if (currentAsk > vMin + belief.size()) {
                // ask should likely never exceed vMax, the maximum
                // fundamental value in the belief vector
                throw new IllegalStateException();
            }
        }

        assert currentAsk >= 0;
        final double tolerance = 0.000001;
        if (
            Math.abs(previousDifference) + tolerance
                < Math.abs(currentDifference)
        ) {
            // previous ask produced smaller difference absolute value, from the
            // conditional expected value. thus, return previous ask.
            // use tolerance because values are floating point
            return currentAsk - 1;
        }
        
        return currentAsk;
    }
    
    /*
     * NB: This implementation returns the highest integer bid value
     * that is no greater than the expected value of the fundamental
     * in case of a sale by a background trader.
     *
     * In the (Das 2004) paper, they may take the bid value that
     * minimizes the absolute value of the difference between
     * the bid and the expected value of the fundamental in case of
     * a sale by a background trader.
     *
     * That is: the implementation below rounds bids down, in favor
     * of the market maker, while the (Das 2004) implementation
     * rounds bids so as to minimize difference from expected value.
     */
    int findBidPriceNoProfit() {
        assert !belief.isEmpty();
        // start from a slightly higher bid, before reducing the bid
        int currentBid = (int) Math.ceil(expectedValue());
        @SuppressWarnings("unused")
        int steps = 0;
        while (currentBid > expectedValueGivenSaleOverSaleProb(currentBid)) {
            currentBid--;
            steps++;
        
            if (currentBid < 0) {
                // bid should never be negative
                throw new IllegalStateException();
            }
        }

        assert currentBid >= 0;
        if (debugging) {
            System.out.println("bid steps: " + steps);
        }
        
        // for testing line search effiency
        // stepsList.add(steps);
        
        return currentBid;
    }
    
    /*
     * NB: This implementation returns the lowest integer ask value
     * that is no less than the expected value of the fundamental
     * in case of a buy by a background trader.
     *
     * In the (Das 2004) paper, they may take the ask value that
     * minimizes the absolute value of the difference between
     * the ask and the expected value of the fundamental in case of
     * a buy by a background trader.
     *
     * That is: the implementation below rounds asks up, in favor
     * of the market maker, while the (Das 2004) implementation
     * rounds asks so as to minimize difference from expected value.
     */
    int findAskPriceNoProfit() {
        assert !belief.isEmpty();
        int currentAsk = (int) expectedValue();
        @SuppressWarnings("unused")
        int steps = 0;
        while (currentAsk < expectedValueGivenBuyOverBuyProb(currentAsk)) {
            currentAsk++;
            steps++;
            
            if (currentAsk > vMin + belief.size()) {
                // bid should likely never exceed vMax, the maximum
                // fundamental value in the belief vector
                throw new IllegalStateException();
            }
        }

        assert currentAsk >= 0;
        if (debugging) {
            System.out.println("ask steps: " + steps);
        }
        
        // for testing line search effiency
        // stepsList.add(steps);
        
        return currentAsk;
    }
    
    /**
     * If market maker's ask price, P_a, is set such that
     * P_a = E(V | BUY), then the input value, P_a or askPrice,
     * should approximately equal the result,
     * which is E(V_{onBuy}) / Pr(BUY),
     * which is the same as E(V | BUY).
     *
     * @param askPrice proposed ask price
     * @return E(fundamental value in case background trader buys)
     *     / Pr(background trader buys from this market maker)
     */
    double expectedValueGivenBuyOverBuyProb(final int askPrice) {
        assert !belief.isEmpty();
        assert askPrice != 0;

        final double expectedValueOnBuy = expectedValueOnBuy(askPrice);
        final double probBuy = probBuy(askPrice);
        return expectedValueOnBuy / probBuy;
    }
    
    /**
     * If market maker's bid price, P_b, is set such that
     * P_b = E(V | SELL), then the input value, P_b or bidPrice,
     * should approximately equal the result,
     * which is E(V_{onSale}) / Pr(SELL),
     * which is the same as E(V | SELL).
     * 
     * @param bidPrice proposed bid price
     * @return E(fundamental value in case background trader sells)
     *     / Pr(background trader sells to this market maker)
     */
    double expectedValueGivenSaleOverSaleProb(final int bidPrice) {
        assert !belief.isEmpty();
        assert bidPrice != 0;

        final double expectedValueOnSale = expectedValueOnSale(bidPrice);
        final double probSell = probSell(bidPrice);
        return expectedValueOnSale / probSell;
    }
    
    /**
     * Return sum_V: V * Pr(V) * Pr(BUY | V)
     * 
     * @param askPrice proposed ask price
     * @return E(fundamental value in case background trader buys)
     */
    double expectedValueOnBuy(final int askPrice) {
        assert !belief.isEmpty();
        assert askPrice != 0;

        double result = 0;
        for (int i = 0; i < belief.size(); i++) {
            final int currentValue = vMin + i;
            result += 
                currentValue * belief.get(i) * probBuyGivenV(currentValue, askPrice);
        }

        return result;
    }
    
    /**
     * Return sum_V: V * Pr(V) * Pr(SELL | V)
     * 
     * @param bidPrice proposed bid price
     * @return E(fundamental value in case background trader sells)
     */
    double expectedValueOnSale(final int bidPrice) {
        assert !belief.isEmpty();
        assert bidPrice != 0;

        double result = 0;
        for (int i = 0; i < belief.size(); i++) {
            final int currentValue = vMin + i;
            result += 
                currentValue * belief.get(i) * probSellGivenV(currentValue, bidPrice);
        }

        return result;
    }
    
    @Override
    public void agentStrategy(final TimeStamp currentTime) {
        super.agentStrategy(currentTime);
        
        assert !this.belief.isEmpty();
        
        final int newBid = findBidWithShadeAndInv();
        final int newAsk = findAskWithShadeAndInv();
        
        log.log(INFO, "%s in %s: Withdraw all orders.", this, primaryMarket);
        withdrawAllOrders();
        
        log.log(INFO, "%s in %s: Spread of %s, bid is %s, ask is %s", 
            this, primaryMarket, 
            new Price(newAsk - newBid), new Price(newBid), new Price(newAsk));
        
        this.createOrderLadder(new Price(newBid), new Price(newAsk));
        
        this.myBidPrice = newBid;
        this.myAskPrice = newAsk;
        
        /*
        System.out.println(
            "MM: v: " + fundamental.getValueAt(currentTime).intValue()
            + ", bid: " + newBid
            + ", ask: " + newAsk
            + '\n'
        );
        */
        
        boolean isWithinSpread = (
            fundamental.getValueAt(currentTime).doubleValue() >= newBid 
            && fundamental.getValueAt(currentTime).doubleValue() <= newAsk
        );
        Observations.BUS.post(new Observations.BMMStatistic(newAsk - newBid, isWithinSpread));
        
        // for testing line search efficiency
        /*
        final int enough = 1000;
        if (stepsList.size() == enough) {
            for (Integer value: stepsList) {
                System.out.println(value);
            }
        }
        */
        
        
        
        // for testing
        updateLists(
            newBid,
            newAsk,
            fundamental.getValueAt(currentTime).intValue()
        );
        
        updateBeliefsList();
        
        // for testing
        final int enough = 8000;
        if (bidsList.size() == enough) {
            printListsToFile(enough);
        }
        
        final int enoughForBeliefs = 200;
        if (beliefsList.size() == enoughForBeliefs) {
            printListsToFile(enoughForBeliefs);
            printBeliefsListToFile();
        }
        
    }
    
    
    private void updateBeliefsList() {
        if (beliefsList.isEmpty()) {
            originalVMin = vMin;
        }
        
        final List<Double> current = new ArrayList<Double>();
        for (final Double item: belief) {
            current.add(Math.log(item));
        }
        beliefsList.add(current);
        
        beliefsListOffsets.add(vMin - originalVMin);
    }
    
    
    /*
     * beliefsList should hold a list of the belief vector
     * from every time step up until now.
     * 
     * This will be printed as comma-separated lines.
     * Each line will hold one entry of the belief vector,
     * over all time steps.
     * This requires iterating over the outer list in the inner
     * loop, and vice versa.
     */
    
    private String getBeliefsListString() {
        assert !this.beliefsList.isEmpty();
        
        final StringBuilder builder = new StringBuilder();
        final DecimalFormat f = new DecimalFormat("0.######");
        final int timeSteps = beliefsList.size();
        final int fundEntries = beliefsList.get(0).size();
        for (int i = 0; i < fundEntries; i++) {
            for (int step = 0; step < timeSteps; step++) {
                builder.append(f.format(beliefsList.get(step).get(i)));
                
                if (step < timeSteps) {
                    builder.append(',');
                }
            }
            
            builder.append('\n');
        }
        
        return builder.toString();
    }
    
    private void printBeliefsListToFile() {
        final String beliefsFile = "beliefs200.csv";
        final String beliefOffsetsFile = "beliefOffsets200.txt";
        
        try {
            setContents(beliefsFile, getBeliefsListString());
            setContents(beliefOffsetsFile, getString(beliefsListOffsets));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }
    
    private void updateLists(
        final int newBid,
        final int newAsk,
        final int fundamental
    ) {
        bidsList.add(newBid);
        asksList.add(newAsk);
        fundamentalsList.add(fundamental);
    }
    
    private void printListsToFile(final int length) {
        final String bidsFile = "bids" + length + ".txt";
        final String asksFile = "asks" + length + ".txt";
        final String fundamentalsFile = "funds" + length + ".txt";
        try {
            setContents(bidsFile, getString(bidsList));
            setContents(asksFile, getString(asksList));
            setContents(fundamentalsFile, getString(fundamentalsList));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }
    
    private static String getString(final List<Integer> list) {
        final StringBuilder sb = new StringBuilder();
        for (Integer current: list) {
            sb.append(current).append('\n');
        }
        return sb.toString();
    }
    
    // only for testing
    static public void setContents(
        final String aFileName,
        final String aContents
    ) throws FileNotFoundException, IOException {
        final File myFile = new File(aFileName);
        myFile.createNewFile();
        if (!myFile.exists()) {
            throw new FileNotFoundException ("File does not exist: " + myFile);
        }
        if (!myFile.isFile()) {
            throw new IllegalArgumentException("Should not be a directory: " + myFile);
        }
        if (!myFile.canWrite()) {
            throw new IllegalArgumentException("File cannot be written: " + myFile);
        }
        
        //use buffering
        Writer output = new BufferedWriter(new FileWriter(myFile));
        try {
        //FileWriter always assumes default encoding is OK!
            output.write( aContents );
        }
        finally {
            output.close();
        }
    }
    
}
