package com.atlassian.jira.webtest.webdriver.tests.visualregression;

import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.webtests.Permissions;
import com.atlassian.pageobjects.elements.PageElementFinder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.atlassian.jira.permission.ProjectPermissions.ASSIGN_ISSUES;
import static com.atlassian.jira.permission.ProjectPermissions.LINK_ISSUES;
import static com.atlassian.jira.permission.ProjectPermissions.SCHEDULE_ISSUES;
import static com.atlassian.jira.permission.ProjectPermissions.SET_ISSUE_SECURITY;
import static com.atlassian.jira.permission.ProjectPermissions.WORK_ON_ISSUES;

/**
 * <p> Visual regression tests for Create Issue screen.</p>
 * Here we basically check the correctness of rendering system and built-in custom field types.
 *
 * @since v6.1
 */
@WebTest({Category.WEBDRIVER_TEST, Category.VISUAL_REGRESSION})
public class TestVisualRegressionCreateIssue extends JiraVisualRegressionTest {

    private static final Logger log = LoggerFactory.getLogger(TestVisualRegressionCreateIssue.class);
    public static final int ROLE_DEVELOPER = 10001;
    public static final int COPY_OF_DEFAULT_SCHEME = 10001;


    @BeforeClass
    public static void restoreInstance() {
        //we cannot restore instance via @Restore annotation as it doesn't support restoring license from backup file
        backdoor.dataImport().restoreDataFromResource("xml/TestVisualRegressionCreateIssue.zip", "");
    }


    /**
     * Tests probably all possible fields available during Create Issue process
     */
    @Test
    public void testCreateIssuePageDefaultConfig() {
        goTo("/secure/CreateIssue!default.jspa?pid=10000&issuetype=1");
        closeInsiderPopUp();

        //Removing "Date Started" value as it's changing across tests
        pageElementFinder.find(By.id("log-work-date-logged-date-picker")).clear();
        //Disabling refresh after resize as it destroys previous modification
        // and shows "Do you really want to quit" dialog on FF
        visualComparer.setRefreshAfterResize(false);

        assertUIMatches("create-new-bug-default-configuration");
    }

    /**
     * <p>Does some tricks with permissions and then checks whether it is reflected on Create Issue screen</p>
     * <ul>
     * <li>Removes <code>Permissions.USER_PICKER</code> permission - should remove "Due date" field</li>
     * <li>Removes <code>Permissions.SCHEDULE_ISSUE</code> permission - should remove "Due date" field</li>
     * <li>Removes <code>Permissions.ASSIGN_ISSUE</code> permission - should remove assignee field</li>
     * <li>Removes <code>Permissions.LINK_ISSUE</code> permission - should remove link issue section</li>
     * <li>Removes <code>Permissions.WORK_ISSUE</code> permission - should remove worklog section</li>
     * <li>Grants <code>Permissions.SET_ISSUE_SECURITY</code> permission - should add issue security settings</li>
     * </ul>
     */
    @Test
    public void testCreateIssuePageRescrictedConfig() {

        final ProjectPermissionKey[] devPermissionsToRemove = {
                SCHEDULE_ISSUES,
                ASSIGN_ISSUES,
                LINK_ISSUES,
                WORK_ON_ISSUES
        };

        try {
            //should "lock" user picker of user fields
            backdoor.permissions().removeGlobalPermission(Permissions.USER_PICKER, "jira-developers");

            removeDevelopersPermissions(devPermissionsToRemove);
            grantDevelopersPermissions(SET_ISSUE_SECURITY);

            goTo("/secure/CreateIssue!default.jspa?pid=10000&issuetype=1");
            closeInsiderPopUp();
            assertUIMatches("create-new-bug-restricted-configuration");
        } finally {
            backdoor.permissions().addGlobalPermission(Permissions.USER_PICKER, "jira-developers");
            grantDevelopersPermissionsSuppressExceptions(devPermissionsToRemove);
            removeDevelopersPermissionsSuppressExceptions(SET_ISSUE_SECURITY);
        }
    }

    private void removeDevelopersPermissionsSuppressExceptions(ProjectPermissionKey... permissions) {
        for (ProjectPermissionKey permission : permissions) {
            try {
                backdoor.permissionSchemes().removeProjectRolePermission(COPY_OF_DEFAULT_SCHEME, permission, ROLE_DEVELOPER);
            } catch (Exception e) {
                log.warn("Cannot remove permission with id {}", permission);
            }
        }
    }

    private void grantDevelopersPermissionsSuppressExceptions(ProjectPermissionKey... permissions) {
        for (ProjectPermissionKey permission : permissions) {
            try {
                backdoor.permissionSchemes().addProjectRolePermission(COPY_OF_DEFAULT_SCHEME, permission, ROLE_DEVELOPER);
            } catch (Exception e) {
                log.warn("Cannot restore permission with id {}", permission);
            }
        }
    }

    private void removeDevelopersPermissions(ProjectPermissionKey... permissions) {
        for (ProjectPermissionKey permission : permissions) {
            backdoor.permissionSchemes().removeProjectRolePermission(COPY_OF_DEFAULT_SCHEME, permission, ROLE_DEVELOPER);
        }
    }

    private void grantDevelopersPermissions(ProjectPermissionKey... permissions) {
        for (ProjectPermissionKey permission : permissions) {
            backdoor.permissionSchemes().addProjectRolePermission(COPY_OF_DEFAULT_SCHEME, permission, ROLE_DEVELOPER);

        }
    }


}
