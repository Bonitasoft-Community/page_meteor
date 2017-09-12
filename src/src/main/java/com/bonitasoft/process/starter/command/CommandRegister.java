package com.bonitasoft.process.starter.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;

import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;

public class CommandRegister {
	static public void main(String[] args) throws Exception {
		String url = "http://localhost:8080";
		String appName = "bonita";
		String login = "walter.bates";
		String password = "bpm";
		String filepath = ".\\target\\";
		String filename = "process-starter-command-1.0.jar";
		
		boolean tearDown = true;
		boolean register = true;
		boolean test = true;
		
		if (args.length != 0) {
			url = "http://" + args[0] + ":" + args[1];
			appName = args[2];
			login = args[3];
			password = args[4];
			filepath = args[5];
			filename = args[6];
			tearDown = Boolean.parseBoolean(args[7]);
			register = Boolean.parseBoolean(args[8]);
			test = Boolean.parseBoolean(args[9]);
		}

		Map<String, String> settings = new HashMap<String, String>();
		settings.put("server.url", url);
		settings.put("application.name", appName);
		APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, settings);
		LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
		APISession apiSession = loginAPI.login(login, password);

		// Tear down
		if(tearDown) {
			TenantAPIAccessor.getCommandAPI(apiSession).removeDependency(filename);
			TenantAPIAccessor.getCommandAPI(apiSession).unregister(ProcessStarterCommand.NAME);
		}
		// Register
		if(register) {
			byte[] byteArray = IOUtils.toByteArray(new FileInputStream(new File(filepath + filename)));
			TenantAPIAccessor.getCommandAPI(apiSession).addDependency(filename, byteArray);
			TenantAPIAccessor.getCommandAPI(apiSession).register(ProcessStarterCommand.NAME, ProcessStarterCommand.SUMMARY, ProcessStarterCommand.class.getName());
		}	

		// Run a test
		if(test) {
			Map<String , Serializable> commandParameters = new HashMap<String ,Serializable>();
			commandParameters.put(ProcessStarterCommand.STARTED_BY, 0L);
			commandParameters.put(ProcessStarterCommand.PROCESS_DEFINITION_ID , 6564783269546221966L);
			commandParameters.put(ProcessStarterCommand.OPERATIONS, new ArrayList<Operation>());
			commandParameters.put(ProcessStarterCommand.CONTEXT, new HashMap<String, Serializable>());
			commandParameters.put(ProcessStarterCommand.PROCESS_CONTRACT_INPUTS, new HashMap<String, Serializable>());
			commandParameters.put(ProcessStarterCommand.ACTIVITY_NAMES, new ArrayList<String>());
			System.out.print("Case has been started: " + ((ProcessInstance)TenantAPIAccessor.getCommandAPI(apiSession).execute(ProcessStarterCommand.NAME, commandParameters)).getId());
		}
	}
}