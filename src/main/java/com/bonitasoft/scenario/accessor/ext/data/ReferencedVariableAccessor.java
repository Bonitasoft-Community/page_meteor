package com.bonitasoft.scenario.accessor.ext.data;

import java.io.Serializable;
import java.util.Map;

import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstance;

import com.bonitasoft.scenario.accessor.Accessor;

abstract public class ReferencedVariableAccessor extends ProcessInstanceDataAccessor {
	static public String REF_SUFFIX = "_ref";

	public ReferencedVariableAccessor(Long instanceId, String name) {
		super(instanceId, name);
	}

	protected Map<String, Serializable> getExecutionContext(Accessor accessor) throws Exception {
		Serializable theProcessInstance = getInstance(accessor);
		if (theProcessInstance instanceof ProcessInstance) {
			return accessor.getDefaultProcessAPI().getProcessInstanceExecutionContext(instanceId);
		} else if (theProcessInstance instanceof ArchivedProcessInstance) {
			return accessor.getDefaultProcessAPI().getArchivedProcessInstanceExecutionContext(((ArchivedProcessInstance) theProcessInstance).getId());
		} else {
			throw new Exception("The process instance retrieved is not supported by the Scenario library: " + theProcessInstance.getClass().getName());
		}
	}

	static public String getName(String executionContextKey) {
		return executionContextKey.substring(0, executionContextKey.length() - REF_SUFFIX.length());
	}
}
