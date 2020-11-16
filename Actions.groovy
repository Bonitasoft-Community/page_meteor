

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Runtime;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;


import org.bonitasoft.web.extension.page.PageContext;
import org.bonitasoft.web.extension.page.PageController;
import org.bonitasoft.web.extension.page.PageResourceProvider;

import org.bonitasoft.engine.api.BusinessDataAPI;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;


import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;

import org.bonitasoft.properties.BonitaProperties;

import org.bonitasoft.command.BonitaCommandDeployment.DeployStatus;

import org.bonitasoft.meteor.MeteorAPI;
import org.bonitasoft.meteor.MeteorAPI.StartParameters;
import org.bonitasoft.meteor.MeteorAPI.StatusParameters;
import org.bonitasoft.meteor.scenario.process.MeteorScenarioProcess.ListProcessParameter;

import org.bonitasoft.meteor.scenario.experience.MeteorScenarioExperience.MeteorExperienceParameter;
import org.bonitasoft.meteor.scenario.experience.MeteorScenarioExperience;

import org.bonitasoft.meteor.MeteorDAO;
import org.bonitasoft.meteor.MeteorDAO.StatusDAO;

import org.bonitasoft.meteor.cmd.CmdMeteor;
import org.bonitasoft.meteor.cmd.CmdMeteor.VERBE

public class Actions {

    private static Logger logger= Logger.getLogger("org.bonitasoft.custompage.meteor.groovy");


    public static Index.ActionAnswer doAction(HttpServletRequest request, String paramJsonSt, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {

        logger.fine("#### MeteorCustompage:Actions ");
        Index.ActionAnswer actionAnswer = new Index.ActionAnswer();
        long timeBegin= System.currentTimeMillis();

        try {
            String action=request.getParameter("action");
            logger.fine("#### MeteorCustompage:Actions  action is["+action+"] !");
            if (action==null || action.length()==0 ) {
                actionAnswer.isManaged=false;
                logger.fine("#### MeteorCustompage:Actions END No Actions");
                return actionAnswer;
            }
            actionAnswer.isManaged=true;

            Long staticInformation=null;

            APISession apiSession = pageContext.getApiSession()
            HttpSession httpSession = request.getSession() ;


            // httpSession.setAttribute("MeteorAPI", null);

            MeteorAPI meteorAPI = MeteorAPI.getMeteorAPI( httpSession );
            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
            IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(apiSession);
            CommandAPI commandAPI = TenantAPIAccessor.getCommandAPI(apiSession);

            List<BEvent> listEvents=new ArrayList<BEvent>();


            if ("ping".equals(action))
            {
                HttpSession session = request.getSession();
                staticInformation = session.getAttribute("ticket");
                if ( staticInformation == null)
                {
                    staticInformation = Long.valueOf( System.currentTimeMillis());
                    session.setAttribute("ticket", staticInformation);

                }
                actionAnswer.responseMap.put("ticket", staticInformation);

            }

            else if ("collectProcesses".equals(action))
            {
                //Make sure no action is executed if the CSRF protection is active and the request header is invalid
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    actionAnswer.isResponseMap=false;
                    return actionAnswer; 
                }
    
                ListProcessParameter listProcessParameter = ListProcessParameter.getInstanceFromJsonSt( paramJsonSt );

                actionAnswer.setResponse( meteorAPI.getListProcesses( listProcessParameter, processAPI));
            }

            else if ("addCasesId".equals(action))
            {
                //Make sure no action is executed if the CSRF protection is active and the request header is invalid
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    actionAnswer.isResponseMap=false;
                    return actionAnswer; 
                }
    
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );

                MeteorExperienceParameter meteorExperienceParameter = MeteorExperienceParameter.getInstanceFromJsonSt( accumulateJson );

                actionAnswer.setResponse( meteorAPI.experienceAction( meteorExperienceParameter, processAPI, identityAPI));

            }
            /** POST is too big, so use the collect_reset, [collect_add] * ,  and then start to do the same as start */
            else if ("collect_reset".equals(action))
            {
                httpSession.setAttribute("accumulate", "" );
                actionAnswer.responseMap.put("status", "ok");

            }
            else if ("collect_add".equals(action))
            {
                String paramJsonPartial = request.getParameter("paramjsonpartial");
                logger.fine("collect_add paramJsonPartial=["+paramJsonPartial+"] json=["+paramJsonSt+"]");

                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                accumulateJson+=paramJsonSt;
                httpSession.setAttribute("accumulate", accumulateJson );
                actionAnswer.responseMap.put("status", "ok");

            }

            else if ("start".equals(action))
            {
                //Make sure no action is executed if the CSRF protection is active and the request header is invalid
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    actionAnswer.isResponseMap=false;
                    return actionAnswer; 
                }

                String accumulateJson = (String) httpSession.getAttribute("accumulate" );

                start( accumulateJson,  listEvents, actionAnswer.responseMap, meteorAPI, processAPI, commandAPI, pageResourceProvider, apiSession.getTenantId()  );

            }
            else if ("loadandstart".equals(action))
            {
                //Make sure no action is executed if the CSRF protection is active and the request header is invalid
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    actionAnswer.isResponseMap=false;
                    return actionAnswer;
                }

                String name="";
                logger.fine("loadandstart paramJson=["+paramJsonSt+"]");

                if (paramJsonSt!=null && paramJsonSt.trim().length()>0)
                {
                    final Object jsonObject = JSONValue.parse(paramJsonSt);
                    name= jsonObject.get("confname");
                }
                else
                {
                    name= request.getParameter("scenario");
                }

                MeteorDAO meteorDAO = MeteorDAO.getInstance();
                MeteorDAO.StatusDAO statusDao= meteorDAO.load( name, apiSession.getTenantId());

                actionAnswer.responseMap.put("description",statusDao.configuration.description );
                actionAnswer.responseMap.put("config", JSONValue.parse(statusDao.configuration.content) );
                String accumulateJson=statusDao.configuration.content;
                actionAnswer.responseMap.put("listeventsconfig", BEventFactory.getHtml(statusDao.listEvents));

                if (accumulateJson!=null)
                    start( accumulateJson,  listEvents, actionAnswer.responseMap, meteorAPI, processAPI, commandAPI, pageResourceProvider, apiSession.getTenantId() );

            }
            else if ("startfromscenarioname".equals(action))
            {    
                //Make sure no action is executed if the CSRF protection is active and the request header is invalid
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    actionAnswer.isResponseMap=false;
                    return actionAnswer;
                }

                String name= request.getParameter("scenario");     
                startFromScenarioName( name, listEvents, actionAnswer.responseMap, meteorAPI, processAPI, commandAPI, pageResourceProvider, apiSession.getTenantId() )
                
            }
            else if ("status".equals(action))
            {
                //Make sure no action is executed if the CSRF protection is active and the request header is invalid
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    actionAnswer.isResponseMap=false;
                    return actionAnswer;
                }

                StatusParameters statusParameters;
                if (paramJsonSt!=null && paramJsonSt.trim().length()>0)
                {
                    statusParameters=StatusParameters.getInstanceFromJsonSt( paramJsonSt );
                }
                else
                {
                    String simulationId= request.getParameter("simulationid");

                    statusParameters=StatusParameters.getInstanceFromSimulationId( simulationId );
                }
                actionAnswer.setResponse( meteorAPI.getStatus(statusParameters, processAPI, commandAPI, apiSession.getTenantId()) );

            }
            // ------- init
            else if ("initpage".equals(action))
            {
                //Make sure no action is executed if the CSRF protection is active and the request header is invalid
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    actionAnswer.isResponseMap=false;
                    return actionAnswer;
                }
                actionAnswer.setResponse( meteorAPI.startup( pageResourceProvider.getPageDirectory(), commandAPI, null, apiSession.getTenantId() ));
                    
              
            }
            // ---------- config
            else if ("saveconfig".equals(action))
            {
                //Make sure no action is executed if the CSRF protection is active and the request header is invalid
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    actionAnswer.isResponseMap=false;
                    return actionAnswer;
                }

                HttpSession session = request.getSession();
                String accumumlateJson = (String) session.getAttribute("accumulate" );
                // then create a big JSON value


                // get the name
                final Object jsonObject = JSONValue.parse(paramJsonSt);
                String name 		= jsonObject.get("scenarioname");
                String description  = jsonObject.get("confdescription");
                logger.fine("BonitaProperties.saveConfig name=["+name+"] description ["+description+"]" );


                MeteorDAO meteorDAO = MeteorDAO.getInstance();
                MeteorDAO.Configuration configuration = new MeteorDAO.Configuration();
                configuration.name=name;
                configuration.description = description;
                configuration.content=accumumlateJson;
                MeteorDAO.StatusDAO statusDao= meteorDAO.save( configuration, true,  apiSession.getTenantId());
                actionAnswer.responseMap.put("listeventsconfig",  BEventFactory.getHtml( statusDao.listEvents));
                actionAnswer.responseMap.put("configList", statusDao.listNamesAllConfigurations);
            }
            else if ("loadconfig".equals(action))
            {
                //Make sure no action is executed if the CSRF protection is active and the request header is invalid
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    actionAnswer.isResponseMap=false;
                    return actionAnswer;
                }

                final Object jsonObject = JSONValue.parse(paramJsonSt);
                String name= jsonObject.get("scenarioname");

                MeteorDAO meteorDAO = MeteorDAO.getInstance();
                MeteorDAO.StatusDAO statusDao= meteorDAO.load( name, apiSession.getTenantId());

                actionAnswer.responseMap.put("description",statusDao.configuration.description );
                actionAnswer.responseMap.put("config", JSONValue.parse(statusDao.configuration.content) );
                actionAnswer.responseMap.put("listeventsconfig", BEventFactory.getHtml(statusDao.listEvents));

                // loadConfig(name, listEvents, answer, pageResourceProvider, apiSession);

            }
            else if ("deleteconfig".equals(action))
            {
                //Make sure no action is executed if the CSRF protection is active and the request header is invalid
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    actionAnswer.isResponseMap=false;
                    return actionAnswer;
                }

                final Object jsonObject = JSONValue.parse(paramJsonSt);
                String name= jsonObject.get("scenarioname");
                logger.fine("BonitaProperties.loadConfig name=["+name+"]" );
                // Load is

                MeteorDAO meteorDAO = MeteorDAO.getInstance();
                MeteorDAO.StatusDAO statusDao= meteorDAO.delete( name, true, apiSession.getTenantId());

                actionAnswer.responseMap.put("listeventsconfig", BEventFactory.getHtml(statusDao.listEvents));
                actionAnswer.responseMap.put("configList", statusDao.listNamesAllConfigurations);


                /*BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider, apiSession.getTenantId() );
                 listEventsConfig.addAll( bonitaProperties.load() );
                 logger.info("BonitaProperties.saveConfig: loadproperties done, events = "+listEventsConfig.size() );
                 bonitaProperties.remove( "config_"+name );
                 listEventsConfig.addAll( bonitaProperties.store() );
                 if (! BEventFactory.isError( listEventsConfig ))
                 listEventsConfig.add( new BEvent(EventConfigurationRemoved, "Configuration "+name));
                 logger.info("BonitaProperties.saveConfig store properties  done, events = "+listEvents.size() );
                 answer = new HashMap<String,Object>();
                 answer.put("listeventsconfig", BEventFactory.getHtml(listEventsConfig));
                 answer.put("configList", getListConfig( bonitaProperties ));
                 */

            } else if ("exportconf".equals(action))
            {
                final Object jsonObject = JSONValue.parse(paramJsonSt);
                List<String> listconfname= jsonObject.get("listscenarioname");

                MeteorDAO meteorDAO = MeteorDAO.getInstance();
                MeteorDAO.StatusDAO statusDao= meteorDAO.exportConfs( listconfname, apiSession.getUserName(),  apiSession.getTenantId());

                // then add the name and the correct content type
                response.addHeader("content-disposition", "attachment; filename="+statusDao.containerName);
                response.addHeader("content-type", "application/zip");
                
                
                File file = new File("c:/temp/meteor.zip");
                OutputStream fop = new FileOutputStream(file);
                statusDao.containerZip.writeTo( fop );
                fop.flush();
                fop.close();
                
                
                OutputStream output = response.getOutputStream();

                if (statusDao.containerZip!=null)
                    statusDao.containerZip.writeTo( output );

                output.flush();
                output.close();
                actionAnswer.isResponseMap=false;
                return actionAnswer;
            } else if ("importconfs".equals(action))
            {
                //Make sure no action is executed if the CSRF protection is active and the request header is invalid
                if (! TokenValidator.checkCSRFToken(request, response)) {
                    actionAnswer.isResponseMap=false;
                    return actionAnswer;
                }

                String filename= request.getParameter("filename");
                MeteorDAO meteorDAO = MeteorDAO.getInstance();
                MeteorDAO.StatusDAO statusDao= meteorDAO.importConfs( filename, true, pageResourceProvider.getPageDirectory(),  apiSession.getTenantId());
                actionAnswer.responseMap.put("listeventsconfig", BEventFactory.getHtml(statusDao.listEvents));
                actionAnswer.responseMap.put("configList", statusDao.listNamesAllConfigurations);
                if (statusDao.configuration!=null)
                {
                    actionAnswer.responseMap.put("description",statusDao.configuration.description );
                    actionAnswer.responseMap.put("config", JSONValue.parse(statusDao.configuration.content) );
                    actionAnswer.responseMap.put("name", statusDao.configuration.name);
                }

            }

            return actionAnswer;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("#### MeteorCustompage:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            actionAnswer.isResponseMap=true;
            actionAnswer.responseMap.put("Error", "MeteorCustompage:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            return actionAnswer;
        }
    }



    /*
     * 
     */
    public static void start( String accumulateJson, List<BEvent> listEvents, Map<String,Object> answer, MeteorAPI meteorAPI, ProcessAPI processAPI, CommandAPI commandAPI, PageResourceProvider pageResourceProvider,long tenantId )
    {
        // so no need to have a force deploy here.
        DeployStatus deployStatus= meteorAPI.deployCommand( pageResourceProvider.getPageDirectory(),  commandAPI, null, tenantId);

        if (BEventFactory.isError( deployStatus.listEvents ))
        {
            listEvents.addAll( deployStatus.listEvents  );
        }

        // logger.info("Json=["+paramJsonSt+"]");
        StartParameters startParameters;
        logger.fine(" We get a LIST JSON size=("+accumulateJson.length()+" - first value =["+ (accumulateJson==null ? null :(accumulateJson.length()>100 ? accumulateJson.substring(0,100) :accumulateJson))+ "]");
        startParameters = StartParameters.getInstanceFromJsonSt( accumulateJson );
        
        answer.putAll( meteorAPI.start(startParameters, processAPI, commandAPI,tenantId));

    }
    
    /**
     * 
     * @param name
     * @param listEvents
     * @param answer
     * @param meteorAPI
     * @param processAPI
     * @param commandAPI
     * @param pageResourceProvider
     * @param tenantId
     */
    public static void startFromScenarioName(String name, List<BEvent> listEvents, Map<String,Object> answer, MeteorAPI meteorAPI, ProcessAPI processAPI, CommandAPI commandAPI, PageResourceProvider pageResourceProvider,long tenantId )
    {
        // so no need to have a force deploy here.
        DeployStatus deployStatus= meteorAPI.deployCommand( pageResourceProvider.getPageDirectory(),  commandAPI, null, tenantId);

        if (BEventFactory.isError( deployStatus.listEvents ))
        {
            listEvents.addAll( deployStatus.listEvents  );
        }
        logger.fine(" StartFromName["+ name + "]");
        answer.putAll( meteorAPI.startFromScenarioName(name, processAPI, commandAPI,tenantId));

    }
}
