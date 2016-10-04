package entity.agent;

import static logger.Log.log;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import activity.AgentStrategy;

import systemmanager.Consts;
import systemmanager.Executor;
import systemmanager.Keys;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;

public class ZIMOAgentTest {

    private Executor exec;
    private FundamentalValue fundamental = new MockFundamental(100000);
    private Market market;
    private SIP sip;
    private static Random rand;
    private static EntityProperties agentProperties = 
        EntityProperties.fromPairs(
            Keys.REENTRY_RATE, 0,
            Keys.MAX_POSITION, 2,
            Keys.PRIVATE_VALUE_VAR, 0,
            Keys.BID_RANGE_MIN, 0,
            Keys.BID_RANGE_MAX, 5000,
            Keys.SIMULATION_LENGTH, 60000
        );
    
    public ZIMOAgent createAgent(Object... parameters) {
        return createAgent(
            fundamental, 
            market, 
            rand, 
            parameters
            );
    }
    
    public ZIMOAgent createAgent(
        final FundamentalValue fundamental, 
        final Market market, 
        final Random rand, 
        final Object... parameters
        ) {
        return new ZIMOAgent(
            exec, 
            TimeStamp.ZERO, 
            fundamental, 
            sip, 
            market,
            rand, 
            EntityProperties.copyFromPairs(
                agentProperties,    
                parameters
                )
            );
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException{
        // Setting up the log file
        log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "ZIMOAgentTest.log"));

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
    
    @Test
    public void withdrawTest() {
        // verify that orders are correctly withdrawn at each re-entry
        ZIMOAgent agent = createAgent(
            Keys.MAX_POSITION, 2,
            Keys.WITHDRAW_ORDERS, true);
        
        MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, 2, 10);
        mm.submitOrderLadder(new Price(75), new Price(85), new Price(95), new Price(105));

        // execute strategy once; then before reenter, change the position balance
        // that way, when execute strategy again, it won't submit new orders
        exec.executeActivity(new AgentStrategy(agent));
        // verify that order submitted
        assertEquals(1, agent.activeOrders.size());
        agent.positionBalance = 10;
        exec.executeActivity(new AgentStrategy(agent));
        // verify that order withdrawn
        assertEquals(0, agent.activeOrders.size());
    }
    
    @Test
    public void randTestZIMO(){
        log.log(DEBUG, "Testing ZIMO 100 random argument bids are correct");
        final MarketMaker marketMaker = new MockMarketMaker(exec, fundamental, sip, market, 2, 10);

        //Testing 100 times
        for(int i = 0; i<100; i++){
            
            int currentTime = 100;
            
            //Creating ZIAgent
            ZIMOAgent testAgent = createAgent(
                Keys.MAX_POSITION, 2,
                Keys.WITHDRAW_ORDERS, true);
            
            marketMaker.submitOrderLadder(new Price(75), new Price(85), new Price(95), new Price(105));
            
            //Execute strategy
            testAgent.agentStrategy(TimeStamp.create(currentTime));
            
            //Retrieve orders
            Collection<Order> orders = testAgent.activeOrders;
            assertEquals(1, orders.size());
            Order order = Iterables.getFirst(orders, null);
            final Price orderPrice = order.getPrice();
            
            //Extracting bid quantity and price
            assertEquals("Incorrect order quantity", 1, order.getQuantity());

            final Quote quote = market.getQuoteProcessor().getQuote();

            //Checking bid quantity and price comply with randomized range
            // Sellers always sell at price higher than valuation ($100 + sell PV)
            // Buyers always buy at price lower than valuation ($100 + buy PV)
            switch(order.getOrderType()){
            case SELL:
                assertTrue(orderPrice.equals((quote.getBidPrice())));
                break;
            case BUY:
                assertTrue(orderPrice.equals((quote.getAskPrice())));
                break;
            default:
                fail("Invalid order type");
                break;
            }
        }
    }
}
