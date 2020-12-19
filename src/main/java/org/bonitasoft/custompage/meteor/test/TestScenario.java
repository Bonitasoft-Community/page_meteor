package org.bonitasoft.custompage.meteor.test;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.ApplicationAPI;
import org.bonitasoft.engine.api.BusinessDataAPI;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.api.PermissionAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.api.ThemeAPI;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.platform.UnknownUserException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.junit.Before;
import org.junit.Test;

public class TestScenario {

    @Before
    public void setUp() throws Exception {
    }

   

    public APISession login() {
        try {
            final Map<String, String> map = new HashMap<>();

            map.put("server.url", "http://localhost:8080");
            map.put("application.name", "bonita");

            APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);

            // get the LoginAPI using the TenantAPIAccessor
            final LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();

            // log in to the tenant to create a session
            final APISession session = loginAPI.login("walter.bates", "bpm");
            return session;
        } catch (final BonitaHomeNotSetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ServerAPIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final UnknownAPITypeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final UnknownUserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public class TestAccessorImpl implements APIAccessor {

        APISession apiSession;

        public TestAccessorImpl(final APISession apiSession) {
            this.apiSession = apiSession;
        }

        public IdentityAPI getIdentityAPI() {
            try {
                return TenantAPIAccessor.getIdentityAPI(apiSession);
            } catch (final Exception e) {
                return null;
            }
        }

        public ProcessAPI getProcessAPI() {
            try {
                return TenantAPIAccessor.getProcessAPI(apiSession);
            } catch (final Exception e) {
                return null;
            }
        }

        public CommandAPI getCommandAPI() {
            try {
                return TenantAPIAccessor.getCommandAPI(apiSession);
            } catch (final Exception e) {
                return null;
            }
        }

        public ProfileAPI getProfileAPI() {
            try {
                return TenantAPIAccessor.getProfileAPI(apiSession);
            } catch (final Exception e) {
                return null;
            }

        }

        public ThemeAPI getThemeAPI() {
            try {
                return TenantAPIAccessor.getThemeAPI(apiSession);
            } catch (final Exception e) {
                return null;
            }

        }

        public PermissionAPI getPermissionAPI() {
            try {
                return TenantAPIAccessor.getPermissionAPI(apiSession);
            } catch (final Exception e) {
                return null;
            }

        }

        public PageAPI getCustomPageAPI() {
            try {
                return TenantAPIAccessor.getCustomPageAPI(apiSession);
            } catch (final Exception e) {
                return null;
            }

        }

        public ApplicationAPI getLivingApplicationAPI() {
            try {
                return TenantAPIAccessor.getLivingApplicationAPI(apiSession);
            } catch (final Exception e) {
                return null;
            }

        }

        public BusinessDataAPI getBusinessDataAPI() {
            try {
                return TenantAPIAccessor.getBusinessDataAPI(apiSession);
            } catch (final Exception e) {
                return null;
            }

        }

    }
}
