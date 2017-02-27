import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
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
	
import com.bonitasoft.custompage.meteor.MeteorAccess;
import com.bonitasoft.custompage.meteor.MeteorAccess.StartParameters;
import com.bonitasoft.custompage.meteor.MeteorAccess.StatusParameters;
import com.bonitasoft.custompage.meteor.MeteorProcessDefinitionList.ListProcessParameter;

import com.bonitasoft.custompage.meteor.cmd.CmdMeteor;

public class Index implements PageController {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
	
		Logger logger= Logger.getLogger("org.bonitasoft");
		
		
		try {
			def String indexContent;
			pageResourceProvider.getResourceAsStream("Index.groovy").withStream { InputStream s-> indexContent = s.getText() };
			response.setCharacterEncoding("UTF-8");
			PrintWriter out = response.getWriter()

			String action=request.getParameter("action");
			logger.info("###################################### action 2.1 is["+action+"] !");
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
            
           
			HashMap<String,Object> answer = null;
			if ("getlistprocesses".equals(action))
			{
				ListProcessParameter listProcessParameter = ListProcessParameter.getInstanceFromJsonSt( paramJson );
				answer = meteorAccess.getListProcesses( listProcessParameter, processAPI);
			}
			else if ("start".equals(action))
			{
                List jarDependencies = new ArrayList<>();
                jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "CustomPageMeteor-1.0.0.jar", pageResourceProvider.getResourceAsStream("lib/CustomPageMeteor-1.0.0.jar")));
                jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand( "bonita-event-1.0.0.jar", pageResourceProvider.getResourceAsStream("lib/bonita-event-1.0.0.jar")));
                
                List<BEvent> listEventsDeploy = meteorAccess.deployCommand(true, "1.0", jarDependencies, commandAPI, null);
                    
				// logger.info("Json=["+paramJson+"]");
				StartParameters startParameters = StartParameters.getInstanceFromJsonSt( paramJson );
				answer = meteorAccess.start(startParameters, processAPI, commandAPI);
				
				//Thread.sleep(1000);
				//HashMap<String,Object> statusexecution = meteorAccess.getStatus( processAPI, commandAPI);
				//answer.putAll( statusexecution );
			} 
            else if ("refresh".equals(action))
            {
                StatusParameters statusParameters = StatusParameters.getInstanceFromJsonSt( paramJson );
                
                answer = meteorAccess.getStatus(statusParameters, processAPI, commandAPI);
                
            }
			
			if (answer==null)
			{
				answer = new HashMap<String,Object> ();
				answer.put("status", "Unknow command");
			}
			
			String jsonDetailsSt = JSONValue.toJSONString( answer );
			logger.info("Meteor.goovy: Return["+jsonDetailsSt+"]");
			
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
		
		
}
