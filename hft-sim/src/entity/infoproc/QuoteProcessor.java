package entity.infoproc;

import entity.market.Market;
import entity.market.Quote;
import event.TimeStamp;

/**
 * Class that updates pertinent information for the system. Generally used for 
 * creating NBBO update events. Serves the purpose of the Information Processor 
 * for a single market.
 * 
 * @author cnris
 */
public interface QuoteProcessor {

	public void sendToQuoteProcessor(Market market, Quote quote,
			TimeStamp currentTime);

	public void processQuote(Market market, Quote quote, TimeStamp currentTime);

	public TimeStamp getLatency();
}