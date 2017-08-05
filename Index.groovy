
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.Runtime;

import org.json.simple.JSONObject;
import org.codehaus.groovy.tools.shell.CommandAlias;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;


import javax.naming.Context;
import javax.naming.InitialContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

import org.apache.commons.lang3.StringEscapeUtils

import org.bonitasoft.engine.identity.User;
import org.bonitasoft.console.common.server.page.PageContext
import org.bonitasoft.console.common.server.page.PageController
import org.bonitasoft.console.common.server.page.PageResourceProvider
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;

import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchResult;

import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;

import org.bonitasoft.ext.properties.BonitaProperties;

import org.bonitasoft.meteor.MeteorAccess;
import org.bonitasoft.meteor.MeteorAccess.StartParameters;
import org.bonitasoft.meteor.MeteorAccess.StatusParameters;
import org.bonitasoft.meteor.MeteorProcessDefinitionList.ListProcessParameter;

import org.bonitasoft.meteor.cmd.CmdMeteor;

public class Index implements PageController {

  private static BEvent EventConfigurationSaved = new BEvent("org.bonitasoft.custompageMeteor", 1, Level.INFO,  "Configuration saved", "The configuration is saved with sucess");
  private static BEvent EventConfigurationLoaded = new BEvent("org.bonitasoft.custompageMeteor", 2, Level.INFO, "Configuration loaded", "The configuration is loaded with sucess");
  private static BEvent EventConfigurationRemoved = new BEvent("org.bonitasoft.custompageMeteor", 3, Level.INFO, "Configuration deleted", "The configuration is deleted");

  Logger logger= Logger.getLogger("org.bonitasoft");
  
  
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
	
		
        Long staticInformation=null;
		long timeBegin= System.currentTimeMillis();
		try {
			def String indexContent;
			pageResourceProvider.getResourceAsStream("Index.groovy").withStream { InputStream s-> indexContent = s.getText() };
			response.setCharacterEncoding("UTF-8");
			PrintWriter out = response.getWriter()

			String action=request.getParameter("action");
			logger.info("###################################### action 2.1.a is["+action+"] !");
			if (action==null || action.length()==0 )
			{
				logger.severe("###################################### RUN Default !");
				
				runTheBonitaIndexDoGet( request, response,pageResourceProvider,pageContext);
				return;
			}
			String paramJson= request.getParameter("paramjson");
			
			APISession apiSession = pageContext.getApiSession()

			HttpSession httpSession = request.getSession() ;
			
			
			// httpSession.setAttribute("meteoraccess", null);
			
		    MeteorAccess meteorAccess = MeteorAccess.getMeteorAccess( httpSession );
			ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
			IdentityAPI identityApi = TenantAPIAccessor.getIdentityAPI(apiSession);
            CommandAPI commandAPI = TenantAPIAccessor.getCommandAPI(apiSession);
            
            List<BEvent> listEvents=new ArrayList<BEvent>();
            
			HashMap<String,Object> answer = null;
            if ("ping".equals(action))
            {
                HttpSession session = request.getSession();
                staticInformation = session.getAttribute("ticket");
                if ( staticInformation == null)
                {
                    staticInformation = Long.valueOf( System.currentTimeMillis());
                    session.setAttribute("ticket", staticInformation);
                    
                }
                answer = new HashMap<String,Object>()
                answer.put("ticket", staticInformation);
                
            }
            
			else if ("getListArtefacts".equals(action))
			{
				ListProcessParameter listProcessParameter = ListProcessParameter.getInstanceFromJsonSt( paramJson );
				answer = meteorAccess.getListProcesses( listProcessParameter, processAPI);
			}
            
            /** POST is too big, so use the collect_reset, [collect_add] * ,  and then start to do the same as start */
            else if ("collect_reset".equals(action))
            {
                HttpSession session = request.getSession();
                session.setAttribute("accumulate", "" );
                answer = new HashMap<String,Object>()
                answer.put("status", "ok");

            }
            else if ("collect_add".equals(action))
            {
               HttpSession session = request.getSession();
               String accumulateJson = (String) session.getAttribute("accumulate" );
               accumulateJson+=paramJson;
               session.setAttribute("accumulate", accumulateJson );
               answer = new HashMap<String,Object>()
               answer.put("status", "ok");

            }
          
			else if ("start".equals(action))
			{
				 HttpSession session = request.getSession();
			     String accumulateJson = (String) session.getAttribute("accumulate" );
			     answer = new HashMap<String,Object>();
			                   
			    start( accumulateJson,  listEvents, answer, meteorAccess, processAPI, commandAPI, pageResourceProvider );

			    //Thread.sleep(1000);
				//HashMap<String,Object> statusexecution = meteorAccess.getStatus( processAPI, commandAPI);
				//answer.putAll( statusexecution );
			} 
			else if ("loadandstart".equals(action))
			{
				answer = new HashMap<String,Object>();
		        final Object jsonObject = JSONValue.parse(paramJson);
		        String name= jsonObject.get("confname");

				String accumulateJson= loadConfig(name, listEvents, answer, pageResourceProvider, apiSession);
				if (accumulateJson!=null)
					start( accumulateJson,  listEvents, answer, meteorAccess, processAPI, commandAPI, pageResourceProvider );
				
			}
            else if ("status".equals(action))
            {
                StatusParameters statusParameters = StatusParameters.getInstanceFromJsonSt( paramJson );
                
                answer = meteorAccess.getStatus(statusParameters, processAPI, commandAPI);
                
            }
            // ------- config
            else if ("initconfig".equals(action))
            {
                BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider, apiSession.getTenantId() );
                listEvents.addAll( bonitaProperties.load() );
                logger.info("BonitaProperties.saveConfig: loadproperties done, events = "+listEvents.size() );
                answer = new HashMap<String,Object>()
                answer.put("configList", getListConfig( bonitaProperties ));
            }
            else if ("saveconfig".equals(action))
            {
            	List<BEvent> listEventsConfig = new ArrayList<BEvent>();
               
                HttpSession session = request.getSession();
                String accumumlateJson = (String) session.getAttribute("accumulate" );
                // then create a big JSON value
                
                
                // get the name
                final Object jsonObject = JSONValue.parse(paramJson);
                String name= jsonObject.get("confname");
                logger.info("BonitaProperties.saveConfig name=["+name+"]" );
                
                // save it 
                BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider, apiSession.getTenantId() );
    
                listEventsConfig.addAll( bonitaProperties.load() );
                logger.info("BonitaProperties.saveConfig: loadproperties done, events = "+listEvents.size() );
    
                bonitaProperties.setProperty( "config_"+name, accumumlateJson );
                listEventsConfig.addAll( bonitaProperties.store() );
                if (! BEventFactory.isError( listEventsConfig ))
                 	listEventsConfig.add( EventConfigurationSaved);
                 
                logger.info("BonitaProperties.saveConfig store properties  done, events = "+listEvents.size() );
                answer = new HashMap<String,Object>()
                answer.put("configList", getListConfig( bonitaProperties ));
           		answer.put("listeventsconfig",  BEventFactory.getHtml( listEventsConfig));
               	
            }
            else if ("loadconfig".equals(action))
            {
            	answer = new HashMap<String,Object>()
        	    final Object jsonObject = JSONValue.parse(paramJson);
            	String name= jsonObject.get("confname");
                        
            	loadConfig(name, listEvents, answer, pageResourceProvider, apiSession);

            }
            else if ("deleteconfig".equals(action))
            {
                List<BEvent> listEventsConfig = new ArrayList<BEvent>();
                final Object jsonObject = JSONValue.parse(paramJson);
                String name= jsonObject.get("confname");
                logger.info("BonitaProperties.loadConfig name=["+name+"]" );
                // Load is
                BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider, apiSession.getTenantId() );
    
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
               	 
            }
            
			if (answer==null)
			{
				answer = new HashMap<String,Object> ();
				answer.put("status", "Unknow command");
			}
			if (! answer.containsKey("listevents"))
				answer.put("listevents",BEventFactory.getHtml(listEvents));
            
			String jsonDetailsSt = JSONValue.toJSONString( answer );
			long timeEnd= System.currentTimeMillis();
			logger.info("###################################### EndMeteor ["+action+"] Return["+jsonDetailsSt+"] in "+(timeEnd-timeBegin)+" ms");
			
			out.write( jsonDetailsSt );
			out.flush();
			out.close();				
			
			
			return;				
			
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			logger.severe("Exception ["+e.toString()+"] at "+exceptionDetails);
		}
		
	}

    /*
     * return all the different configuration detected
     */
    private List<String> getListConfig( BonitaProperties bonitaProperties )
    {
        List<String>listConfig = new ArrayList<String>();
        for (String key : bonitaProperties.keySet() )
        {
            if (key.startsWith("config_"))
            { 
            	key=key.substring("config_".length());
            	if (key.indexOf(BonitaProperties.cstMarkerSplitTooLargeKey)>0)
            		key = key.substring(0,key.indexOf(BonitaProperties.cstMarkerSplitTooLargeKey));
            		
            	listConfig.add( key );
            }
        }
        return listConfig;
    }
	
	/** -------------------------------------------------------------------------
	 *
	 *runTheBonitaIndexDoGet
	 * 
	 */
	private void runTheBonitaIndexDoGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
				try {
						def String indexContent;
						pageResourceProvider.getResourceAsStream("index.html").withStream { InputStream s->
								indexContent = s.getText()
						}
						
						def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";
						
						indexContent= indexContent.replace("@_USER_LOCALE_@", request.getParameter("locale"));
						indexContent= indexContent.replace("@_PAGE_RESOURCE_@", pageResource);
						
						response.setCharacterEncoding("UTF-8");
						PrintWriter out = response.getWriter();
						out.print(indexContent);
						out.flush();
						out.close();
				} catch (Exception e) {
						e.printStackTrace();
				}
		}
	
	/**
	 * load the config.
	 * @return null in case of error, else the JSON string configuration
	 */
	public String loadConfig(String name, List<BEvent> listEvents, Map<String,Object> answer, PageResourceProvider pageResourceProvider, APISession apiSession) {
        List<BEvent> listEventsConfig = new ArrayList<BEvent>();
        logger.info("BonitaProperties.loadConfig name=["+name+"]" );
        // Load is
        BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider, apiSession.getTenantId() );

        listEventsConfig.addAll( bonitaProperties.load() );
        logger.info("BonitaProperties.load: loadproperties done, events = "+listEventsConfig.size() );
		
		bonitaProperties.traceInLog();
         String jsonConfiguration = bonitaProperties.getProperty( "config_"+name );
         
         logger.info("BonitaProperties.load store properties  done, events = "+listEventsConfig.size() +" jsonConfiguration="+jsonConfiguration);
         
         // due to the split, we reload a list of MAP like
          //  [
		   // 		{ "processes" : {  "Variables" :"", ...} },
		   // 		{ "processes" : {  "Variables" :"", ...} },
		   // 		{ "processes" : {  "Variables" :"", ...} },
		   // 		{ "scenarii"  : {  },

			// RESULT expected:
			// {  "processes" : [], "scenarii": [] }
			/*
         List< Map<String,Object>> listFromConfig = JSONValue.parse(jsonConfiguration);
		   
		   // then recreate one variable with processes and scenarii
		   Map<String, Object> finalResult = new HashMap<String,Object>();
		   for (Map<String,Object> mapSplited : listFromConfig)
		   {
		   		for (String key : mapSplited.keySet())
		   		{
		   			 List<Object> listKey = finalResult.get( key );
		   			 if (listKey==null)
		   			 	listKey = new ArrayList();
		   			 listKey.add( mapSplited.get( key ));
		   			 finalResult.put( key, listKey );
		   		} 
		   }                 
		   */
       	 answer.put("config", JSONValue.parse(jsonConfiguration) );
       	 
         if (! BEventFactory.isError( listEventsConfig ))
           	 listEventsConfig.add( EventConfigurationLoaded);
       	 answer.put("listeventsconfig", BEventFactory.getHtml(listEventsConfig));
       	 if (BEventFactory.isError( listEventsConfig ))
       		 return null;
       	 else
       		 return jsonConfiguration;
	}
		
	/*
	 * 
	 */
	public void start(String accumulateJson, List<BEvent> listEvents, Map<String,Object> answer, MeteorAccess meteorAccess, ProcessAPI processAPI, CommandAPI commandAPI, PageResourceProvider pageResourceProvider )
	{

        List jarDependencies = new ArrayList<>();
        jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "bdm-jpql-query-executor-command-1.0.jar", pageResourceProvider.getResourceAsStream("lib/bdm-jpql-query-executor-command-1.0.jar")));
        jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "process-starter-command-1.0.jar", pageResourceProvider.getResourceAsStream("lib/process-starter-command-1.0.jar")));
        jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "scenario-utils-2.0.jar", pageResourceProvider.getResourceAsStream("lib/scenario-utils-2.0.jar")));
        jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "CustomPageMeteor-1.0.0.jar", pageResourceProvider.getResourceAsStream("lib/CustomPageMeteor-1.0.0.jar")));
        jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "bonita-event-1.0.0.jar", pageResourceProvider.getResourceAsStream("lib/bonita-event-1.0.0.jar")));
        
        List<BEvent> listEventsDeploy = meteorAccess.deployCommand(true, "1.0", jarDependencies, commandAPI, null);
        if (BEventFactory.isError( listEventsDeploy))
        {
            listEvents.addAll(listEventsDeploy );
        }
        
        listEventsDeploy.addAll( meteorAccess.deployCommandGroovyScenario(true, "1.0", new ArrayList<>(), commandAPI, null));
        
        
		// logger.info("Json=["+paramJson+"]");
          StartParameters startParameters; 
        if (accumulateJson!=null)
        {
            logger.info(" We get a LIST JSON size=("+accumulateJson.length()+" - first value =["+ (accumulateJson==null ? null :(accumulateJson.length()>100 ? accumulateJson.substring(0,100) :accumulateJson))+ "]");
            startParameters = StartParameters.getInstanceFromJsonSt( accumulateJson );
        }
        else
        {
            logger.info(" We get a STRING size=("+paramJson+"]");                    
		    startParameters = StartParameters.getInstanceFromJsonSt( paramJson );
        }
		answer.putAll( meteorAccess.start(startParameters, processAPI, commandAPI));

	}
}
