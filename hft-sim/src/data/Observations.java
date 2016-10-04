package data;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static logger.Log.log;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import logger.Log;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import sumstats.SumStats;
import systemmanager.Consts;
import systemmanager.Consts.DiscountFactor;
import systemmanager.Keys;
import systemmanager.SimulationSpec;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import entity.agent.Agent;
import entity.agent.BackgroundAgent;
import entity.agent.BayesMarketMaker;
import entity.agent.HFTAgent;
import entity.agent.MarketMaker;
import entity.agent.MaxEfficiencyAgent;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * This class represents the summary of statistics after a run of the
 * simulation. The majority of the statistics that it collects are processed via
 * the EventBus, which is a message passing interface that can handle any style
 * of object. The rest, which mainly includes agent and player payoffs, is
 * processed when the call to getFeatures or getPlayerObservations is made.
 * 
 * Because this uses message passing, if you want the observation data structure
 * to get data, you must make sure to "register" it with the EventBus by calling
 * BUS.register(observations).
 * 
 * A single observation doesn't have that ability to save its results. Instead
 * you need to add it to a MultiSimulationObservation and use that class to
 * actually do the output.
 * 
 * To add statistics to the Observation:
 * <ol>
 * <li>Add the appropriate data structures to the object to record the
 * information you're interested in.</li>
 * <li>Create a listener method (located at the bottom, to tell what objects to
 * handle, and what to do with them.</li>
 * <li>Modify get features to take the aggregate data you stored, and turn it
 * into a String, double pair.</li>
 * <li>Wherever the relevant data is added simply add a line like
 * "Observations.BUS.post(dataObj);" with your appropriate data</li>
 * </ol>
 * 
 * @author erik
 * 
 */
public class Observations {
	
	// static event bus to record statics messages during simulation
	public static final EventBus BUS = new EventBus();
	
	// Statistics objects filled during execution
	protected final SumStats executionTimes;
	protected final SumStats prices;
	protected final TimeSeries transPrices;
	protected final TimeSeries nbboSpreads;
	protected final Multiset<Class<? extends Agent>> numTrans;
	protected final Map<Market, TimeSeries> spreads;
	protected final Map<Market, TimeSeries> midQuotes;
	protected final Map<Market, TimeSeries> bids; // best bid in each market
	protected final Map<Market, TimeSeries> asks; // best ask in each market
	protected final SumStats controlFundamentalValue;
	protected final SumStats controlPrivateValue;
	
	protected final SumStats marketmakerSpreads;
	protected final SumStats marketmakerLadderCenter;
	protected final SumStats marketmakerExecutionTimes;
	protected final SumStats marketmakerTruncRungs;
	
	protected double zirpGreedyOrders;
	protected double zirpNonGreedyOrders;
	
	// Static information needed for observations
	protected final Collection<? extends Player> players;
	protected final Collection<? extends Agent> agents;
	protected final Collection<? extends Market> markets;
	protected final FundamentalValue fundamental;
	protected final SimulationSpec spec;
	protected final Set<Class<? extends Agent>> agentTypes;
	protected final int simLength;
	protected final SumStats isWithinRange;
	protected final SumStats meanSpread;

	public static final boolean VERBOSE_OUTPUT = true;

	/**
	 * Constructor needs to be called before the simulation starts, but with the
	 * final object collections.
	 */
	public Observations(SimulationSpec spec, Collection<? extends Market> markets,
			Collection<? extends Agent> agents, Collection<? extends Player> players,
			FundamentalValue fundamental) {
		this.players = players;
		this.agents = agents;
		this.markets = markets;
		this.fundamental = fundamental;
		this.spec = spec;
		
		this.simLength = spec.getSimulationProps().getAsInt(Keys.SIMULATION_LENGTH);
		
		// This is so that every agent type is output at the end, even if they
		// completed no transactions
		ImmutableSet.Builder<Class<? extends Agent>> agentTypesBuilder = ImmutableSet.builder();
		for (Agent agent : agents)
			agentTypesBuilder.add(agent.getClass());
		agentTypes = agentTypesBuilder.build();
		
		ImmutableMap.Builder<Market, TimeSeries> spreadsBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<Market, TimeSeries> midQuotesBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Market, TimeSeries> bidsBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Market, TimeSeries> asksBuilder = ImmutableMap.builder();
		for (Market market : markets) {
			spreadsBuilder.put(market, TimeSeries.create());
			midQuotesBuilder.put(market, TimeSeries.create());
			bidsBuilder.put(market, TimeSeries.create());
            asksBuilder.put(market, TimeSeries.create());
		}
		spreads = spreadsBuilder.build();
		midQuotes = midQuotesBuilder.build();
		bids = bidsBuilder.build();
        asks = asksBuilder.build();
		
		this.executionTimes = SumStats.create();
		this.numTrans = HashMultiset.create();
		this.prices = SumStats.create();
		this.transPrices = TimeSeries.create();
		this.nbboSpreads = TimeSeries.create();
		this.controlPrivateValue = SumStats.create();
		this.controlFundamentalValue = SumStats.create();
		
		this.marketmakerExecutionTimes = SumStats.create();
		this.marketmakerLadderCenter = SumStats.create();
		this.marketmakerSpreads = SumStats.create();
		this.marketmakerTruncRungs = SumStats.create();
		
		this.isWithinRange = SumStats.create();
		this.meanSpread = SumStats.create();
	}
	
	/**
	 * Gets the player observations relevant to EGTA.
	 */
	public List<PlayerObservation> getPlayerObservations() {
		Builder<PlayerObservation> playerObservations = ImmutableList.builder();
		for (Player player : players)
			playerObservations.add(player.getObservation());
		return playerObservations.build();
	}
	
	/**
	 * Gets the features, which are relevant to the non EGTA case. Features that
	 * aren't aggregated by the EventBus, like the surplus ones, are calculated
	 * at the current time, instead of being summed during the simulation.
	 */
	public Map<String, Double> getFeatures() {
		ImmutableMap.Builder<String, Double> features = ImmutableMap.builder();
		
		if (VERBOSE_OUTPUT) {
		      features.put("fund_end_price", fundamental.getValueAt(TimeStamp.create(simLength-1)).doubleValue());
		        features.put("exectime_mean", executionTimes.mean());
		        features.put("trans_mean_price", prices.mean());
		        features.put("trans_stddev_price", prices.stddev());
		}

		
		double numAllTrans = 0;
	    SumStats mmTrans = SumStats.create();
		for (Class<? extends Agent> type : agentTypes) {
			double agentTrans = numTrans.count(type);
			if (VERBOSE_OUTPUT) {
		         features.put("trans_" + type.getSimpleName().toLowerCase() + "_num", 
		                    agentTrans);
			}
			numAllTrans += agentTrans;
			if (MarketMaker.class.isAssignableFrom(type)) {
			    mmTrans.add(agentTrans);
			}
		}
		if (VERBOSE_OUTPUT) {
		      features.put("trans_num", numAllTrans);
		}
		
		SumStats medians = SumStats.create();
		// collect data on median spreads, omitting values that are NaN or Infinity
		SumStats finiteNotNanMedians = SumStats.create();
		for (Entry<Market, TimeSeries> entry : spreads.entrySet()) {
			DescriptiveStatistics spreads = DSPlus.from(entry.getValue().sample(1, simLength));
			double median = DSPlus.median(spreads);
			
			if (VERBOSE_OUTPUT) {
		         features.put("spreads_median_market_" + entry.getKey().getID(), median);
			}
			medians.add(median);
			if (!Double.isInfinite(median) && !Double.isNaN(median)) {
			    finiteNotNanMedians.add(median);
			}
		}
		if (finiteNotNanMedians.getN() != 0) {
		    // if there are no entries, the mean will be undefined, so skip
		    if (VERBOSE_OUTPUT) {
		          features.put("mean_median_spread_not_inf_nan", finiteNotNanMedians.mean());
		    }
		}
		
		// average of median market spreads (for all markets in this model)
		if (VERBOSE_OUTPUT) {
		      features.put("spreads_mean_markets", medians.mean());
		}
		
		// take rms error of midquote versus \hat{r}_T(t), the estimated fundamental
		// for the final time step at time t, for each market.
		// report the mean rms error.
		double rootMeanSquareErrorTotal = 0.0;
		for (Entry<Market, TimeSeries> entry: midQuotes.entrySet()) {
		    double squaredErrorCurrentTotal = 0.0;
		    double[] midquoteArray = DSPlus.from(entry.getValue().sample(1, simLength)).getValues();
		    int definedValues = 0;
		    for (int i = 0; i < midquoteArray.length; i++) {
		        // skip values where the midquote is undefined, because the rms is undefined
		        if (!Double.isNaN(midquoteArray[i])) {
		            final double estimatedFund = 
	                    getEstimatedFundamental(
                            TimeStamp.create(i), simLength, fundamental.kappa, fundamental.meanValue
                        );
		            double error = midquoteArray[i] - estimatedFund;
		            squaredErrorCurrentTotal += error * error;
                    definedValues++;
		        }
		    }
		    
		    final double meanSquareError = squaredErrorCurrentTotal / definedValues;
		    rootMeanSquareErrorTotal += Math.sqrt(meanSquareError);
		}
		final double meanRms = rootMeanSquareErrorTotal / midQuotes.keySet().size();
		if (VERBOSE_OUTPUT) {
	        features.put("mean_rms_midquote_error_vs_estim_rt", meanRms);
		}
        
        // over time steps where there is a bid or ask price,
        // take the minimum of (\hat{r}_T(t) - bid) and (ask - \hat{r}_T(t)),
        // or the only one of these that is defined if exactly one is.
        // if this minimum is zero, then a trade can be had at no expected loss.
        // if it is positive, the positive amount is the least expected loss available for a trade.
        // if it is negative, then a trade can be had at the bid or ask for an expected profit,
        // based on public value alone.
        double meanMinShadeTotal = 0.0;
        for (final Market market: bids.keySet()) {
            final double[] bidsArray = DSPlus.from(bids.get(market).sample(1, simLength)).getValues();
            final double[] asksArray = DSPlus.from(asks.get(market).sample(1, simLength)).getValues();
            
            double minShadeCurrentTotal = 0.0;
            int definedCount = 0;
            for (int i = 0; i < bidsArray.length; i++) {
                final double bidCurrent = bidsArray[i];
                final double askCurrent = asksArray[i];
                
                if (Double.isNaN(bidCurrent) && Double.isNaN(askCurrent)) {
                    continue;
                }
                final double estimatedFund = 
                    getEstimatedFundamental(
                        TimeStamp.create(i), simLength, fundamental.kappa, fundamental.meanValue
                    );
                definedCount++;
                if (Double.isNaN(bidCurrent)) {
                    minShadeCurrentTotal += askCurrent - estimatedFund;
                } else if (Double.isNaN(askCurrent)) {
                    minShadeCurrentTotal += estimatedFund - bidCurrent;
                } else {
                    minShadeCurrentTotal += 
                        Math.min(askCurrent - estimatedFund, estimatedFund - bidCurrent);
                }
            }
            
            final double meanMinShadeCurrent = minShadeCurrentTotal / definedCount;
            meanMinShadeTotal += meanMinShadeCurrent;
        }
        meanMinShadeTotal /= bids.keySet().size();
        if (VERBOSE_OUTPUT) {
            features.put("mean_min_shade_vs_estim_rt", meanMinShadeTotal);
        }
        
        // record the fraction of those time steps where a bid-ask spread is defined,
        // in which the estimated final fundamental is in the range [bid, ask].
        double fractionEstimateInSpreadTotal = 0.0;
        for (final Market market: bids.keySet()) {
            final double[] bidsArray = DSPlus.from(bids.get(market).sample(1, simLength)).getValues();
            final double[] asksArray = DSPlus.from(asks.get(market).sample(1, simLength)).getValues();
            
            int numEstimateInSpread = 0;
            int definedCount = 0;
            for (int i = 0; i < bidsArray.length; i++) {
                final double bidCurrent = bidsArray[i];
                final double askCurrent = asksArray[i];
                
                if (Double.isNaN(bidCurrent) || Double.isNaN(askCurrent)) {
                    continue;
                }
                definedCount++;
                final double estimatedFund = 
                    getEstimatedFundamental(
                        TimeStamp.create(i), simLength, fundamental.kappa, fundamental.meanValue
                    );
                if (bidCurrent <= estimatedFund && askCurrent >= estimatedFund) {
                    numEstimateInSpread++;
                }
            }
            
            final double fractionEstimateInSpread = 1.0 * numEstimateInSpread / definedCount;
            fractionEstimateInSpreadTotal += fractionEstimateInSpread;
        }
        final double meanFractionEstimateInSpreadTotal = 
            fractionEstimateInSpreadTotal / bids.keySet().size();
        if (VERBOSE_OUTPUT) {
            features.put("mean_fraction_estim_rt_in_spread", meanFractionEstimateInSpreadTotal);
        }
		
		DescriptiveStatistics spreads = DSPlus.from(nbboSpreads.sample(1, simLength));
		if (VERBOSE_OUTPUT) {
		      features.put("spreads_median_nbbo", DSPlus.median(spreads));
		}
		
		TimeSeries fundPrices = fundamental.asTimeSeries();
		for (int period : Consts.PERIODS)
			periodBased(features, fundPrices, period);
		
		if (VERBOSE_OUTPUT) {
		      // Market maker
		      features.put("mm_spreads_mean", marketmakerSpreads.mean());
		        features.put("mm_ladder_mean", marketmakerLadderCenter.mean());
		        features.put("mm_spreads_stddev", marketmakerSpreads.stddev());
		        features.put("mm_exectime_mean", marketmakerExecutionTimes.mean());
		        features.put("mm_rungs_trunc_mean", marketmakerTruncRungs.mean());
		        
		        // Bayes Market Maker
		        features.put("bmm_spreads_mean", meanSpread.mean());
		        features.put("bmm_within_range_frac", isWithinRange.mean());
		        
		        // ZIRP
		        features.put("zirp_greedy", zirpGreedyOrders);
		        features.put("zirp_nongreedy", zirpNonGreedyOrders);
		}
		
		// Profit and Surplus (and Private Value)
		SumStats 
		    backgroundArrivals = SumStats.create(),
		    mmArrivals = SumStats.create(),
			modelProfit = SumStats.create(),
			backgroundAgentProfit = SumStats.create(),
			hftProfit = SumStats.create(),
			marketMakerProfit = SumStats.create(),
			marketMakerAbsValInventory = SumStats.create(), // absolute value of inventory
			marketMakerPositioningProfit = SumStats.create(), // profit from change in fund
	        marketMakerSpreadProfit = SumStats.create(); // profit from collecting half-spread
		// store distribution of positions
		int[] positions = new int[2 * spec.getSimulationProps().getAsInt(Keys.MAX_POSITION) + 1];
		
		for (Agent agent : agents) {
			long profit = agent.getPostLiquidationProfit();
			modelProfit.add(profit);
			if (agent instanceof BackgroundAgent) {
				backgroundAgentProfit.add(profit);
				backgroundArrivals.add(agent.getStrategyArrivals());
				// backgroundLiquidation.add(agent.getLiquidationProfit());
			} else if (agent instanceof HFTAgent) {
				hftProfit.add(profit);
			} else if (agent instanceof MarketMaker) {
				marketMakerProfit.add(profit);
				marketMakerAbsValInventory.add(Math.abs((double) agent.getPositionBalance()));
				marketMakerPositioningProfit.add(agent.getPositioningProfit());
				marketMakerSpreadProfit.add(agent.getSpreadProfit());
				mmArrivals.add(agent.getStrategyArrivals());
			}
			if (agent instanceof BayesMarketMaker) {
			    if (VERBOSE_OUTPUT) {
		             features.put("bmm_inventory", (double) agent.getPositionBalance());
			    }
			}
			
			if (agent instanceof MaxEfficiencyAgent) {
				MaxEfficiencyAgent ag = (MaxEfficiencyAgent) agent;
				positions[ag.getPosition() + spec.getSimulationProps().getAsInt(Keys.MAX_POSITION)]++;
			}
		}
		// check if its in MAX EFF mode (i.e. there are MaxEfficiencyAgents)
		for (AgentProperties props : spec.getAgentProps()) {
			if (props.getAgentType().equals(Consts.AgentType.MAXEFFICIENCY)) {
				int maxPosition = spec.getSimulationProps().getAsInt(Keys.MAX_POSITION);
				for (int i = 0; i < positions.length; i++) {
				    if (VERBOSE_OUTPUT) {
	                    features.put("position_" + (i - maxPosition) + "_num", (double) positions[i]);
				    }
				}
			}
		}

		if (VERBOSE_OUTPUT) {
		      features.put("profit_sum_total", modelProfit.sum());
		        features.put("profit_sum_background", backgroundAgentProfit.sum());
		        // features.put("profit_sum_liquidation", backgroundLiquidation.sum());
		        features.put("profit_sum_marketmaker", marketMakerProfit.sum());
		        features.put("background_arrivals", backgroundArrivals.sum());
		        features.put("mm_arrivals", mmArrivals.sum());
		        
		        // sum of each MM's absolute value of inventory (units held at final time step), over MMs
		        features.put("sum_absv_inventory_mm", marketMakerAbsValInventory.sum());
		        
		        // sum of each MM's profit component from collecting the half-spread, over MMs
		        features.put("sum_spread_profit_mm", marketMakerSpreadProfit.sum());
		        
		        // sum of each MM's profit component from change in fundamental value of units already held, over MMs
		        features.put("sum_positioning_profit_mm", marketMakerPositioningProfit.sum());
		        
		        // total number of "spreads earned," which is min(buys, sells) for each MM, summed over MMs
		        features.put("total_spreads_earned_mm", (mmTrans.sum() - marketMakerAbsValInventory.sum()) / 2.0);
		        features.put("profit_sum_hft", hftProfit.sum());
		}
		
		Map<Class<? extends Agent>, SumStats> agentSurplus = Maps.newHashMap();
		for (DiscountFactor discount : DiscountFactor.values()) {
			SumStats surplus = SumStats.create();
			// go through all agents & update for each agent type
			for (Agent agent : agents)
				if (agent instanceof BackgroundAgent) {
					controlPrivateValue.add(((BackgroundAgent) agent).getPrivateValueMean().doubleValue());
					
					surplus.add(((BackgroundAgent) agent).getDiscountedSurplus(discount)); // only includes surplus from PV
					surplus.add(agent.getLiquidationProfit()); 	// also add proceeds from liquidation
					if (!agentSurplus.containsKey(agent.getClass()))
						agentSurplus.put(agent.getClass(), SumStats.create());

					agentSurplus.get(agent.getClass()).add(
						((BackgroundAgent) agent).getDiscountedSurplus(discount)
						+ agent.getLiquidationProfit()
					);
				}
			
			if (VERBOSE_OUTPUT) {
		         features.put("surplus_sum_" + discount, surplus.sum());
		            for (Class<? extends Agent> type : agentTypes)
		                if (BackgroundAgent.class.isAssignableFrom(type))
		                    features.put("surplus_" + type.getSimpleName().toLowerCase() 
		                            + "_sum_" + discount, agentSurplus.get(type).sum());
			}
		}
				
		if (VERBOSE_OUTPUT) {
		      // for control variates
	        List<Double> fundTimeSeries = fundPrices.sample(1, simLength);
	        for (double v : fundTimeSeries)
	            controlFundamentalValue.add(v);
	        features.put("control_mean_fund", controlFundamentalValue.mean());
	        features.put("control_var_fund", controlFundamentalValue.variance());
	        features.put("control_mean_private", controlPrivateValue.mean());
		}
		
		return features.build();
	}
	
	/**
	 * Statistics that are based on the sampling period
	 */
	protected void periodBased(ImmutableMap.Builder<String, Double> features, 
			TimeSeries fundPrices, int period) {
		// Price discovery
		String key = period == 1 ? "trans_rmsd" : "trans_freq_" + period + "_rmsd"; 
		DescriptiveStatistics pr = DSPlus.from(transPrices.sample(period, simLength));
		DescriptiveStatistics fundStat = DSPlus.from(fundPrices.sample(period, simLength));
		if (VERBOSE_OUTPUT) {
		      features.put(key, DSPlus.rmsd(pr, fundStat));
		}

		// Volatility
		String prefix = period == 1 ? "vol" : "vol_freq_" + period;

		SumStats stddev = SumStats.create();
		SumStats logPriceVol = SumStats.create();
		SumStats logRetVol = SumStats.create();

		for (Entry<Market, TimeSeries> entry : midQuotes.entrySet()) {
			TimeSeries mq = entry.getValue();
			// compute log price volatility for this market
			Iterable<Double> filtered = Iterables.filter(mq.sample(period, simLength), 
					not(equalTo(Double.NaN)));
			double stdev = SumStats.fromData(filtered).stddev();

			if (VERBOSE_OUTPUT) {
		         features.put(prefix + "_stddev_price_market_" + entry.getKey().getID(), stdev);
			}

			stddev.add(stdev);
			if (stdev != 0)
				// XXX ?ideal? don't add if stddev is 0
				logPriceVol.add(Math.log(stdev));

			// compute log-return volatility for this market
			// XXX Note change in log-return vol from before. Before if the ratio was
			// NaN it go thrown out. Now the previous value is used. Not sure if
			// this is correct
			DescriptiveStatistics mktLogReturns = DSPlus.fromLogRatioOf(mq.sample(period, simLength));
			double logStdev = mktLogReturns.getStandardDeviation();

			if (VERBOSE_OUTPUT) {
		         features.put(prefix + "_stddev_log_return_market_" + entry.getKey().getID(), logStdev);
			}
			logRetVol.add(logStdev);
		}

		if (VERBOSE_OUTPUT) {
		      // average measures across all markets in this model
	        features.put(prefix + "_mean_stddev_price", stddev.mean());
	        features.put(prefix + "_mean_log_price", logPriceVol.mean());
	        features.put(prefix + "_mean_log_return", logRetVol.mean());
		}
	}
	
	// --------------------------------------
	// Everything with an @Subscribe is a listener for objects that contain statistics.
	
	@Subscribe public void processZIRP(ZIRPStatistic statistic) {
		if (statistic.greedy) zirpGreedyOrders++;
		else zirpNonGreedyOrders++;
	}
	
	@Subscribe public void processMarketMaker(MarketMakerStatistic statistic) {
		marketmakerLadderCenter.add((statistic.ask.doubleValue() + statistic.bid.doubleValue())/2);
		marketmakerSpreads.add(statistic.ask.doubleValue() - statistic.bid.doubleValue());
	}
	
	@Subscribe public void processLadder(LadderStatistic statistic) {
		marketmakerTruncRungs.add(statistic.num);
	}
	
	@Subscribe public void processSpread(SpreadStatistic statistic) {
		TimeSeries series = spreads.get(statistic.owner);
		series.add(statistic.time.getInTicks(), statistic.val);
	}
	
    @Subscribe public void processBid(BidStatistic statistic) {
        TimeSeries series = bids.get(statistic.owner);
        series.add(statistic.time.getInTicks(), statistic.val);
    }
    
    @Subscribe public void processAsk(AskStatistic statistic) {
        TimeSeries series = asks.get(statistic.owner);
        series.add(statistic.time.getInTicks(), statistic.val);
    }
	
	@Subscribe public void processMidQuote(MidQuoteStatistic statistic) {
		TimeSeries series = midQuotes.get(statistic.owner);
		series.add(statistic.time.getInTicks(), statistic.val);
	}
	
	@Subscribe public void processNBBOSpread(NBBOStatistic statistic) {
		nbboSpreads.add(statistic.time.getInTicks(), statistic.val);
	}
	
   @Subscribe public void processBMMStatistic(BMMStatistic statistic) {
        this.isWithinRange.add(statistic.isWithinSpread ? 1 : 0);
        this.meanSpread.add(statistic.diff);
    }
	
	@Subscribe public void processTransaction(Transaction transaction) {
		long execTime = transaction.getExecTime().getInTicks();
		long buyerExecTime = execTime - transaction.getBuyOrder().getSubmitTime().getInTicks();
		long sellerExecTime = execTime - transaction.getSellOrder().getSubmitTime().getInTicks();
		for (int quantity = 0; quantity < transaction.getQuantity(); quantity++) {
			// only measure execution time for background traders
			if (transaction.getBuyer() instanceof BackgroundAgent) {
				executionTimes.add(buyerExecTime);
			}
			if (transaction.getSeller() instanceof BackgroundAgent) {
				executionTimes.add(sellerExecTime);
			}
			// Also measure for market makers
			if (transaction.getBuyer() instanceof MarketMaker) {
				marketmakerExecutionTimes.add(buyerExecTime);
			}
			if (transaction.getSeller() instanceof MarketMaker) {
				marketmakerExecutionTimes.add(sellerExecTime);
			}
		}

		prices.add(transaction.getPrice().doubleValue());

		transPrices.add((int) transaction.getExecTime().getInTicks(), 
				transaction.getPrice().doubleValue());
		
		// update number of transactions
		numTrans.add(transaction.getBuyer().getClass());
		numTrans.add(transaction.getSeller().getClass());
	}
	
	@Subscribe public void deadStat(DeadEvent d) {
		log.log(Log.Level.ERROR, "Unhandled Statistic: %s", d);
	}
	
	// --------------------------------------
	// These are all statistics classes that are listened for
	
	public static class ZIRPStatistic {
		protected final Agent ag;
		protected final boolean greedy;
		
		public ZIRPStatistic(Agent ag, boolean greedy) {
			this.ag = ag;
			this.greedy = greedy;
		}
	}
	
	public static class MarketMakerStatistic {
		protected final MarketMaker mm;
		protected final Price bid;
		protected final Price ask;
		
		public MarketMakerStatistic(MarketMaker mm, Price ladderBid, Price ladderAsk) {
			this.mm = mm;
			this.bid = ladderBid;
			this.ask = ladderAsk;
		}
	}
	
	public static class LadderStatistic {
		protected final MarketMaker mm;
		protected final int num;
		
		public LadderStatistic(MarketMaker mm, int rungsTruncated) {
			this.mm = mm;
			this.num = rungsTruncated;
		}
	}
	
	public static class NBBOStatistic {
		protected final double val;
		protected final TimeStamp time;

		public NBBOStatistic(double val, TimeStamp time) {
			this.val = val;
			this.time = time;
		}
	}
	
    public static class BMMStatistic {
        protected final double diff;
        protected final boolean isWithinSpread;

        public BMMStatistic(double aDiff, boolean aIsWithinSpread) {
            this.diff = aDiff;
            this.isWithinSpread = aIsWithinSpread;
        }
    }
	
	public static abstract class MarketStatistic {
		protected final double val;
		protected final Market owner;
		protected final TimeStamp time;

		public MarketStatistic(Market owner, double val, TimeStamp time) {
			this.owner = owner;
			this.val = val;
			this.time = time;
		}
	}
	
	public static class SpreadStatistic extends MarketStatistic {
		public SpreadStatistic(Market market, double val, TimeStamp time) {
			super(market, val, time);
		}
	}
	
    public static class BidStatistic extends MarketStatistic {
        public BidStatistic(Market market, double val, TimeStamp time) {
            super(market, val, time);
        }
    }
    
    public static class AskStatistic extends MarketStatistic {
        public AskStatistic(Market market, double val, TimeStamp time) {
            super(market, val, time);
        }
    }
	
	public static class MidQuoteStatistic extends MarketStatistic {
		public MidQuoteStatistic(Market market, double val, TimeStamp time) {
			super(market, val, time);
		}
	}

	public static abstract class PriceStatistic {
		protected final double val;
		
		public PriceStatistic(Price value) {
			this.val = value.doubleValue();
		}
	}
	
    protected double getEstimatedFundamental(
        TimeStamp time, int simLength, 
        double kappa, double fundamentalMean
    ) {
        
        final int stepsLeft = (int) (simLength - time.getInTicks());
        final double kappaCompToPower = Math.pow(1 - kappa, stepsLeft);
        return fundamental.getValueAt(time).intValue() * kappaCompToPower 
            + fundamentalMean * (1 - kappaCompToPower);
    }
    
}
