package com.bonitasoft.scenario.accessor.ext.data;

import java.io.Serializable;

import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;

import com.bonitasoft.scenario.accessor.Accessor;

public class LocalVariableAccessor extends ActivityInstanceDataAccessor {
	public LocalVariableAccessor(Long activityInstanceId, String name) {
		super(activityInstanceId, name);
	}

	@Override
	public Serializable getValue(Accessor accessor) throws Exception {
		Serializable theActivityInstance = getInstance(accessor);
		if (theActivityInstance instanceof ActivityInstance) {
			return accessor.getDefaultProcessAPI().getActivityDataInstance(name, instanceId).getValue();
		} else if (theActivityInstance instanceof ArchivedActivityInstance) {
			return accessor.getDefaultProcessAPI().getArchivedActivityDataInstance(name, instanceId).getValue();
		} else {
			throw new Exception("The activity instance retrieved is not supported by the Scenario library: " + theActivityInstance.getClass().getName());
		}
	}
}
