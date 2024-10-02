package com.atlassian.jira.webtest.webdriver.tests.projectconfig;

import com.atlassian.integrationtesting.runner.restore.RestoreOnce;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.elements.LozengeUsedBy;
import com.atlassian.jira.pageobjects.project.issuesecurity.IssueSecurity;
import com.atlassian.jira.pageobjects.project.issuesecurity.IssueSecurityPage;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.pageobjects.elements.query.Poller;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @since v4.4
 */
@WebTest({Category.WEBDRIVER_TEST, Category.ADMINISTRATION, Category.PLUGINS, Category.PROJECTS})
@RestoreOnce("xml/projectconfig/TestIssueSecurityTab.xml")
public class TestIssueSecurityPanel extends BaseJiraWebTest {
    @Test
    public void notAdmin() {
        final IssueSecurityPage issueSecurityPage = jira.quickLogin("fred", "fred", IssueSecurityPage.class, "HSP");

        // No security levels in the table.
        assertEquals(0, issueSecurityPage.getIssueSecurities().size());
        assertTrue(issueSecurityPage.getNoSecurityLevelsMessage().isPresent());

        // Assert the cog actions aren't present
        assertFalse(issueSecurityPage.isSchemeLinked());
        assertFalse(issueSecurityPage.isSchemeChangeAvailable());

        Poller.waitUntilFalse(issueSecurityPage.getUsedBy().isPresent());

        final IssueSecurityPage lalaIssueSecurityPage = pageBinder.navigateToAndBind(IssueSecurityPage.class, "LALA");
        Poller.waitUntilFalse(lalaIssueSecurityPage.getUsedBy().isPresent());
    }

    @Test
    public void projectWithoutSecurityLevels() {
        final IssueSecurityPage issueSecurityPage = jira.goTo(IssueSecurityPage.class, "HSP");

        // No security levels in the table.
        assertEquals(0, issueSecurityPage.getIssueSecurities().size());
        assertTrue(issueSecurityPage.getNoSecurityLevelsMessage().isPresent());

        // No link to edit scheme is present.
        assertFalse(issueSecurityPage.isSchemeLinked());

        // Link to scheme select is present.
        assertTrue(issueSecurityPage.isSchemeChangeAvailable());

        Poller.waitUntilFalse(issueSecurityPage.getUsedBy().isPresent());
    }

    @Test
    public void projectWithSecurityLevels() {
        final IssueSecurityPage issueSecurityPage = jira.goTo(IssueSecurityPage.class, "MKY");

        // No security levels in the table.
        assertEquals(3, issueSecurityPage.getIssueSecurities().size());
        assertFalse(issueSecurityPage.getNoSecurityLevelsMessage().isPresent());

        final List<IssueSecurity> securityList = CollectionBuilder.<IssueSecurity>newBuilder(
                new IssueSecurity().setName("Classified").setDefault(true).setDescription("most people can see this").setEntities(CollectionBuilder.newBuilder("Current assignee", "Reporter").asList()),
                new IssueSecurity().setName("Secret").setDescription("<strong>don't even think about telling your wife</strong>").setEntities(CollectionBuilder.newBuilder("Project Role (Developers)", "Project lead", "Reporter").asList()),
                new IssueSecurity().setName("Top Secret").setDescription("We will kill you.").setEntities(CollectionBuilder.newBuilder("Single user (Fred Normal)").asList())
        ).asList();
        Assert.assertEquals(securityList, issueSecurityPage.getIssueSecurities());

        // link to edit scheme is present.
        assertTrue(issueSecurityPage.isSchemeLinked());

        // Link to scheme select is present.
        assertTrue(issueSecurityPage.isSchemeChangeAvailable());

        final LozengeUsedBy usedBy = issueSecurityPage.getUsedBy();
        Poller.waitUntilTrue(usedBy.isPresent());
        assertEquals("3 PROJECTS", usedBy.getProjectsTriggerText().get());
        assertEquals(Arrays.asList("LALA", "XSS", "monkey"), usedBy.getProjectsNames().get());
    }
}
