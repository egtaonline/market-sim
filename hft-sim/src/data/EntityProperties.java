package data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import systemmanager.Defaults;
import utils.Maps2;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

/**
 * Class that represents the properties of an entity. These are generally loaded
 * from a simulation spec file.
 * 
 * Contains methods to get values as any type, potentially with a default value.
 * If a default value isn't used, and the key doesn't exist, you'll get a null
 * object, or a null pointer.
 * 
 * All the getAs<Type> methods with two input arguments will check for the
 * EntityProperties object having a parameter matching the first key; if that
 * key doesn't exist, it will then check for the second key. The second key
 * is also the default key; that is, if nothing is found, then the 
 * EntityProperties object will return the default value hard-coded for the 
 * second key.
 * 
 * @author erik
 * 
 */
public class EntityProperties implements Serializable {
	
	private static final long serialVersionUID = -7220533203495890410L;
	
	private static final Set<String> trueStrings = ImmutableSet.of("t", "true");
	private final static Splitter configSplitter = Splitter.on('_');
	private static final Splitter arraySplitter = Splitter.on('-');
	private final static MapJoiner configJoiner = Joiner.on('_').withKeyValueSeparator("_");

	/**
	 * Store everything as strings, and convert out when called
	 */
	protected final Map<String, String> properties;
	protected final String configString;

	protected EntityProperties(Map<String, String> backedProperties, String configString) {
		this.properties = backedProperties;
		this.configString = configString;
	}
	
	/**
	 * Create an empty EntityProperties
	 * 
	 * @return
	 */
	public static EntityProperties empty() {
		return new EntityProperties(Maps.<String, String> newHashMap(), "");
	}
	
	/**
	 * Make a deep copy of another entity properties
	 * 
	 * @param from
	 * @return
	 */
	public static EntityProperties copy(EntityProperties from) {
		return new EntityProperties(Maps.newHashMap(from.properties), from.configString);
	}
	
	/**
	 * Make an entity properties from pairs of strings and objects. There must
	 * be an even number of parameters, and every other parameter, including the
	 * first, must be a string.
	 * 
	 * @param keysAndValues
	 * @return
	 */
	public static EntityProperties fromPairs(Object... keysAndValues) {
		EntityProperties created = EntityProperties.empty();
		created.putPairs(keysAndValues);
		return created;
	}
	
	/**
	 * Same as fromPairs, but with a default entity properties that the initial
	 * configuration is copied from. If a key appears in both from and
	 * keysAndValues, the value in keysAndValues will be used.
	 * 
	 * @param from
	 * @param keysAndValues
	 * @return
	 */
	public static EntityProperties copyFromPairs(EntityProperties from, Object... keysAndValues) {
		EntityProperties created = EntityProperties.copy(from);
		created.putPairs(keysAndValues);
		return created;
	}
	
	/**
	 * Parse an entity properties from a config string where keys and values are
	 * underscore delimited.
	 * 
	 * <code>fromPairs(pairs...)</code> and
	 * <code>fromConfigString(Joiner.on('_').join(pairs...))</code> will have
	 * the same result.
	 * 
	 * @param configString
	 * @return
	 */
	public static EntityProperties fromConfigString(String configString) {
		Map<String, String> properties = parseConfigString(configString);
		return new EntityProperties(properties, configString);
	}

	/**
	 * Parses a config string into a map from keys to values
	 * @param configString
	 * @return
	 */
	protected static Map<String, String> parseConfigString(String configString) {
		Iterable<String> args = configSplitter.split(checkNotNull(configString, "Config String"));
		checkArgument(Iterables.size(args) % 2 == 0, "Not key value pair");
		Map<String, String> parsed = Maps.newHashMap();
		for (Iterator<String> it = args.iterator(); it.hasNext();)
			parsed.put(it.next(), it.next());
		return parsed;
	}

	public Set<String> keys() {
		return properties.keySet();
	}

	public boolean hasKey(String key) {
		return properties.containsKey(key);
	}

	public boolean remove(String key) {
		return properties.remove(key) != null;
	}

	public String getAsString(String key) {
		Optional<String> opt = Optional.fromNullable(properties.get(key))
				.or(Optional.fromNullable(Defaults.get(key)));
		if (!opt.isPresent())
			throw new IllegalStateException("Default value of " + key + " is not defined");
		return opt.get();
	}
	
	public String getAsString(String key, String defaultKey) {
		Optional<String> opt = Optional.fromNullable(properties.get(key))
				.or(Optional.fromNullable(properties.get(defaultKey)))
				.or(Optional.fromNullable(Defaults.get(defaultKey)));
		if (!opt.isPresent())
			throw new IllegalStateException("Default value of " + defaultKey + " is not defined");
		return opt.get();
	}

	public int getAsInt(String key) {
		return Integer.parseInt(getAsString(key));
	}

	public int getAsInt(String key, String defaultKey) {
		return Integer.parseInt(getAsString(key, defaultKey));
	}

	public double getAsDouble(String key) {
		return Double.parseDouble(getAsString(key));
	}

	public double getAsDouble(String key, String defaultKey) {
		return Double.parseDouble(getAsString(key, defaultKey));
	}

	public float getAsFloat(String key) {
		return Float.parseFloat(getAsString(key));
	}
	
	public float getAsFloat(String key, String defaultKey) {
		return Float.parseFloat(getAsString(key, defaultKey));
	}

	public long getAsLong(String key) {
		return Long.parseLong(getAsString(key));
	}

	public long getAsLong(String key, String defaultKey) {
		return Long.parseLong(getAsString(key, defaultKey));
	}

	public boolean getAsBoolean(String key) {
		return parseBoolean(getAsString(key));
	}

	public boolean getAsBoolean(String key, String defaultKey) {
		return parseBoolean(getAsString(key, defaultKey));
	}
	
	public int[] getAsIntArray(String key) {
		return parseIntArr(getAsString(key));
	}
	
	public int[] getAsIntArray(String key, String defaultKey){
		return parseIntArr(getAsString(key, defaultKey));
	}

	public void put(String key, String value) {
		properties.put(key, value);
	}

	public void put(String key, Number value) {
		properties.put(key, value.toString());
	}

	public void put(String key, boolean value) {
		properties.put(key, Boolean.toString(value));
	}
	
	public void putPairs(Object... keysAndValues) {
		properties.putAll(Maps2.fromPairs(keysAndValues));
	}
	
	public String getConfigString() {
		return configString;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof EntityProperties))
			return false;
		final EntityProperties e = (EntityProperties) o;
		return properties.equals(e.properties);
	}

	@Override
	public int hashCode() {
		return properties.hashCode();
	}

	public String toConfigString() {
		return configJoiner.join(properties);
	}

	@Override
	public String toString() {
		return properties.toString();
	}
	
	protected static boolean parseBoolean(String string) {
		return string != null && trueStrings.contains(string.toLowerCase());
	}
	
	// TODO Make this apply to general arrays instead of just int
	protected static int[] parseIntArr(String string) {
		return Ints.toArray(Collections2.transform(arraySplitter.splitToList(string), Ints.stringConverter()));
	}
}
