package com.atlassian.jira.rest.v2.issue;

import com.atlassian.jira.issue.fields.rest.json.beans.SecuritySchemeJsonBean;
import com.atlassian.jira.issue.fields.rest.json.beans.SecuritySchemesJsonBean;

import java.util.Arrays;

public class SecuritySchemeExample {
    public static final SecuritySchemeJsonBean DOC_EXAMPLE;
    public static final SecuritySchemeJsonBean DOC_FULL_BEAN_EXAMPLE;

    static {
        DOC_EXAMPLE = new SecuritySchemeJsonBean(
                SecuritySchemeJsonBean.getSelf(Examples.REST_BASE_URL + "/", String.valueOf(1000)),
                1000l,
                "Default Issue Security Scheme",
                "Description for the default issue security scheme");
        DOC_EXAMPLE.setDefaultSecurityLevelId(10021l);

        DOC_FULL_BEAN_EXAMPLE = new SecuritySchemeJsonBean(
                SecuritySchemeJsonBean.getSelf(Examples.REST_BASE_URL + "/", String.valueOf(1000)),
                1000l,
                "Default Issue Security Scheme",
                "Description for the default issue security scheme");
        DOC_FULL_BEAN_EXAMPLE.setDefaultSecurityLevelId(10021l);
        DOC_FULL_BEAN_EXAMPLE.setLevels(Arrays.asList(SecurityLevelBeanExample.DOC_EXAMPLE));
    }

    public static final SecuritySchemesJsonBean DOC_LIST_EXAMPLE = SecuritySchemesJsonBean.fromList(Arrays.asList(DOC_EXAMPLE));
}
