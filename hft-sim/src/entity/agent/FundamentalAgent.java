package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.INFO;
import iterators.ExpInterarrivals;

import java.util.Iterator;
import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class FundamentalAgent extends BackgroundAgent {

    private static final long serialVersionUID = 2805525672676331559L;
    
    private final int noiseStandardDev;
    private final boolean withdrawOrders;

    public FundamentalAgent(
        final Scheduler scheduler, 
        final TimeStamp arrivalTime,
        final FundamentalValue fundamental, 
        final SIP sip, 
        final Market market, 
        final Random rand,
        final EntityProperties props
    ) {
        this(scheduler, arrivalTime, fundamental, sip, market, rand,
            ExpInterarrivals.create(
                props.getAsDouble(Keys.BACKGROUND_REENTRY_RATE, Keys.REENTRY_RATE), rand
            ),
            props.getAsDouble(Keys.PRIVATE_VALUE_VAR),
            props.getAsInt(Keys.AGENT_TICK_SIZE, Keys.TICK_SIZE),
            props.getAsInt(Keys.MAX_POSITION),
            props.getAsInt(Keys.BID_RANGE_MIN),
            props.getAsInt(Keys.BID_RANGE_MAX),
            props.getAsBoolean(Keys.WITHDRAW_ORDERS),
            (int) props.getAsDouble(Keys.NOISE_STDEV)
        );
    }
    
    private FundamentalAgent(Scheduler scheduler, TimeStamp arrivalTime,
        FundamentalValue fundamental, SIP sip, Market market, Random rand,
        Iterator<TimeStamp> reentry, final double pvVar, int tickSize,
        final int maxAbsPosition,
        int bidRangeMin, int bidRangeMax,
        final boolean aWithdrawOrders,
        final int aNoiseStandardDev
    ) {
        super(scheduler, arrivalTime, fundamental, sip, market, rand, reentry, 
            new PrivateValue(maxAbsPosition, pvVar, rand),
            tickSize, bidRangeMin, bidRangeMax);
        if (aNoiseStandardDev < 0) {
            throw new IllegalArgumentException();
        }
        
        withdrawOrders = aWithdrawOrders;
        noiseStandardDev = aNoiseStandardDev;
    }
    
    @Override
    public void agentStrategy(final TimeStamp currentTime) {
        super.agentStrategy(currentTime);

        if (!currentTime.equals(arrivalTime)) log.log(INFO, "%s Wake up.", this);
        if (withdrawOrders) {
            log.log(INFO, "%s Withdraw all orders.", this);
            withdrawAllOrders();
        }
        
        executeFundamentalStrategy(currentTime, noiseStandardDev);
    }
}
