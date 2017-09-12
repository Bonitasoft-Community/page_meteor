
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.io.File;
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

import java.text.SimpleDateFormat;

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

import org.bonitasoft.engine.api.TenantAPIAccessor;
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

import org.bonitasoft.meteor.MeteorAPI;
import org.bonitasoft.meteor.MeteorAPI.StartParameters;
import org.bonitasoft.meteor.MeteorAPI.StatusParameters;
import org.bonitasoft.meteor.MeteorProcessDefinitionList.ListProcessParameter;

import org.bonitasoft.meteor.MeteorDAO;
import org.bonitasoft.meteor.MeteorDAO.StatusDAO;

import org.bonitasoft.meteor.cmd.CmdMeteor;

public class Index implements PageController {


  Logger logger= Logger.getLogger("org.bonitasoft");
  
  
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
	
		
        Long staticInformation=null;
		long timeBegin= System.currentTimeMillis();
		try {
			//def String indexContent;
			//pageResourceProvider.getResourceAsStream("Index.groovy").withStream { InputStream s-> indexContent = s.getText() };
			response.setCharacterEncoding("UTF-8");
			
			String action=request.getParameter("action");
			logger.info("###################################### action 1.0.C is["+action+"] !");
			if (action==null || action.length()==0 )
			{
				logger.severe("###################################### RUN Default !");
				
				runTheBonitaIndexDoGet( request, response,pageResourceProvider,pageContext);
				return;
			}
			String paramJsonEncode= request.getParameter("paramjson");
            String paramJsonSt = (paramJsonEncode==null ? null : java.net.URLDecoder.decode(paramJsonEncode, "UTF-8"));
        			
            APISession apiSession = pageContext.getApiSession()
			HttpSession httpSession = request.getSession() ;
			
			
			// httpSession.setAttribute("MeteorAPI", null);
			
		    MeteorAPI meteorAPI = MeteorAPI.getMeteorAPI( httpSession );
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
				ListProcessParameter listProcessParameter = ListProcessParameter.getInstanceFromJsonSt( paramJsonSt );
				
				answer = meteorAPI.getListProcesses( listProcessParameter, processAPI);
			}
            
            /** POST is too big, so use the collect_reset, [collect_add] * ,  and then start to do the same as start */
            else if ("collect_reset".equals(action))
            {
                httpSession.setAttribute("accumulate", "" );
                answer = new HashMap<String,Object>()
                answer.put("status", "ok");

            }
            else if ("collect_add".equals(action))
            {
            	String paramJsonPartial = request.getParameter("paramjsonpartial");
            	logger.info("collect_add paramJsonPartial=["+paramJsonPartial+"] json=["+paramJsonSt+"]");
				
               String accumulateJson = (String) httpSession.getAttribute("accumulate" );
               accumulateJson+=paramJsonSt;
               httpSession.setAttribute("accumulate", accumulateJson );
               answer = new HashMap<String,Object>()
               answer.put("status", "ok");

            }
          
			else if ("start".equals(action))
			{
				String accumulateJson = (String) httpSession.getAttribute("accumulate" );
			    answer = new HashMap<String,Object>();
			    start( accumulateJson,  listEvents, answer, meteorAPI, processAPI, commandAPI, pageResourceProvider );

			} 
			else if ("loadandstart".equals(action))
			{
				answer = new HashMap<String,Object>();
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
		            	
            	answer.put("description",statusDao.configuration.description ); 
            	answer.put("config", JSONValue.parse(statusDao.configuration.content) );
            	String accumulateJson=statusDao.configuration.content;
               	answer.put("listeventsconfig", BEventFactory.getHtml(statusDao.listEvents));
		       
		               	
		               	
				if (accumulateJson!=null)
					start( accumulateJson,  listEvents, answer, meteorAPI, processAPI, commandAPI, pageResourceProvider );
				
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
                answer = meteorAPI.getStatus(statusParameters, processAPI, commandAPI);
                
            }
            // ------- init 
            else if ("initpage".equals(action))
            {
                // first process
                ListProcessParameter listProcessParameter = ListProcessParameter.getInstanceFromJsonSt( paramJsonSt );
        		answer = meteorAPI.getListProcesses( listProcessParameter, processAPI);

        		// second configuration
                MeteorDAO meteorDAO = MeteorDAO.getInstance();
            	MeteorDAO.StatusDAO statusDao = meteorDAO.getListNames(pageResourceProvider.getPageName(), apiSession.getTenantId()  ) ;
            	answer.put("listeventsconfig",  BEventFactory.getHtml( statusDao.listEvents));
                answer.put("configList", statusDao.listNamesAllConfigurations);
                
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
            	answer = new HashMap<String,Object>();
				
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
                answer.put("listeventsconfig",  BEventFactory.getHtml( statusDao.listEvents));
                answer.put("configList", statusDao.listNamesAllConfigurations);
            }
            else if ("loadconfig".equals(action))
            {
            	answer = new HashMap<String,Object>()
        	    final Object jsonObject = JSONValue.parse(paramJsonSt);
            	String name= jsonObject.get("confname");
                        
            	MeteorDAO meteorDAO = MeteorDAO.getInstance();
            	MeteorDAO.StatusDAO statusDao= meteorDAO.load( name, pageResourceProvider.getPageName(), apiSession.getTenantId());
            	
            	answer.put("description",statusDao.configuration.description ); 
            	answer.put("config", JSONValue.parse(statusDao.configuration.content) );
               	answer.put("listeventsconfig", BEventFactory.getHtml(statusDao.listEvents));
             
            	// loadConfig(name, listEvents, answer, pageResourceProvider, apiSession);

            }
            else if ("deleteconfig".equals(action))
            {
            	answer = new HashMap<String,Object>();
				
                final Object jsonObject = JSONValue.parse(paramJsonSt);
                String name= jsonObject.get("confname");
                logger.info("BonitaProperties.loadConfig name=["+name+"]" );
                // Load is
                
                MeteorDAO meteorDAO = MeteorDAO.getInstance();
            	MeteorDAO.StatusDAO statusDao= meteorDAO.delete( name, true, pageResourceProvider.getPageName(), apiSession.getTenantId());
              
               	 answer.put("listeventsconfig", BEventFactory.getHtml(statusDao.listEvents));
               	 answer.put("configList", statusDao.listNamesAllConfigurations);
            		
                 
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
                response.addHeader("content-disposition", "attachment; filename=MeteorTest.zip");
                response.addHeader("content-type", "application/zip");
            	OutputStream output = response.getOutputStream();

            	if (statusDao.containerZip!=null)
            		statusDao.containerZip.writeTo( output );
            	
            	output.flush();
            	output.close();
                return;
			} else if ("importconfs".equals(action))
			{
				answer = new HashMap<String,Object>();
				
				String filename= request.getParameter("filename");
                MeteorDAO meteorDAO = MeteorDAO.getInstance();
            	MeteorDAO.StatusDAO statusDao= meteorDAO.importConfs( filename, true, pageResourceProvider.getPageDirectory(), pageResourceProvider.getPageName(), apiSession.getTenantId());
            	answer.put("listeventsconfig", BEventFactory.getHtml(statusDao.listEvents));
               	answer.put("configList", statusDao.listNamesAllConfigurations);
               	if (statusDao.configuration!=null)
               	{
               		answer.put("description",statusDao.configuration.description ); 
               		answer.put("config", JSONValue.parse(statusDao.configuration.content) );
               		answer.put("name", statusDao.configuration.name);
               	}
         
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
			
			PrintWriter out = response.getWriter()

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
						
						//def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";						
						//indexContent= indexContent.replace("@_USER_LOCALE_@", request.getParameter("locale"));
						//indexContent= indexContent.replace("@_PAGE_RESOURCE_@", pageResource);
						
						response.setCharacterEncoding("UTF-8");
						PrintWriter out = response.getWriter();
						out.print(indexContent);
						out.flush();
						out.close();
				} catch (Exception e) {
						e.printStackTrace();
				}
		}
	
	 /*
     * return all the different configuration detected
     *
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
            	Map<String,Object> oneConf = new HashMap<String,Object>();
            	oneConf.put("name", key);
            	listConfig.add( oneConf );
            }
        }
        return listConfig;
    }
    */
	
	
	/*
	 * 
	 */
	public void start(String accumulateJson, List<BEvent> listEvents, Map<String,Object> answer, MeteorAPI meteorAPI, ProcessAPI processAPI, CommandAPI commandAPI, PageResourceProvider pageResourceProvider )
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
        if (accumulateJson!=null)
        {
            logger.info(" We get a LIST JSON size=("+accumulateJson.length()+" - first value =["+ (accumulateJson==null ? null :(accumulateJson.length()>100 ? accumulateJson.substring(0,100) :accumulateJson))+ "]");
            startParameters = StartParameters.getInstanceFromJsonSt( accumulateJson );
        }
        else
        {
            logger.info(" We get a STRING size=("+paramJsonSt+"]");                    
		    startParameters = StartParameters.getInstanceFromJsonSt( paramJsonSt );
        }
		answer.putAll( meteorAPI.start(startParameters, processAPI, commandAPI));

	}
}
