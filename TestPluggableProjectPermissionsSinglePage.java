package com.atlassian.jira.webtest.webdriver.tests.security;

import com.atlassian.jira.functest.framework.FunctTestConstants;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.config.EnableAUIFlags;
import com.atlassian.jira.pageobjects.config.ResetData;
import com.atlassian.jira.pageobjects.pages.EditPermissionsSinglePage;
import com.atlassian.jira.pageobjects.pages.EditPermissionsSinglePage.RevokePermissionsDialog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.atlassian.jira.pageobjects.pages.EditPermissionsSinglePage.GrantPermissionsDialog;
import static com.atlassian.jira.pageobjects.pages.EditPermissionsSinglePage.PermissionsEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@WebTest(Category.WEBDRIVER_TEST)
@EnableAUIFlags
@ResetData
public class TestPluggableProjectPermissionsSinglePage extends BaseJiraWebTest {
    private static final String PROJECT_PERMISSION_KEY = "func.test.project.permission";
    private static final String PROJECT_PERMISSION_KEY_COMPLETE = "com.atlassian.jira.dev.func-test-plugin:" + PROJECT_PERMISSION_KEY;

    private static final int DEFAULT_PERMISSION_SCHEME = 0;
    private static final String USERS_GROUP = "jira-users";
    private static final String ADMINISTRATORS_GROUP = "jira-administrators";
    private static final String PROJECT_LEAD = "admin.permission.types.project.lead";
    private static final String REPORTER = "admin.permission.types.reporter";

    @Before
    public void setup() {
        enablePermission();
        backdoor.userProfile().changeUserLanguage(FunctTestConstants.ADMIN_USERNAME, FunctTestConstants.MOON_LOCALE);
    }

    @After
    public void tearDown() {
        backdoor.userProfile().changeUserLanguage(FunctTestConstants.ADMIN_USERNAME, "");
    }

    @Test
    public void pluggableProjectPermissionIsDisplayed() {
        final EditPermissionsSinglePage editPermissionsSinglePage = goToPermissionSchemePage();
        assertTrue(editPermissionsSinglePage.hasPermissionEntry(PROJECT_PERMISSION_KEY));
    }

    @Test
    public void pluggableProjectPermissionIsNotDisplayedIfPermissionIsDisabled() {
        disablePermission();

        final EditPermissionsSinglePage editPermissionsSinglePage = goToPermissionSchemePage();
        assertFalse(editPermissionsSinglePage.hasPermissionEntry(PROJECT_PERMISSION_KEY));
    }

    @Test
    public void pluggableProjectPermissionEntitiesAreRestoredAfterReEnabling() {
        EditPermissionsSinglePage editPermissionsSinglePage = goToPermissionSchemePage();
        final PermissionsEntry permissionEntry = editPermissionsSinglePage.getPermissionEntry(PROJECT_PERMISSION_KEY);
        addGroupPermissionToEntry(permissionEntry, USERS_GROUP);

        disablePermission();
        enablePermission();

        assertTrue(permissionEntry.hasPermissionForGroup(USERS_GROUP));
    }

    @Test
    public void canAddAndDeleteSinglePluggableProjectPermissionEntity() {
        final EditPermissionsSinglePage editPermissionsSinglePage = goToPermissionSchemePage();
        final PermissionsEntry permissionsRow = editPermissionsSinglePage.getPermissionEntry(PROJECT_PERMISSION_KEY);

        assertFalse(permissionsRow.hasPermissionForGroup(USERS_GROUP));

        addGroupPermissionToEntry(permissionsRow, USERS_GROUP);
        assertTrue(permissionsRow.hasPermissionForGroup(USERS_GROUP));

        removePermissionFromEntry(permissionsRow, USERS_GROUP);
        assertFalse(permissionsRow.hasPermissionForGroup(USERS_GROUP));
    }

    @Test
    public void canAddAndDeleteMultiplePluggableProjectPermissionEntities() {
        final EditPermissionsSinglePage editPermissionsSinglePage = goToPermissionSchemePage();
        final PermissionsEntry permissionsRow = editPermissionsSinglePage.getPermissionEntry(PROJECT_PERMISSION_KEY);

        assertFalse(permissionsRow.hasPermissionForGroup(USERS_GROUP));
        assertFalse(permissionsRow.hasPermissionForGroup(ADMINISTRATORS_GROUP));

        addGroupPermissionToEntry(permissionsRow, USERS_GROUP);
        assertTrue(permissionsRow.hasPermissionForGroup(USERS_GROUP));
        assertFalse(permissionsRow.hasPermissionForGroup(ADMINISTRATORS_GROUP));

        addGroupPermissionToEntry(permissionsRow, ADMINISTRATORS_GROUP);
        assertTrue(permissionsRow.hasPermissionForGroup(USERS_GROUP));
        assertTrue(permissionsRow.hasPermissionForGroup(ADMINISTRATORS_GROUP));

        removePermissionFromEntry(permissionsRow, USERS_GROUP);
        assertFalse(permissionsRow.hasPermissionForGroup(USERS_GROUP));
        assertTrue(permissionsRow.hasPermissionForGroup(ADMINISTRATORS_GROUP));
    }

    @Test
    public void canAddAndDeleteEntitiesThatDoNotRequireAdditionalParameters() {
        final EditPermissionsSinglePage editPermissionsSinglePage = goToPermissionSchemePage();
        final PermissionsEntry permissionsRow = editPermissionsSinglePage.getPermissionEntry(PROJECT_PERMISSION_KEY);

        assertFalse(permissionsRow.hasSecurityTypeWithNoInputField(PROJECT_LEAD));
        assertFalse(permissionsRow.hasSecurityTypeWithNoInputField(REPORTER));

        addSecurityTypeWithNoInputField(editPermissionsSinglePage, PROJECT_LEAD);

        assertTrue(permissionsRow.hasSecurityTypeWithNoInputField(PROJECT_LEAD));
        assertFalse(permissionsRow.hasSecurityTypeWithNoInputField(REPORTER));

        addSecurityTypeWithNoInputField(editPermissionsSinglePage, REPORTER);

        assertTrue(permissionsRow.hasSecurityTypeWithNoInputField(PROJECT_LEAD));
        assertTrue(permissionsRow.hasSecurityTypeWithNoInputField(REPORTER));
    }

    private void addSecurityTypeWithNoInputField(final EditPermissionsSinglePage editPermissionsSinglePage, final String securityType) {
        final PermissionsEntry permissionEntry = editPermissionsSinglePage.getPermissionEntry(PROJECT_PERMISSION_KEY);

        final GrantPermissionsDialog grantPermissionsDialog = permissionEntry.openGrantPermissionDialog();
        grantPermissionsDialog.showMore();
        grantPermissionsDialog.selectSecurityTypeWithNoInputField(securityType);
        grantPermissionsDialog.submitAssertSuccessful();
    }

    private EditPermissionsSinglePage goToPermissionSchemePage() {
        return goToPermissionSchemePage(DEFAULT_PERMISSION_SCHEME);
    }

    private EditPermissionsSinglePage goToPermissionSchemePage(final int schemeId) {
        return jira.visit(EditPermissionsSinglePage.class, schemeId);
    }

    private void enablePermission() {
        backdoor.plugins().enablePluginModule(PROJECT_PERMISSION_KEY_COMPLETE);
    }

    private void disablePermission() {
        backdoor.plugins().disablePluginModule(PROJECT_PERMISSION_KEY_COMPLETE);
    }

    private void addGroupPermissionToEntry(PermissionsEntry permEntry, String group) {
        final GrantPermissionsDialog grantPermissionsDialog = permEntry.openGrantPermissionDialog();
        grantPermissionsDialog.setGroup(group);
        grantPermissionsDialog.submitAssertSuccessful();
    }

    private void removePermissionFromEntry(PermissionsEntry permissionsEntry, String permissionName) {
        final RevokePermissionsDialog grantPermissionsDialog = permissionsEntry.openRevokePermissionDialog();
        grantPermissionsDialog.markForRemoval(permissionName);
        grantPermissionsDialog.submit();
    }
}
