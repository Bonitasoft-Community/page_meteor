package com.bonitasoft.scenario.runner;

import java.io.Serializable;
import java.util.logging.Level;

import com.bonitasoft.scenario.accessor.Accessor;

abstract public class RunListener {
	protected Accessor accessor = null;

	public void setAccessor(Accessor accessor) {
		this.accessor = accessor;
	}

	abstract public void advancementCallback(Integer advancement);

	abstract public void logCallback(Level level, Serializable message);

	abstract public void catchEvent(Serializable event);

	protected Accessor getAccessor() {
		return accessor;
	}
}
