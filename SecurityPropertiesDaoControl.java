package com.atlassian.jira.functest.framework.backdoor;

import com.atlassian.jira.testkit.client.JIRAEnvironmentData;
import com.sun.jersey.api.client.WebResource;

public class SecurityPropertiesDaoControl extends BackdoorControl<SecurityPropertiesDaoControl> {
    public SecurityPropertiesDaoControl(final JIRAEnvironmentData environmentData) {
        super(environmentData);
    }

    private WebResource resource() {
        return createResource().path("securitypropertiesdao");
    }

    public String get(final String key) {
        return resource().path("get").queryParam("key", key).get(String.class);
    }

    public String getButDoNotCreate(final String key) {
        return resource().path("getButDoNotCreate").queryParam("key", key).get(String.class);
    }

    public String getOrCreate(final String key, final String value) {
        return resource().path("getOrCreate").queryParam("key", key).queryParam("value", value).put(String.class);
    }

    public void delete(final String key) {
        resource().path("delete").queryParam("key", key).delete();
    }
}
