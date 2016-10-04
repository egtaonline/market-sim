package utils;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public abstract class Maps2 {

	/**
	 * Creates an immutable map from strings to strings where the input is
	 * successive pairs of arbitrary objects.
	 */
	public static ImmutableMap<String, String> fromPairs(Object... keyValuePairs) {
		checkArgument(keyValuePairs.length % 2 == 0, "Must have an even number of inputs");
		Builder<String, String> builder = ImmutableMap.builder();
		for (int i = 0; i < keyValuePairs.length; i += 2)
			builder.put(keyValuePairs[i].toString(), keyValuePairs[i+1].toString());
		return builder.build();
	}
	
}
