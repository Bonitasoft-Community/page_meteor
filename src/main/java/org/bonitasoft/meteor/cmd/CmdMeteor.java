package org.bonitasoft.meteor.cmd;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import org.bonitasoft.command.BonitaCommandApiAccessor;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.connector.ConnectorAPIAccessorImpl;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.meteor.MeteorAPI.StartParameters;
import org.bonitasoft.meteor.MeteorAPI.StatusParameters;
import org.bonitasoft.meteor.MeteorDAO;
import org.bonitasoft.meteor.MeteorOperation;

public class CmdMeteor extends BonitaCommandApiAccessor {

    public static String cstParamPing = "ping";

    static Logger logger = Logger.getLogger(CmdMeteor.class.getName());
    private static String logHeader = "CommandMeteor ~~~~~~ ";

    /** Start From Name : to be call by a simple command, only parameters is the configuration name */
    public enum VERBE {
        START, PING, STATUS, ABORT, STARTFROMSCENARIONAME
    };

    // public final static String cstParamCommandName = "CommandName";
    // public final static String cstParamCommandNameStart = "START";
    public final static String cstParamCommandNameStartParams = "startparameters";
    public final static String cstParamCommandNameStatusParams = "statusparameters";
    public final static String cstParamCommandNameScenarioName = "scenarioname";
    
    
    // public final static String cstParamCommandNamePing = "PING";
    // public final static String cstParamCommandNameStatus = "STATUS";
    // public final static String cstParamCommandNameAbort = "ABORT";

    public final static String cstParamTenantId = "tenantid";
    public final static String cstParamStartParameter = "startparameters";

    /**
     * chaque start execution receive a uniq Id. Then, STATUS and ABORT use this
     * id.
     */
    public final static String cstParamCommandExecId = "ExecId";

    public final static String cstParamResultListEventsSt = "listevents";
    public final static String cstParamResultSimulationId = "simulationid";

    /**
     * Change the time of an timer. parameters are tenantid : optional, 1 per
     * default activityid : name of the activity ELSE the activityName +
     * processinstanceid shoud be provided activityname (if not activityid is
     * given) processinstanceid processinstanceid of the case to change
     * timername the name of the boundary timer newtimerdate the new date of
     * this process intance. Format is yyyyMMdd HH:MM:ss
     */
    @Override
    public ExecuteAnswer executeCommandApiAccessor(ExecuteParameters executeParameters, APIAccessor apiAccessor) {


        ExecuteAnswer executeAnswer = new ExecuteAnswer();

        // keep HashMap to return a Serializable

        logger.info(logHeader+"Start command CmdMeteor");

        long tenantId = executeParameters.tenantId;

        final ConnectorAPIAccessorImpl connectorAccessorAPI = new ConnectorAPIAccessorImpl(tenantId);
        //VERBE verbEnum = null;
        // verbEnum = VERBE.valueOf(executeParameters.verb);

        final String commandName = executeParameters.verb;
        if (VERBE.PING.toString().equals(commandName)) {
            logger.info(logHeader+"COMMANDMETEOR.Ping ");

            final SimpleDateFormat sdf = new SimpleDateFormat("dd/mm/yyyy HH:MM:SS");
            executeAnswer.result.put("pingstatus", "Ping is ok at " + sdf.format(new Date()));

        } else if (VERBE.START.toString().equals(commandName)) {

            @SuppressWarnings("unchecked")
            final StartParameters startParameters = StartParameters.getInstanceFromJsonList((ArrayList<String>) executeParameters.parametersCommand.get(cstParamCommandNameStartParams));
            logger.fine(logHeader+"COMMANDMETEOR.Start params[" + startParameters.toString() + "]");
            startParameters.tenantId = tenantId;
            executeAnswer.result = MeteorOperation.start(startParameters, connectorAccessorAPI).getMap();

        } else if (VERBE.STATUS.toString().equals(commandName)) {
            logger.fine(logHeader+"COMMANDMETEOR.Status ");
            final StatusParameters statusParameters = StatusParameters.getInstanceFromJsonSt((String) executeParameters.parametersCommand.get(cstParamCommandNameStatusParams));

            executeAnswer.result = MeteorOperation.getStatus(statusParameters, connectorAccessorAPI).getMap();
        
        }  else if (VERBE.STARTFROMSCENARIONAME.toString().equals(commandName)) {
            String name = (String) executeParameters.parametersCommand.get( cstParamCommandNameScenarioName );
            MeteorDAO meteorDAO = MeteorDAO.getInstance();
            MeteorDAO.StatusDAO statusDao= meteorDAO.load( name, tenantId);
            if (BEventFactory.isError( statusDao.listEvents) ) {
                executeAnswer.result.put(CmdMeteor.cstParamResultListEventsSt, BEventFactory.getHtml(statusDao.listEvents));    
            }
            else {
                String accumulateJson=statusDao.configuration.content;
                final StartParameters startParameters = StartParameters.getInstanceFromJsonSt( accumulateJson );
                executeAnswer.result = MeteorOperation.start(startParameters, connectorAccessorAPI).getMap();
            }
            
        } else if (VERBE.ABORT.toString().equals(commandName)) {
            logger.fine(logHeader+"COMMANDMETEOR.Abort ");

        } else {

            executeAnswer.result.put("status", "Unknow command [" + commandName + "]");
        }
        return executeAnswer;
    }
    @Override
    public ExecuteAnswer afterDeployment(ExecuteParameters executeParameters, TenantServiceAccessor serviceAccessor) {
        ExecuteAnswer executeAnswer = new ExecuteAnswer();
        executeAnswer.listEvents = MeteorDAO.getInstance().checkAndUpdateEnvironment(executeParameters.tenantId);
        executeAnswer.result.put("status", BEventFactory.isError(executeAnswer.listEvents) ? "FAIL" : "OK");
        return executeAnswer;
    }

    /*
     * *************************************************************************
     * *******
     */
    /*                                                                                  */
    /* Command management */
    /*                                                                                  */
    /*                                                                                  */
    /*
     * *************************************************************************
     * *******
     */
    public final static String CSTCOMMANDNAME = "meteorcmd";
    public final static String CSTCOMMANDDESCRIPTION = "Run the meteor robots, to creates cases / execute task";

    // public static String jarName = "CustomPageMeteor-1.0.0.jar";

   

}
