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
import org.bonitasoft.meteor.MeteorAPI.StatusParameters;
import org.bonitasoft.meteor.MeteorDAO;
import org.bonitasoft.meteor.MeteorOperation;
import org.bonitasoft.meteor.MeteorOperation.MeteorResult;
import org.bonitasoft.meteor.MeteorStartParameters;

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
    public final static String CSTPARAM_COMMANDNAMESTARTPARAMS = "startparameters";
    public final static String CSTPARAM_COMMANDNAMESTATUSPARAMS = "statusparameters";
    public final static String CSTPARAM_COMMANDNAMESCENARIONAME = "scenarioname";
    
    
    // public final static String cstParamCommandNamePing = "PING";
    // public final static String cstParamCommandNameStatus = "STATUS";
    // public final static String cstParamCommandNameAbort = "ABORT";

    public final static String CSTPARAM_TENANTID = "tenantid";
    public final static String CSTPARAM_STARTPARAMETER = "startparameters";

    /**
     * each start execution receive a unique Id. Then, STATUS and ABORT use this
     * id.
     */
    public final static String CSTPARAM_COMMANDEXECID = "ExecId";

    public final static String CSTPARAM_RESULTLISTEVENTSST = "listevents";
    public final static String CSTPARAM_RESULTLOG = "log";
    public final static String CSTPARAM_RESULTSIMULATIONID = "simulationid";

    
    @Override
    public ExecuteAnswer executeCommandApiAccessor(ExecuteParameters executeParameters, APIAccessor apiAccessor, TenantServiceAccessor serviceAccessor) {
     

        ExecuteAnswer executeAnswer = new ExecuteAnswer();

        // keep HashMap to return a Serializable

        logger.info(logHeader+"Start Command CmdMeteor");

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
            final MeteorStartParameters startParameters = MeteorStartParameters.getInstanceFromJsonSt( (String) executeParameters.parametersCommand.get(CSTPARAM_COMMANDNAMESTARTPARAMS));
            logger.fine(logHeader+"COMMANDMETEOR.Start params[" + startParameters.toString() + "]");
            startParameters.setTenantId( tenantId );
            MeteorResult meteorResult  = MeteorOperation.start(startParameters, connectorAccessorAPI);
            executeAnswer.result = meteorResult.getMap();
            executeAnswer.listEvents = meteorResult.listEvents;

        } else if (VERBE.STATUS.toString().equals(commandName)) {
            logger.fine(logHeader+"COMMANDMETEOR.Status ");
            final StatusParameters statusParameters = StatusParameters.getInstanceFromJsonSt((String) executeParameters.parametersCommand.get(CSTPARAM_COMMANDNAMESTATUSPARAMS));

            MeteorResult meteorResult  = MeteorOperation.getStatus(statusParameters, connectorAccessorAPI);
            executeAnswer.result = meteorResult.getMap();
            executeAnswer.listEvents = meteorResult.listEvents;
        
        }  else if (VERBE.STARTFROMSCENARIONAME.toString().equals(commandName)) {
            String name = (String) executeParameters.parametersCommand.get( CSTPARAM_COMMANDNAMESCENARIONAME );
            MeteorDAO meteorDAO = MeteorDAO.getInstance();
            MeteorDAO.StatusDAO statusDao= meteorDAO.load( name, tenantId);
            if (BEventFactory.isError( statusDao.listEvents) ) {
                executeAnswer.result.put(CmdMeteor.CSTPARAM_RESULTLISTEVENTSST, BEventFactory.getHtml(statusDao.listEvents));
                executeAnswer.listEvents = statusDao.listEvents;
            }
            else {
                String accumulateJson=statusDao.configuration.content;
                final MeteorStartParameters startParameters = MeteorStartParameters.getInstanceFromJsonSt( accumulateJson );
                MeteorResult meteorResult  = MeteorOperation.start(startParameters, connectorAccessorAPI);
                executeAnswer.result = meteorResult.getMap();
                executeAnswer.listEvents = meteorResult.listEvents;
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

    @Override
    public String getName() {

        return "Meteor";
    }

  

    // public static String jarName = "CustomPageMeteor-1.0.0.jar";

   

}
