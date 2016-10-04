package data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import systemmanager.Keys;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;

public class MultiSimulationObservationsTest {

	@Test
	public void singlePlayerTest() {
		MultiSimulationObservations obs = new MultiSimulationObservations(true, 1);
		
		obs.addObservation(new MockObservations(
				ImmutableList.of(new PlayerObservation("back", "a", 5, 
						ImmutableMap.of(Keys.PV_BUY1, -10.0, Keys.PV_SELL1, 15.0, Keys.PV_POSITION1_MAX_ABS, 15.0))),
				ImmutableMap.<String, Double> of()));
		obs.addObservation(new MockObservations(
				ImmutableList.of(new PlayerObservation("back", "a", 10, 
						ImmutableMap.of(Keys.PV_BUY1, -12.0, Keys.PV_SELL1, 20.0, Keys.PV_POSITION1_MAX_ABS, 20.0))),
				ImmutableMap.<String, Double> of()));
		obs.addObservation(new MockObservations(
				ImmutableList.of(new PlayerObservation("back", "a", 21, 
						ImmutableMap.of(Keys.PV_BUY1, -11.0, Keys.PV_SELL1, 10.0, Keys.PV_POSITION1_MAX_ABS, 11.0))),
				ImmutableMap.<String, Double> of()));
		
		assertEquals(12, obs.playerObservations.get(0).payoff.mean(), 0.001);
		// Sample Standard Deviation...
		assertEquals(8.185, obs.playerObservations.get(0).payoff.stddev(), 0.001);
		
		JsonArray players = obs.toJson().getAsJsonObject().get("players").getAsJsonArray();
		assertEquals(12, players.get(0).getAsJsonObject().get("payoff").getAsDouble(), 0.001);
		assertEquals(8.185, players.get(0).getAsJsonObject().get("features").getAsJsonObject().get("payoff_stddev").getAsDouble(), 0.001);
//		assertEquals(15.0, players.get(0).getAsJsonObject().get("features").getAsJsonObject().get(Keys.PV_SELL1).getAsDouble(), 0.001);
//		assertEquals(-11.0, players.get(0).getAsJsonObject().get("features").getAsJsonObject().get(Keys.PV_BUY1).getAsDouble(), 0.001);
//		assertEquals(15.333, players.get(0).getAsJsonObject().get("features").getAsJsonObject().get(Keys.PV_POSITION1_MAX_ABS).getAsDouble(), 0.001);
	}
	
	@Test
	public void twoPlayerTest() {
		MultiSimulationObservations obs = new MultiSimulationObservations(true, 1);
		
		obs.addObservation(new MockObservations(
				ImmutableList.of(
						new PlayerObservation("back", "a", 5, 
								ImmutableMap.of(Keys.PV_BUY1, -10.0, Keys.PV_SELL1, 20.0, Keys.PV_POSITION1_MAX_ABS, 20.0)),
						new PlayerObservation("back", "b", 10,
								ImmutableMap.of(Keys.PV_BUY1, -12.0, Keys.PV_SELL1, 24.0, Keys.PV_POSITION1_MAX_ABS, 24.0))),
				ImmutableMap.<String, Double> of()));
		obs.addObservation(new MockObservations(
				ImmutableList.of(
						new PlayerObservation("back", "a", 10, 
								ImmutableMap.of(Keys.PV_BUY1, -20.0, Keys.PV_SELL1, 30.0, Keys.PV_POSITION1_MAX_ABS, 30.0)),
						new PlayerObservation("back", "b", 20, 
								ImmutableMap.of(Keys.PV_BUY1, -24.0, Keys.PV_SELL1, 20.0, Keys.PV_POSITION1_MAX_ABS, 24.0))),
				ImmutableMap.<String, Double> of()));
		
		assertEquals(7.5, obs.playerObservations.get(0).payoff.mean(), 0.001);
		assertEquals(15, obs.playerObservations.get(1).payoff.mean(), 0.001);
		
		JsonArray players = obs.toJson().getAsJsonObject().get("players").getAsJsonArray();
		assertEquals(7.5, players.get(0).getAsJsonObject().get("payoff").getAsDouble(), 0.001);
		assertEquals("a", players.get(0).getAsJsonObject().get("strategy").getAsString());
//		assertEquals(25.0, players.get(0).getAsJsonObject().get("features").getAsJsonObject().get(Keys.PV_SELL1).getAsDouble(), 0.001);
//		assertEquals(-15.0, players.get(0).getAsJsonObject().get("features").getAsJsonObject().get(Keys.PV_BUY1).getAsDouble(), 0.001);
//		assertEquals(25.0, players.get(0).getAsJsonObject().get("features").getAsJsonObject().get(Keys.PV_POSITION1_MAX_ABS).getAsDouble(), 0.001);
		
		assertEquals(15, players.get(1).getAsJsonObject().get("payoff").getAsDouble(), 0.001);
		assertEquals("b", players.get(1).getAsJsonObject().get("strategy").getAsString());
//		assertEquals(22.0, players.get(1).getAsJsonObject().get("features").getAsJsonObject().get(Keys.PV_SELL1).getAsDouble(), 0.001);
//		assertEquals(-18.0, players.get(1).getAsJsonObject().get("features").getAsJsonObject().get(Keys.PV_BUY1).getAsDouble(), 0.001);
//		assertEquals(24.0, players.get(1).getAsJsonObject().get("features").getAsJsonObject().get(Keys.PV_POSITION1_MAX_ABS).getAsDouble(), 0.001);
	}
}
