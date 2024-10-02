package com.atlassian.jira.webtest.webdriver.tests.admin.systemfields;

import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.functest.rule.SinceBuildRule.SinceBuild;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.components.menu.JiraAuiDropdownMenu;
import com.atlassian.jira.pageobjects.pages.admin.systemfields.ViewSystemFields;
import com.atlassian.jira.pageobjects.util.TraceContext;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import com.google.common.collect.ImmutableList;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Collection;
import java.util.stream.Collectors;

import static com.atlassian.jira.issue.IssueFieldConstants.DESCRIPTION;
import static com.atlassian.jira.issue.IssueFieldConstants.ISSUE_LINKS;
import static com.atlassian.jira.issue.IssueFieldConstants.SUMMARY;
import static com.atlassian.jira.pageobjects.pages.admin.systemfields.ViewSystemFields.ActionLink.CONTEXTS;
import static com.atlassian.jira.pageobjects.pages.admin.systemfields.ViewSystemFields.ActionLink.SCREENS_DC;
import static com.atlassian.jira.pageobjects.pages.admin.systemfields.ViewSystemFields.tdElementWithFieldId;
import static com.atlassian.jira.pageobjects.pages.admin.systemfields.ViewSystemFields.tdElementWithTextOnly;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@WebTest({Category.WEBDRIVER_TEST, Category.ADMINISTRATION})
public class TestViewSystemFieldsDC extends BaseJiraWebTest {
    private final WebDriverTester tester = jira.getTester();

    @Inject
    private TraceContext traceContext;

    @BeforeClass
    public static void beforeClass() {
        backdoor.restoreBlankDataCenterInstance();
    }

    @Test
    public void shouldDisplayDescriptionFieldOnPage() {
        ViewSystemFields.SystemField descriptionSystemField = getSystemField(DESCRIPTION);
        assertTrue(descriptionSystemField.getPageElement().isVisible());
    }

    @SinceBuild(buildNumber = 816000)
    @Test
    public void shouldDisplayActionDropdownMenuForDescription() {
        final ViewSystemFields.SystemField descriptionSystemField = getSystemField(DESCRIPTION);
        final JiraAuiDropdownMenu<?> actions = descriptionSystemField.getActionDropdownMenu();
        waitUntilTrue(actions.isTriggerVisible());
        assertThat(actions.hasItemBy(SCREENS_DC.getSelector(descriptionSystemField)), equalTo(true));
        assertThat(actions.hasItemBy(CONTEXTS.getSelector(descriptionSystemField)), equalTo(true));
    }

    @Test
    public void shouldDisplayActionDropdownMenuWithScreensLinkForNonDescriptionField() {
        ViewSystemFields.SystemField summarySystemField = getSystemField(SUMMARY);
        waitUntilTrue(summarySystemField.getActionDropdownMenu().isTriggerVisible());
        final JiraAuiDropdownMenu<?> actions = summarySystemField.getActionDropdownMenu();
        assertThat(actions.hasItemBy(SCREENS_DC.getSelector(summarySystemField)), equalTo(true));
        assertThat(actions.hasItemBy(CONTEXTS.getSelector(summarySystemField)), equalTo(false));
    }

    @SinceBuild(buildNumber = 816000)
    @Test
    public void shouldDisplayContextSizeAndScreenSize() {
        final ViewSystemFields.SystemField descriptionSystemField = getSystemField(DESCRIPTION);
        assertThat(descriptionSystemField.getContexts(), tdElementWithFieldId("0 Contexts", DESCRIPTION));
        assertThat(descriptionSystemField.getScreens(), tdElementWithFieldId("1 screen", DESCRIPTION));

        final ViewSystemFields.SystemField projectSystemField = getSystemField(ISSUE_LINKS);
        assertThat(projectSystemField.getContexts(), tdElementWithTextOnly("-"));
        assertThat(projectSystemField.getScreens(), tdElementWithFieldId("0 screens", ISSUE_LINKS));

        final ViewSystemFields.SystemField summarySystemField = getSystemField(SUMMARY);
        assertThat(summarySystemField.getContexts(), tdElementWithTextOnly("-"));
        assertThat(summarySystemField.getScreens(), tdElementWithFieldId("1 screen", SUMMARY));
    }

    @SinceBuild(buildNumber = 816000)
    @Test
    public void shouldRedirectToContextsPage() {
        final ViewSystemFields.SystemField descriptionSystemField = getSystemField(DESCRIPTION);
        descriptionSystemField.getActionDropdownMenu().openAndClick(CONTEXTS.getSelector(descriptionSystemField));

        assertTrue(getCurrentUrl().endsWith(getContextPageUrl(DESCRIPTION)));
    }

    @SinceBuild(buildNumber = 816000)
    @Test
    public void shouldRedirectToScreensPage() {
        final ViewSystemFields.SystemField descriptionSystemField = getSystemField(DESCRIPTION);
        descriptionSystemField.getActionDropdownMenu().openAndClick(SCREENS_DC.getSelector(descriptionSystemField));

        assertTrue(getCurrentUrl().endsWith(getScreensPageUrl(DESCRIPTION)));
    }

    @Test
    public void shouldDisplayAllMeaningfulSystemFields() {
        final ViewSystemFields viewSystemFields = goToViewSystemFields();
        final Collection<ViewSystemFields.SystemField> allFields = viewSystemFields.getAllFields();
        assertThat(
                allFields.stream().map(ViewSystemFields.SystemField::getId).collect(Collectors.toList()),
                equalTo(
                        ImmutableList.of("description", "summary", "issuetype", "priority", "resolution", "versions",
                                "fixVersions", "components", "labels", "environment", "issuelinks", "attachment", "assignee",
                                "reporter", "duedate", "timetracking", "security"
                        )
                )
        );
    }

    @Test
    public void shouldShowScreensDialog() {
        final ViewSystemFields viewSystemFields = goToViewSystemFields();
        final ViewSystemFields.SystemField descriptionField = viewSystemFields.getSystemField(DESCRIPTION);
        final ViewSystemFields.ScreensDialog dialog = descriptionField.openScreensDialog();

        waitUntil(dialog.isDialogFocused(), equalTo(true));
    }

    @SinceBuild(buildNumber = 816000)
    @Test
    public void shouldShowContextsDialog() {
        final ViewSystemFields viewSystemFields = goToViewSystemFields();
        final ViewSystemFields.SystemField descriptionField = viewSystemFields.getSystemField(DESCRIPTION);
        final ViewSystemFields.ContextsDialog dialog = descriptionField.openContextsDialog();

        waitUntil(dialog.isDialogFocused(), equalTo(true));
    }

    private String getCurrentUrl() {
        return tester.getDriver().getCurrentUrl();
    }

    private String getContextPageUrl(String fieldId) {
        return "/secure/admin/ConfigureField.jspa?fieldId=" + fieldId + "&selectedTab=contexts";
    }

    private String getScreensPageUrl(String fieldId) {
        if (fieldId.equals(DESCRIPTION)) {
            return "/secure/admin/ConfigureField.jspa?fieldId=" + fieldId + "&selectedTab=screens";
        } else {
            return "/secure/admin/AssociateFieldToScreens!default.jspa?fieldId=" + fieldId + "&returnUrl=ViewSystemFields.jspa";
        }
    }

    private ViewSystemFields.SystemField getSystemField(String fieldId) {
        return traceContext.doWithCheckpoint(() -> {
            final ViewSystemFields viewSystemFields = pageBinder.navigateToAndBind(ViewSystemFields.class);
            return viewSystemFields.getSystemField(fieldId);
        });
    }

    private ViewSystemFields goToViewSystemFields() {
        return traceContext.doWithCheckpoint(() -> pageBinder.navigateToAndBind(ViewSystemFields.class));
    }
}
