package com.atlassian.jira.webtest.webdriver.tests.security;

import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.config.LoginAs;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.jira.rest.api.issue.IssueCreateResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.atlassian.jira.permission.ProjectPermissions.BROWSE_PROJECTS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@WebTest({com.atlassian.jira.functest.framework.suite.Category.WEBDRIVER_TEST})
public class TestAnonymousAccess extends BaseJiraWebTest {

    private static final String TEST_PROJECT_KEY = "TST";
    private static final String TEST_PROJECT_NAME = TEST_PROJECT_KEY + " project";
    private static final String TEST_PROJECT_NAME_ISSUE_SUMMARY = TEST_PROJECT_NAME + " issue summary";
    private static final long DEFAULT_PERMISSION_SCHEMA = 0L;

    @Before
    public void setUp() {
        backdoor.restoreBlankInstance();
        jira.backdoor().project().addProject(TEST_PROJECT_NAME, TEST_PROJECT_KEY, jira.getAdminCredentials().getUsername());
        jira.backdoor().permissionSchemes().addEveryonePermission(DEFAULT_PERMISSION_SCHEMA, BROWSE_PROJECTS);
    }

    @After
    public void tearDown() {
        jira.backdoor().permissionSchemes().removeEveryonePermission(DEFAULT_PERMISSION_SCHEMA, BROWSE_PROJECTS);
        jira.backdoor().project().deleteProject(TEST_PROJECT_KEY);
    }

    @Test
    @LoginAs(anonymous = true)
    public void testAnonymousAccessOk() throws Exception {
        final IssueCreateResponse newIssue = jira.backdoor().issues()
                .createIssue(TEST_PROJECT_KEY, TEST_PROJECT_NAME_ISSUE_SUMMARY);

        final ViewIssuePage viewIssuePage = pageBinder.navigateToAndBind(ViewIssuePage.class, newIssue.key);

        assertThat(viewIssuePage.getSummary(), is(TEST_PROJECT_NAME_ISSUE_SUMMARY));
    }
}
