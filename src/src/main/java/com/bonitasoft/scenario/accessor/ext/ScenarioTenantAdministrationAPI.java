package com.bonitasoft.scenario.accessor.ext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;

import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.Constants;
import com.bonitasoft.scenario.accessor.parameter.Extractor;
import com.bonitasoft.scenario.accessor.resource.ResourceType;

public class ScenarioTenantAdministrationAPI {
	static public boolean deployBDM(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
		String methodName = "deployBDM";
		parameters = Extractor.preProcessParameters(parameters);
		
		accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

		byte[] bdmResource = (byte[])Extractor.getScenarioResource(parameters.get(Constants.RESOURCE_NAME), Constants.RESOURCE_NAME, true, accessor, ResourceType.BDM);
		
        try {
            if (!accessor.getDefaultTenantAdministrationAPI().isPaused()) {
        		accessor.log(Level.FINE, methodName + ": stop tenant services");
            	accessor.getDefaultTenantAdministrationAPI().pause();
            }
    		accessor.log(Level.FINE, methodName + ": clean and uninstall old one");
            accessor.getDefaultTenantAdministrationAPI().cleanAndUninstallBusinessDataModel();
    		accessor.log(Level.FINE, methodName + ": install new one");
            accessor.getDefaultTenantAdministrationAPI().installBusinessDataModel(bdmResource);
        } finally {
            if (accessor.getDefaultTenantAdministrationAPI().isPaused()) {
        		accessor.log(Level.FINE, methodName + ": resume tenant services");
            	accessor.getDefaultTenantAdministrationAPI().resume();
            }
        }
		
		return true;
	}
}
