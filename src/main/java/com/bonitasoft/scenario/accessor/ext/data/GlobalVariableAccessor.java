package com.bonitasoft.scenario.accessor.ext.data;

import java.io.Serializable;

import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstance;

import com.bonitasoft.scenario.accessor.Accessor;

public class GlobalVariableAccessor extends ProcessInstanceDataAccessor {
	public GlobalVariableAccessor(Long processInstanceId, String name) {
		super(processInstanceId, name);
	}

	@Override
	public Serializable getValue(Accessor accessor) throws Exception {
		Serializable theProcessInstance = getInstance(accessor);
		if (theProcessInstance instanceof ProcessInstance) {
			return accessor.getDefaultProcessAPI().getProcessDataInstance(name, instanceId).getValue();
		} else if (theProcessInstance instanceof ArchivedProcessInstance) {
			return accessor.getDefaultProcessAPI().getArchivedProcessDataInstance(name, instanceId).getValue();
		} else {
			throw new Exception("The process instance retrieved is not supported by the Scenario library: " + theProcessInstance.getClass().getName());
		}
	}
}
