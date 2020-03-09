package org.bonitasoft.meteor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.command.BonitaCommandDeployment;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.meteor.MeteorAPI.StatusParameters;
import org.bonitasoft.meteor.cmd.CmdMeteor;

/**
 * ********************************************************************************
 * * This class is COPY / PAST form the class in the meteor project
 * It's not possible in Bonita to have the same librairy in two different page, because the dependencies are GLOBAL
 * So, if truckMilk use a MeteorLibrary 1.2  and Meteor want to use a MeteorLibrairy 1.3, Meteor may not be able to  upload the librairy
 * Or opposite, meteor is already deployed with MeteorLibrary 1.3, then TruckMilk is install, and redeploy MeteorLibrary 1.2 ...
 * 
 * So to avoid this, this class is a simple copy paste
 * 
*/
public class MeteorClientAPI {
    private final static String logHeader = "MeteorClientAPI ~~ ";
    private static Logger logger = Logger.getLogger(MeteorClientAPI.class.getName());

    /**
     *  
     */
    public final static String cstCommandName = "meteorcmd";
    public enum VERBE {
        START, PING, STATUS, ABORT, STARTFROMSCENARIONAME
    };
    
    public Map<String, Object> startFromScenarioName(String name, ProcessAPI processAPI, CommandAPI commandAPI, long tenantId) {
        logger.info(logHeader + "~~~~~~~~~~ MeteorAPI.startFromName() name=" + name);
        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(CmdMeteor.CSTCOMMANDNAME);
        Map<String, Object> resultCommand = new HashMap<String, Object>();

        final HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(CmdMeteor.cstParamCommandNameScenarioName, name);
        resultCommand = bonitaCommand.callCommand(CmdMeteor.VERBE.STARTFROMSCENARIONAME.toString(), parameters, tenantId, commandAPI);
        logger.info(logHeader + "~~~~~~~~~~ MeteorAPI.startFromName() : END " + resultCommand);
        return resultCommand;
        
    }
    
    /**
     * getStatus
     */
    public Map<String, Object> getStatus(final StatusParameters statusSimulation, final ProcessAPI processAPI, final CommandAPI commandAPI, long tenantId) {

        logger.fine(logHeader + "MeteorAPI.getStatus()");
        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(CmdMeteor.CSTCOMMANDNAME);
        Map<String, Object> resultCommand = new HashMap<String, Object>();

        final HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(CmdMeteor.cstParamCommandNameStatusParams, statusSimulation.getJson());

        // parameters.put(CmdMeteor.cstParamCommandName, CmdMeteor.cstParamCommandNameStatus);

        logger.fine(logHeader + "~~~~~~~~~~ MeteorAPI.start() Call Command ["+CmdMeteor.VERBE.STATUS+"]");
        resultCommand = bonitaCommand.callCommand(CmdMeteor.VERBE.STATUS.toString(), parameters, tenantId, commandAPI);

        return resultCommand;
    }

}
