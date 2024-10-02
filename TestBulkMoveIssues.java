package com.atlassian.jira.webtest.webdriver.tests.bulk;

import com.atlassian.collectors.CollectorsUtil;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.components.fields.IssueHistoryDataMatchers;
import com.atlassian.jira.pageobjects.navigator.AdvancedSearch;
import com.atlassian.jira.pageobjects.navigator.BulkEdit;
import com.atlassian.jira.pageobjects.navigator.ChooseOperation;
import com.atlassian.jira.pageobjects.navigator.MoveConfirmationPage;
import com.atlassian.jira.pageobjects.navigator.MoveDetails;
import com.atlassian.jira.pageobjects.navigator.MoveIssuesContainer;
import com.atlassian.jira.pageobjects.navigator.MoveSetFields;
import com.atlassian.jira.pageobjects.pages.viewissue.HistoryModule;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.rest.api.issue.IssueCreateResponse;
import com.atlassian.jira.rest.api.issue.IssueFields;
import com.atlassian.jira.rest.api.issue.ResourceRef;
import com.atlassian.jira.testkit.client.IssueSecuritySchemesControl;
import com.atlassian.jira.testkit.client.restclient.Component;
import com.atlassian.jira.testkit.client.restclient.Issue;
import com.atlassian.jira.testkit.client.restclient.IssueLink;
import com.atlassian.jira.testkit.client.restclient.Version;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for BulkEdit of multi select system fields.
 *
 * @since v7.0
 */
@WebTest({Category.WEBDRIVER_TEST, Category.BULK_OPERATIONS})
public class TestBulkMoveIssues extends BaseJiraWebTest {
    public static final String ISSUE_BUG = "1";
    public static final String ISSUE_TASK = "3";
    public static final String ISSUE_IMPROVEMENT = "4";

    public static final String PROJECT_HOMOSAP = "homosapien";
    public static final String PROJECT_NEO = "neanderthal";
    public static final String PROJECT_MONKEY = "monkey";

    public static final String PROJECT_HOMOSAP_KEY = "HSP";
    public static final String PROJECT_NEO_KEY = "NDT";
    public static final String PROJECT_MONKEY_KEY = "MKY";

    public static final String PROJECT_HOMOSAP_ID = "10000";
    public static final String PROJECT_MONKEY_ID = "10001";
    public static String PROJECT_NEO_ID;

    public static final String ADMIN_USERNAME = "admin";
    public static final String FRED_USERNAME = "fred";
    public static final String FRED_PASSWORD = "fred";

    public static final String JIRA_DEV_GROUP = "jira-developers";

    private Map<String, String> projectKeyToId;
    private Map<String, String> projectIdToKey;
    private Map<String, String> projectNameToKey;

    public static final String PERM_SCHEME_NAME = "New Permission Scheme";
    public static final String PERM_SCHEME_DESC = "permission scheme for testing";
    public static final long PERM_SCHEME_ID = 10000;
    public static final String DEFAULT_PERM_SCHEME = "Default Permission Scheme";
    public static final long DEFAULT_PERM_SCHEME_ID = 0;


    @Inject
    PageElementFinder elementFinder;

    @Before
    public void initializeTestData() {
        backdoor.restoreBlankInstance();
        long id = backdoor.project().addProject(PROJECT_NEO, PROJECT_NEO_KEY, FRED_USERNAME);
        PROJECT_NEO_ID = Long.toString(id);
        backdoor.usersAndGroups().addUserToGroup(FRED_USERNAME, JIRA_DEV_GROUP);
        backdoor.subtask().enable();
        backdoor.flags().clearFlags();

        projectIdToKey = new HashMap<>();
        projectKeyToId = new HashMap<>();
        projectNameToKey = new HashMap<>();
        projectIdToKey.put(PROJECT_HOMOSAP_ID, PROJECT_HOMOSAP_KEY);
        projectIdToKey.put(PROJECT_MONKEY_ID, PROJECT_MONKEY_KEY);
        projectIdToKey.put(PROJECT_NEO_ID, PROJECT_NEO_KEY);

        projectIdToKey.entrySet().forEach(e -> projectKeyToId.put(e.getValue(), e.getKey()));

        projectNameToKey.put(PROJECT_HOMOSAP, PROJECT_HOMOSAP_KEY);
        projectNameToKey.put(PROJECT_MONKEY, PROJECT_MONKEY_KEY);
        projectNameToKey.put(PROJECT_NEO, PROJECT_NEO_KEY);
    }

    @Test
    // This test is flaky. See: https://bulldog.internal.atlassian.com/browse/MNSTR-4867
    public void testIssueWithoutSubtasksToSubtaskMove() {
        final String targetIssueType = "Sub-task";
        Collection<IssueDto> issueToMove = new HashSet<>();
        issueToMove.addAll(setupIssues(PROJECT_MONKEY_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 2));
        issueToMove.addAll(setupIssues(PROJECT_MONKEY_KEY, ISSUE_TASK, FRED_PASSWORD, null, 2));

        setupIssues(PROJECT_NEO_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 2);
        setupIssues(PROJECT_NEO_KEY, ISSUE_TASK, FRED_PASSWORD, null, 2);

        IssueDto newParentIssue = setupIssues(PROJECT_HOMOSAP_KEY, ISSUE_IMPROVEMENT, ADMIN_USERNAME, null, 4).iterator().next();

        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery("project=" + PROJECT_MONKEY_KEY).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        //add some more issues to the initial projects so parent selector has some data to work with
        setupIssues(PROJECT_MONKEY_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 2);

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();

        Collection<MoveIssuesContainer> containers = moveDetails.getMoveIssuesContainers();
        assertThat("There should be two groups of issues", containers, iterableWithSize(2));
        Iterator<MoveIssuesContainer> it = containers.iterator();
        testMoveIssueContainer(it.next(), PROJECT_HOMOSAP, targetIssueType, newParentIssue.getKey());
        testMoveIssueContainer(it.next(), PROJECT_HOMOSAP, targetIssueType, newParentIssue.getKey());

        MoveSetFields setFields = moveDetails.next();
        MoveConfirmationPage confirmationPage = setFields.next(MoveConfirmationPage.class);

        assertThat(String.format("Confirmation page should contain information about new parent issue %s", newParentIssue.getKey()), confirmationPage.getFormText(), containsString(newParentIssue.getKey()));

        confirmationPage.confirm().acknowledge();

        //check if issues have moved to subtasks
        final Collection<Issue> issuesAfter = issueToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                issuesAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));
        assertThat(String.format("New parent should be set to %s", newParentIssue.getKey()),
                issuesAfter, everyItem(IssueMatchers.issueWithParentWithKey(newParentIssue.getKey())));
    }

    @Test
    // This test is flaky. See: https://bulldog.internal.atlassian.com/browse/MNSTR-4867
    public void testIssueWithSubtasksToSubtaskMove() {
        backdoor.flags().clearFlags();

        final String targetIssueType = "Sub-task";
        final String targetProject = PROJECT_HOMOSAP;
        Collection<IssueDto> issueToMove = new HashSet<>();
        Collection<IssueDto> subtaskToMove = new HashSet<>();

        setupIssues(PROJECT_MONKEY_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 2).stream().forEach(idto -> {
            issueToMove.add(idto);
            subtaskToMove.addAll(setupSubtasks(PROJECT_MONKEY_ID, idto.getKey(), 2));
        });
        setupIssues(PROJECT_NEO_KEY, ISSUE_TASK, FRED_PASSWORD, null, 2).stream().forEach(idto -> {
            issueToMove.add(idto);
            subtaskToMove.addAll(setupSubtasks(PROJECT_NEO_ID, idto.getKey(), 2));
        });

        //create destination issues
        IssueDto newParentIssue = setupIssues(PROJECT_HOMOSAP_KEY, ISSUE_IMPROVEMENT, ADMIN_USERNAME, null, 4).iterator().next();

        //create subtask for destination
        setupSubtasks(PROJECT_HOMOSAP_ID, newParentIssue.getKey(), 2);

        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery(String.format("(project=%s OR project=%s) and issuetype not in (Sub-task)", PROJECT_MONKEY_KEY, PROJECT_NEO_KEY)).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();

        //add some more issues so parent selector has some data to work with
        setupIssues(PROJECT_MONKEY_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 2);
        setupIssues(PROJECT_NEO_KEY, ISSUE_TASK, FRED_PASSWORD, null, 2);

        Collection<MoveIssuesContainer> containers = moveDetails.getMoveIssuesContainers();
        assertThat("There should be two groups of issues", containers, iterableWithSize(2));
        Iterator<MoveIssuesContainer> it = containers.iterator();
        testMoveIssueContainer(it.next(), targetProject, targetIssueType, newParentIssue.getKey());
        testMoveIssueContainer(it.next(), targetProject, targetIssueType, newParentIssue.getKey());

        //subtask context
        moveDetails = moveDetails.next(MoveDetails.class);

        Collection<MoveIssuesContainer> containerForSubtasks = moveDetails.getMoveIssuesContainers();
        assertThat("There should be two groups of issues", containerForSubtasks, iterableWithSize(2));
        Iterator<MoveIssuesContainer> sit = containerForSubtasks.iterator();
        testMoveIssueContainerForSubtask(sit.next(), targetProject, targetIssueType, newParentIssue.getKey());
        testMoveIssueContainerForSubtask(sit.next(), targetProject, targetIssueType, newParentIssue.getKey());

        //set fields for issues, and later for subtasks
        MoveSetFields setFields = moveDetails.next().next(MoveSetFields.class);
        MoveConfirmationPage confirmationPage = setFields.next(MoveConfirmationPage.class);

        Iterable<MoveConfirmationPage.MoveIssuesConfirmContainer> moveIssuesConfirmContainerIterable = confirmationPage.getMoveIssuesConfirmContainers();
        assertThat("There should be two groups of confirmation containers", moveIssuesConfirmContainerIterable, iterableWithSize(2));
        assertThat(String.format("All target projects should be %s", targetProject), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetProject", equalTo(targetProject))));
        assertThat(String.format("All target issue types should be %s", targetIssueType), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetIssueType", equalTo(targetIssueType))));
        assertThat(String.format("All new parent issues be %s", newParentIssue.getKey()), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetParentIssue", equalTo(newParentIssue.getKey()))));

        confirmationPage.confirm().acknowledge();

        //check if issues have moved to subtasks
        final Collection<Issue> issuesAfter = issueToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                issuesAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                issuesAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));
        assertThat(String.format("New parent should be set to %s", newParentIssue.getKey()),
                issuesAfter, everyItem(IssueMatchers.issueWithParentWithKey(newParentIssue.getKey())));
        assertThat("Issue should have no subtasks", issuesAfter, everyItem(IssueMatchers.issueWithSubtaskNum(0)));

        //check new parent for subtasks
        final Collection<Issue> subtasksAfter = subtaskToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                subtasksAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                subtasksAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));
        assertThat(String.format("New parent should be set to %s", newParentIssue.getKey()),
                subtasksAfter, everyItem(IssueMatchers.issueWithParentWithKey(newParentIssue.getKey())));

        Issue newParentIssueAfter = backdoor.issues().getIssue(newParentIssue.getKey());
        assertThat("New parent should have 14 links (2 own + 4 issues moved and 8 subtasks of those issues)", newParentIssueAfter.fields.subtasks.size(), equalTo(14));
    }

    @Test
    public void testSubtaskToIssueMove() {
        final String targetIssueType = "Task";
        Collection<IssueDto> parentIssues = new HashSet<>();
        Collection<IssueDto> subtaskToMove = new HashSet<>();

        setupIssues(PROJECT_MONKEY_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 1).stream().forEach(idto -> {
            parentIssues.add(idto);
            subtaskToMove.addAll(setupSubtasks(PROJECT_MONKEY_ID, idto.getKey(), 2));
        });
        setupIssues(PROJECT_NEO_KEY, ISSUE_TASK, FRED_PASSWORD, null, 1).stream().forEach(idto -> {
            parentIssues.add(idto);
            subtaskToMove.addAll(setupSubtasks(PROJECT_NEO_ID, idto.getKey(), 2));
        });

        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery(String.format("(project=%s OR project=%s) and issuetype in (Sub-task)", PROJECT_MONKEY_KEY, PROJECT_NEO_KEY)).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();

        Collection<MoveIssuesContainer> containers = moveDetails.getMoveIssuesContainers();
        assertThat("There should be two groups of issues", containers, iterableWithSize(2));
        Iterator<MoveIssuesContainer> it = containers.iterator();
        Collection<String> parentIssueKeys = parentIssues.stream().map(IssueDto::getKey).collect(Collectors.toSet());
        testMoveIssueContainerForSubtaskToIssue(it.next(), targetIssueType, parentIssueKeys);
        testMoveIssueContainerForSubtaskToIssue(it.next(), targetIssueType, parentIssueKeys);
        assertThat("All parent issue keys should be preselected", parentIssueKeys.size(), equalTo(0));

        //set fields for issues, two destination projects so there is second set fields page
        MoveSetFields setFields = moveDetails.next().next(MoveSetFields.class);
        MoveConfirmationPage confirmationPage = setFields.next(MoveConfirmationPage.class);

        Iterable<MoveConfirmationPage.MoveIssuesConfirmContainer> moveIssuesConfirmContainerIterable = confirmationPage.getMoveIssuesConfirmContainers();
        assertThat("There should be 2 container groups", Iterables.size(moveIssuesConfirmContainerIterable), equalTo(2));
        assertThat(String.format("All target issue types should be %s", targetIssueType), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetIssueType", equalTo(targetIssueType))));
        assertThat("There should be no target parent issue on this page", moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetParentIssue", nullValue())));
        //both projects should be visible on page
        assertThat(String.format("Project %s should be present only once on page", PROJECT_MONKEY),
                Iterables.size(Iterables.filter(moveIssuesConfirmContainerIterable, c -> PROJECT_MONKEY.equals(c.getTargetProject()))), equalTo(1));
        assertThat(String.format("Project %s should be present only once on page", PROJECT_NEO),
                Iterables.size(Iterables.filter(moveIssuesConfirmContainerIterable, c -> PROJECT_NEO.equals(c.getTargetProject()))), equalTo(1));

        confirmationPage.confirm().acknowledge();

        final Collection<Issue> subtasksAfterFromMonkey = subtaskToMove.stream().filter(dto -> dto.getProjectId().equals(PROJECT_MONKEY_ID)).map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        final Collection<Issue> subtasksAfterFromNeo = subtaskToMove.stream().filter(dto -> dto.getProjectId().equals(PROJECT_NEO_ID)).map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        final ImmutableList<Issue> subtasksAfter = ImmutableList.<Issue>builder().addAll(subtasksAfterFromMonkey).addAll(subtasksAfterFromNeo).build();
        assertThat(String.format("Target issue project should be %s", PROJECT_MONKEY_ID),
                subtasksAfterFromMonkey, everyItem(IssueMatchers.issueWithProjectId(PROJECT_MONKEY_ID)));
        assertThat(String.format("Target issue project should be %s", PROJECT_NEO_ID),
                subtasksAfterFromNeo, everyItem(IssueMatchers.issueWithProjectId(PROJECT_NEO_ID)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                subtasksAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));
        // no parent
        assertThat("Issue should have no parent", subtasksAfter, everyItem(IssueMatchers.issueWithParentKey(null)));

        //change to assertThat
        final Collection<Issue> parentIssuesAfter = parentIssues.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat("Old parent should have no links to subtasks", parentIssuesAfter, everyItem(IssueMatchers.issueWithSubtaskNum(0)));

        // test history tab
        IssueDto movedIssueBefore = subtaskToMove.iterator().next();
        Issue movedIssueAfter = backdoor.issues().getIssue(movedIssueBefore.getKey());
        final HistoryModule historyTab = jira.goToViewIssue(movedIssueAfter.key).getActivitySection().historyModule();
        Iterable<HistoryModule.IssueHistoryItem> historyItems = historyTab.getHistoryItemContainers(2).stream()
            .flatMap(container -> container.getHistoryDataElements().stream())
            .collect(Collectors.toList());

        HistoryModule.IssueHistoryItem[] tab = Iterables.toArray(historyItems, HistoryModule.IssueHistoryItem.class);

        IssueDto movedIssueParentDto = parentIssues.stream().filter(dto -> dto.getKey().equals(movedIssueBefore.getParentKey())).iterator().next();
        assertThat("Parent should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Parent", String.format("%s [ %s ]", movedIssueParentDto.getKey(), movedIssueParentDto.getId()), "")));
    }

    @Test
    public void testIssueViewerForIssueWithSubtasksBulkOperation() {
        Collection<IssueDto> parentIssues = new HashSet<>();

        setupIssues(PROJECT_MONKEY_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 2).stream().forEach(idto -> {
            parentIssues.add(idto);
            setupSubtasks(PROJECT_MONKEY_ID, idto.getKey(), 2);
        });
        setupIssues(PROJECT_NEO_KEY, ISSUE_TASK, FRED_PASSWORD, null, 1).stream().forEach(idto -> {
            parentIssues.add(idto);
            setupSubtasks(PROJECT_NEO_ID, idto.getKey(), 2);
        });

        // take any parent issue
        IssueDto parentIssue = parentIssues.iterator().next();

        ViewIssuePage viewIssuePage = jira.visit(ViewIssuePage.class, parentIssue.getKey());
        BulkEdit bulkEdit = viewIssuePage.getSubTasksModule().bulkOperation();
        Collection<String> issueKeysFromBulkEditPage = ImmutableSet.copyOf(bulkEdit.getIssueKeys());
        Issue issue = backdoor.issues().getIssue(parentIssue.getKey());
        Collection<String> issueKeysDb = ImmutableSet.copyOf(issue.fields.subtasks.stream().map(IssueLink.IssueLinkRef::key).collect(Collectors.toList()));
        assertThat("All subtasks from task should be present on page", issueKeysFromBulkEditPage, equalTo(issueKeysDb));
    }

    @Test
    public void testIssueViewerForIssueWithSubtasksOpenIssueNavigator() {
        Collection<IssueDto> parentIssues = new HashSet<>();

        setupIssues(PROJECT_MONKEY_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 2).stream().forEach(idto -> {
            parentIssues.add(idto);
            setupSubtasks(PROJECT_MONKEY_ID, idto.getKey(), 2);
        });
        setupIssues(PROJECT_NEO_KEY, ISSUE_TASK, FRED_PASSWORD, null, 1).stream().forEach(idto -> {
            parentIssues.add(idto);
            setupSubtasks(PROJECT_NEO_ID, idto.getKey(), 2);
        });

        // take any parent issue
        IssueDto parentIssue = parentIssues.iterator().next();

        ViewIssuePage viewIssuePage = jira.visit(ViewIssuePage.class, parentIssue.getKey());
        AdvancedSearch advancedSearch = viewIssuePage.getSubTasksModule().openIssueNavigator();

        Collection<String> issueKeysFromPage = ImmutableSet.copyOf(advancedSearch.getResults().getIssueKeys());

        Issue issue = backdoor.issues().getIssue(parentIssue.getKey());
        Collection<String> issueKeysDb = ImmutableSet.copyOf(issue.fields.subtasks.stream().map(IssueLink.IssueLinkRef::key).collect(Collectors.toList()));
        assertThat("All subtasks from issue should be present on page", issueKeysFromPage, equalTo(issueKeysDb));
    }

    @Test
    // This test is flaky. See: https://bulldog.internal.atlassian.com/browse/MNSTR-4867
    public void testIssueWithSubtasksWithSecurityLevelToSubtaskWithoutSecurityLevelConversion() {
        //setup 2 projects. source with security level set and destination without security level set
        IssueSecuritySchemesControl issueSecuritySchemesControl = backdoor.getTestkit().issueSecuritySchemesControl();

        final long issueSecuritySchemesId = issueSecuritySchemesControl.createScheme("TestIssueSecurityLevelScheme", "Test");
        final long securityLevelId = issueSecuritySchemesControl.addSecurityLevel(issueSecuritySchemesId, "TestLevel1", "desc");
        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId, securityLevelId, ADMIN_USERNAME);
        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId, securityLevelId, FRED_USERNAME);

        backdoor.project().setIssueSecurityScheme(backdoor.project().getProjectId(PROJECT_NEO_KEY), issueSecuritySchemesId);
        backdoor.permissionSchemes().addGroupPermission(DEFAULT_PERM_SCHEME_ID, ProjectPermissions.SET_ISSUE_SECURITY, JIRA_DEV_GROUP);

        //create source issue with security level set
        Collection<IssueDto> issueToMove = new HashSet<>();
        Collection<IssueDto> subtaskToMove = new HashSet<>();
        setupIssues(PROJECT_NEO_KEY, ISSUE_BUG, ADMIN_USERNAME, securityLevelId, 1).stream().forEach(idto -> {
            issueToMove.add(idto);
            subtaskToMove.addAll(setupSubtasks(PROJECT_NEO_ID, idto.getKey(), 2));
        });
        //create destination issue
        IssueDto newParentIssue = setupIssues(PROJECT_HOMOSAP_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 1).iterator().next();

        final String targetIssueType = "Sub-task";
        final String targetProject = PROJECT_HOMOSAP;
        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery(String.format("project=%s and issuetype not in (Sub-task)", PROJECT_NEO_KEY)).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();

        Collection<MoveIssuesContainer> containers = moveDetails.getMoveIssuesContainers();
        assertThat("There should be one group of issues", containers, iterableWithSize(1));
        Iterator<MoveIssuesContainer> it = containers.iterator();
        testMoveIssueContainer(it.next(), targetProject, targetIssueType, newParentIssue.getKey());

        //subtask context
        moveDetails = moveDetails.next(MoveDetails.class);

        Collection<MoveIssuesContainer> containerForSubtasks = moveDetails.getMoveIssuesContainers();
        assertThat("There should be one group of issue", containerForSubtasks, iterableWithSize(1));
        Iterator<MoveIssuesContainer> sit = containerForSubtasks.iterator();
        testMoveIssueContainerForSubtask(sit.next(), targetProject, targetIssueType, newParentIssue.getKey());

        //set fields for issues, and later for subtasks
        MoveSetFields setFields = moveDetails.next();
        final String securityLevelWarningText = "The value of this field must be changed to be valid in the target project, but you are not able to update this field in the target project. It will be set to the field's default value for the affected issues.";
        checkIfSecurityLevelWarningIsPresent(setFields, securityLevelWarningText, false, false);
        setFields = setFields.next(MoveSetFields.class);
        checkIfSecurityLevelWarningIsPresent(setFields, securityLevelWarningText, false, false);

        MoveConfirmationPage confirmationPage = setFields.next(MoveConfirmationPage.class);

        Iterable<MoveConfirmationPage.MoveIssuesConfirmContainer> moveIssuesConfirmContainerIterable = confirmationPage.getMoveIssuesConfirmContainers();
        assertThat("There should be two groups of issues", Iterables.size(moveIssuesConfirmContainerIterable), equalTo(2));
        assertThat(String.format("All target projects should be %s", targetProject), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetProject", equalTo(targetProject))));
        assertThat(String.format("All target issue types should be %s", targetIssueType), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetIssueType", equalTo(targetIssueType))));
        assertThat(String.format("All new parent issues be %s", newParentIssue.getKey()), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetParentIssue", equalTo(newParentIssue.getKey()))));

        assertThat("There should be only security level field updated", moveIssuesConfirmContainerIterable,
                everyItem(Matchers.hasProperty("updateFields", iterableWithSize(1)))
        );
        assertThat("There should be only security level field updated", moveIssuesConfirmContainerIterable,
                everyItem(Matchers.hasProperty("updateFields", everyItem(Matchers.hasProperty("field", containsString("Security Level")))))
        );
        assertThat("Security Level field should be set to None", moveIssuesConfirmContainerIterable,
                everyItem(Matchers.hasProperty("updateFields", everyItem(Matchers.hasProperty("value", containsString("None")))))
        );

        confirmationPage.confirm().acknowledge();

        //check if issues have moved to subtasks
        final Collection<Issue> issuesAfter = issueToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                issuesAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                issuesAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));
        assertThat(String.format("New parent should be set to %s", newParentIssue.getKey()),
                issuesAfter, everyItem(IssueMatchers.issueWithParentWithKey(newParentIssue.getKey())));
        assertThat("Issue should have no subtasks", issuesAfter, everyItem(IssueMatchers.issueWithSubtaskNum(0)));
        assertThat("Security Level should be set to null", issuesAfter, everyItem(IssueMatchers.issueWithSecurityLevelName(null)));

        //check new parent for subtasks
        final Collection<Issue> subtasksAfter = subtaskToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                subtasksAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                subtasksAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));
        assertThat(String.format("New parent should be set to %s", newParentIssue.getKey()),
                subtasksAfter, everyItem(IssueMatchers.issueWithParentWithKey(newParentIssue.getKey())));
        assertThat("Security Level for subtask should be set to null", subtasksAfter, everyItem(IssueMatchers.issueWithSecurityLevelName(null)));

        Issue newParentIssueAfter = backdoor.issues().getIssue(newParentIssue.getKey());
        assertThat("New parent should have 3 links", newParentIssueAfter.fields.subtasks.size(), equalTo(3));

        // test history tab
        IssueDto movedIssueBefore = issueToMove.iterator().next();
        Issue movedIssueAfter = backdoor.issues().getIssue(movedIssueBefore.getKey());
        final HistoryModule historyTab = jira.goToViewIssue(movedIssueAfter.key).getActivitySection().historyModule();
        Iterable<HistoryModule.IssueHistoryItem> historyItems = historyTab.getHistoryItemContainers(3).stream()
            .flatMap(container -> container.getHistoryDataElements().stream())
            .collect(Collectors.toList());

        assertThat("Project should move", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Project", "neanderthal [ 10010 ]", "homosapien [ 10000 ]")));
        assertThat("Issue key shoud change when moving between projects", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Key", movedIssueBefore.getKey(), movedIssueAfter.key)));
        assertThat("Issue type should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Issue Type", "Bug [ 1 ]", "Sub-task [ 10000 ]")));
        assertThat("Parent should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Parent", "", "HSP-1 [ 10003 ]")));
        assertThat("Security should be copied from new parent", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Security", "TestLevel1 [ 10000 ]", "")));
    }

    @Test
    // This test is flaky. See: https://bulldog.internal.atlassian.com/browse/MNSTR-4867
    public void testIssueWithSubtasksWithoutSecurityLevelToSubtaskWithSecurityLevelConversion() {
        // setup 2 projects. source with security level set and destination without security level set
        IssueSecuritySchemesControl issueSecuritySchemesControl = backdoor.getTestkit().issueSecuritySchemesControl();

        final String securityLevelName = "TestLevel1";
        final long issueSecuritySchemesId = issueSecuritySchemesControl.createScheme("TestIssueSecurityLevelScheme", "Test");
        final long securityLevelId = issueSecuritySchemesControl.addSecurityLevel(issueSecuritySchemesId, securityLevelName, "desc");
        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId, securityLevelId, ADMIN_USERNAME);
        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId, securityLevelId, FRED_USERNAME);

        backdoor.project().setIssueSecurityScheme(backdoor.project().getProjectId(PROJECT_HOMOSAP_KEY), issueSecuritySchemesId);
        backdoor.permissionSchemes().addGroupPermission(DEFAULT_PERM_SCHEME_ID, ProjectPermissions.SET_ISSUE_SECURITY, JIRA_DEV_GROUP);

        //create source issue with security level not set
        Collection<IssueDto> issueToMove = new HashSet<>();
        Collection<IssueDto> subtaskToMove = new HashSet<>();
        setupIssues(PROJECT_NEO_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 1).stream().forEach(idto -> {
            issueToMove.add(idto);
            subtaskToMove.addAll(setupSubtasks(PROJECT_NEO_ID, idto.getKey(), 2));
        });
        //create destination issue with security level set
        IssueDto newParentIssue = setupIssues(PROJECT_HOMOSAP_KEY, ISSUE_BUG, ADMIN_USERNAME, securityLevelId, 1).iterator().next();

        //move it to destination project
        final String targetIssueType = "Sub-task";
        final String targetProject = PROJECT_HOMOSAP;
        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery(String.format("project=%s and issuetype not in (Sub-task)", PROJECT_NEO_KEY)).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();

        Collection<MoveIssuesContainer> containers = moveDetails.getMoveIssuesContainers();
        assertThat("There should be one group of issues", containers, iterableWithSize(1));
        Iterator<MoveIssuesContainer> it = containers.iterator();
        testMoveIssueContainer(it.next(), targetProject, targetIssueType, newParentIssue.getKey());

        //subtask context
        moveDetails = moveDetails.next(MoveDetails.class);

        Collection<MoveIssuesContainer> containerForSubtasks = moveDetails.getMoveIssuesContainers();
        assertThat("There should be one group of issue", containerForSubtasks, iterableWithSize(1));
        Iterator<MoveIssuesContainer> sit = containerForSubtasks.iterator();
        testMoveIssueContainerForSubtask(sit.next(), targetProject, targetIssueType, newParentIssue.getKey());

        //set fields for issues, and later for subtasks
        MoveSetFields setFields = moveDetails.next();
        //should be info about setting value from parent
        final String securityLevelWarningText = "The security level of subtasks is inherited from parents.";
        checkIfSecurityLevelWarningIsPresent(setFields, securityLevelWarningText, true, true);
        setFields = setFields.next(MoveSetFields.class);
        checkIfSecurityLevelWarningIsPresent(setFields, securityLevelWarningText, true, true);

        MoveConfirmationPage confirmationPage = setFields.next(MoveConfirmationPage.class);

        Iterable<MoveConfirmationPage.MoveIssuesConfirmContainer> moveIssuesConfirmContainerIterable = confirmationPage.getMoveIssuesConfirmContainers();
        assertThat("There should be two groups of issues", Iterables.size(moveIssuesConfirmContainerIterable), equalTo(2));
        assertThat(String.format("All target projects should be %s", targetProject), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetProject", equalTo(targetProject))));
        assertThat(String.format("All target issue types should be %s", targetIssueType), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetIssueType", equalTo(targetIssueType))));
        assertThat(String.format("All new parent issues be %s", newParentIssue.getKey()), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetParentIssue", equalTo(newParentIssue.getKey()))));

        assertThat("There should be only security level field updated", moveIssuesConfirmContainerIterable,
                everyItem(Matchers.hasProperty("updateFields", iterableWithSize(1)))
        );
        assertThat("There should be only security level field updated", moveIssuesConfirmContainerIterable,
                everyItem(Matchers.hasProperty("updateFields", everyItem(Matchers.hasProperty("field", containsString("Security Level")))))
        );
        assertThat(String.format("Security Level field should be set to %s", securityLevelName), moveIssuesConfirmContainerIterable,
                everyItem(Matchers.hasProperty("updateFields", everyItem(Matchers.hasProperty("value", containsString(securityLevelName)))))
        );

        confirmationPage.confirm().acknowledge();

        //check if issues have moved to subtasks
        final Collection<Issue> issuesAfter = issueToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                issuesAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                issuesAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));
        assertThat(String.format("New parent should be set to %s", newParentIssue.getKey()),
                issuesAfter, everyItem(IssueMatchers.issueWithParentWithKey(newParentIssue.getKey())));
        assertThat("Issue should have no subtasks", issuesAfter, everyItem(IssueMatchers.issueWithSubtaskNum(0)));
        assertThat(String.format("Security Level should be set to %s", securityLevelName), issuesAfter, everyItem(IssueMatchers.issueWithSecurityLevelName(securityLevelName)));

        //check new parent for subtasks
        final Collection<Issue> subtasksAfter = subtaskToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                subtasksAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                subtasksAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));
        assertThat(String.format("New parent should be set to %s", newParentIssue.getKey()),
                subtasksAfter, everyItem(IssueMatchers.issueWithParentWithKey(newParentIssue.getKey())));
        assertThat(String.format("Security Level for subtask should be set to %s", securityLevelName), subtasksAfter, everyItem(IssueMatchers.issueWithSecurityLevelName(securityLevelName)));

        Issue newParentIssueAfter = backdoor.issues().getIssue(newParentIssue.getKey());
        assertThat("New parent should have 3 links", newParentIssueAfter.fields.subtasks.size(), equalTo(3));

        // test history tab
        IssueDto movedIssueBefore = issueToMove.iterator().next();
        Issue movedIssueAfter = backdoor.issues().getIssue(movedIssueBefore.getKey());
        final HistoryModule historyTab = jira.goToViewIssue(movedIssueAfter.key).getActivitySection().historyModule();
        Iterable<HistoryModule.IssueHistoryItem> historyItems = historyTab.getHistoryItemContainers(2).stream()
            .flatMap(container -> container.getHistoryDataElements().stream())
            .collect(Collectors.toList());

        assertThat("Project should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Project", "neanderthal [ 10010 ]", "homosapien [ 10000 ]")));
        assertThat("Issue key shoud change when moving between projects", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Key", movedIssueBefore.getKey(), movedIssueAfter.key)));
        assertThat("Issue type should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Issue Type", "Bug [ 1 ]", "Sub-task [ 10000 ]")));
        assertThat("Parent should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Parent", "", "HSP-1 [ 10003 ]")));
        assertThat("Security should be copied from new parent", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Security", "", "TestLevel1 [ 10000 ]")));
    }

    @Test
    public void testMoveIssueFromProjectWithIssueTypeToOtherProjectWithoutIssueTypeSelectsCorrectDestinationIssueType() {
        backdoor.restoreDataFromResource("TestBulkMoveIssues.zip");
        backdoor.flags().clearFlags();

        final String newIssueType = "New issue type";
        final String newSubtaskType = "New sub-task";
        final String PROJECT_SOURCE_KEY = "SP";
        final String PROJECT_DESTINATION_KEY = "DP";
        final String SOURCE_PROJECT = "Source Project";
        final String DESTINATION_PROJECT = "Destination Project";
        final String defaultNewIssueType = "Task";
        final String defaultNewSubtaskType = "Sub-task";

        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery(String.format("project=%s and issuetype not in (subTaskIssueTypes())", PROJECT_SOURCE_KEY)).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();

        Collection<MoveIssuesContainer> containers = moveDetails.getMoveIssuesContainers();
        final MoveIssuesContainer mic = assertMoveIssuesContainer(containers, DESTINATION_PROJECT, newIssueType, defaultNewIssueType, 2);
        mic.selectIssueType("Task");

        //subtask context
        moveDetails = moveDetails.next(MoveDetails.class);

        containers = moveDetails.getMoveIssuesContainers();
        assertMoveIssuesContainer(containers, null, newSubtaskType, defaultNewSubtaskType, 1);
    }

    @Test
    public void testValidateFieldsForNewProject() {
        final String monkeyGroup = "monkey-users";
        backdoor.usersAndGroups().addGroup(monkeyGroup);
        backdoor.usersAndGroups().addUserToGroup(FRED_USERNAME, "jira-developers");
        backdoor.usersAndGroups().removeUserFromGroup(FRED_USERNAME, monkeyGroup);
        backdoor.usersAndGroups().addUserToGroup(ADMIN_USERNAME, monkeyGroup);

        final Long monkeyPermissionScheme = backdoor.permissionSchemes().copyDefaultScheme("monkey-scheme");
        backdoor.permissionSchemes().removeGroupPermission(monkeyPermissionScheme, ProjectPermissions.ASSIGNABLE_USER, "jira-developers");
        backdoor.permissionSchemes().addGroupPermission(monkeyPermissionScheme, ProjectPermissions.ASSIGNABLE_USER, monkeyGroup);
        backdoor.project().setPermissionScheme(backdoor.project().getProjectId(PROJECT_MONKEY_KEY), monkeyPermissionScheme);

        backdoor.issues().createIssue(PROJECT_HOMOSAP_KEY, "issue to move 1", FRED_USERNAME);
        backdoor.issues().createIssue(PROJECT_HOMOSAP_KEY, "issue to move 2", FRED_USERNAME);

        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        MoveDetails moveIssue = searchPage.enterQuery("").submit().toolsMenu().open().bulkChange().selectAllIssues().chooseOperation().moveIssues();
        for (MoveIssuesContainer container : moveIssue.getMoveIssuesContainers()) {
            container.selectProject(PROJECT_MONKEY);
        }

        MoveSetFields setFields = moveIssue.next();
        List<MoveSetFields.FieldUpdateRow> fields = ImmutableList.copyOf(setFields.getFieldUpdateRows());
        assertThat(fields, hasSize(1));
        assertEquals("Assignee", fields.get(0).getField());
        fields.get(0).input(FRED_USERNAME);
        MoveSetFields validationInfo = setFields.next(MoveSetFields.class);

        List<String> errorMessages = ImmutableList.copyOf(validationInfo.getFieldUpdateRows()).stream()
                .filter(row -> row.getField().toLowerCase().equals("assignee"))
                .map(MoveSetFields.FieldUpdateRow::getErrorMessage)
                .collect(Collectors.toList());
        assertThat(errorMessages, contains("User 'fred' cannot be assigned issues."));
    }

    private MoveIssuesContainer assertMoveIssuesContainer(final Collection<MoveIssuesContainer> containers, final String DESTINATION_PROJECT,
                                                          final String newIssueType, final String defaultNewIssueType, final int issueTypeSuggestionsCount) {
        assertThat("There should be one group of issues", containers, iterableWithSize(1));
        Iterator<MoveIssuesContainer> it = containers.iterator();
        final MoveIssuesContainer mic = it.next();
        if (DESTINATION_PROJECT != null) {
            mic.selectProject(DESTINATION_PROJECT);
        }
        String issueTypeSelected = mic.getIssueTypeSelected();
        assertThat("\"New issue type\" should not be selected by default because it's invalid in destination project",
                issueTypeSelected, not(equalTo(newIssueType)));
        assertThat("\"Task\" should be selected by default",
                issueTypeSelected, equalTo(defaultNewIssueType));
        Iterable<String> issueTypes = mic.getAvailableIssueTypeSuggestions();
        assertThat(String.format("There should be %d issue type suggestions", issueTypeSuggestionsCount), issueTypes, iterableWithSize(issueTypeSuggestionsCount));
        return mic;
    }

    public Iterable<String> transformComponents(Iterable<Component> components) {
        return Iterables.transform(components, c -> c.name);
    }

    public Iterable<String> transformVersions(Iterable<Version> versions) {
        return Iterables.transform(versions, v -> v.name);
    }

    private void checkIfSecurityLevelWarningIsPresent(final MoveSetFields setFields, final String securityLevelWarningTxt, final boolean retainCheckboxPresent, final boolean retainCheckboxDisabled) {
        Iterable<MoveSetFields.FieldUpdateRow> fieldUpdateRows = setFields.getFieldUpdateRows();
        //check for text that warns that security field will be cleared
        fieldUpdateRows.forEach(row -> {
            assertThat("There should be only Security Level field", row.getField(), Matchers.containsString("Security Level"));
            assertThat("There should be warning for Security Level field", row.getContent(), Matchers.containsString(securityLevelWarningTxt));
            if (retainCheckboxPresent) {
                assertThat("There should be retain checkbox present", row.isRetainCheckboxPresent(), is(true));
                if (retainCheckboxDisabled) {
                    assertThat("There retain checkbox should be disabled, since we explicitly set the security level from parent", row.isRetainCheckboxDisabled(), is(true));
                }
            } else {
                assertThat("There should be NO retain checkbox present", row.isRetainCheckboxPresent(), is(false));
            }
        });
    }

    private void testMoveIssueContainer(final MoveIssuesContainer mic, String targetProject, String targetIssueType, String parentIssueKey) {
        assertFalse(mic.isParentSelectVisible().now());
        mic.selectProject(targetProject);
        mic.selectIssueType(targetIssueType);
        Poller.waitUntilTrue("Parent issue selector should become visible", mic.isParentSelectVisible());
        String currentProjectKey = mapProjectNameToProjectKey(mic.getProjectFrom());
        Iterable<String> parentIssueSuggestions = mic.typeParentIssueAndReturnSuggestions(currentProjectKey);

        parentIssueSuggestions.forEach(
                s -> assertThat(
                        String.format("Should only allow to select parent issues from %s project", currentProjectKey),
                        s,
                        anyOf(startsWith(currentProjectKey), equalTo(currentProjectKey))
                ));


        String targetProjectKey = mapProjectNameToProjectKey(targetProject);
        //check that only homosapien parent issue suggestions are returned
        parentIssueSuggestions = mic.typeParentIssueAndReturnSuggestions(targetProjectKey);
        parentIssueSuggestions.forEach(
                s -> assertThat(
                        String.format("Should only allow to select parent issues from %s project", targetProjectKey),
                        s,
                        anyOf(startsWith(targetProjectKey), equalTo(targetProjectKey))
                ));
        mic.selectParentIssue(parentIssueKey);
    }

    private void testMoveIssueContainerForSubtask(final MoveIssuesContainer mic, final String targetProject, final String targetIssueType, final String parentIssueKey) {
        //not present project selector
        assertFalse("Project selector should not be present for subtasks", mic.isProjectSelectPresent().now());
        //not present parent selector
        assertFalse("Parent selector should not be present for subtasks", mic.isParentSelectPresent().now());
        //correct destination project
        assertThat(String.format("Target project should be preselected and set to %s", targetProject), mic.getTargetProjectSelected(), equalTo(targetProject));
        //correct destination parent issue
        assertThat(String.format("Target parent issue should be preselected and set to %s", parentIssueKey), mic.getTargetParentIssueSelected(), equalTo(parentIssueKey));
        //only subtask present in issue type selector
        Iterable<String> issueTypeSuggestions = mic.getAvailableIssueTypeSuggestions();
        assertThat("Should only allow to select sub-task issue type", Iterables.size(issueTypeSuggestions), equalTo(1));
        issueTypeSuggestions.forEach(s -> assertThat("Should only allow to select sub-task issue type", s, equalTo(targetIssueType)));
        mic.selectIssueType(targetIssueType);
    }

    private void testMoveIssueContainerForSubtaskToIssue(final MoveIssuesContainer mic, final String targetIssueType, final Collection<String> parentIssueKeys) {
        //present project selector
        assertTrue("Project selector should be present for subtasks", mic.isProjectSelectPresent().now());
        //present parent selector
        assertTrue("Parent selector should be present for subtasks", mic.isParentSelectPresent().now());
        assertThat("Target project selected should not be present", mic.getTargetProjectSelectedEl().isPresent().now(), is(false));
        //correct destination parent issue
        assertThat(String.format("Target parent issue should be preselected and set to one of %s", parentIssueKeys), mic.getParentIssueValue(), Matchers.isIn(parentIssueKeys));
        parentIssueKeys.remove(mic.getParentIssueValue());
        //all types available in issue type selector
        Iterable<String> issueTypes = mic.getAvailableIssueTypeSuggestions();
        assertThat("There should be 5 issue type suggestions", Iterables.size(issueTypes), Matchers.equalTo(5));
        issueTypes.forEach(s -> assertThat("All issue types should be available", s, Matchers.isIn(ImmutableSet.of("New Feature", "Task", "Improvement", "Bug", "Sub-task"))));
        //select target issue type
        mic.selectIssueType(targetIssueType);
    }

    private String mapProjectNameToProjectKey(final String projectName) {
        String key = projectNameToKey.get(projectName);
        if (key != null) {
            return key;
        }
        throw new RuntimeException(String.format("Unsupported project name: %s", projectName));
    }

    private Collection<IssueDto> setupIssues(final String projectKey, String issueTypeId, String assignee, final Long securityLevelId, final int howMany) {
        Collection<IssueDto> issueIds = new ArrayList<>();

        for (int i = 0; i < howMany; i++) {
            final String summary = String.format("comment for %s: %s", projectKey, Integer.toBinaryString(i));
            IssueCreateResponse issueCreateResp = backdoor.issues().createIssue(projectKey, summary, assignee);
            assertNotNull(issueCreateResp.id);
            issueIds.add(new IssueDtoBuilder().setId(issueCreateResp.id).setKey(issueCreateResp.key).setAssignee(assignee).
                    setIssueTypeId(issueTypeId).setProjectKey(projectKey).setProjectId(projectKeyToId.get(projectKey)).
                    setSecurityLevelId(securityLevelId).createIssueDto());

            IssueFields issueFields = new IssueFields();
            if (issueTypeId != null || securityLevelId != null) {
                if (issueTypeId != null) {
                    issueFields.issueType(ResourceRef.withId(issueTypeId));
                }
                if (securityLevelId != null) {
                    issueFields.securityLevel(ResourceRef.withId("" + securityLevelId));
                }
                backdoor.issues().setIssueFields(issueCreateResp.id, issueFields);
            }

        }

        return issueIds;
    }

    private Collection<IssueDto> setupSubtasks(final String projectId, String parentKey, final int howMany) {
        Collection<IssueDto> issueIds = new ArrayList<>();

        for (int i = 0; i < howMany; i++) {
            final String summary = String.format("subtask of %s->%s: %s", projectId, parentKey, Integer.toBinaryString(i));
            IssueCreateResponse issueCreateResp = backdoor.issues().createSubtask(projectId, parentKey, summary);

            assertNotNull(issueCreateResp.id);
            issueIds.add(new IssueDtoBuilder().setId(issueCreateResp.id).setKey(issueCreateResp.key).setProjectId(projectId).setProjectKey(projectKeyToId.get(projectId)).
                    setParentKey(parentKey).createIssueDto());
        }

        return issueIds;
    }

    static class IssueDto {
        private final String id;
        private final String key;

        private final String projectKey;
        private final String projectId;
        private final String issueTypeId;
        private final String assignee;
        private final String parentId;
        private final String parentKey;
        private final Long securityLevelId;

        private IssueDto(final String id, final String key, final String projectKey, final String projectId, final String issueTypeId, final String assignee,
                         final String parentId, final String parentKey, final Long securityLevelId) {
            this.id = id;
            this.key = key;
            this.projectKey = projectKey;
            this.projectId = projectId;
            this.issueTypeId = issueTypeId;
            this.assignee = assignee;
            this.parentId = parentId;
            this.parentKey = parentKey;
            this.securityLevelId = securityLevelId;
        }

        public String getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public String getProjectKey() {
            return projectKey;
        }

        public String getProjectId() {
            return projectId;
        }

        public String getIssueTypeId() {
            return issueTypeId;
        }

        public String getAssignee() {
            return assignee;
        }

        public String getParentId() {
            return parentId;
        }

        public String getParentKey() {
            return parentKey;
        }

        public boolean isSubtask() {
            return getParentId() != null || getParentKey() != null;
        }

        public Long getSecurityLevelId() {
            return securityLevelId;
        }
    }

    private static class IssueDtoBuilder {
        private String id;
        private String key;
        private String projectKey;
        private String projectId;
        private String issueTypeId;
        private String assignee;
        private String parentId;
        private String parentKey;
        private Long securityLevelId;

        public IssueDtoBuilder setId(final String id) {
            this.id = id;
            return this;
        }

        public IssueDtoBuilder setKey(final String key) {
            this.key = key;
            return this;
        }

        public IssueDtoBuilder setProjectKey(final String projectKey) {
            this.projectKey = projectKey;
            return this;
        }

        public IssueDtoBuilder setProjectId(final String projectId) {
            this.projectId = projectId;
            return this;
        }

        public IssueDtoBuilder setIssueTypeId(final String issueTypeId) {
            this.issueTypeId = issueTypeId;
            return this;
        }

        public IssueDtoBuilder setAssignee(final String assignee) {
            this.assignee = assignee;
            return this;
        }

        public IssueDtoBuilder setParentId(final String parentId) {
            this.parentId = parentId;
            return this;
        }

        public IssueDtoBuilder setParentKey(final String parentKey) {
            this.parentKey = parentKey;
            return this;
        }

        public IssueDtoBuilder setSecurityLevelId(final Long securityLevelId) {
            this.securityLevelId = securityLevelId;
            return this;
        }

        public TestBulkMoveIssues.IssueDto createIssueDto() {
            return new TestBulkMoveIssues.IssueDto(id, key, projectKey, projectId, issueTypeId, assignee, parentId, parentKey, securityLevelId);
        }
    }
}
