IMPORTS

import java.util.Map
import java.util.Date
import java.util.HashMap
import java.util.Iterator
import java.io.Serializable
import java.lang.Throwable

import org.bonitasoft.engine.api.IdentityAPI
import org.bonitasoft.engine.api.ProcessAPI


import org.bonitasoft.engine.api.TenantAdministrationAPI
import org.bonitasoft.engine.bpm.document.DocumentValue

import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.process.ProcessDefinition

import com.bonitasoft.scenario.accessor.ext.ScenarioIdentityAPI
import com.bonitasoft.scenario.accessor.ext.ScenarioProcessAPI
import com.bonitasoft.scenario.accessor.Constants
import com.bonitasoft.scenario.accessor.ext.ScenarioBdmAPI
import com.bonitasoft.scenario.accessor.ext.ScenarioTenantAdministrationAPI
import com.bonitasoft.scenario.accessor.exception.AssertionFailureException

try {
// --------- Extend the APIAccessor capabilities

accessor.metaClass.getIdentityAPI = {
	IdentityAPI identityAPI = getDefaultIdentityAPI()

	identityAPI.metaClass.deployOrganization = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioIdentityAPI.deployOrganization(accessor, parameters) }
	identityAPI.metaClass.deployProfiles = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioIdentityAPI.deployProfiles(accessor, parameters) }

	return identityAPI
}

accessor.metaClass.getProcessAPI = {
	ProcessAPI processAPI = getDefaultProcessAPI()

	processAPI.metaClass.startProcess = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.startProcess(accessor, parameters) }
	processAPI.metaClass.deployProcess = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.deployProcess(accessor, parameters) }
	processAPI.metaClass.purgeProcesses = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.purgeProcesses(accessor, parameters) }
	processAPI.metaClass.undeployProcesses = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.undeployProcesses(accessor, parameters) }
	processAPI.metaClass.isHumanTaskAvailableInProcessInstance = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.isHumanTaskAvailableInProcessInstance(accessor, parameters) }
	processAPI.metaClass.isProcessInstanceArchived = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.isProcessInstanceArchived(accessor, parameters) }
	processAPI.metaClass.executeUserTask = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.executeUserTask(accessor, parameters) }
	processAPI.metaClass.releaseUserTask = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.releaseUserTask(accessor, parameters) }
	processAPI.metaClass.assignUserTask = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.assignUserTask(accessor, parameters) }
	processAPI.metaClass.isActivityInstanceArchived = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.isActivityInstanceArchived(accessor, parameters) }
	processAPI.metaClass.caseContext = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.caseContext(accessor, parameters) }
	
	return processAPI
}

accessor.metaClass.getCommandAPI = {
	return getDefaultCommandAPI()
}

accessor.metaClass.getProfileAPI = {
	return getDefaultProfileAPI()
}

accessor.metaClass.getMonitoringAPI = {
	return getDefaultMonitoringAPI()
}

accessor.metaClass.getPlatformMonitoringAPI = {
	return getDefaultPlatformMonitoringAPI()
}

accessor.metaClass.getLogAPI = {
	return getDefaultLogAPI()
}

accessor.metaClass.getNodeAPI = {
	return getDefaultNodeAPI()
}

accessor.metaClass.getReportingAPI = {
	return getDefaultReportingAPI()
}

accessor.metaClass.getThemeAPI = {
	return getDefaultThemeAPI()
}

accessor.metaClass.getPermissionAPI = {
	return getDefaultPermissionAPI()
}

accessor.metaClass.getCustomPageAPI = {
	return getDefaultCustomPageAPI()
}

accessor.metaClass.getLivingApplicationAPI = {
	return getDefaultLivingApplicationAPI()
}

accessor.metaClass.getBusinessDataAPI = {
	return getDefaultBusinessDataAPI()
}

accessor.metaClass.getTenantAdministrationAPI = {
	TenantAdministrationAPI tenantAdministrationAPI = getDefaultTenantAdministrationAPI()

	tenantAdministrationAPI.metaClass.deployBDM = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioTenantAdministrationAPI.deployBDM(accessor, parameters) }

	return tenantAdministrationAPI
}

accessor.metaClass.getBdmAPI = {
	ScenarioBdmAPI scenarioBdmAPI = getDefaultBdmAPI()

	scenarioBdmAPI.metaClass.findList = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioBdmAPI.findList(accessor, parameters) }

	return scenarioBdmAPI
}

// ---------- Extend Type to add contextual API shorcuts

ProcessDefinition.metaClass.start = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.startProcess(accessor, [process:getId()] + parameters) }

ProcessDefinition.metaClass.purge = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.purgeProcesses(accessor, [processes:[getId()]] + parameters) }

ProcessDefinition.metaClass.undeploy = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.undeployProcesses(accessor, [processes:[getId()]] + parameters) }

HumanTaskInstance.metaClass.assign = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.assignUserTask(accessor, [task:getId()] + parameters) }

HumanTaskInstance.metaClass.execute = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.executeUserTask(accessor, [task:getId()] + parameters) }

ActivityInstance.metaClass.isArchived = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.isActivityInstanceArchived(accessor, [task:getId()] + parameters) }

ProcessInstance.metaClass.isHumanTaskAvailable = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.isHumanTaskAvailableInProcessInstance(accessor, [process_instance:getId()] + parameters) }

ProcessInstance.metaClass.isArchived = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.isProcessInstanceArchived(accessor, [process_instance:getId()] + parameters) }

HumanTaskInstance.metaClass.release = { Map<String, Serializable> parameters = new HashMap<String, Serializable>() -> ScenarioProcessAPI.releaseUserTask(accessor, [task:getId()] + parameters) }

ProcessInstance.metaClass.data = { ScenarioProcessAPI.caseContext(accessor, [process_instance:getId()]) }

ArchivedProcessInstance.metaClass.data = { ScenarioProcessAPI.caseContext(accessor, [process_instance:getSourceObjectId()]) }

ActivityInstance.metaClass.data = { ScenarioProcessAPI.caseContext(accessor, [task:getId()]) }

ArchivedActivityInstance.metaClass.data = { ScenarioProcessAPI.caseContext(accessor, [task:getSourceObjectId()]) }

// ---------- Add user function

user = { Long user = null, Closure cl ->
	accessor.user = user
	cl()
	accessor.resetUser()
}

// ---------- Add wait function

wait = { Map<String, Serializable> parameters = new HashMap<String, Serializable>(), Closure cl ->
	Long endDate = new Date(new Date().getTime() + parameters.get(Constants.LIMIT, 2500L)).getTime()
	while(new Date().getTime() < endDate) {
		result = cl()
		if(result != null) {
			if(result instanceof Boolean) {
				if((Boolean)result) {
					return result
				}
			} else if(result instanceof Number) {
				if(((Number)result).doubleValue() > 0) {
					return result
				}
			} else if(result instanceof String) {
				if(!((String)result).isEmpty()) {
					return result
				}
			} else {
				return result
			}
		}
		
		sleep(parameters.get(Constants.PERIOD, 500L))
	}
		
	return false
}

// ---------- Add error/warn functions

error = { String message = null, Closure cl ->
	try {
		assert cl()
	} catch(AssertionError assertionError) {
		accessor.scenarioResult.addError(assertionError, message)
		throw new AssertionFailureException()
	}
}

warn = { String message = null, Closure cl ->
	try {
		assert cl()
	} catch(AssertionError assertionError) {
		accessor.scenarioResult.addWarn(assertionError, message)
	}
}

// ---------- Add timing functions

chrono = { Closure cl ->
	def startTime = System.currentTimeMillis()
	cl()
	return System.currentTimeMillis() - startTime
}

beforeTimestamp = { Long beforeTimestamp, Closure cl ->
	cl()
	return beforeTimestamp - System.currentTimeMillis() > 0
}

afterTimestamp = { Long afterTimestamp, Closure cl ->
	cl()
	return System.currentTimeMillis() - afterTimestamp > 0
}

betweenTimestamp = { Long fromTimestamp, Long toTimestamp, Closure cl ->
	cl()
	def Long endDateTime = System.currentTimeMillis()
	return toTimestamp - endDateTime > 0 && endDateTime - fromTimestamp > 0
}

beforeDuration = { Long beforeDuration, Closure cl ->
	def startTime = System.currentTimeMillis()
	cl()
	return System.currentTimeMillis() - startTime < beforeDuration
}

afterDuration = { Long afterDuration, Closure cl ->
	def startTime = System.currentTimeMillis()
	cl()
	return System.currentTimeMillis() - startTime > afterDuration
}

betweenDuration = { Long fromDuration, Long toDuration, Closure cl ->
	def startTime = System.currentTimeMillis()
	cl()
	def Long duration = System.currentTimeMillis() - startTime
	return duration > fromDuration && duration < toDuration
}
