package com.atlassian.jira.webtest.webdriver.tests.security;

import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.config.ResetData;
import com.atlassian.jira.pageobjects.pages.admin.configuration.ViewGeneralConfigurationPage;
import com.atlassian.pageobjects.elements.PageElement;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;


import static com.atlassian.jira.webtests.Permissions.ADMINISTER;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Webdriver test for the ViewApplicationProperties page
 *
 * @since v8.11
 */
@WebTest({Category.WEBDRIVER_TEST, Category.ADMINISTRATION})
@ResetData
public class TestGeneralConfigurationPage extends BaseJiraWebTest {
    private static final String JIRA_ADMINISTRATOR_USER = "bob";
    private static final String JIRA_ADMINISTRATOR_PASS = "bob";
    private static final String JIRA_ADMINISTRATOR_GROUP_NAME = "admins";

    @Before
    public void setUp() {
        backdoor.usersAndGroups().addGroup(JIRA_ADMINISTRATOR_GROUP_NAME);
        backdoor.permissions().addGlobalPermission(ADMINISTER, JIRA_ADMINISTRATOR_GROUP_NAME);

        backdoor.usersAndGroups().addUser(
                JIRA_ADMINISTRATOR_USER,
                JIRA_ADMINISTRATOR_PASS,
                "bob",
                "bob@bob.com"
        );

        backdoor.usersAndGroups().addUserToGroup(JIRA_ADMINISTRATOR_USER, JIRA_ADMINISTRATOR_GROUP_NAME);
    }

    @AfterClass
    public static void tearDownClass() {
        backdoor.usersAndGroups().deleteUser(JIRA_ADMINISTRATOR_USER);
        backdoor.usersAndGroups().deleteGroup(JIRA_ADMINISTRATOR_GROUP_NAME);
    }

    @Test
    public void announcementBannerSidebarEntryIsNotDisplayedWithoutSysAdministratorPermission() {
        ViewGeneralConfigurationPage configurationPage =
                jira.quickLogin(JIRA_ADMINISTRATOR_USER, JIRA_ADMINISTRATOR_PASS, ViewGeneralConfigurationPage.class);

        PageElement sideBarEntry = configurationPage.findSideBarEntry("edit_announcement");

        assertFalse(sideBarEntry.isPresent());
    }

    @Test
    public void announcementBannerSideBarEntryIsDisplayedForSysAdministrators() {
        ViewGeneralConfigurationPage configurationPage =
                jira.quickLoginAsSysadmin(ViewGeneralConfigurationPage.class);

        PageElement sideBarEntry = configurationPage.findSideBarEntry("edit_announcement");

        assertTrue(sideBarEntry.isPresent());
    }
}

