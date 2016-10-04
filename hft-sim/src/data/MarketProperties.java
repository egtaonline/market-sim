package data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import systemmanager.Consts.MarketType;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

/**
 * Entity properties bundled with MarketType
 * 
 * @author erik
 * 
 */
public class MarketProperties extends EntityProperties {
	
	private static final long serialVersionUID = -8634339070031699521L;
	
	protected final MarketType type;
	
	protected MarketProperties(MarketType type, Map<String, String> properties, String configString) {
		super(properties, configString);
		this.type = type;
	}
	
	public static MarketProperties empty(MarketType type) {
		return new MarketProperties(type, Maps.<String, String> newHashMap(), "");
	}
	
	public static MarketProperties create(MarketType type, EntityProperties defaults, String configString) {
		Map<String, String> props = Maps.newHashMap(checkNotNull(defaults).properties);
		props.putAll(parseConfigString(configString));
		return new MarketProperties(type, props, configString);
	}
	
	public MarketType getMarketType() {
		return type;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof MarketProperties))
			return false;
		final MarketProperties e = (MarketProperties) o;
		return Objects.equal(e.type, type) && super.equals(e);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(type, super.hashCode());
	}
	@Override
	public String toString() {
		return type + " " + super.toString();
	}

}
