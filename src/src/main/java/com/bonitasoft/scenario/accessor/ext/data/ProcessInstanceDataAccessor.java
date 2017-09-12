package com.bonitasoft.scenario.accessor.ext.data;

import java.io.Serializable;

import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.ext.ScenarioProcessAPI;

abstract class ProcessInstanceDataAccessor extends DataAccessor {
	protected ProcessInstanceDataAccessor(Long processInstanceId, String name) {
		super(processInstanceId, name);
	}

	protected Serializable getInstance(Accessor accessor) throws Exception {
		return ScenarioProcessAPI.getProcessInstance(accessor, instanceId);
	}
}
