package com.atlassian.jira.webtest.webdriver.tests.security;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.pages.AddPermissionPage;
import com.atlassian.jira.pageobjects.pages.DeletePermissionPage;
import com.atlassian.jira.pageobjects.pages.EditPermissionsPage;
import org.junit.Before;
import org.junit.Test;

import static com.atlassian.jira.pageobjects.pages.EditPermissionsPage.PermissionsRow;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@WebTest(Category.WEBDRIVER_TEST)
public class TestPluggableProjectPermissions extends BaseJiraWebTest {
    private static final String PROJECT_PERMISSION_KEY_COMPLETE = "com.atlassian.jira.dev.func-test-plugin:func.test.project.permission";
    private static final String PROJECT_PERMISSION_NAME = "func.test.project.permission.name";

    private static final int DEFAULT_PERMISSION_SCHEME = 0;
    private static final String USERS_GROUP = "jira-users";

    @Before
    public void setup() {
        backdoor.restoreBlankInstance();
        backdoor.darkFeatures().enableForSite("com.atlassian.jira.permission-schemes.single-page-ui.disabled");
        enablePermission();
    }

    @Test
    public void pluggableProjectPermissionIsDisplayed() {
        EditPermissionsPage editPermissionsPage = goToPermissionSchemePage();
        assertTrue(editPermissionsPage.hasPermissionRow(PROJECT_PERMISSION_NAME));
    }

    @Test
    public void pluggableProjectPermissionIsNotDisplayedIfPermissionIsDisabled() {
        disablePermission();

        EditPermissionsPage editPermissionsPage = goToPermissionSchemePage();
        assertFalse(editPermissionsPage.hasPermissionRow(PROJECT_PERMISSION_NAME));
    }

    @Test
    public void canAddAndDeletePluggableProjectPermissionEntities() {
        EditPermissionsPage editPermissionsPage = addPermissionGroup();

        PermissionsRow permissionsRow = editPermissionsPage.getPermissionsRowByPermission(PROJECT_PERMISSION_NAME);
        assertTrue(permissionsRow.hasPermissionForGroup(USERS_GROUP));

        DeletePermissionPage deletePermissionPage = editPermissionsPage.deleteForGroup(PROJECT_PERMISSION_NAME, USERS_GROUP);
        editPermissionsPage = deletePermissionPage.delete();

        permissionsRow = editPermissionsPage.getPermissionsRowByPermission(PROJECT_PERMISSION_NAME);
        assertFalse(permissionsRow.hasPermissionForGroup(USERS_GROUP));
    }

    @Test
    public void pluggableProjectPermissionEntitiesAreRestoredAfterReEnabling() {
        addPermissionGroup();

        disablePermission();
        enablePermission();

        EditPermissionsPage editPermissionsPage = goToPermissionSchemePage();
        PermissionsRow permissionsRow = editPermissionsPage.getPermissionsRowByPermission(PROJECT_PERMISSION_NAME);
        assertTrue(permissionsRow.hasPermissionForGroup(USERS_GROUP));
    }

    @Test
    public void pluggableProjectPermissionIsNotDisplayIfConditionsAreNotFulfilled() {
        backdoor.applicationProperties().setOption(APKeys.JIRA_OPTION_VOTING, false);

        EditPermissionsPage editPermissionsPage = goToPermissionSchemePage();
        assertFalse(editPermissionsPage.hasPermissionRow(PROJECT_PERMISSION_NAME));

        backdoor.applicationProperties().setOption(APKeys.JIRA_OPTION_VOTING, true);
    }

    private EditPermissionsPage addPermissionGroup() {
        EditPermissionsPage editPermissionsPage = goToPermissionSchemePage();

        AddPermissionPage addPermissionPage = editPermissionsPage.addForPermission(PROJECT_PERMISSION_NAME);
        addPermissionPage.setGroup(USERS_GROUP);

        return addPermissionPage.add();
    }

    private EditPermissionsPage goToPermissionSchemePage() {
        return jira.visit(EditPermissionsPage.class, DEFAULT_PERMISSION_SCHEME);
    }

    private void enablePermission() {
        backdoor.plugins().enablePluginModule(PROJECT_PERMISSION_KEY_COMPLETE);
    }

    private void disablePermission() {
        backdoor.plugins().disablePluginModule(PROJECT_PERMISSION_KEY_COMPLETE);
    }
}
