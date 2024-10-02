package com.atlassian.jira.webtest.webdriver.tests.security;

import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.config.LoginAs;
import com.atlassian.jira.pageobjects.config.ResetData;
import com.atlassian.jira.pageobjects.pages.admin.roles.GroupRoleActorActionPage;
import com.atlassian.jira.pageobjects.pages.admin.roles.UserRoleActorActionPage;
import com.atlassian.jira.pageobjects.pages.admin.roles.ViewDefaultProjectRoleActorsPage;
import com.atlassian.jira.pageobjects.pages.admin.roles.ViewProjectRolesPage;
import com.atlassian.pageobjects.elements.PageElement;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Webdriver test for the ViewDefaultProjectRoleActors page
 */
@WebTest({Category.WEBDRIVER_TEST, Category.ADMINISTRATION})
@ResetData
public class TestDefaultProjectRoleActorsPage extends BaseJiraWebTest {
    // Our XSS attack is to change the document title
    private static final String injectedXSSContent = "XSS HACKING";
    private static final String XSSPayload = "document.title='" + injectedXSSContent + "'";

    /**
     * Tests against XSS in ViewDefaultProjectRoleActors.jspa (JRA-60912)
     */
    @LoginAs(admin = true)
    @Test
    public void testXSSUsingProjectRoleName() {
        // Make a new project role with the attack string
        final String roleId = jira.goTo(ViewProjectRolesPage.class)
                .addProjectRole("(none'-(" + XSSPayload + ")-'nonexwr", "A legitimate project role")
                .getProjectRoleId().now();
        // Add a default user
        final UserRoleActorActionPage userRoleActorActionPage = jira.goTo(UserRoleActorActionPage.class, roleId);
        userRoleActorActionPage.getPicker().query("admin");
        userRoleActorActionPage.add();
        // Add a default group
        final GroupRoleActorActionPage groupRoleActorActionPage = jira.goTo(GroupRoleActorActionPage.class, roleId);
        groupRoleActorActionPage.getPicker().query("jira-administrators");
        groupRoleActorActionPage.add();

        final ViewDefaultProjectRoleActorsPage defaultProjectRoleActorsPage = jira.goTo(ViewDefaultProjectRoleActorsPage.class, roleId);
        final PageElement defaultUser = defaultProjectRoleActorsPage.getDefaultUserRoleActorsElement();
        final PageElement defaultGroup = defaultProjectRoleActorsPage.getDefaultGroupRoleActorsElement();

        // Verify that the vulnerable attribute does not have code injected into it
        assertAttributeNoXSS(defaultUser, "onclick");
        assertAttributeNoXSS(defaultGroup, "onclick");

        // Double check that our XSS code is never executed
        defaultUser.click();
        defaultGroup.click();
        assertNoXSSExecution();
    }

    private void assertNoXSSExecution() {
        assertThat(jira.getTester().getDriver().getTitle(), not(containsString(injectedXSSContent)));
    }

    private void assertAttributeNoXSS(final PageElement element, final String attribute) {
        assertThat(element.getAttribute(attribute), not(containsString(XSSPayload)));
    }
}
