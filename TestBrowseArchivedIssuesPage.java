package com.atlassian.jira.webtest.webdriver.tests.admin.archiving;

import com.atlassian.jira.functest.framework.DevMode;
import com.atlassian.jira.functest.framework.backdoor.PermissionControlExt;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.functest.rule.SinceBuildRule.SinceBuild;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.components.menu.IssuesMenu;
import com.atlassian.jira.pageobjects.config.EnableAnalytics;
import com.atlassian.jira.pageobjects.config.LoginAs;
import com.atlassian.jira.pageobjects.framework.util.TimedQueryFactory;
import com.atlassian.jira.pageobjects.pages.admin.archiving.BrowseArchivePage;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.testkit.client.model.FeatureFlag;
import com.atlassian.jira.webtests.LicenseKeys;
import com.atlassian.pageobjects.binder.PageBindingException;
import com.atlassian.pageobjects.binder.PageBindingWaitException;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.DefaultTimeouts;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static com.atlassian.jira.functest.framework.FunctTestConstants.ADMIN_USERNAME;
import static com.atlassian.jira.functest.framework.suite.Category.WEBDRIVER_TEST;
import static com.atlassian.jira.testkit.client.model.FeatureFlag.featureFlag;
import static com.atlassian.pageobjects.elements.query.Conditions.forSupplier;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@WebTest(WEBDRIVER_TEST)
@LoginAs(user = ADMIN_USERNAME)
@DevMode(enabled = false)
@EnableAnalytics
public class TestBrowseArchivedIssuesPage extends BaseJiraWebTest {
    private static final FeatureFlag ISSUE_ARCHIVING_FEATURE_FLAG = featureFlag("com.atlassian.jira.issues.archiving");
    private static final FeatureFlag ARCHIVE_BROWSE_FEATURE_FLAG = featureFlag("com.atlassian.jira.issues.archiving.browse");

    @Inject
    private TimedQueryFactory queryFactory;
    private PermissionControlExt permissionsControl;

    @Before
    public void setUp() throws Exception {
        backdoor.restoreBlankInstance(LicenseKeys.COMMERCIAL_FOR_ARCHIVING);
        backdoor.darkFeatures().enableForSite(ARCHIVE_BROWSE_FEATURE_FLAG);

        permissionsControl = new PermissionControlExt(jira.environmentData());
    }

    @After
    public void tearDown() {
        backdoor.darkFeatures().enableForSite(ISSUE_ARCHIVING_FEATURE_FLAG);
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testEmptyState() {
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);

        assertThat(backdoor.archive().list().getArchivedIssues(), is(empty()));
        assertTrue(browsePage.isEmpty());
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testHappyPath() {
        jira.quickLoginAsSysadmin();
        final String issueArchived = backdoor.issues().createIssue("HSP", "Issue archived").key;
        final String projectArchived = backdoor.issues().createIssue("HSP", "Project archived").key;
        backdoor.issues().archiveIssue(issueArchived);
        backdoor.project().archiveProject("HSP");

        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
        browsePage.clickSearchButton();

        final List<BrowseArchivePage.ArchiveRow> rows = Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(2));
        assertThat(rows.stream().map(row -> row.getColumnText("Key")).collect(Collectors.toList()), contains(projectArchived, issueArchived));
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testIssuesMenu_enabled() {
        jira.quickLoginAsSysadmin();
        jira.gotoHomePage();
        pageBinder.bind(IssuesMenu.class).open().archivedIssues();
        pageBinder.bind(BrowseArchivePage.class);
    }

    @Test
    @SinceBuild(buildNumber = 804000)
    @com.atlassian.jira.pageobjects.config.LoginAs(user = "fred")
    public void testIssuesMenu_enabledForUserWithGlobalAccess() {
        permissionsControl.addGlobalPermissionByKey("GLOBAL_BROWSE_ARCHIVE", "jira-users");
        jira.gotoHomePage();
        pageBinder.bind(IssuesMenu.class).open().archivedIssues();
        pageBinder.bind(BrowseArchivePage.class);
    }

    @Test
    @SinceBuild(buildNumber = 804000)
    @com.atlassian.jira.pageobjects.config.LoginAs(user = "fred")
    public void testIssuesMenu_enabledForUserWithProjectAccess() {
        backdoor.permissionSchemes().addUserPermission(0L, ProjectPermissions.BROWSE_ARCHIVE, "fred");
        jira.gotoHomePage();
        pageBinder.bind(IssuesMenu.class).open().archivedIssues();
        pageBinder.bind(BrowseArchivePage.class);
    }

    @Test(expected = NoSuchElementException.class)
    @SinceBuild(buildNumber = 803000)
    public void testIssuesMenu_disabled() {
        backdoor.darkFeatures().disableForSite(ARCHIVE_BROWSE_FEATURE_FLAG);

        jira.quickLoginAsSysadmin();
        jira.gotoHomePage();
        pageBinder.bind(IssuesMenu.class).open().archivedIssues();
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testHappyPath_columns() {
        jira.quickLoginAsSysadmin();
        createArchivedIssue();
        backdoor.project().addProject("ARCH", "ARCH", "admin");
        final String projectArchived = backdoor.issues().createIssue("ARCH", "Project archived").key;
        backdoor.project().archiveProject("ARCH");

        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
        browsePage.clickSearchButton();

        final List<BrowseArchivePage.ArchiveRow> rows = Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(2));
        final BrowseArchivePage.ArchiveRow rowProject = rows.get(0);
        final BrowseArchivePage.ArchiveRow rowIssue = rows.get(1);

        assertEquals(rowProject.getColumnText("Project"), "ARCH (Archived)");
        assertEquals(rowIssue.getColumnText("Project"), "homosapien");

        assertEquals(rowProject.getColumnText("Status"), "OPEN");
        assertEquals(rowIssue.getColumnText("Status"), "RESOLVED");

        assertEquals(rowProject.getColumnText("Resolved"), "");
        assertEquals(rowIssue.getColumnText("Resolved"), rowIssue.getColumnText("Created"));

        assertEquals(rowProject.getColumnText("Status"), "OPEN");
        assertEquals(rowIssue.getColumnText("Status"), "RESOLVED");

        assertEquals(rowProject.getColumnText("Reporter"), "Administrator");
        assertEquals(rowIssue.getColumnText("Reporter"), "Administrator");

        assertEquals(rowProject.getColumnText("Archived by"), "Administrator");
        assertEquals(rowIssue.getColumnText("Archived by"), "Administrator");

        assertEquals(rowProject.getColumnText("Assignee"), "Administrator");
        assertEquals(rowIssue.getColumnText("Assignee"), "Administrator");

        assertEvent("admin.browse.archived.issues.view");
    }

    @Test(expected = PageBindingWaitException.class)
    @SinceBuild(buildNumber = 803000)
    public void testFeatureFlag_archiving() {
        backdoor.darkFeatures().disableForSite(ISSUE_ARCHIVING_FEATURE_FLAG);

        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
    }

    @Test(expected = PageBindingWaitException.class)
    @SinceBuild(buildNumber = 803000)
    public void testFeatureFlag_browse() {
        backdoor.darkFeatures().disableForSite(ARCHIVE_BROWSE_FEATURE_FLAG);

        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
    }

    @Test(expected = PageBindingWaitException.class)
    @SinceBuild(buildNumber = 803000)
    public void testLicenseCheck() {
        backdoor.restoreBlankInstance(LicenseKeys.COMMERCIAL);

        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testPermissionCheck() {
        backdoor.usersAndGroups().addUser("justadmin");
        backdoor.permissions().addGlobalPermission(Permissions.ADMINISTER, "jira-users");
        jira.quickLogin("fred", "fred");

        try {
            jira.visit(BrowseArchivePage.class);
            throw new AssertionError("Expected to fail archive page bind");
        } catch (PageBindingException e) {
            // TBD
            // final JiraLoginPage loginPage = pageBinder.bind(JiraLoginPage.class);
            // assertThat(loginPage.getMessages().iterator().next().getText(), containsString("'Fred Normal' does not have permission to access this page."));
        }
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testArchivedByLinkSendAnalytics() {
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
        browsePage.clickSearchButton();

        final BrowseArchivePage.ArchiveRow row = Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1)).get(0);
        row.clickCellLink("Archived by");
        assertEvent("admin.browse.archived.issue.archivedby.click");
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testProjectLinkSendAnalytics() {
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
        browsePage.clickSearchButton();

        final BrowseArchivePage.ArchiveRow row = Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1)).get(0);
        row.clickCellLink("Project");
        assertEvent("admin.browse.archived.issue.project.click");
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testIssueKeyLinkSendAnalytics() {
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
        browsePage.clickSearchButton();

        final BrowseArchivePage.ArchiveRow row = Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1)).get(0);
        row.clickCellLink("Key");
        assertEvent("admin.browse.archived.issue.key.click");
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testSummaryLinkSendAnalytics() {
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
        browsePage.clickSearchButton();

        final BrowseArchivePage.ArchiveRow row = Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1)).get(0);
        row.clickCellLink("Summary");
        assertEvent("admin.browse.archived.issue.summary.click");
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testReporterLinkSendAnalytics() {
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
        browsePage.clickSearchButton();

        final BrowseArchivePage.ArchiveRow row = Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1)).get(0);
        row.clickCellLink("Reporter");
        assertEvent("admin.browse.archived.issue.reporter.click");
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testAssigneeLinkSendAnalytics() {
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
        browsePage.clickSearchButton();

        final BrowseArchivePage.ArchiveRow row = Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1)).get(0);
        row.clickCellLink("Assignee");
        assertEvent("admin.browse.archived.issue.assignee.click");
    }

    @Test
    @SinceBuild(buildNumber = 803000)
    public void testSearchButton() {
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
        browsePage.clickSearchButton();

        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1));
        createArchivedIssue();
        browsePage.clickSearchButton();

        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(2));
    }

    @Test
    @EnableAnalytics
    @SinceBuild(buildNumber = 803000)
    public void testDateFromFilter() {
        final DateTime today = new DateTime();
        final DateTime tomorrow = today.plusDays(1);
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);

        browsePage.clickDatesFilterButton();
        browsePage.setDateFrom(tomorrow.toString("MMM/dd/yyyy"));
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(0));

        browsePage.clickDatesFilterButton();
        browsePage.setDateFrom(today.toString("MMM/dd/yyyy"));
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1));
        assertEvent("admin.browse.archived.issues.filter.date.update");
    }

    @Test
    @EnableAnalytics
    @SinceBuild(buildNumber = 803000)
    public void testDateToFilter() {
        final DateTime today = new DateTime();
        final DateTime yesterday = today.minusDays(1);
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);

        browsePage.clickDatesFilterButton();
        browsePage.setDateTo(yesterday.toString("MMM/dd/yyyy"));
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(0));

        browsePage.clickDatesFilterButton();
        browsePage.setDateTo(today.toString("MMM/dd/yyyy"));
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1));
        assertEvent("admin.browse.archived.issues.filter.date.update");
    }

    @Test
    @EnableAnalytics
    @SinceBuild(buildNumber = 803000)
    public void testArchiverFilter() {
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);

        PageElement archiverFilter = browsePage.getArchiverFilterInput();
        archiverFilter.javascript().execute("arguments[0].dispatchEvent(new Event('focus'))");
        assertEvent("admin.browse.archived.issues.archiver.filter.dropdown.click");

        archiverFilter.type("fred");
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getArchiverOptions), hasSize(1));
        archiverFilter.type(Keys.ENTER);
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(0));
        assertEvent("admin.browse.archived.issues.filter.applied");

        dropAnalyticsEvents();
        archiverFilter.clear();

        archiverFilter.type("admin");
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getArchiverOptions), hasSize(1));
        archiverFilter.type(Keys.ENTER);
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1));
        assertEvent("admin.browse.archived.issues.filter.applied");
    }

    @Test
    @EnableAnalytics
    @SinceBuild(buildNumber = 803000)
    public void testProjectsFilter() {
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);

        PageElement projectsFilter = browsePage.getProjectsFilterInput();
        projectsFilter.javascript().execute("arguments[0].dispatchEvent(new Event('focus'))");
        assertEvent("admin.browse.archived.issues.project.filter.dropdown.click");

        projectsFilter.type("MKY");
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getProjectOptions), hasSize(1));
        projectsFilter.type(Keys.ENTER);
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(0));
        assertEvent("admin.browse.archived.issues.filter.applied");

        dropAnalyticsEvents();
        projectsFilter.clear();

        projectsFilter.type(Keys.ENTER);
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1));
        assertEvent("admin.browse.archived.issues.filter.applied");
    }

    @Test
    @EnableAnalytics
    @SinceBuild(buildNumber = 803000)
    public void testIssueTypesFilter() {
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);

        PageElement issueTypesFilter = browsePage.getIssueTypesFilterInput();
        issueTypesFilter.javascript().execute("arguments[0].dispatchEvent(new Event('focus'))");
        assertEvent("admin.browse.archived.issues.issuetype.filter.dropdown.click");

        issueTypesFilter.type("task");
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getIssueTypeOptions), hasSize(1));
        issueTypesFilter.type(Keys.ENTER);
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(0));
        assertEvent("admin.browse.archived.issues.filter.applied");

        dropAnalyticsEvents();
        issueTypesFilter.clear();

        issueTypesFilter.javascript().execute("arguments[0].dispatchEvent(new Event('blur'))");

        // I can't get web driver to behave with this. It keeps all options in the list, unlike actual user interaction
        /*issueTypesFilter.type("bug");
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getIssueTypeOptions), hasSize(1));
        issueTypesFilter.type("\n");
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1));
        assertEvent("admin.browse.archived.issues.filter.applied");*/
    }

    @Test
    @EnableAnalytics
    @SinceBuild(buildNumber = 803000)
    public void testReporterFilter() {
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);

        PageElement reporterFilter = browsePage.getReporterFilterInput();
        reporterFilter.javascript().execute("arguments[0].dispatchEvent(new Event('focus'))");
        assertEvent("admin.browse.archived.issues.reporter.filter.dropdown.click");

        reporterFilter.type("fred");
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getReporterOptions), hasSize(1));
        reporterFilter.type(Keys.ENTER);
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(0));
        assertEvent("admin.browse.archived.issues.filter.applied");

        dropAnalyticsEvents();
        reporterFilter.clear();

        reporterFilter.type("admin");
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getReporterOptions), hasSize(1));
        reporterFilter.type(Keys.ENTER);
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(1));
        assertEvent("admin.browse.archived.issues.filter.applied");
    }

    @Test
    @EnableAnalytics
    @SinceBuild(buildNumber = 803000)
    public void testExportIsHiddenForNotSysadminsModal() {
        backdoor.permissionSchemes().addUserPermission(0L, ProjectPermissions.BROWSE_ARCHIVE, "fred");
        jira.quickLogin("fred", "fred");

        createArchivedIssue();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
        assertFalse("Export button should not be visible for not sys-admins", browsePage.getExportButton().isPresent());
    }

    @Test
    @EnableAnalytics
    @SinceBuild(buildNumber = 803000)
    public void testExportModal() {
        createArchivedIssue();
        createArchivedIssue();
        createArchivedIssue();
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);

        assertTrue("Export button should be visible for sys-admins", browsePage.getExportButton().isPresent());

        PageElement reporterFilter = browsePage.getReporterFilterInput();
        reporterFilter.javascript().execute("arguments[0].dispatchEvent(new Event('focus'))");
        reporterFilter.type("fred");
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getReporterOptions), hasSize(1));
        reporterFilter.type(Keys.ENTER);
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(0));

        browsePage.getExportButton().click();
        assertEvent("admin.browse.archived.issues.export.dropdown.click");

        browsePage.getExportDropdown().find(By.id("export-all-csv")).click();
        assertEvent("admin.browse.archived.issues.export.all.click");

        waitUntilTrue(browsePage.getExportForm().timed().isPresent());
        assertTrue(browsePage.getExportDescription().getText().contains("3 issues"));

        browsePage.cancelExportModal();
        assertEvent("admin.browse.archived.issues.export.modal.cancel.click");

        browsePage.getExportButton().click();
        browsePage.getExportDropdown().find(By.id("export-filtered-csv")).click();
        assertEvent("admin.browse.archived.issues.export.filtered.click");

        waitUntilTrue(browsePage.getExportForm().timed().isPresent());
        assertTrue(browsePage.getExportDescription().getText().contains("0 issues"));
        browsePage.cancelExportModal();

        reporterFilter.clear();
        reporterFilter.type("admin");
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getReporterOptions), hasSize(1));
        reporterFilter.type(Keys.ENTER);
        browsePage.clickSearchButton();
        Poller.waitUntil(queryFactory.forSupplier(browsePage::getRows), hasSize(3));
        browsePage.getExportButton().click();
        browsePage.getExportDropdown().find(By.id("export-filtered-csv")).click();
        waitUntilTrue(browsePage.getExportForm().timed().isPresent());
        assertTrue(browsePage.getExportDescription().getText().contains("3 issues"));
    }

    @Test
    public void testPaginationAlignement() {
        for (int i = 0; i < 51; i++) {
            createArchivedIssue();
        }
        jira.quickLoginAsSysadmin();
        final BrowseArchivePage browsePage = jira.visit(BrowseArchivePage.class);
        browsePage.clickSearchButton();

        // if this class is updated by Atlaskit we have to update the selector in  sad-atlaskit-hacks.less
        waitUntilTrue("Couldn't authenticate node", forSupplier(
                DefaultTimeouts.DEFAULT_AJAX_ACTION, () -> browsePage.ensureCSSSelectorIsPresent("div[class^='DynamicTable__PaginationWrapper']")));
    }

    @Test
    public void testRouting_Dates() {
        DateTime date = new DateTime();
        createArchivedIssue();
        BrowseArchivePage browseArchivePage = jira.visit(BrowseArchivePage.class);

        browseArchivePage.chooseDateFrom(date);
        assertThat(getCurrentUrl(), containsString("dateFrom=" + date.toString("YYYY-MM-dd")));
        assertThat(getCurrentUrl(), not(containsString("dateTo=")));

        browseArchivePage.chooseDateTo(date);
        assertThat(getCurrentUrl(), containsString("dateFrom=" + date.toString("YYYY-MM-dd") + "&dateTo=" + date.toString("YYYY-MM-dd")));

        jira.getTester().gotoUrl(getCurrentUrl());
        waitUntilTrue(queryFactory.forSupplier(
                () -> jira.getTester().getDriver().getPageSource().contains("Archived date: from " + date.toString("YYYY-MM-dd") + " to " + date.toString("YYYY-MM-dd"))
        ));
    }

    @Test
    public void testRouting_ArchivedBy() {
        createArchivedIssue();
        BrowseArchivePage browseArchivePage = jira.visit(BrowseArchivePage.class);
        browseArchivePage.chooseArchiver("fred");

        assertThat(getCurrentUrl(), containsString("archivedBy=fred"));

        jira.getTester().gotoUrl(getCurrentUrl());
        browseArchivePage.ensureArchiverSelectedOption("Fred Normal");
    }

    @Test
    @DevMode(enabled = false)
    public void testRouting_ProjectKey() {
        createArchivedIssue();
        BrowseArchivePage browseArchivePage = jira.visit(BrowseArchivePage.class);
        browseArchivePage.chooseProject("HSP");

        assertThat(getCurrentUrl(), containsString("project=HSP"));

        jira.getTester().gotoUrl(getCurrentUrl());
        browseArchivePage.ensureProjectSelectedOption("homosapien (HSP)");
    }

    @Test
    public void testRouting_IssueTypes() {
        createArchivedIssue();
        BrowseArchivePage browseArchivePage = jira.visit(BrowseArchivePage.class);
        browseArchivePage.chooseIssueType("New Feature");

        assertThat(getCurrentUrl(), containsString("issueType=2"));

        jira.getTester().gotoUrl(getCurrentUrl());
        browseArchivePage.ensureIssueTypeSelectedOption("New Feature");
    }

    @Test
    public void testRouting() {
        createArchivedIssue();

        BrowseArchivePage browseArchivePage = jira.visit(BrowseArchivePage.class);
        DateTime date = new DateTime();

        browseArchivePage
                .chooseIssueType("bug")
                .chooseProject("HSP")
                .chooseReporter("fred")
                .chooseArchiver("admin")
                .chooseDateFrom(date)
                .chooseDateTo(date);

        String dateFromTo = "dateFrom=" + date.toString("YYYY-MM-dd") + "&dateTo=" + date.toString("YYYY-MM-dd");
        assertThat(getCurrentUrl(), containsString("issueType=1&project=HSP&reporter=fred&archivedBy=admin&" + dateFromTo));

        date = date.minusDays(1);

        browseArchivePage
                .chooseIssueType("task")
                .chooseProject("MKY")
                .chooseReporter("admin")
                .chooseArchiver("fred")
                .chooseDateFrom(date)
                .chooseDateTo(date);

        dateFromTo = "dateFrom=" + date.toString("YYYY-MM-dd") + "&dateTo=" + date.toString("YYYY-MM-dd");
        assertThat(getCurrentUrl(), containsString("issueType=3&project=MKY&reporter=admin&archivedBy=fred&" + dateFromTo));
    }

    @Test
    public void testExportFilteredIssuesForm() {
        createArchivedIssue();
        BrowseArchivePage browseArchivePage = jira.visit(BrowseArchivePage.class);

        DateTime date = new DateTime().withZone(DateTimeZone.forOffsetHours(0));
        long timestamp = date.minusMillis(date.getMillisOfDay()).getMillis();

        browseArchivePage
                .chooseArchiver("fred")
                .chooseReporter("admin")
                .chooseDateTo(date)
                .chooseDateFrom(date)
                .chooseIssueType("New Feature")
                .chooseProject("HSP");

        browseArchivePage.clickSearchButton();
        browseArchivePage.getExportButton().click();
        browseArchivePage.getExportDropdown().find(By.id("export-filtered-csv")).click();

        assertThat(jira.getTester().getDriver().getPageSource(), containsString("<input name=\"archivedBy\" type=\"hidden\" value=\"fred\">"));
        assertThat(jira.getTester().getDriver().getPageSource(), containsString("<input name=\"reporter\" type=\"hidden\" value=\"admin\">"));
        assertThat(jira.getTester().getDriver().getPageSource(), containsString("<input name=\"archivedBefore\" type=\"hidden\" value=\"" + timestamp + "\">"));
        assertThat(jira.getTester().getDriver().getPageSource(), containsString("<input name=\"archivedAfter\" type=\"hidden\" value=\"" + timestamp + "\">"));
        assertThat(jira.getTester().getDriver().getPageSource(), containsString("<input name=\"issueType\" type=\"hidden\" value=\"2\">"));
        assertThat(jira.getTester().getDriver().getPageSource(), containsString("<input name=\"projectKey\" type=\"hidden\" value=\"HSP\">"));
    }

    private String getCurrentUrl() {
        return jira.getTester().getDriver().getCurrentUrl();
    }

    private void createArchivedIssue() {
        final String issueArchived = backdoor.issues().createIssue("HSP", "Issue archived").key;
        backdoor.issues().transitionIssue(issueArchived, 5);
        backdoor.issues().archiveIssue(issueArchived);
    }

    private void assertEvent(String name) {
        await().until(() -> backdoor.analyticsEventsControl().matchEvents(name).size(), Matchers.greaterThanOrEqualTo(1));
    }

    private void dropAnalyticsEvents() {
        backdoor.analyticsEventsControl().clear();
    }
}
