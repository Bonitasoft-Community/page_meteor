package com.bonitasoft.scenario.accessor.ext.waiter;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;

import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.ext.ScenarioProcessAPI;

public class TaskExecutionWaiter extends Waiter {
	public TaskExecutionWaiter(Accessor accessor, Map<String, Serializable> parameters) {
		super(accessor, parameters);
	}

	@Override
	protected Serializable check() throws Exception {
		accessor.log(Level.FINE, "The task execution waiter is checking");
		return ScenarioProcessAPI.isActivityInstanceArchived(accessor, parameters);
	}
}
