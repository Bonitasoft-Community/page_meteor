package com.bonitasoft.scenario.accessor.ext.data;

import java.io.Serializable;

import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.ext.ScenarioProcessAPI;

abstract class ActivityInstanceDataAccessor extends DataAccessor {
	protected ActivityInstanceDataAccessor(Long activityInstanceId, String name) {
		super(activityInstanceId, name);
	}

	protected Serializable getInstance(Accessor accessor) throws Exception {
		return ScenarioProcessAPI.getActivityInstance(accessor, instanceId);
	}
}
