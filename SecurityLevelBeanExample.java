package com.atlassian.jira.rest.v2.issue;

import com.atlassian.jira.issue.fields.rest.json.beans.SecurityLevelJsonBean;

/**
 * @since v5.0
 */
class SecurityLevelBeanExample {
    /**
     * Example SecurityLevel bean. JSON:
     */
    static final SecurityLevelJsonBean DOC_EXAMPLE;

    static {
        String id = "10021";
        DOC_EXAMPLE = new SecurityLevelJsonBean(
                SecurityLevelJsonBean.getSelf(Examples.REST_BASE_URL + "/", id),
                id,
                "Only the reporter and internal staff can see this issue.",
                "Reporter Only");
    }
}
