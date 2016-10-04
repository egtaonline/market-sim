package entity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import systemmanager.Scheduler;

/**
 * This class is the base for all things that may perform an action/activity 
 * in the simulation (e.g. agents, markets, etc).
 * 
 * Acts as the Receiver class in the Command pattern.
 * 
 * @author ewah
 */
public abstract class Entity implements Serializable {

	private static final long serialVersionUID = -7406324829959902527L;
	
	protected final Scheduler scheduler;
	protected final int id;
	
	public Entity(int agentID, Scheduler scheduler) {
		this.id = agentID;
		this.scheduler = checkNotNull(scheduler);
	}
	
	public final int getID() {
		return this.id;
	}
	
	protected String name() {
		return getClass().getSimpleName();
	}

	@Override
	public String toString() {
		return "(" + id + ')';
	}
	
}
