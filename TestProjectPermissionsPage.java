package com.atlassian.jira.webtest.webdriver.tests.security;

import com.atlassian.integrationtesting.runner.restore.Restore;
import com.atlassian.jira.functest.framework.FunctTestConstants;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.config.EnableAUIFlags;
import com.atlassian.jira.pageobjects.elements.AuiFlag;
import com.atlassian.jira.pageobjects.elements.GlobalFlags;
import com.atlassian.jira.pageobjects.elements.LozengeUsedBy;
import com.atlassian.jira.pageobjects.pages.EditPermissionsSinglePage;
import com.atlassian.jira.pageobjects.pages.EditPermissionsSinglePage.GrantPermissionsDialog;
import com.atlassian.jira.pageobjects.pages.EditPermissionsSinglePage.PermissionsEntry;
import com.atlassian.jira.pageobjects.pages.admin.AnnouncementBannerPage;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.testkit.beans.PermissionSchemeAttributeBean;
import com.atlassian.jira.testkit.beans.UserDTO;
import com.atlassian.jira.testkit.client.model.FeatureFlag;
import com.atlassian.jira.testkit.client.restclient.PermissionSchemeRestClient;
import com.atlassian.jira.webtest.webdriver.util.AUIFlags;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static com.atlassian.jira.permission.PermissionSchemeAttributeManager.AttributeKeys.EXTENDED_ADMINISTER_PROJECTS_ENABLED_ATTRIBUTE;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@WebTest(Category.WEBDRIVER_TEST)
@Restore("xml/TestProjectPermission.xml")
@EnableAUIFlags
public class TestProjectPermissionsPage extends BaseJiraWebTest {
    private static final long DEFAULT_PERMISSION_SCHEME = 0L;
    private static final String INVALID_SCHEME_ID_MESSAGE_KEY = "admin.permission.project.invalid.id.requested";
    private static final String SUCCESS_GRANT_SINGLE = "admin.permissions.feedback.successfulgrant.single";

    private static final String PROJECT_PERMISSION_KEY = "func.test.project.permission";
    private static final String PROJECT_PERMISSION_NAME = PROJECT_PERMISSION_KEY + ".name";
    private static final String PROJECT_PERMISSION_KEY_COMPLETE = "com.atlassian.jira.dev.func-test-plugin:" + PROJECT_PERMISSION_KEY;
    private static final String USERS_GROUP = "jira-users";

    private static PermissionSchemeRestClient permissionSchemeRestClient = new PermissionSchemeRestClient(jira.environmentData());

    public static final FeatureFlag EXTENDEDADMIN_HIDE_FEATURE = FeatureFlag.featureFlag("admin.permissions.extendedadmin.hide.checkbox");

    @Inject
    PageElementFinder elementFinder;

    private AUIFlags auiFlags;

    @Before
    public void setup() {
        auiFlags = pageBinder.bind(AUIFlags.class);
        backdoor.userProfile().changeUserLanguage(FunctTestConstants.ADMIN_USERNAME, FunctTestConstants.MOON_LOCALE);
        backdoor.plugins().enablePluginModule(PROJECT_PERMISSION_KEY_COMPLETE);
    }

    @After
    public void tearDown() {
        backdoor.userProfile().changeUserLanguage(FunctTestConstants.ADMIN_USERNAME, "");
    }

    @Test
    public void headerPluginPointIsAvailableWhenPageIsFullyLoaded() {
        final EditPermissionsSinglePage editPermissionsSinglePage = jira.visit(EditPermissionsSinglePage.class, DEFAULT_PERMISSION_SCHEME);
        assertTrue("header plugin point should be available", editPermissionsSinglePage.hasHeaderPluginPoint());
    }

    @Test
    public void userStillAbleToViewIssueAfterRename() {
        // The user with userKey and userName 3cpO
        // <ApplicationUser id="10100" userKey="3cpo" lowerUserName="3cpo"/>
        // is assigned to the "Star Wars Permission Scheme"
        // <SchemePermissions id="10252" scheme="10000" type="user" parameter="3cpo" permissionKey="BROWSE_PROJECTS"/>
        // The user should be able to view the issue
        userShouldBeAbleToViewIssue("3cpO", "3cpO", "R2D2-1");

        // Confirm darthVader username does not exist
        final boolean doesUserNameExist = jira.backdoor().usersAndGroups().userExists("darthVader");
        assertThat("darthVader should not exist as username", doesUserNameExist, Matchers.is(false));
        // After renaming the user 3cpO to darthVader
        renameUser("3cpo", "darthVader");
        // <ApplicationUser id="10100" userKey="3cpo" lowerUserName="darthvader"/>

        // the permission scheme still has the parameter="3cpo"
        // <SchemePermissions id="10252" scheme="10000" type="user" parameter="3cpo" permissionKey="BROWSE_PROJECTS"/>
        // Verify that user can still see issue
        userShouldBeAbleToViewIssue("darthVader", "3cpO", "R2D2-1");
    }

    @Test
    public void addingSingleUserToCreateIssuePermission() {
        String username = "greg";
        String password = "password";
        String issueKey = "TESPM-1";
        userShouldNotBeAbleToViewIssue(username, password, issueKey);

        // Login as admin
        jira.logout();
        jira.quickLoginAsAdmin();
        setCrossOriginIframeInAnnouncementBanner();

        // Grant Greg permission to create an issue
        final EditPermissionsSinglePage editPermissionsSinglePage = jira.visit(EditPermissionsSinglePage.class, DEFAULT_PERMISSION_SCHEME);
        final PermissionsEntry permissionEntry = editPermissionsSinglePage.getPermissionEntry(ProjectPermissions.BROWSE_PROJECTS);
        GrantPermissionsDialog grantPermissionsDialog = permissionEntry.openGrantPermissionDialog();
        //tests if cancel is working - hides the aui blanket
        grantPermissionsDialog.cancel();
        grantPermissionsDialog = permissionEntry.openGrantPermissionDialog();
        grantPermissionsDialog.setSingleUser(username);
        grantPermissionsDialog.submitAssertSuccessful();

        userShouldBeAbleToViewIssue(username, password, issueKey);
    }

    private void renameUser(String orgUserName, String newName) {
        final UserDTO userToRename = jira.backdoor().usersAndGroups().getUserByName(orgUserName);
        backdoor.usersAndGroups().updateUser(new UserDTO(true,
                userToRename.getDirectoryId(),
                newName + " Display Name",
                newName + "@example.com",
                userToRename.getKey(),
                newName,
                newName,
                0L));
    }

    private void userShouldNotBeAbleToViewIssue(String userName, String password, String issueKey) {
        jira.logout();
        jira.gotoLoginPage().loginAndGoToHome(userName, password);
        jira.visitDelayed(ViewIssuePage.class, issueKey);
        Poller.waitUntilTrue("message", elementFinder.find(By.className("issue-error-content")).timed().isPresent());
        jira.logout();
    }

    private void userShouldBeAbleToViewIssue(String userName, String password, String issueKey) {
        jira.logout();
        final ViewIssuePage viewIssuePage = jira.gotoLoginPage().login(userName, password, ViewIssuePage.class, issueKey);
        assertTrue(viewIssuePage.isAt().now());
        jira.logout();
    }

    @Test
    public void sharedByLozengeShouldBeVisibleForSharedPermissionSchemes() {
        setCrossOriginIframeInAnnouncementBanner();
        backdoor.userProfile().changeUserLanguage(FunctTestConstants.ADMIN_USERNAME, "");

        final EditPermissionsSinglePage editPermissionsSinglePage = jira.visit(EditPermissionsSinglePage.class, DEFAULT_PERMISSION_SCHEME);
        final LozengeUsedBy usedBy = editPermissionsSinglePage.getUsedBy();

        assertTrue("shared by container is present", usedBy.isPresent().now());
        assertTrue("shared by trigger element is present", usedBy.hasProjects());
        assertFalse("shared by dialog is closed", usedBy.getProjects().isOpen().now());
        assertThat("Should be able to see associated projects in inline dialog", usedBy.getProjectsNames().get(), equalTo(singletonList("TestProjectPermissions")));
    }

    @Test
    public void loadingPageWithAnInvalidIdDisplaysWarning() {
        jira.visitDelayed(EditPermissionsSinglePage.class, 12345); // any invalid scheme id

        GlobalFlags flags = pageBinder.bind(GlobalFlags.class);
        Poller.waitUntilTrue(flags.flagContainerPresent());

        assertThat("user was redirected to the listing page", jira.getTester().getDriver().getCurrentUrl(), containsString("ViewPermissionSchemes"));

        AuiFlag invalidPermissionSchemeIdFlag = flags.getFlagWithText(INVALID_SCHEME_ID_MESSAGE_KEY);
        assertNotNull("flag notifying of an invalid permission scheme should be present", invalidPermissionSchemeIdFlag);
        assertEquals("flag should be a warning", AuiFlag.Type.WARNING, invalidPermissionSchemeIdFlag.getType());

        invalidPermissionSchemeIdFlag.dismiss();
        assertThat("flag about invalid permission scheme was dismissed", flags.doesNotContainFlagWithText(INVALID_SCHEME_ID_MESSAGE_KEY), is(true));
    }

    @Test
    public void grantPermissionButtonIsPresent() {
        final EditPermissionsSinglePage editPermissionsSinglePage = jira.visit(EditPermissionsSinglePage.class, DEFAULT_PERMISSION_SCHEME);
        assertTrue("Grant permission button should be present", editPermissionsSinglePage.hasGrantPermissionButton());
    }

    @Test
    public void grantPermissionButtonOpensDialogWithNoPermissionKey() {
        setCrossOriginIframeInAnnouncementBanner();
        final EditPermissionsSinglePage editPermissionsSinglePage = jira.visit(EditPermissionsSinglePage.class, DEFAULT_PERMISSION_SCHEME);
        auiFlags.closeAllFlags();
        GrantPermissionsDialog grantPermissionsDialog = editPermissionsSinglePage.openDialogFromGrantPermissionButton();

        MatcherAssert.assertThat("Dialog should open without a permission specified", grantPermissionsDialog.getPermissionName(), equalTo(""));
    }

    @Test
    // This test is flaky. See: https://bulldog.internal.atlassian.com/browse/MNSTR-764
    public void canAddPermissionFromGrantPermissionButton() {
        setCrossOriginIframeInAnnouncementBanner();
        final EditPermissionsSinglePage editPermissionsSinglePage = jira.visit(EditPermissionsSinglePage.class, DEFAULT_PERMISSION_SCHEME);
        auiFlags.closeAllFlags();
        final PermissionsEntry permissionsRow = editPermissionsSinglePage.getPermissionEntry(PROJECT_PERMISSION_KEY);
        final GrantPermissionsDialog grantPermissionsDialog = editPermissionsSinglePage.openDialogFromGrantPermissionButton();

        assertFalse("Should not have permission yet", permissionsRow.hasPermissionForGroup(USERS_GROUP));

        grantPermissionsDialog.setPermissionName(PROJECT_PERMISSION_NAME);
        grantPermissionsDialog.setGroup(USERS_GROUP);
        grantPermissionsDialog.submitAssertSuccessful();

        assertTrue("Permission should have been granted", permissionsRow.hasPermissionForGroup(USERS_GROUP));
    }

    @Test
    public void extendedPermissionsShouldBeVisibleWhenDarkFeatureIsUnset() {
        final EditPermissionsSinglePage editPermissionsSinglePage = jira.visit(EditPermissionsSinglePage.class, DEFAULT_PERMISSION_SCHEME);
        editPermissionsSinglePage.assertExtendedPermissionIsVisible();
    }

    @Test
    public void extendedPermissionsShouldBeHiddenWhenDarkFeatureIsSet() {
        try {
            backdoor.darkFeatures().enableForSite(EXTENDEDADMIN_HIDE_FEATURE);

            final EditPermissionsSinglePage editPermissionsSinglePage = jira.visit(EditPermissionsSinglePage.class, DEFAULT_PERMISSION_SCHEME);
            editPermissionsSinglePage.assertExtendedPermissionIsHidden();
        } finally {
            backdoor.darkFeatures().resetForSite(EXTENDEDADMIN_HIDE_FEATURE);
        }
    }

    @Test
    public void shouldCheckExtendedProjectAdminPermission() {
        permissionSchemeRestClient = new PermissionSchemeRestClient(jira.environmentData());
        try {
            // default state - tuned on
            final EditPermissionsSinglePage editPermissionsSinglePage = jira.visit(EditPermissionsSinglePage.class, DEFAULT_PERMISSION_SCHEME);
            assertExtendedAdmin(true);
            editPermissionsSinglePage.assertExtendedPermissionIsChecked();


            // switching off
            editPermissionsSinglePage.toggleExtendedProjectAdminCheckbox();
            Poller.waitUntilTrue(editPermissionsSinglePage.hasOptOutFlagDialogDisplayed());

            jira.getTester().getDriver().navigate().refresh();
            assertExtendedAdmin(false);
            editPermissionsSinglePage.assertExtendedPermissionIsUnchecked();


            //switching on again
            editPermissionsSinglePage.toggleExtendedProjectAdminCheckbox();

            jira.getTester().getDriver().navigate().refresh();
            assertExtendedAdmin(true);
            editPermissionsSinglePage.assertExtendedPermissionIsChecked();
        } finally {
            PermissionSchemeAttributeBean bean = new PermissionSchemeAttributeBean(EXTENDED_ADMINISTER_PROJECTS_ENABLED_ATTRIBUTE, "true");
            permissionSchemeRestClient.setAttribute(DEFAULT_PERMISSION_SCHEME, bean);
        }
    }

    @Test
    public void canRemovePermissionFromRemovePermissionButton() {
        setCrossOriginIframeInAnnouncementBanner();
        final EditPermissionsSinglePage editPermissionsSinglePage = jira.visit(EditPermissionsSinglePage.class, DEFAULT_PERMISSION_SCHEME);
        auiFlags.closeAllFlags();
        final PermissionsEntry permissionsRow = editPermissionsSinglePage.getPermissionEntry(PROJECT_PERMISSION_KEY);
        final GrantPermissionsDialog grantPermissionsDialog = editPermissionsSinglePage.openDialogFromGrantPermissionButton();

        assertFalse("Should not have permission yet", permissionsRow.hasPermissionForGroup(USERS_GROUP));

        grantPermissionsDialog.setPermissionName(PROJECT_PERMISSION_NAME);
        grantPermissionsDialog.setGroup(USERS_GROUP);
        grantPermissionsDialog.submitAssertSuccessful();

        assertTrue("Permission should have been granted", permissionsRow.hasPermissionForGroup(USERS_GROUP));

        EditPermissionsSinglePage.RevokePermissionsDialog revokePermissionsDialog = editPermissionsSinglePage.getPermissionEntry(PROJECT_PERMISSION_KEY).openRevokePermissionDialog();
        //tests if cancel is working - hides the aui blanket
        revokePermissionsDialog.cancel();
        revokePermissionsDialog = editPermissionsSinglePage.getPermissionEntry(PROJECT_PERMISSION_KEY).openRevokePermissionDialog();
        revokePermissionsDialog.markForRemoval(USERS_GROUP);
        revokePermissionsDialog.submit();

        assertFalse("Permission should have been revoked", permissionsRow.hasPermissionForGroup(USERS_GROUP));
    }

    private void assertExtendedAdmin(boolean isTurnedOn) {
        final PermissionSchemeAttributeBean bean = permissionSchemeRestClient.getAttribute(DEFAULT_PERMISSION_SCHEME, EXTENDED_ADMINISTER_PROJECTS_ENABLED_ATTRIBUTE).body;
        assertEquals(isTurnedOn, Boolean.valueOf(bean.getValue()));
    }

    private void setCrossOriginIframeInAnnouncementBanner() {
        final AnnouncementBannerPage announcementBannerPage = pageBinder.navigateToAndBind(AnnouncementBannerPage.class);
        announcementBannerPage
                .fillNewAnnouncement("<iframe id=\"cross-origin-iframe\" style=\"height: 50px;\" src=\"http://example.com\"></iframe>")
                .selectPublicVisibility()
                .setAnnouncement();
    }
}
