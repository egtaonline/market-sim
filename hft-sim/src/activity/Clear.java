package activity;

import static com.google.common.base.Preconditions.checkNotNull;
import entity.market.Market;
import event.TimeStamp;

/**
 * Class for Activity of clearing the orderbook.
 * 
 * @author ewah
 */
public class Clear extends Activity {

	protected final Market market;

	public Clear(Market market) {
		this.market = checkNotNull(market, "Market");
	}

	@Override
	public void execute(TimeStamp currentTime) {
		this.market.clear(currentTime);
	}

	@Override
	public String toString() {
		return super.toString() + market;
	}
	
}
