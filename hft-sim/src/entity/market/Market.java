package entity.market;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static data.Observations.BUS;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static java.lang.Math.min;
import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import systemmanager.Scheduler;
import activity.SendToQP;
import activity.SendToTP;
import activity.WithdrawOrder;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import data.Observations.MidQuoteStatistic;
import data.Observations.SpreadStatistic;
import data.Observations.BidStatistic;
import data.Observations.AskStatistic;
import entity.Entity;
import entity.agent.Agent;
import entity.agent.BackgroundAgent;
import entity.agent.SchedulerAgent;
import entity.infoproc.BestBidAsk;
import entity.infoproc.MarketQuoteProcessor;
import entity.infoproc.MarketTransactionProcessor;
import entity.infoproc.QuoteProcessor;
import entity.infoproc.SIP;
import entity.infoproc.TransactionProcessor;
import entity.market.clearingrule.ClearingRule;
import event.TimeStamp;
import fourheap.FourHeap;
import fourheap.MatchedOrders;
import fourheap.Order.OrderType;

/**
 * Base class for all markets. This class provides almost all market
 * functionality that one should need for creating a market.
 * 
 * A market needs two things to function appropriately. First is a latency. This
 * represents how long it takes background agents (with only one primary market)
 * to get information from the market. A latency of TimeStamp.IMMEDIATE makes
 * this immediate. The second thing they need is a ClearingRule. A ClearingRule
 * is a class that takes a set of MatchedOrders and assigns a price to them.
 * This facilitates the difference in pricing between a call market and a CDA.
 * 
 * By default the only method that schedules more activities is a Clear. A clear
 * will trigger a SendToIP for every IP this Market knows about (via the method
 * updateQuote). Every other time another action needs to happen it needs to be
 * overridden by the subclass. For example, a CDA will trigger a Clear after a
 * SubmitOrder, and sendToIP activities after a WithdrawOrder. A call market will
 * schedule another Clear after an existing one.
 * 
 * @author ewah
 */
public abstract class Market extends Entity {

	private static final long serialVersionUID = 8806298743451593261L;
	public static int nextID = 1;
	
	protected final FourHeap<Price, MarketTime, Order> orderbook;
	protected final ClearingRule clearingRule;
	protected final Random rand;
	protected long marketTime; // keeps track of internal market actions

	protected final MarketQuoteProcessor quoteProcessor;				// market QP, member of qps collection
	protected final MarketTransactionProcessor transactionProcessor;	// market TP, member of tps collection
	protected final SIP sip;
	protected final Collection<QuoteProcessor> qps;			// agent QPs to update with this market's quotes
	protected final Collection<TransactionProcessor> tps;	// agent TPs to update with this market's transactions
	
	protected Quote quote; // Current quote

	// Book keeping
	protected final Multiset<Price> askPriceQuantity, bidPriceQuantity; // How many orders are at a specific price
	// FIXME These two are used only for testing, and nothing else. Is there a better way? This could be a lot of extra memory
	protected final Collection<Order> orders; // All orders ever submitted to the market
	protected final List<Transaction> allTransactions; // All successful transactions, implicitly time ordered
	
	// used so that SchedulerAgent can received updates on transactions that clear in each time step.
	// may be null, if no SchedulerAgent is used.
	protected SchedulerAgent schedulerAgent;

	/**
	 * Constructor
	 * 
	 * @param sip
	 *            the SIP for this simulation
	 * @param latency
	 *            how long it takes agents with market as a primary model get
	 *            get notified of market changes
	 * @param clearingRule
	 *            a class that dictates how the prices on matching orders are
	 *            set
	 * @param rand
	 *            the random number generator to use
	 */
	public Market(Scheduler scheduler, SIP sip, TimeStamp latency,
			ClearingRule clearingRule, Random rand) {
		
		this(scheduler, sip, latency, latency, clearingRule, rand);
	}
	
	/**
	 * Constructor
	 * 
	 * @param sip
	 *            the SIP for this simulation
	 * @param quoteLatency
	 *            how long it takes agents with market as a primary model get
	 *            get notified of quote updates
	 * @param transactionLatency
	 *            how long it takes agents with market as a primary model get
	 *            get notified of new transactions
	 * @param clearingRule
	 *            a class that dictates how the prices on matching orders are
	 *            set
	 * @param rand
	 *            the random number generator to use
	 */
	public Market(Scheduler scheduler, SIP sip, TimeStamp quoteLatency,
			TimeStamp transactionLatency, ClearingRule clearingRule, Random rand) {
		super(nextID++, scheduler);
		this.orderbook = FourHeap.<Price, MarketTime, Order> create();
		this.clearingRule = clearingRule;
		this.rand = rand;
		this.marketTime = 0;
		this.quote = new Quote(this, null, 0, null, 0, TimeStamp.ZERO);

		this.qps = Lists.newArrayList();
		this.tps = Lists.newArrayList();
		this.sip = sip;
		this.quoteProcessor = new MarketQuoteProcessor(scheduler, quoteLatency, this);
		this.transactionProcessor = new MarketTransactionProcessor(scheduler, transactionLatency, this);
		qps.add(sip);
		tps.add(sip);
		qps.add(quoteProcessor);
		tps.add(transactionProcessor);

		this.askPriceQuantity = HashMultiset.create();
		this.bidPriceQuantity = HashMultiset.create();
		this.orders = Lists.newArrayList();
		this.allTransactions = Lists.newArrayList();
	}
	
	public void setSchedulerAgent(final SchedulerAgent schedAgent) {
	    assert this.schedulerAgent == null;
	    this.schedulerAgent = schedAgent;
	}

	/**
	 * Add an IP to this market that will get notified when the market quote and
	 * transactions change.
	 * 
	 * @param ip
	 *            the IP to add
	 */
	public void addQP(QuoteProcessor qp) {
		checkNotNull(qp, "QP");
		qps.add(qp);
	}
	
	/**
	 * Add an IP to this market that will get notified when the market quote and
	 * transactions change.
	 * 
	 * @param ip
	 *            the IP to add
	 */
	public void addTP(TransactionProcessor tp) {
		checkNotNull(tp, "TP");
		tps.add(tp);
	}

	public MarketQuoteProcessor getQuoteProcessor() {
		return this.quoteProcessor;
	}
	
	public MarketTransactionProcessor getTransactionProcessor() {
		return this.transactionProcessor;
	}
	
	/**
	 * Convenience method to fully remove an order.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * NOTE: Don't subclass this method, subclass the main one
	 * 
	 * @param order
	 *            order to remove
	 * @param currentTime
	 *            the current time
	 * @return any side effect activities (base case none)
	 */
	public void withdrawOrder(Order order, TimeStamp currentTime) {
		withdrawOrder(order, order.getQuantity(), currentTime);
	}
	
	/**
	 * Method to withdraw specific quantity from an order.
	 * 
	 * This will work even if quantity has decreased after order was submitted.
	 * Trying to cancel a higher quantity than the order has to offer will
	 * simply result in the entire order being cancelled.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param order
	 *            order to withdraw quantity from
	 * @param quantity
	 *            quantity to withdraw, always positive
	 * @param currentTime
	 *            the current time
	 * @return any side effect activities (base case none)
	 */
	public void withdrawOrder(Order order, int quantity, TimeStamp currentTime) {
		marketTime++;
		checkArgument(quantity >= 0, "Quantity must be non negative");
		if (order.getQuantity() == 0) return;
		quantity = min(quantity, order.getQuantity());
		
		Multiset<Price> priceQuant = order.getOrderType() == SELL ? askPriceQuantity : bidPriceQuantity;
		priceQuant.remove(order.getPrice(), quantity);
		
		orderbook.withdrawOrder(order, quantity);
		
		if (order.getQuantity() == 0)
			order.agent.removeOrder(order);
	}

	/**
	 * Clears the order book.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param currentTime
	 *            the current time
	 * @return any side effect activities (base case SendToIP activities)
	 */
	public void clear(TimeStamp currentTime) {
		marketTime++;
		MarketTime transactionTime = new MarketTime(currentTime, marketTime);
		Collection<MatchedOrders<Price, MarketTime, Order>> matchedOrders = orderbook.clear();
		Builder<Transaction> transactionBuilder = ImmutableList.builder();
		for (Entry<MatchedOrders<Price, MarketTime, Order>, Price> e : clearingRule.pricing(matchedOrders).entrySet()) {

			Order buy = e.getKey().getBuy();
			Order sell = e.getKey().getSell();

			Transaction trans = new Transaction(buy.getAgent(),
				sell.getAgent(), this, buy, sell, e.getKey().getQuantity(),
				e.getValue(), transactionTime);
			log.log(INFO, "%s", trans);
			
			askPriceQuantity.remove(sell.getPrice(), trans.getQuantity());
			bidPriceQuantity.remove(buy.getPrice(), trans.getQuantity());
			
			transactionBuilder.add(trans);
			allTransactions.add(trans);
			BUS.post(trans);
		}
		
		List<Transaction> transactions = transactionBuilder.build();
        // if there is a SchedulerAgent,
        // inform it of each Transaction.
        if (this.schedulerAgent != null) {
            this.schedulerAgent.transactionLog(transactions);
        }
		if (!transactions.isEmpty()) {
			for (TransactionProcessor tp : tps) {
				scheduler.executeActivity(new SendToTP(this, transactions, tp));
			}
		}
		updateQuote(currentTime);
	}

	/**
	 * Updates the Markets current quote and returns a set of SendToIP
	 * activities for every IP the market "knows" about
	 * 
	 * @param transactions
	 *            the transactions that are new since the last quote update
	 * @param currentTime
	 *            the current time
	 * @return the SendToIP activities required to propagate information
	 */
	protected void updateQuote(TimeStamp currentTime) {
		Price ask = orderbook.askQuote();
		Price bid = orderbook.bidQuote();
		int quantityAsk = askPriceQuantity.count(ask);
		int quantityBid = bidPriceQuantity.count(bid);
		/*
		 * TODO In certain circumstances, there will be no orders with the
		 * current ask or bid price in the market. This is a result of the ask
		 * price being set as part of a matched order. The correct "quantity" is
		 * 0, but it doesn't tell how many orders are available at that price.
		 * This is possible to do a number of ways. One could use a sorted map
		 * which has log time, one could scan the heap which would be worst case
		 * linear, or probably the best thing would be to have the heap have a
		 * hashmap to check the quantity based off of wither matched or
		 * unmatched order... I think this may be fine the way it is, only
		 * because if quantity is 0, but price isn't null, then you'll have to
		 * bid over instead of equal to the quote in order to execute. Also,
		 * this will only happen when there are matched orders when a quote is
		 * generated, which is currently never possible
		 */
		MarketTime quoteTime = new MarketTime(currentTime, marketTime);
		quote = new Quote(this, bid, quantityBid, ask, quantityAsk, quoteTime);

		log.log(INFO, "%s %s", this, quote);

		BUS.post(new MidQuoteStatistic(this, quote.getMidquote(), currentTime));
		BUS.post(new SpreadStatistic(this, quote.getSpread(), currentTime));
		if (quote.getBidPrice() == null) {
            BUS.post(new BidStatistic(this, Double.NaN, currentTime));
		} else {
		      BUS.post(new BidStatistic(this, quote.getBidPrice().doubleValue(), currentTime));
		}
        if (quote.getAskPrice() == null) {
            BUS.post(new AskStatistic(this, Double.NaN, currentTime));
        } else {
            BUS.post(new AskStatistic(this, quote.getAskPrice().doubleValue(), currentTime));
        }

		for (QuoteProcessor qp : qps)
			scheduler.executeActivity(new SendToQP(this, quote, qp));
	}
	
	/*
	 * TODO Add IOC / Fill or Kill Order (potentially change current syntax so
	 * that a withdraw without a duration never expires, and one with duration
	 * IMMEDIATE is a Fill or Kill. Not sure if this will work / make sense.
	 */

	/**
	 * Submit an order that doesn't expire
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * NOTE: Don't subclass this method, subclass the main one.
	 * 
	 * @param agent
	 *            The agent that's submitting the order
	 * @param type
	 *            The order type (BUY or SELL) from fourheap
	 * @param price
	 *            The price of the order
	 * @param quantity
	 *            The quantity of the order (negative for sell orders)
	 * @param currentTime
	 *            The current time
	 * @return any side effect activities (base case none)
	 */
	public void submitOrder(Agent agent, OrderType type, Price price, int quantity,
			TimeStamp currentTime) {
		submitOrder(agent, type, price, quantity, currentTime, TimeStamp.IMMEDIATE);
	}

	/**
	 * Submit an order that will "expire." An order expires by returning a
	 * withdrawOrder activity scheduled at currentTime + duration in the future.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param agent
	 *            The agent submitting the order
	 * @param type
	 *            The type of the order (BUY/SELL)
	 * @param price
	 *            The price of the order
	 * @param quantity
	 *            The quantity of the order, negative for sell orders
	 * @param currentTime
	 *            The current time
	 * @param duration
	 *            The amount of time to wait before canceling the order. Use
	 *            TimeStamp.IMMEDIATE for an order that doesn't expire.
	 * @return The side effect activities (base case potentially a WithdrawOrder
	 *         for this method)
	 */
	public void submitOrder(Agent agent, OrderType type, Price price,
			int quantity, TimeStamp currentTime, TimeStamp duration) {
		checkNotNull(type, "Order type");
		checkArgument(quantity > 0, "Quantity must be positive");
		marketTime++;

		// Check that within position limits for background agent
		if (agent instanceof BackgroundAgent)
			if (!((BackgroundAgent) agent).withinMaxPosition(type, quantity)) return;
		
		Order order = Order.create(type, agent, this, price, quantity, new MarketTime(currentTime, marketTime));
		log.log(INFO, "%s", order);

		Multiset<Price> priceQuant = order.getOrderType() == BUY ? bidPriceQuantity : askPriceQuantity;
		priceQuant.add(order.getPrice(), quantity);
		
		orderbook.insertOrder(order);
		orders.add(order);
		agent.addOrder(order);
		
		if (!duration.equals(TimeStamp.IMMEDIATE))
			scheduler.scheduleActivity(currentTime.plus(duration), new WithdrawOrder(order));
	}

	/**
	 * Submit a routed order that doesn't expire. The placed bid will be routed
	 * to the Market that appears to offer the best execution according to the
	 * NBBO and this market's current up to date quote.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param agent
	 *            The agent that's submitting the order
	 * @param type
	 *            The type of the order (BUY/SELL)
	 * @param price
	 *            The price of the order
	 * @param quantity
	 *            The quantity of the order, negative for sell orders
	 * @param currentTime
	 *            The currentTime
	 * @return Any side effect activities (none in base case)
	 */
	public void submitNMSOrder(Agent agent, OrderType type, Price price,
			int quantity, TimeStamp currentTime) {
		submitNMSOrder(agent, type, price, quantity, currentTime, TimeStamp.IMMEDIATE);
	}

	/**
	 * Submit a routed order that "expires" after duration. The placed bid will
	 * be routed to the Market that appears to offer the best execution
	 * according to the NBBO and this market's current up to date quote. If
	 * duration is TimeStamp.IMMEDIATE than the order will not expire.
	 * 
	 * NOTE: This should only be called by the corresponding activity
	 * 
	 * @param agent
	 *            The agent submitting the order
	 * @param type
	 * 			  The type of the order (BUY/SELL)
	 * @param price
	 *            The price of the order
	 * @param quantity
	 *            The quantity of the order, negative for sell orders
	 * @param currentTime
	 *            The current time
	 * @param duration
	 *            The duration before the bid should expire. If duration =
	 *            TimeStamp(1), the bid will expire after 1 millisecond. To make
	 *            an order that doesn't expire use TimeStamp.IMMEDIATE.
	 * @return Any side effect activities (base case possibly a withdraw order)
	 */
	/*
	 * TODO How should call markets handle Reg NMS. Can't route to call market
	 * to get immediate execution. NMSOrder will not route properly for a call
	 * market if there is another market in the model
	 */
	public void submitNMSOrder(Agent agent, OrderType type, Price price,
			int quantity, TimeStamp currentTime, TimeStamp duration) {
		checkNotNull(type, "Order type");
		checkArgument(quantity > 0, "Quantity must be positive");
		
		BestBidAsk nbbo = sip.getNBBO();
		Market bestMarket = this;

		if (quantity > 0) { // buy
			boolean nbboBetter = nbbo.getBestAsk() != null
					&& nbbo.getBestAsk().lessThan(quote.getAskPrice());
			boolean willTransact = price.greaterThan(nbbo.getBestAsk());
			if (nbboBetter && willTransact)
				bestMarket = nbbo.getBestAskMarket();
		} else { // sell
			boolean nbboBetter = nbbo.getBestBid() != null
					&& nbbo.getBestBid().greaterThan(quote.getBidPrice());
			boolean willTransact = price.lessThan(nbbo.getBestBid());
			if (nbboBetter && willTransact)
				bestMarket = nbbo.getBestBidMarket();
		}

		if (!bestMarket.equals(this))
			log.log(INFO, "Routing %s %s %d @ %s from %s %s to NBBO %s %s",
					agent, type, quantity, price, this, quote, bestMarket, nbbo);

		bestMarket.submitOrder(agent, type, price, quantity, currentTime, duration);
	}
	
	/**
	 * Get a list of all transactions in the market. This should NOT be used by
	 * agents to get transaction lists.
	 * 
	 * @return An immutable list of Transactions.
	 */
	public List<Transaction> getTransactions() {
		return ImmutableList.copyOf(allTransactions);
	}
	
	@Override
    protected String name() {
		String oldName = super.name();
		return oldName.substring(0, oldName.length() - 6);
	}

	/**
	 * Base Market just returns the id in []. Subclasses should override to also
	 * add information about the type of market.
	 */
	@Override
	public String toString() {
		return name() + " [" + id + "]";
	}
	
}
