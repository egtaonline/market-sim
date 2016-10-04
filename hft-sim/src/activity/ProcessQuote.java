package activity;

import static com.google.common.base.Preconditions.checkNotNull;
import entity.infoproc.QuoteProcessor;
import entity.market.Market;
import entity.market.Quote;
import event.TimeStamp;

/**
 * Class for Activity of IP processing a quote received from a given Market.
 * 
 * @author ewah
 */
public class ProcessQuote extends Activity {

	protected final QuoteProcessor ip;
	protected final Market market;
	protected final Quote quote;

	public ProcessQuote(QuoteProcessor ip, Market market, Quote quote) {
		this.ip = checkNotNull(ip, "IP");
		this.market = checkNotNull(market, "Market");
		this.quote = checkNotNull(quote, "Quote");
	}

	@Override
	public void execute(TimeStamp currentTime) {
		this.ip.processQuote(market, quote, currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + market + " -> " + ip + " : " + quote;
	}
	
}
