package data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import systemmanager.Consts.AgentType;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

/**
 * Entity properties that is bundled with an AgentType.
 * 
 * @author erik
 * 
 */
public class AgentProperties extends EntityProperties {

	private static final long serialVersionUID = -8267036814743083118L;
	
	protected final AgentType type;
	
	protected AgentProperties(AgentType type, Map<String, String> backedProperties, String stratString) {
		super(backedProperties, stratString);
		this.type = type;
	}
	
	public static AgentProperties fromConfigString(String configString) {
		int index = checkNotNull(configString).indexOf(':');
		if (index == -1)
			return new AgentProperties(AgentType.valueOf(configString), Maps.<String, String> newHashMap(), configString);
		return new AgentProperties(AgentType.valueOf(configString.substring(0, index)),
				parseConfigString(configString.substring(index + 1)), configString);
	}
	
	public static AgentProperties fromConfigString(String configString, EntityProperties defaults) {
		Map<String, String> props = Maps.newHashMap(checkNotNull(defaults).properties);
		int index = checkNotNull(configString).indexOf(':');
		if (index == -1)
			return new AgentProperties(AgentType.valueOf(configString), props, configString);
		props.putAll(parseConfigString(configString.substring(index + 1)));
		return new AgentProperties(AgentType.valueOf(configString.substring(0, index)), props, configString);
	}
	
	public static AgentProperties create(AgentType type, EntityProperties defaults, String configString) {
		Map<String, String> props = Maps.newHashMap(checkNotNull(defaults).properties);
		props.putAll(parseConfigString(configString));
		return new AgentProperties(type, props, configString);
	}

	public AgentType getAgentType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof AgentProperties))
			return false;
		final AgentProperties e = (AgentProperties) o;
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
