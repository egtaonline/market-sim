package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
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
import fourheap.Order.OrderType;

public class ZIMOAgent extends BackgroundAgent {
    
    private static final long serialVersionUID = -801069197387425530L;
    
    public ZIMOAgent(
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
            props.getAsInt(Keys.BID_RANGE_MAX)
        );
    }
    
    private ZIMOAgent(Scheduler scheduler, TimeStamp arrivalTime,
        FundamentalValue fundamental, SIP sip, Market market, Random rand,
        Iterator<TimeStamp> reentry, final double pvVar, int tickSize,
        final int maxAbsPosition,
        int bidRangeMin, int bidRangeMax) {
        super(scheduler, arrivalTime, fundamental, sip, market, rand, reentry, 
            new PrivateValue(maxAbsPosition, pvVar, rand),
            tickSize, bidRangeMin, bidRangeMax);        
    }
    
    @Override
    public void agentStrategy(final TimeStamp currentTime) {
        super.agentStrategy(currentTime);

        if (!currentTime.equals(arrivalTime)) log.log(INFO, "%s Wake up.", this);
        log.log(INFO, "%s Withdraw all orders.", this);
        withdrawAllOrders();

        // 50% chance of being either long or short
        OrderType orderType = BUY;
        if (rand.nextBoolean()) orderType = SELL;

        // log.log(INFO, "%s Submit %s order.", this, orderType);
        executeZIMOStrategy(orderType, currentTime);
    }
}
