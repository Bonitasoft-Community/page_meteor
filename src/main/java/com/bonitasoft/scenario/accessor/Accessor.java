package com.bonitasoft.scenario.accessor;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ApplicationAPI;
import org.bonitasoft.engine.api.BusinessDataAPI;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.api.PermissionAPI;
// import org.bonitasoft.engine.api.LogAP;
// import org.bonitasoft.engine.api.MonitoringAPI;
// import org.bonitasoft.engine.api.NodeAPI;
// import org.bonitasoft.engine.api.PlatformMonitoringAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAdministrationAPI;
// import org.bonitasoft.engine.api.ReportingAPI;
import org.bonitasoft.engine.api.ThemeAPI;
import org.bonitasoft.engine.api.impl.ClientInterceptor;
import org.bonitasoft.engine.api.impl.ServerAPIFactory;
import org.bonitasoft.engine.api.internal.ServerAPI;
import org.bonitasoft.engine.connector.ConnectorAPIAccessorImpl;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.service.ModelConvertor;
import org.bonitasoft.engine.service.TenantServiceAccessor;
// import org.bonitasoft.engine.service.impl.TenantServiceSingleton;
import org.bonitasoft.engine.service.TenantServiceSingleton;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.SSessionException;
import org.bonitasoft.engine.session.SessionService;
import org.bonitasoft.engine.session.model.SSession;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;

import com.bonitasoft.scenario.accessor.configuration.ScenarioConfiguration;
import com.bonitasoft.scenario.accessor.ext.ScenarioBdmAPI;
import com.bonitasoft.scenario.accessor.resource.Resource;
import com.bonitasoft.scenario.administration.CommandsAdministration;
import com.bonitasoft.scenario.runner.RunListener;
import com.bonitasoft.scenario.runner.ScenarioResult;
import com.bonitasoft.scenario.runner.context.RunContext;
import com.bonitasoft.scenario.runner.context.ScenarioType;

import groovy.lang.Binding;

public class Accessor {

    final static public String HOOK = "accessor";

    private Long tenantId = null;
    private APISession apiSession = null;
    private boolean updateSession = true;
    private Long user = null;
    private String username = null;

    private String[] gsContent = null;
    private Binding scriptBinding = null;

    private String runName = null;
    private Map<String, Serializable> parameters = null;
    private Resource resource = null;
    private ScenarioType scenarioType = null;
    private String scenarioName = null;
    private List<RunListener> runListeners = null;
    private ScenarioConfiguration scenarioConfiguration = null;
    private Integer advancement = null;
    private ScenarioResult scenarioResult = null;

    public Accessor(String[] gsContent, Binding scriptBinding, RunContext runContext, List<RunListener> runListeners, ScenarioResult scenarioResult) throws Exception {
        super();
        this.tenantId = runContext.getTenantId();

        this.gsContent = gsContent;
        this.scriptBinding = scriptBinding;

        this.runName = runContext.toString();
        this.parameters = runContext.getParameters();
        this.resource = runContext.getResource();
        this.scenarioType = runContext.getScenarioType();
        this.scenarioName = runContext.getName();
        this.scenarioConfiguration = runContext.getScenarioConfiguration();
        this.advancement = runContext.getAdvancement();

        // Hook the accessor to the context
        this.runListeners = runListeners;
        for (RunListener runListener : this.runListeners) {
            runListener.setAccessor(this);
        }
        this.resource.setAccessor(this);

        this.scenarioResult = scenarioResult;
        this.scenarioResult.setAccessor(this);

        // For development purpose only
        // CommandsAdministration.registerCommands(this.getDefaultCommandAPI(),
        // true);
        CommandsAdministration.registerCommands(this.getDefaultCommandAPI(), false);
    }

    public void log(Level level, String message, Throwable throwable) {
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(Constants.LEVEL, level);
        parameters.put(Constants.MESSAGE, message);
        parameters.put(Constants.THROWABLE, throwable);
        log(parameters);
    }

    public void log(Level level, String message) {
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(Constants.LEVEL, level);
        parameters.put(Constants.MESSAGE, message);
        log(parameters);
    }

    public void log(String message) {
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(Constants.MESSAGE, message);
        log(parameters);
    }

    public void log(Map<String, Serializable> parameters) {
        Level level = (parameters.containsKey(Constants.LEVEL) ? (Level) parameters.get(Constants.LEVEL) : Level.INFO);
        Serializable message = parameters.get(Constants.MESSAGE);
        String messageStr = runName + (message != null && !message.toString().isEmpty() ? " - " + message.toString() : "");
        if (parameters.containsKey(Constants.THROWABLE)) {
            ScenarioConfiguration.logger.log(level, messageStr, (Throwable) parameters.get(Constants.THROWABLE));
        } else {
            ScenarioConfiguration.logger.log(level, messageStr);
        }

        for (RunListener runListener : runListeners) {
            runListener.logCallback(level, messageStr);
        }
    }

    public void status(Map<String, Serializable> parameters) {
        log(parameters);

        if (parameters.containsKey(Constants.ADVANCEMENT)) {
            setAdvancement(Integer.parseInt(parameters.get(Constants.ADVANCEMENT).toString()));
        }
    }

    public void throwEvent(Serializable event) {
        for (RunListener runListener : runListeners) {
            runListener.catchEvent(event);
        }
    }

    // Default Bonitasoft APIs & BDM DAOs

    private APISession getAPISession() throws SSessionException {
        if (updateSession) {
            apiSession = null;
            updateSession = false;
        }
        if (apiSession == null) {
            TenantServiceAccessor tenantServiceAccessor = TenantServiceSingleton.getInstance(tenantId);
            SessionAccessor sessionAccessor = tenantServiceAccessor.getSessionAccessor();
            SessionService sessionService = tenantServiceAccessor.getSessionService();
            SSession session = sessionService.createSession(tenantId, (user == null ? 1L : user), (username == null ? ConnectorAPIAccessorImpl.class.getSimpleName() : username), true);
            sessionAccessor.setSessionInfo(session.getId(), tenantId);
            apiSession = ModelConvertor.toAPISession(session, null);
        }

        return apiSession;
    }

    // final PlatformServiceAccessor platformServiceAccessor = null;
    // final PlatformSessionService platformSessionService =
    // platformServiceAccessor.getPlatformSessionService();
    // platformSessionService.createSession(ConnectorAPIAccessorImpl.class.getSimpleName());

    private static ServerAPI getServerAPI() {
        return ServerAPIFactory.getServerAPI(false);
    }

    private static <T> T getAPI(final Class<T> clazz, final APISession session) {
        final ServerAPI serverAPI = getServerAPI();
        final ClientInterceptor sessionInterceptor = new ClientInterceptor(clazz.getName(), serverAPI, session);
        return (T) Proxy.newProxyInstance(APIAccessor.class.getClassLoader(), new Class[] { clazz }, sessionInterceptor);
    }

    public IdentityAPI getDefaultIdentityAPI() throws SSessionException {
        return getAPI(IdentityAPI.class, getAPISession());
    }

    public ProcessAPI getDefaultProcessAPI() throws SSessionException {
        return getAPI(ProcessAPI.class, getAPISession());
    }

    public CommandAPI getDefaultCommandAPI() throws SSessionException {
        return getAPI(CommandAPI.class, getAPISession());
    }

    public ProfileAPI getDefaultProfileAPI() throws SSessionException {
        return getAPI(ProfileAPI.class, getAPISession());
    }

    /*
     * public MonitoringAPI getDefaultMonitoringAPI() throws SSessionException {
     * return getAPI(MonitoringAPI.class, getAPISession()); }
     * public PlatformMonitoringAPI getDefaultPlatformMonitoringAPI() throws
     * SSessionException { return getAPI(PlatformMonitoringAPI.class,
     * getAPISession()); }
     * public LogAPI getDefaultLogAPI() throws SSessionException { return
     * getAPI(LogAPI.class, getAPISession()); }
     * public NodeAPI getDefaultNodeAPI() throws SSessionException { return
     * getAPI(NodeAPI.class, getAPISession()); }
     * public ReportingAPI getDefaultReportingAPI() throws SSessionException {
     * return getAPI(ReportingAPI.class, getAPISession()); }
     */
    public ThemeAPI getDefaultThemeAPI() throws SSessionException {
        return getAPI(ThemeAPI.class, getAPISession());
    }

    public PermissionAPI getDefaultPermissionAPI() throws SSessionException {
        return getAPI(PermissionAPI.class, getAPISession());
    }

    public PageAPI getDefaultCustomPageAPI() throws SSessionException {
        return getAPI(PageAPI.class, getAPISession());
    }

    public ApplicationAPI getDefaultLivingApplicationAPI() throws SSessionException {
        return getAPI(ApplicationAPI.class, getAPISession());
    }

    public BusinessDataAPI getDefaultBusinessDataAPI() throws SSessionException {
        return getAPI(BusinessDataAPI.class, getAPISession());
    }

    public TenantAdministrationAPI getDefaultTenantAdministrationAPI() throws SSessionException {
        return getAPI(TenantAdministrationAPI.class, getAPISession());
    }

    // public <T extends BusinessObjectDAO> T getDAO(final Class<T>
    // daoInterface) throws Exception {
    // Class<T> daoImplClass;
    // daoImplClass = loadClass(daoInterface);
    //
    // if (daoImplClass != null) {
    // final Constructor<T> constructor =
    // daoImplClass.getConstructor(APISession.class);
    // return constructor.newInstance(getAPISession());
    // }
    //
    // return null;
    // }
    //
    // protected <T extends BusinessObjectDAO> Class<T> loadClass(final Class<T>
    // daoInterface) throws ClassNotFoundException {
    // final String implementationClassName = daoInterface.getName() +
    // IMPL_SUFFIX;
    // return (Class<T>) Class.forName(implementationClassName, true,
    // Thread.currentThread().getContextClassLoader());
    // }

    // Scenario Additional APIs

    public ScenarioBdmAPI getDefaultBdmAPI() {
        return new ScenarioBdmAPI();
    }

    // Getters/Setters

    public Binding getScriptBinding() {
        return scriptBinding;
    }

    public String[] getGsContent() {
        return gsContent;
    }

    public Resource getResource() {
        return resource;
    }

    public Map<String, Serializable> getParameters() {
        return parameters;
    }

    public String getRunName() {
        return runName;
    }

    public ScenarioType getScenarioType() {
        return scenarioType;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public ScenarioConfiguration getScenarioConfiguration() {
        return scenarioConfiguration;
    }

    public Integer getAdvancement() {
        return advancement;
    }

    public void setAdvancement(Integer advancement) {
        if (advancement == null) {
            advancement = 0;
        } else if (advancement < 0) {
            advancement = 0;
        } else if (advancement > 100) {
            advancement = 100;
        }

        this.advancement = advancement;

        for (RunListener runListener : runListeners) {
            runListener.advancementCallback(advancement);
        }
    }

    public void setAdvancementAsComplete() {
        setAdvancement(100);
    }

    public ScenarioResult getScenarioResult() {
        return scenarioResult;
    }

    public Long getUser() {
        return user;
    }

    public void setUser(Long user) throws Exception {
        if (user != null) {
            User retrievedUser = getDefaultIdentityAPI().getUser(user);
            this.user = retrievedUser.getId();
            this.username = retrievedUser.getUserName();
        } else {
            this.user = null;
            this.username = null;
        }

        this.updateSession = true;
    }

    public void resetUser() {
        this.user = null;

        this.updateSession = true;
    }
}
