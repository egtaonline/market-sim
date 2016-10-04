package entity.agent;

import iterators.EveryStepArrivals;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import activity.Activity;
import activity.AgentArrival;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

public class SchedulerAgent extends ReentryAgent {
    
    private static final long serialVersionUID = 660476054499964977L;
    
    private Collection<Agent> agents;
    
    private MarketMaker marketMaker;
    private ZIMOAgent zimoAgent;
    private FundamentalAgent fundAgent;
    
    // probability that an arriving background trader
    // will be a FundamentalAgent, instead of a ZIMOAgent.
    private final double probFundAgent;
    
    private TransactionType transactionState;
    
    /**
     * Indicates what transactions the MarketMaker has been involved
     * in since the previous time step.
     * 
     * BG_BUY: a background agent has bought from the market maker.
     * BG_SELL: a background agent has sold to the market maker.
     * NO_TRADE: no agent has bought or sold to (from) the market maker.
     */
    private enum TransactionType {
        BG_BUY, BG_SELL, NO_TRADE
    }

    public SchedulerAgent(Scheduler scheduler,
        FundamentalValue fundamental, SIP sip, Market market, Random rand,
        final EntityProperties props
    ) {
        super(
            scheduler, 
            TimeStamp.ZERO, // arrive at initial time
            fundamental, 
            sip, 
            market,
            rand, 
            new EveryStepArrivals(), // re-enter at every time step
            props.getAsInt(Keys.AGENT_TICK_SIZE, Keys.TICK_SIZE)
        );
        
        this.probFundAgent = props.getAsDouble(Keys.PROB_FUND_AGENT);
        if (this.probFundAgent < 0 || this.probFundAgent > 1) {
            throw new IllegalArgumentException();
        }
        
        this.agents = new HashSet<Agent>();
        this.transactionState = TransactionType.NO_TRADE;
    }
    
    @Override
    public void agentStrategy(final TimeStamp currentTime) {
        super.agentStrategy(currentTime);

        scheduleTimeStep(currentTime);
    }
    
    public void setAgents(final Collection<Agent> newAgents) {
        this.agents.clear();
        this.agents.addAll(newAgents);
        testAgentSet();
        setupDefaultAgents();
    }
    
    private void setupDefaultAgents() {
        this.marketMaker = null;
        this.zimoAgent = null;
        this.fundAgent = null;
        
        for (final Agent agent: this.agents) {
            if (
                agent instanceof FundamentalMarketMaker 
                || agent instanceof BayesMarketMaker
            ) {
                this.marketMaker = (MarketMaker) agent;
            } else if (agent instanceof ZIMOAgent) {
                this.zimoAgent = (ZIMOAgent) agent;
            } else if (agent instanceof FundamentalAgent) {
                this.fundAgent = (FundamentalAgent) agent;
            }
        }
        
        if (
            this.marketMaker == null
            || this.zimoAgent == null
            || this.fundAgent == null
        ) {
            throw new IllegalStateException();
        }
    }
    
    private void testAgentSet() {
        int countMarketMakers = 0;
        int countZimoAgents = 0;
        int countFundAgents = 0;
        for (final Agent agent: this.agents) {
            if (
                agent instanceof FundamentalMarketMaker 
                || agent instanceof BayesMarketMaker
            ) {
                countMarketMakers++;
            } else if (agent instanceof ZIMOAgent) {
                countZimoAgents++;
            } else if (agent instanceof FundamentalAgent) {
                countFundAgents++;
            }
        }
        
        if (
            countMarketMakers != 1 
            || countZimoAgents != 1
            || countFundAgents != 1
            || this.agents.size() != 4 // 4 agents: 1 MM, 2 traders, 1 scheduler
        ) {
            throw new IllegalStateException();
        }
    }
    
    /**
     * Called by the Market object after each time step,
     * so the SchedulerAgent can update whether a BayesMarketMaker
     * bought, sold, or did not transact.
     * 
     * Usage:
     * Market object should call after each time step,
     * as the market clears.
     * 
     * @param newTransactions a list of Transaction events that occurred
     * in the previous time step, passed by the Market object.
     */
    public void transactionLog(final List<Transaction> newTransactions) {
        assert newTransactions != null;
        
        if (!(this.marketMaker instanceof BayesMarketMaker)) {
            // no BayesMarketMaker, so no need to update
            return;
        }
        
        for (Transaction transaction: newTransactions) {
            if (transaction.getBuyer().equals(this.marketMaker)) {
                // System.out.println("Scheduler: sell occurred");
                this.transactionState = TransactionType.BG_SELL;
                return;
            }
            if (transaction.getSeller().equals(this.marketMaker)) {
                // System.out.println("Scheduler: buy occurred");
                this.transactionState = TransactionType.BG_BUY;
                return;
            }
        }
        
        // System.out.println("Scheduler: no trade occurred");
        
        this.transactionState = TransactionType.NO_TRADE;
    }
    
    // 1. if this.marketMaker is a BayesMarketMaker:
    //     inform BayesMarketMaker of whether it traded in previous time step:
    //     call one of: 
    //     this.marketMaker.sellOccurred(), buyOccurred(), or noTradeOccurred()
    // 2. if this.marketMaker is a BayesMarketMaker:
    //     inform BayesMarketMaker of whether a jump in fundamental value occurred
    //     in the previous time step:
    //     if so, call: this.marketMaker.jumpOccurred().
    private void scheduleTimeStep(final TimeStamp currentTime) {
        if (this.marketMaker instanceof BayesMarketMaker) {
            final BayesMarketMaker bayesMM = (BayesMarketMaker) this.marketMaker;
            
            switch (transactionState) {
            case BG_BUY:
                bayesMM.buyOccurred();
                break;
            case BG_SELL:
                bayesMM.sellOccurred();
                break;
            case NO_TRADE:
                bayesMM.noTradeOccurred();
                break;
            default:
                throw new IllegalStateException();
            }
            
            final TimeStamp previousTime = TimeStamp.create(currentTime.getInTicks() - 1);
            if (
                !fundamental.getValueAt(currentTime).equals(
                    fundamental.getValueAt(previousTime)
            )) {
                // fundamental value changed since previous time step
                bayesMM.jumpOccurred();
            }
        }
        
        final Activity marketMakerAct = getMarketMakerActivity();
        final Activity backgroundTraderAct = getBackgroundTraderActivity();
        scheduler.scheduleActivities(
            currentTime, marketMakerAct, backgroundTraderAct
        );
    }
    
    private Activity getMarketMakerActivity() {
        return new AgentArrival(this.marketMaker);
    }
    
    private Activity getBackgroundTraderActivity() {
        if (rand.nextDouble() < this.probFundAgent) {
            return new AgentArrival(this.fundAgent);
        }
        
        return new AgentArrival(this.zimoAgent);
    }
}
