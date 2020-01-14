package com.bonitasoft.scenario.accessor.ext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserCriterion;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;

import com.bonitasoft.scenario.accessor.Accessor;
import com.bonitasoft.scenario.accessor.Constants;
import com.bonitasoft.scenario.accessor.parameter.Extractor;
import com.bonitasoft.scenario.accessor.resource.ResourceType;

public class ScenarioIdentityAPI {

    static public boolean deployProfiles(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
        String methodName = "deployProfiles";
        parameters = Extractor.preProcessParameters(parameters);

        accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

        byte[] profilesResource = (byte[]) Extractor.getScenarioResource(parameters.get(Constants.RESOURCE_NAME), Constants.RESOURCE_NAME, false, accessor, ResourceType.PROFILES);

        if (profilesResource != null) {
            // Load a serialized profile if specified
            accessor.log(Level.FINE, methodName + ": import the given profiles");
            // Only BONITASOFT_SUBSCRIPTION
            // accessor.getDefaultProfileAPI().importProfiles(profilesResource,
            // ImportPolicy.REPLACE_DUPLICATES);
        } else {
            // Add all the profiles to all by default
            accessor.log(Level.FINE, methodName + ": no profiles resource provided so add all the profiles to all the users");
            final List<Long> profiles = new ArrayList<Long>();
            for (final Profile profile : accessor.getDefaultProfileAPI().searchProfiles(new SearchOptionsBuilder(0, Integer.MAX_VALUE).sort("name", Order.DESC).done()).getResult()) {
                final long profileId = profile.getId();
                profiles.add(profileId);
            }

            final List<User> users = accessor.getDefaultIdentityAPI().getUsers(0, Integer.MAX_VALUE, UserCriterion.USER_NAME_ASC);
            for (final User user : users) {
                final long id = user.getId();
                for (final Long profile : profiles) {
                    accessor.getDefaultProfileAPI().createProfileMember(profile, id, -1L, -1L);
                }
            }
        }

        return true;
    }

    static public boolean deployOrganization(Accessor accessor, Map<String, Serializable> parameters) throws Exception {
        String methodName = "deployOrganization";
        parameters = Extractor.preProcessParameters(parameters);

        accessor.log(Level.FINE, methodName + ": parameters processing " + Arrays.toString(parameters.entrySet().toArray()));

        String organization = (String) Extractor.getScenarioResource(parameters.get(Constants.RESOURCE_NAME), Constants.RESOURCE_NAME, false, accessor, ResourceType.ORGANIZATION);

        accessor.log(Level.FINE, methodName + ": undeploy all processes");
        ScenarioProcessAPI.undeployProcesses(accessor, new HashMap<String, Serializable>());

        // Delete the current organization/flush the initial
        accessor.log(Level.FINE, methodName + ": delete the old organization");
        accessor.getDefaultIdentityAPI().deleteOrganization();

        // Import the new organization
        accessor.log(Level.FINE, methodName + ": import the new organization");
        accessor.getDefaultIdentityAPI().importOrganization(organization);

        return true;
    }
}
