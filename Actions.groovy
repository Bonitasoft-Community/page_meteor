

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

import org.bonitasoft.console.common.server.page.PageContext
import org.bonitasoft.console.common.server.page.PageController
import org.bonitasoft.console.common.server.page.PageResourceProvider
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

import org.bonitasoft.ext.properties.BonitaProperties;

import org.bonitasoft.meteor.MeteorAPI;
import org.bonitasoft.meteor.MeteorAPI.StartParameters;
import org.bonitasoft.meteor.MeteorAPI.StatusParameters;
import  org.bonitasoft.meteor.scenario.process.MeteorMain.ListProcessParameter;

import org.bonitasoft.meteor.MeteorDAO;
import org.bonitasoft.meteor.MeteorDAO.StatusDAO;

import org.bonitasoft.meteor.cmd.CmdMeteor;

public class Actions {

	private static Logger logger= Logger.getLogger("org.bonitasoft.custompage.longboard.groovy");
	
	
	public static Index.ActionAnswer doAction(HttpServletRequest request, String paramJsonSt, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
				
		logger.info("#### MeteorCustompage:Actions start");
		Index.ActionAnswer actionAnswer = new Index.ActionAnswer();	
		long timeBegin= System.currentTimeMillis();
		
		try {
			String action=request.getParameter("action");
			logger.info("#### MeteorCustompage:Actions  action is["+action+"] !");
			if (action==null || action.length()==0 )
			{
				actionAnswer.isManaged=false;
				logger.info("#### MeteorCustompage:Actions END No Actions");
				return actionAnswer;
			}
			actionAnswer.isManaged=true;
			
			Long staticInformation=null;
				
			APISession apiSession = pageContext.getApiSession()
			HttpSession httpSession = request.getSession() ;
						
						
			// httpSession.setAttribute("MeteorAPI", null);
			
		    MeteorAPI meteorAPI = MeteorAPI.getMeteorAPI( httpSession );
			ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
			IdentityAPI identityApi = TenantAPIAccessor.getIdentityAPI(apiSession);
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
            
			else if ("getListArtefacts".equals(action))
			{
				ListProcessParameter listProcessParameter = ListProcessParameter.getInstanceFromJsonSt( paramJsonSt );
				
				 actionAnswer.setResponse( meteorAPI.getListProcesses( listProcessParameter, processAPI));
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
            	logger.info("collect_add paramJsonPartial=["+paramJsonPartial+"] json=["+paramJsonSt+"]");
				
            	String accumulateJson = (String) httpSession.getAttribute("accumulate" );
            	accumulateJson+=paramJsonSt;
            	httpSession.setAttribute("accumulate", accumulateJson );
            	actionAnswer.responseMap.put("status", "ok");

            }
          
			else if ("start".equals(action))
			{
				String accumulateJson = (String) httpSession.getAttribute("accumulate" );
			    start( accumulateJson,  listEvents, actionAnswer.responseMap, meteorAPI, processAPI, commandAPI, pageResourceProvider );

			} 
			else if ("loadandstart".equals(action))
			{
				String name="";
				logger.info("loadandstart paramJson=["+paramJsonSt+"]");

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
            	MeteorDAO.StatusDAO statusDao= meteorDAO.load( name, pageResourceProvider.getPageName(), apiSession.getTenantId());
		            	
            	actionAnswer.responseMap.put("description",statusDao.configuration.description ); 
            	actionAnswer.responseMap.put("config", JSONValue.parse(statusDao.configuration.content) );
            	String accumulateJson=statusDao.configuration.content;
            	actionAnswer.responseMap.put("listeventsconfig", BEventFactory.getHtml(statusDao.listEvents));
		       
		               	
		               	
				if (accumulateJson!=null)
					start( accumulateJson,  listEvents, actionAnswer.responseMap, meteorAPI, processAPI, commandAPI, pageResourceProvider );
				
			}

			else if ("status".equals(action))
            {
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
				actionAnswer.setResponse( meteorAPI.getStatus(statusParameters, processAPI, commandAPI) );
                
            }
            // ------- init 
            else if ("initpage".equals(action))
            {
                // first process
                ListProcessParameter listProcessParameter = ListProcessParameter.getInstanceFromJsonSt( paramJsonSt );
                actionAnswer.setResponse( meteorAPI.getListProcesses( listProcessParameter, processAPI));

        		// second configuration
                MeteorDAO meteorDAO = MeteorDAO.getInstance();
            	MeteorDAO.StatusDAO statusDao = meteorDAO.getListNames(pageResourceProvider.getPageName(), apiSession.getTenantId()  ) ;
            	actionAnswer.responseMap.put("listeventsconfig",  BEventFactory.getHtml( statusDao.listEvents));
            	actionAnswer.responseMap.put("configList", statusDao.listNamesAllConfigurations);
                
             	/*
                BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider, apiSession.getTenantId() );
                listEvents.addAll( bonitaProperties.load() );
                logger.info("BonitaProperties.saveConfig: loadproperties done, events = "+listEvents.size() );
                answer = new HashMap<String,Object>()
                answer.put("configList", getListConfig( bonitaProperties ));
                */
            }
            // ---------- config
            else if ("saveconfig".equals(action))
            {
            	HttpSession session = request.getSession();
                String accumumlateJson = (String) session.getAttribute("accumulate" );
                // then create a big JSON value
                
                
                // get the name
                final Object jsonObject = JSONValue.parse(paramJsonSt);
                String name 		= jsonObject.get("confname");
                String description  = jsonObject.get("confdescription");
                logger.info("BonitaProperties.saveConfig name=["+name+"] description ["+description+"]" );
                
                
                MeteorDAO meteorDAO = MeteorDAO.getInstance();
                MeteorDAO.Configuration configuration = new MeteorDAO.Configuration();
                configuration.name=name;
                configuration.description = description;
                configuration.content=accumumlateJson;
                MeteorDAO.StatusDAO statusDao= meteorDAO.save( configuration, true, pageResourceProvider.getPageName(), apiSession.getTenantId());
                actionAnswer.responseMap.put("listeventsconfig",  BEventFactory.getHtml( statusDao.listEvents));
                actionAnswer.responseMap.put("configList", statusDao.listNamesAllConfigurations);
            }
            else if ("loadconfig".equals(action))
            {
            	final Object jsonObject = JSONValue.parse(paramJsonSt);
            	String name= jsonObject.get("confname");
                        
            	MeteorDAO meteorDAO = MeteorDAO.getInstance();
            	MeteorDAO.StatusDAO statusDao= meteorDAO.load( name, pageResourceProvider.getPageName(), apiSession.getTenantId());
            	
            	actionAnswer.responseMap.put("description",statusDao.configuration.description ); 
            	actionAnswer.responseMap.put("config", JSONValue.parse(statusDao.configuration.content) );
            	actionAnswer.responseMap.put("listeventsconfig", BEventFactory.getHtml(statusDao.listEvents));
             
            	// loadConfig(name, listEvents, answer, pageResourceProvider, apiSession);

            }
            else if ("deleteconfig".equals(action))
            {
            	final Object jsonObject = JSONValue.parse(paramJsonSt);
                String name= jsonObject.get("confname");
                logger.info("BonitaProperties.loadConfig name=["+name+"]" );
                // Load is
                
                MeteorDAO meteorDAO = MeteorDAO.getInstance();
            	MeteorDAO.StatusDAO statusDao= meteorDAO.delete( name, true, pageResourceProvider.getPageName(), apiSession.getTenantId());
              
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
            	List<String> listconfname= jsonObject.get("listconfname");
            	
                MeteorDAO meteorDAO = MeteorDAO.getInstance();
            	MeteorDAO.StatusDAO statusDao= meteorDAO.exportConfs( listconfname, apiSession.getUserName(), pageResourceProvider.getPageName(), apiSession.getTenantId());
            
			    // then add the name and the correct content type 
                response.addHeader("content-disposition", "attachment; filename="+statusDao.containerName);
                response.addHeader("content-type", "application/zip");
            	OutputStream output = response.getOutputStream();

            	if (statusDao.containerZip!=null)
            		statusDao.containerZip.writeTo( output );
            	
            	output.flush();
            	output.close();
            	actionAnswer.isResponseMap=false;
                return actionAnswer;
			} else if ("importconfs".equals(action))
			{
				String filename= request.getParameter("filename");
                MeteorDAO meteorDAO = MeteorDAO.getInstance();
            	MeteorDAO.StatusDAO statusDao= meteorDAO.importConfs( filename, true, pageResourceProvider.getPageDirectory(), pageResourceProvider.getPageName(), apiSession.getTenantId());
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
	public static void start(String accumulateJson, List<BEvent> listEvents, Map<String,Object> answer, MeteorAPI meteorAPI, ProcessAPI processAPI, CommandAPI commandAPI, PageResourceProvider pageResourceProvider )
	{

		/*
        List jarDependencies = new ArrayList<>();
        jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "bdm-jpql-query-executor-command-1.0.jar", pageResourceProvider.getResourceAsStream("lib/bdm-jpql-query-executor-command-1.0.jar")));
        jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "process-starter-command-1.0.jar", pageResourceProvider.getResourceAsStream("lib/process-starter-command-1.0.jar")));
        jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "scenario-utils-2.0.jar", pageResourceProvider.getResourceAsStream("lib/scenario-utils-2.0.jar")));
        jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "CustomPageMeteor-1.0.0.jar", pageResourceProvider.getResourceAsStream("lib/CustomPageMeteor-1.0.0.jar")));
        jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "bonita-event-1.0.0.jar", pageResourceProvider.getResourceAsStream("lib/bonita-event-1.0.0.jar")));
        */
        File fileJar = pageResourceProvider.getResourceAsFile("lib/CustomPageMeteor-1.0.0.jar");
        long timeFile=fileJar.lastModified();
        
        // so no need to have a force deploy here.
        List<BEvent> listEventsDeploy = meteorAPI.deployCommand(false, String.valueOf(timeFile), pageResourceProvider.getPageDirectory(),  commandAPI, null);
        if (BEventFactory.isError( listEventsDeploy))
        {
            listEvents.addAll(listEventsDeploy );
        }
        
        listEventsDeploy.addAll( meteorAPI.deployCommandGroovyScenario(true, "1.0", new ArrayList<>(), commandAPI, null));
        
        
		// logger.info("Json=["+paramJsonSt+"]");
          StartParameters startParameters; 
          logger.info(" We get a LIST JSON size=("+accumulateJson.length()+" - first value =["+ (accumulateJson==null ? null :(accumulateJson.length()>100 ? accumulateJson.substring(0,100) :accumulateJson))+ "]");
          startParameters = StartParameters.getInstanceFromJsonSt( accumulateJson );
       
		answer.putAll( meteorAPI.start(startParameters, processAPI, commandAPI));

	}
	
}
