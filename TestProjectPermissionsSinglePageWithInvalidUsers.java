package com.atlassian.jira.webtest.webdriver.tests.security;

import com.atlassian.integrationtesting.runner.restore.RestoreOnce;
import com.atlassian.jira.functest.framework.FunctTestConstants;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.config.UserLanguage;
import com.atlassian.jira.pageobjects.pages.EditPermissionsSinglePage;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Note: an invalid user is a user that has been deleted in user management, but still has configuration present in the
 * system.
 */
@WebTest(Category.WEBDRIVER_TEST)
@RestoreOnce("TestPermissionsWithInvalidUsers.xml")
@UserLanguage(user = FunctTestConstants.ADMIN_USERNAME, language = FunctTestConstants.MOON_LOCALE)
public class TestProjectPermissionsSinglePageWithInvalidUsers extends BaseJiraWebTest {

    private static final int DEFAULT_PERMISSION_SCHEME = 0;

    private static final String CREATE_ISSUE_DATA_PERMISSION_KEY = "CREATE_ISSUES";
    private static final String ISSUE_PERMISSIONS_SECTION_TITLE = "admin.permission.group.issue.permissions";
    private static final String ATTACHMENTS_PERMISSIONS_SECTION_TITLE = "admin.permission.group.attachments.permissions";
    private static final String PROJECT_ADMIN_PERMISSION_KEY = "admin.permissions.PROJECT_ADMIN";
    private static final String DELETED_USER = "deleted_user";
    private static final String NORMAL_USER = "normal_user";

    @Test
    public void invalidUsernamesShouldHaveDifferentStyling() {
        final EditPermissionsSinglePage editPermissionsSinglePage = goToPermissionDefaultSchemePage();

        assertTrue("Invalid users should have an additional style class",
                editPermissionsSinglePage.isSingleUserPermDisplayedAsInvalidUser(CREATE_ISSUE_DATA_PERMISSION_KEY, DELETED_USER));
        assertFalse("Valid users should not have an additional style class",
                editPermissionsSinglePage.isSingleUserPermDisplayedAsInvalidUser(CREATE_ISSUE_DATA_PERMISSION_KEY, NORMAL_USER));
    }

    @Test
    public void sectionsWithInvalidUserPermissionsShouldDisplayWarning() {
        final EditPermissionsSinglePage editPermissionsSinglePage = goToPermissionDefaultSchemePage();

        assertTrue("Section with permissions associated with invalid users should show warning message",
                editPermissionsSinglePage.hasInvalidUserWarningForSection(ISSUE_PERMISSIONS_SECTION_TITLE));
        assertFalse("Section with no permissions associated with invalid users should not display a warning",
                editPermissionsSinglePage.hasInvalidUserWarningForSection(ATTACHMENTS_PERMISSIONS_SECTION_TITLE));

        assertTrue("The administer project permission should be present in the warning for the deleted user",
                editPermissionsSinglePage.invalidUserWarningMessageContains(PROJECT_ADMIN_PERMISSION_KEY, DELETED_USER));
        assertFalse("The administer project permission should not have a warning for the valid user",
                editPermissionsSinglePage.invalidUserWarningMessageContains(PROJECT_ADMIN_PERMISSION_KEY, NORMAL_USER));
    }

    @Test
    public void removeDialogShouldShowWarningForInvalidUsers() {
        final EditPermissionsSinglePage editPermissionsSinglePage = goToPermissionDefaultSchemePage();
        final EditPermissionsSinglePage.PermissionsEntry permissionEntry = editPermissionsSinglePage.getPermissionEntry(CREATE_ISSUE_DATA_PERMISSION_KEY);
        final EditPermissionsSinglePage.RevokePermissionsDialog grantPermissionsDialog = permissionEntry.openRevokePermissionDialog();

        assertTrue("Dialog shows warning for deleted user", grantPermissionsDialog.isInvalidUserWarningShownFor(DELETED_USER));
        assertFalse("Dialog should not show warning for valid user", grantPermissionsDialog.isInvalidUserWarningShownFor(NORMAL_USER));

        assertTrue("Deleted username is displayed with additional styling", grantPermissionsDialog.isUsernameDisplayedAsInvalid(DELETED_USER));
        assertFalse("Valid usernames are displayed without additional styling", grantPermissionsDialog.isUsernameDisplayedAsInvalid(NORMAL_USER));
    }

    private EditPermissionsSinglePage goToPermissionDefaultSchemePage() {
        return jira.visit(EditPermissionsSinglePage.class, DEFAULT_PERMISSION_SCHEME);
    }
}
