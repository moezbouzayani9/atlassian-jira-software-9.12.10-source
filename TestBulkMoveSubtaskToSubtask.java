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
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.rest.api.issue.IssueCreateResponse;
import com.atlassian.jira.rest.api.issue.IssueFields;
import com.atlassian.jira.rest.api.issue.ResourceRef;
import com.atlassian.jira.testkit.client.IssueSecuritySchemesControl;
import com.atlassian.jira.testkit.client.restclient.Component;
import com.atlassian.jira.testkit.client.restclient.Issue;
import com.atlassian.jira.testkit.client.restclient.Version;
import com.atlassian.jira.util.collect.CollectionUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.core.IsAnything.anything;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for BulkEdit of multi select system fields.
 *
 * @since v7.0
 */
@WebTest({Category.WEBDRIVER_TEST, Category.BULK_OPERATIONS})
public class TestBulkMoveSubtaskToSubtask extends BaseJiraWebTest {
    public static final String ISSUE_BUG = "1";
    public static final String ISSUE_TASK = "3";
    public static final String ISSUE_IMPROVEMENT = "4";

    final String PROJECT_NOT_SECURED1 = "notsecured1";
    final String PROJECT_NOT_SECURED1_KEY = "NS1";
    final String PROJECT_NOT_SECURED2 = "notsecured2";
    final String PROJECT_NOT_SECURED2_KEY = "NS2";
    final String PROJECT_SECURED1 = "secured1";
    final String PROJECT_SECURED1_KEY = "SS1";
    final String PROJECT_SECURED1a = "securedd1";
    final String PROJECT_SECURED1a_KEY = "SA1";
    final String PROJECT_SECURED2 = "secured2";
    final String PROJECT_SECURED2_KEY = "SS2";

    public static final String ADMIN_USERNAME = "admin";
    public static final String FRED_USERNAME = "fred";
    public static final String FRED_PASSWORD = "fred";

    public static final String JIRA_DEV_GROUP = "jira-developers";

    private Map<String, String> projectKeyToId;
    private Map<String, String> projectIdToKey;
    private Map<String, String> projectNameToKey;

    final String ISSUE_SECURITY_LEVEL_SCHEME_1 = "Secured1IssueSecurityLevelScheme";
    final String ISSUE_SECURITY_LEVEL_SCHEME_2 = "Secured2IssueSecurityLevelScheme";
    long issueSecuritySchemesId1;
    long issueSecuritySchemesId2;

    final String securityLevelNameSecured12Level1 = "Secured12Level1";
    final String securityLevelNameSecured1Level2 = "Secured1Level2";
    final String securityLevelNameSecured2Level2 = "Secured2Level2";
    long securityLevelIdScheme1Level1;
    long securityLevelIdScheme1Level2;
    long securityLevelIdScheme2Level1;
    long securityLevelIdScheme2Level2;

    public static final long DEFAULT_PERM_SCHEME_ID = 0;

    IssueDto parentNotSecured1_1;
    IssueDto parentNotSecured1_2;
    IssueDto parentNotSecured2_1;
    IssueDto parentNotSecured2_2;
    IssueDto parentSecured1_1;
    IssueDto parentSecured1_2;
    IssueDto parentSecured1a_1;
    IssueDto parentSecured1a_2;
    IssueDto parentSecured2_1;
    IssueDto parentSecured2_2;

    Collection<IssueDto> subtasks = new ArrayList<>();

    @Before
    public void initializeTestData() {
        backdoor.restoreBlankInstance();
        backdoor.flags().clearFlags();
        projectIdToKey = new HashMap<>();
        projectKeyToId = new HashMap<>();
        projectNameToKey = new HashMap<>();

        backdoor.usersAndGroups().addUserToGroup(FRED_USERNAME, JIRA_DEV_GROUP);
        backdoor.subtask().enable();

        IssueSecuritySchemesControl issueSecuritySchemesControl = backdoor.getTestkit().issueSecuritySchemesControl();

        // setup project not secured 1
        String PROJECT_NOT_SECURED1_ID = Long.toString(backdoor.project().addProject(PROJECT_NOT_SECURED1, PROJECT_NOT_SECURED1_KEY, FRED_USERNAME));
        projectIdToKey.put(PROJECT_NOT_SECURED1_ID, PROJECT_NOT_SECURED1_KEY);
        projectNameToKey.put(PROJECT_NOT_SECURED1, PROJECT_NOT_SECURED1_KEY);

        // setup project not secured 2
        String PROJECT_NOT_SECURED2_ID = Long.toString(backdoor.project().addProject(PROJECT_NOT_SECURED2, PROJECT_NOT_SECURED2_KEY, FRED_USERNAME));
        projectIdToKey.put(PROJECT_NOT_SECURED2_ID, PROJECT_NOT_SECURED2_KEY);
        projectNameToKey.put(PROJECT_NOT_SECURED2, PROJECT_NOT_SECURED2_KEY);

        //setup project secured 1
        String PROJECT_SECURED1_ID = Long.toString(backdoor.project().addProject(PROJECT_SECURED1, PROJECT_SECURED1_KEY, FRED_USERNAME));
        projectIdToKey.put(PROJECT_SECURED1_ID, PROJECT_SECURED1_KEY);
        projectNameToKey.put(PROJECT_SECURED1, PROJECT_SECURED1_KEY);

        //setup project secured 1a
        String PROJECT_SECURED1a_ID = Long.toString(backdoor.project().addProject(PROJECT_SECURED1a, PROJECT_SECURED1a_KEY, FRED_USERNAME));
        projectIdToKey.put(PROJECT_SECURED1a_ID, PROJECT_SECURED1a_KEY);
        projectNameToKey.put(PROJECT_SECURED1a, PROJECT_SECURED1a_KEY);

        //setup project secured 2
        String PROJECT_SECURED2_ID = Long.toString(backdoor.project().addProject(PROJECT_SECURED2, PROJECT_SECURED2_KEY, FRED_USERNAME));
        projectIdToKey.put(PROJECT_SECURED2_ID, PROJECT_SECURED2_KEY);
        projectNameToKey.put(PROJECT_SECURED2, PROJECT_SECURED2_KEY);

        projectIdToKey.entrySet().forEach(e -> projectKeyToId.put(e.getValue(), e.getKey()));

        issueSecuritySchemesId1 = issueSecuritySchemesControl.createScheme(ISSUE_SECURITY_LEVEL_SCHEME_1, "Secured1");
        issueSecuritySchemesId2 = issueSecuritySchemesControl.createScheme(ISSUE_SECURITY_LEVEL_SCHEME_2, "Secured2");

        securityLevelIdScheme1Level1 = issueSecuritySchemesControl.addSecurityLevel(issueSecuritySchemesId1, securityLevelNameSecured12Level1, "desc");
        securityLevelIdScheme1Level2 = issueSecuritySchemesControl.addSecurityLevel(issueSecuritySchemesId1, securityLevelNameSecured1Level2, "desc");
        securityLevelIdScheme2Level1 = issueSecuritySchemesControl.addSecurityLevel(issueSecuritySchemesId2, securityLevelNameSecured12Level1, "desc");
        securityLevelIdScheme2Level2 = issueSecuritySchemesControl.addSecurityLevel(issueSecuritySchemesId2, securityLevelNameSecured2Level2, "desc");

        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId1, securityLevelIdScheme1Level1, ADMIN_USERNAME);
        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId1, securityLevelIdScheme1Level1, FRED_USERNAME);
        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId1, securityLevelIdScheme1Level2, ADMIN_USERNAME);
        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId1, securityLevelIdScheme1Level2, FRED_USERNAME);
        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId2, securityLevelIdScheme2Level1, ADMIN_USERNAME);
        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId2, securityLevelIdScheme2Level1, FRED_USERNAME);
        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId2, securityLevelIdScheme2Level2, ADMIN_USERNAME);
        issueSecuritySchemesControl.addUserToSecurityLevel(issueSecuritySchemesId2, securityLevelIdScheme2Level2, FRED_USERNAME);

        backdoor.project().setIssueSecurityScheme(backdoor.project().getProjectId(PROJECT_SECURED1_KEY), issueSecuritySchemesId1);
        backdoor.project().setIssueSecurityScheme(backdoor.project().getProjectId(PROJECT_SECURED1a_KEY), issueSecuritySchemesId1);
        backdoor.project().setIssueSecurityScheme(backdoor.project().getProjectId(PROJECT_SECURED2_KEY), issueSecuritySchemesId2);
        backdoor.permissionSchemes().addGroupPermission(DEFAULT_PERM_SCHEME_ID, ProjectPermissions.SET_ISSUE_SECURITY, JIRA_DEV_GROUP);

        // parent/child not secured 1
        parentNotSecured1_1 = setupIssues(PROJECT_NOT_SECURED1_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 1).iterator().next();
        subtasks.addAll(setupSubtasks(parentNotSecured1_1.getProjectId(), parentNotSecured1_1.getKey(), 2));
        parentNotSecured1_2 = setupIssues(PROJECT_NOT_SECURED1_KEY, ISSUE_BUG, ADMIN_USERNAME, null, 1).iterator().next();
        subtasks.addAll(setupSubtasks(parentNotSecured1_2.getProjectId(), parentNotSecured1_2.getKey(), 2));

        // parent/child not secured 2
        parentNotSecured2_1 = setupIssues(PROJECT_NOT_SECURED2_KEY, ISSUE_TASK, ADMIN_USERNAME, null, 1).iterator().next();
        subtasks.addAll(setupSubtasks(parentNotSecured2_1.getProjectId(), parentNotSecured2_1.getKey(), 2));
        parentNotSecured2_2 = setupIssues(PROJECT_NOT_SECURED2_KEY, ISSUE_TASK, ADMIN_USERNAME, null, 1).iterator().next();
        subtasks.addAll(setupSubtasks(parentNotSecured2_2.getProjectId(), parentNotSecured2_2.getKey(), 2));

        // parent/child secured 1
        parentSecured1_1 = setupIssues(PROJECT_SECURED1_KEY, ISSUE_IMPROVEMENT, ADMIN_USERNAME, securityLevelIdScheme1Level1, 1).iterator().next();
        subtasks.addAll(setupSubtasks(parentSecured1_1.getProjectId(), parentSecured1_1.getKey(), 2));
        parentSecured1_2 = setupIssues(PROJECT_SECURED1_KEY, ISSUE_IMPROVEMENT, ADMIN_USERNAME, securityLevelIdScheme1Level2, 1).iterator().next();
        subtasks.addAll(setupSubtasks(parentSecured1_2.getProjectId(), parentSecured1_2.getKey(), 2));

        // parent/child secured 1a
        parentSecured1a_1 = setupIssues(PROJECT_SECURED1a_KEY, ISSUE_BUG, ADMIN_USERNAME, securityLevelIdScheme1Level1, 1).iterator().next();
        subtasks.addAll(setupSubtasks(parentSecured1a_1.getProjectId(), parentSecured1a_1.getKey(), 2));
        parentSecured1a_2 = setupIssues(PROJECT_SECURED1a_KEY, ISSUE_BUG, ADMIN_USERNAME, securityLevelIdScheme1Level2, 1).iterator().next();
        subtasks.addAll(setupSubtasks(parentSecured1a_2.getProjectId(), parentSecured1a_2.getKey(), 2));

        // parent/child secured 2
        parentSecured2_1 = setupIssues(PROJECT_SECURED2_KEY, ISSUE_IMPROVEMENT, ADMIN_USERNAME, securityLevelIdScheme2Level1, 1).iterator().next();
        subtasks.addAll(setupSubtasks(parentSecured2_1.getProjectId(), parentSecured2_1.getKey(), 2));
        parentSecured2_2 = setupIssues(PROJECT_SECURED2_KEY, ISSUE_IMPROVEMENT, ADMIN_USERNAME, securityLevelIdScheme2Level2, 1).iterator().next();
        subtasks.addAll(setupSubtasks(parentSecured2_2.getProjectId(), parentSecured2_2.getKey(), 2));
    }

    @Test
    public void testSubtaskSecuredToSubtaskSecuredDifferentSecurityLevel() {
        //secured1_1 to secured2_1 in different project
        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery(String.format("project=%s and issuetype in (Sub-task)", PROJECT_SECURED1_KEY)).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();
        //wait for JS to execute
        moveDetails.waitForJavaScriptToProcessParentIssuePicker();

        assertThat("Use the above project and issue type pair for all other combinations checkbox is present", moveDetails.isSameAllCheckboxPresent().now(), equalTo(true));

        //secured1_1 to secured1_1 in different project
        final String targetIssueType = "Sub-task";
        final String targetProject = PROJECT_SECURED2;
        Collection<MoveIssuesContainer> containerForSubtasks = moveDetails.getMoveIssuesContainers();
        assertThat("There should be two groups of issues", containerForSubtasks.size(), equalTo(2));
        Iterator<MoveIssuesContainer> sit = containerForSubtasks.iterator();

        MoveIssuesContainer mic1 = sit.next();
        MoveIssuesContainer mic2 = sit.next();
        Map<String, MoveIssuesContainer> parentIssueToMic = ImmutableMap.of(mic1.getParentIssueValue(), mic1, mic2.getParentIssueValue(), mic2);

        MoveIssuesContainer micParent1 = parentIssueToMic.get(parentSecured1_1.getKey());
        MoveIssuesContainer micParent2 = parentIssueToMic.get(parentSecured1_2.getKey());
        assertThat("There should be one group with parent preset to parentSecured1_1", micParent1, notNullValue());
        assertThat("There should be one group with parent preset to parentSecured1_2", micParent2, notNullValue());
        testMoveIssueContainerForSubtaskToSubtask(micParent1, targetProject, targetIssueType, parentSecured2_1.getKey());
        testMoveIssueContainerForSubtaskToSubtask(micParent2, targetProject, targetIssueType, parentSecured2_2.getKey());

        //set fields for issues, two destination projects so there is second set fields page
        MoveSetFields setFields = moveDetails.next();
        final String securityLevelWarningText = "The security level of subtasks is inherited from parents.";
        checkIfSecurityLevelWarningIsPresent(setFields, securityLevelWarningText, true, true);
        setFields = setFields.next(MoveSetFields.class);
        checkIfSecurityLevelWarningIsPresent(setFields, securityLevelWarningText, true, true);

        MoveConfirmationPage confirmationPage = setFields.next(MoveConfirmationPage.class);
        Iterable<MoveConfirmationPage.MoveIssuesConfirmContainer> moveIssuesConfirmContainerIterable = confirmationPage.getMoveIssuesConfirmContainers();
        assertThat("There should be two groups of issues", Iterables.size(moveIssuesConfirmContainerIterable), equalTo(2));
        assertThat(String.format("All target projects should be %s", targetProject), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetProject", equalTo(targetProject))));
        assertThat(String.format("All target issue types should be %s", targetIssueType), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetIssueType", equalTo(targetIssueType))));
        // parent issues, 2 different
        Collection<String> targetParentIssues = CollectionUtil.toList(moveIssuesConfirmContainerIterable).stream().map(c -> c.getTargetParentIssue()).collect(CollectorsUtil.toImmutableList());
        logger.info(targetParentIssues.toString());
        assertThat("There should be 2 different parent issues", targetParentIssues, Matchers.containsInAnyOrder(parentSecured2_1.getKey(), parentSecured2_2.getKey()));

        //updated security levels
        Collection<String> targetSecurityLevels = CollectionUtil.toList(moveIssuesConfirmContainerIterable).stream().map(c -> c.getUpdateFields())
                .map(CollectionUtil::toList).flatMap(Collection::stream).filter(field -> field.getField().equals("Security Level")).map(f -> f.getValue()).
                        collect(CollectorsUtil.toImmutableList());
        assertThat("There should be 2 different security level", targetSecurityLevels,
                Matchers.containsInAnyOrder(securityLevelNameSecured12Level1, securityLevelNameSecured2Level2)
        );

        confirmationPage.confirm().acknowledge();

        //check if issues have moved to subtasks
        Collection<IssueDto> subtasksToMove = subtasks.stream().filter(st -> st.getParentKey().equals(parentSecured1_1.getKey()) || st.getParentKey().equals(parentSecured1_2.getKey())).collect(CollectorsUtil.toImmutableList());
        final Collection<Issue> subtasksAfter = subtasksToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                subtasksAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                subtasksAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));

        Collection<IssueDto> subtasksToMoveToSecured2_1 = subtasksToMove.stream().filter(st -> st.getParentKey().equals(parentSecured1_1.getKey())).collect(CollectorsUtil.toImmutableList());
        Collection<IssueDto> subtasksToMoveToSecured2_2 = subtasksToMove.stream().filter(st -> st.getParentKey().equals(parentSecured1_2.getKey())).collect(CollectorsUtil.toImmutableList());
        final Collection<Issue> subtasksAfterToSecured2_1 = subtasksToMoveToSecured2_1.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        final Collection<Issue> subtasksAfterToSecured2_2 = subtasksToMoveToSecured2_2.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());

        assertThat(String.format("New parent should be set to %s", parentSecured2_1.getKey()),
                subtasksAfterToSecured2_1, everyItem(IssueMatchers.issueWithParentWithKey(parentSecured2_1.getKey())));
        assertThat(String.format("New parent should be set to %s", parentSecured2_2.getKey()),
                subtasksAfterToSecured2_2, everyItem(IssueMatchers.issueWithParentWithKey(parentSecured2_2.getKey())));

        //security levels
        assertThat(String.format("Security Level for subtask should be set to %s", securityLevelNameSecured12Level1), subtasksAfterToSecured2_1, everyItem(IssueMatchers.issueWithSecurityLevelName(securityLevelNameSecured12Level1)));
        assertThat(String.format("Security Level for subtask should be set to %s", securityLevelNameSecured2Level2), subtasksAfterToSecured2_2, everyItem(IssueMatchers.issueWithSecurityLevelName(securityLevelNameSecured2Level2)));

        //current parent issues should have no subtasks
        Issue oldParentIssueAfter = backdoor.issues().getIssue(parentSecured1_1.getKey());
        assertThat("Issue should have no subtasks", oldParentIssueAfter.fields.subtasks.size(), equalTo(0));
        oldParentIssueAfter = backdoor.issues().getIssue(parentSecured1_2.getKey());
        assertThat("Issue should have no subtasks", oldParentIssueAfter.fields.subtasks.size(), equalTo(0));

        //new parent issues should have new subtasks
        Issue newParentIssueAfter = backdoor.issues().getIssue(parentSecured2_1.getKey());
        assertThat("New parent should have 4 links", newParentIssueAfter.fields.subtasks.size(), equalTo(4));
        newParentIssueAfter = backdoor.issues().getIssue(parentSecured2_2.getKey());
        assertThat("New parent should have 4 links", newParentIssueAfter.fields.subtasks.size(), equalTo(4));


        // test history tab
        IssueDto movedIssueBefore = subtasksToMoveToSecured2_1.iterator().next();
        Issue movedIssueAfter = backdoor.issues().getIssue(movedIssueBefore.getKey());
        HistoryModule historyTab = jira.goToViewIssue(movedIssueAfter.key).getActivitySection().historyModule();
        Iterable<HistoryModule.IssueHistoryItem> historyItems = historyTab.getHistoryItemContainers(2).stream()
            .flatMap(container -> container.getHistoryDataElements().stream())
            .collect(Collectors.toList());
        assertThat("There should be 4 properties changed", Iterables.size(historyItems), equalTo(4));
        assertThat("Project should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Project", "secured1 [ 10012 ]", "secured2 [ 10014 ]")));
        assertThat("Parent should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Parent", "SS1-1 [ 10012 ]", "SS2-1 [ 10024 ]")));
        assertThat("Security should be copied from new parent", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Security", "Secured12Level1 [ 10000 ]", "Secured12Level1 [ 10002 ]")));
        assertThat("Issue key should change when moving between projects", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Key", movedIssueBefore.getKey(), movedIssueAfter.key)));

        movedIssueBefore = subtasksToMoveToSecured2_2.iterator().next();
        movedIssueAfter = backdoor.issues().getIssue(movedIssueBefore.getKey());
        historyTab = jira.goToViewIssue(movedIssueAfter.key).getActivitySection().historyModule();
        historyItems = historyTab.getHistoryItemContainers(2).stream()
            .flatMap(container -> container.getHistoryDataElements().stream())
            .collect(Collectors.toList());
        assertThat("There should be 4 properties changed", Iterables.size(historyItems), equalTo(4));
        assertThat("Project should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Project", "secured1 [ 10012 ]", "secured2 [ 10014 ]")));
        assertThat("Parent should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Parent", "SS1-4 [ 10015 ]", "SS2-4 [ 10027 ]")));
        assertThat("Security should be copied from new parent", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Security", "Secured1Level2 [ 10001 ]", "Secured2Level2 [ 10003 ]")));
        assertThat("Issue key should change when moving between projects", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Key", movedIssueBefore.getKey(), movedIssueAfter.key)));
    }

    @Test
    public void testSubtaskSecuredToIssueSecuredDifferentSecurityLevel() {
        //secured1_1 to secured2_1 in different project
        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery(String.format("project=%s and issuetype in (Sub-task)", PROJECT_SECURED1_KEY)).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();
        //wait for JS to execute
        moveDetails.waitForJavaScriptToProcessParentIssuePicker();

        assertThat("Use the above project and issue type pair for all other combinations checkbox is present", moveDetails.isSameAllCheckboxPresent().now(), equalTo(true));

        //secured1_1 to secured1_1 in different project
        final String targetIssueType = "Bug";
        final String targetProject = PROJECT_SECURED2;
        Collection<MoveIssuesContainer> containerForSubtasks = moveDetails.getMoveIssuesContainers();
        assertThat("There should be two groups of issues", containerForSubtasks.size(), equalTo(2));
        Iterator<MoveIssuesContainer> sit = containerForSubtasks.iterator();

        MoveIssuesContainer mic1 = sit.next();
        MoveIssuesContainer mic2 = sit.next();
        Map<String, MoveIssuesContainer> parentIssueToMic = ImmutableMap.of(mic1.getParentIssueValue(), mic1, mic2.getParentIssueValue(), mic2);

        MoveIssuesContainer micParent1 = parentIssueToMic.get(parentSecured1_1.getKey());
        MoveIssuesContainer micParent2 = parentIssueToMic.get(parentSecured1_2.getKey());
        assertThat("There should be one group with parent preset to parentSecured1_1", micParent1, notNullValue());
        assertThat("There should be one group with parent preset to parentSecured1_2", micParent2, notNullValue());
        testMoveIssueContainerForSubtaskToIssue(micParent1, targetProject, targetIssueType, parentSecured2_1.getKey());
        testMoveIssueContainerForSubtaskToIssue(micParent2, targetProject, targetIssueType, parentSecured2_2.getKey());

        //set fields for issues, two destination projects so there is second set fields page
        MoveSetFields setFields = moveDetails.next();
        checkIfSecurityLevelSelectIsPresent(setFields, "None\nSecured12Level1\nSecured2Level2");

        MoveConfirmationPage confirmationPage = setFields.next(MoveConfirmationPage.class);
        Iterable<MoveConfirmationPage.MoveIssuesConfirmContainer> moveIssuesConfirmContainerIterable = confirmationPage.getMoveIssuesConfirmContainers();
        assertThat("There should be one group of issues", Iterables.size(moveIssuesConfirmContainerIterable), equalTo(1));
        assertThat(String.format("All target projects should be %s", targetProject), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetProject", equalTo(targetProject))));
        assertThat(String.format("All target issue types should be %s", targetIssueType), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetIssueType", equalTo(targetIssueType))));

        //updated security levels
        Collection<String> targetSecurityLevels = CollectionUtil.toList(moveIssuesConfirmContainerIterable).stream().map(c -> c.getUpdateFields())
                .map(CollectionUtil::toList).flatMap(Collection::stream).filter(field -> field.getField().equals("Security Level")).map(f -> f.getValue()).
                        collect(CollectorsUtil.toImmutableList());
        assertThat("There should be one security level set to None", targetSecurityLevels,
                Matchers.containsInAnyOrder("None")
        );

        confirmationPage.confirm().acknowledge();

        //check if subtasks have moved to issues
        Collection<IssueDto> subtasksToMove = subtasks.stream().filter(st -> st.getParentKey().equals(parentSecured1_1.getKey()) || st.getParentKey().equals(parentSecured1_2.getKey())).collect(CollectorsUtil.toImmutableList());
        final Collection<Issue> issuesAfter = subtasksToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                issuesAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                issuesAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));
        assertThat("Issues should have no parent",
                issuesAfter, everyItem(IssueMatchers.issueWithParentKey(null)));
        //security levels
        assertThat(String.format("Security Level for subtask should be set to None", securityLevelNameSecured12Level1), issuesAfter, everyItem(IssueMatchers.issueWithoutSecurityLevel()));

        //current parent issues should have no subtasks
        Issue oldParentIssueAfter = backdoor.issues().getIssue(parentSecured1_1.getKey());
        assertThat("Issue should have no subtasks", oldParentIssueAfter.fields.subtasks.size(), equalTo(0));
        oldParentIssueAfter = backdoor.issues().getIssue(parentSecured1_2.getKey());
        assertThat("Issue should have no subtasks", oldParentIssueAfter.fields.subtasks.size(), equalTo(0));

        // test history tab
        IssueDto movedIssueBefore = subtasksToMove.iterator().next();
        Issue movedIssueAfter = backdoor.issues().getIssue(movedIssueBefore.getKey());
        HistoryModule historyTab = jira.goToViewIssue(movedIssueAfter.key).getActivitySection().historyModule();
        Iterable<HistoryModule.IssueHistoryItem> historyItems = historyTab.getHistoryItemContainers(2).stream()
                .flatMap(container -> container.getHistoryDataElements().stream())
                .collect(Collectors.toList());
        assertThat("There should be 5 properties changed", Iterables.size(historyItems), equalTo(5));
        assertThat("Project should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Project", "secured1 [ 10012 ]", "secured2 [ 10014 ]")));
        assertThat("Issue type should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Issue Type", "Sub-task [ 10000 ]", "Bug [ 1 ]")));
        assertThat("Parent should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Parent", "SS1-1 [ 10012 ]", "")));
        assertThat("Security should be copied from new parent", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Security", "Secured12Level1 [ 10000 ]", "")));
        assertThat("Issue key should change when moving between projects", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Key", movedIssueBefore.getKey(), movedIssueAfter.key)));
    }

    @Test
    public void testSubtaskSecuredToSubtaskSecuredSameSecurityLevel() {
        //secured1_1 to secured2_1 in different project
        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery(String.format("parent = %s", parentSecured1_1.getKey())).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();
        //wait for JS to execute
        moveDetails.waitForJavaScriptToProcessParentIssuePicker();

        assertThat("Use the above project and issue type pair for all other combinations checkbox is present", moveDetails.isSameAllCheckboxPresent().now(), equalTo(true));

        //secured1_1 to secured1_1 in different project
        final String targetIssueType = "Sub-task";
        final String targetProject = PROJECT_SECURED1a;
        Collection<MoveIssuesContainer> containerForSubtasks = moveDetails.getMoveIssuesContainers();
        assertThat("There should be one group of issues", containerForSubtasks.size(), equalTo(1));
        testMoveIssueContainerForSubtaskToSubtask(containerForSubtasks.iterator().next(), targetProject, targetIssueType, parentSecured1a_1.getKey());

        //set fields for issues, two destination projects so there is second set fields page
        MoveSetFields setFields = moveDetails.next();
        assertThat("All fields should be retained", setFields.isAllFieldsRetained(), equalTo(true));

        MoveConfirmationPage confirmationPage = setFields.next(MoveConfirmationPage.class);
        Iterable<MoveConfirmationPage.MoveIssuesConfirmContainer> moveIssuesConfirmContainerIterable = confirmationPage.getMoveIssuesConfirmContainers();
        assertThat("There should be one group of issues", Iterables.size(moveIssuesConfirmContainerIterable), equalTo(1));
        assertThat(String.format("All target projects should be %s", targetProject), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetProject", equalTo(targetProject))));
        assertThat(String.format("All target issue types should be %s", targetIssueType), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetIssueType", equalTo(targetIssueType))));
        // parent issue
        Collection<String> targetParentIssues = CollectionUtil.toList(moveIssuesConfirmContainerIterable).stream().map(c -> c.getTargetParentIssue()).collect(CollectorsUtil.toImmutableList());
        assertThat("There should be 1 parent issues", targetParentIssues, Matchers.contains(parentSecured1a_1.getKey()));

        // updated security levels
        MoveConfirmationPage.MoveIssuesConfirmContainer micc = moveIssuesConfirmContainerIterable.iterator().next();
        assertThat("No fields should be updated", micc.getUpdateFieldsTabPresent().now(), is(false));

        confirmationPage.confirm().acknowledge();

        //check if issues have moved to subtasks
        Collection<IssueDto> subtasksToMove = subtasks.stream().filter(st -> st.getParentKey().equals(parentSecured1_1.getKey())).collect(CollectorsUtil.toImmutableList());
        final Collection<Issue> subtasksAfter = subtasksToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                subtasksAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                subtasksAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));

        assertThat(String.format("New parent should be set to %s", parentSecured1a_1.getKey()),
                subtasksAfter, everyItem(IssueMatchers.issueWithParentWithKey(parentSecured1a_1.getKey())));

        //security levels
        assertThat(String.format("Security Level for subtask should be set to %s", securityLevelNameSecured12Level1), subtasksAfter, everyItem(IssueMatchers.issueWithSecurityLevelName(securityLevelNameSecured12Level1)));

        //current parent issues should have no subtasks
        Issue oldParentIssueAfter = backdoor.issues().getIssue(parentSecured1_1.getKey());
        assertThat("Issue should have no subtasks", oldParentIssueAfter.fields.subtasks.size(), equalTo(0));

        //new parent issues should have new subtasks
        Issue newParentIssueAfter = backdoor.issues().getIssue(parentSecured1a_1.getKey());
        assertThat("New parent should have 4 links", newParentIssueAfter.fields.subtasks.size(), equalTo(4));

        // test history tab
        IssueDto movedIssueBefore = subtasksToMove.iterator().next();
        Issue movedIssueAfter = backdoor.issues().getIssue(movedIssueBefore.getKey());
        HistoryModule historyTab = jira.goToViewIssue(movedIssueAfter.key).getActivitySection().historyModule();
        Iterable<HistoryModule.IssueHistoryItem> historyItems = historyTab.getHistoryItemContainers(2).stream()
            .flatMap(container -> container.getHistoryDataElements().stream())
            .collect(Collectors.toList());
        assertThat("There should be 3 properties changed", Iterables.size(historyItems), equalTo(3));
        assertThat("Project should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Project", "secured1 [ 10012 ]", "securedd1 [ 10013 ]")));
        assertThat("Parent should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Parent", "SS1-1 [ 10012 ]", "SA1-1 [ 10018 ]")));
        assertThat("Issue key should change when moving between projects", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Key", movedIssueBefore.getKey(), movedIssueAfter.key)));
    }

    @Test
    public void testSubtaskNotSecuredToSubtaskSecured() {
        //secured1_1 to secured2_1 in different project
        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery(String.format("parent = %s", parentNotSecured1_1.getKey())).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();
        //wait for JS to execute
        moveDetails.waitForJavaScriptToProcessParentIssuePicker();

        assertThat("Use the above project and issue type pair for all other combinations checkbox is present", moveDetails.isSameAllCheckboxPresent().now(), equalTo(true));

        //secured1_1 to secured1_1 in different project
        final String targetIssueType = "Sub-task";
        final String targetProject = PROJECT_SECURED1;
        Collection<MoveIssuesContainer> containerForSubtasks = moveDetails.getMoveIssuesContainers();
        assertThat("There should be one group of issues", containerForSubtasks.size(), equalTo(1));
        testMoveIssueContainerForSubtaskToSubtask(containerForSubtasks.iterator().next(), targetProject, targetIssueType, parentSecured1_1.getKey());

        MoveSetFields setFields = moveDetails.next();
        checkIfSecurityLevelWarningIsPresent(setFields, "The security level of subtasks is inherited from parents.", true, true);

        MoveConfirmationPage confirmationPage = setFields.next(MoveConfirmationPage.class);
        Iterable<MoveConfirmationPage.MoveIssuesConfirmContainer> moveIssuesConfirmContainerIterable = confirmationPage.getMoveIssuesConfirmContainers();
        assertThat("There should be one group of issues", Iterables.size(moveIssuesConfirmContainerIterable), equalTo(1));
        assertThat(String.format("All target projects should be %s", targetProject), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetProject", equalTo(targetProject))));
        assertThat(String.format("All target issue types should be %s", targetIssueType), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetIssueType", equalTo(targetIssueType))));
        // parent issue
        Collection<String> targetParentIssues = CollectionUtil.toList(moveIssuesConfirmContainerIterable).stream().map(c -> c.getTargetParentIssue()).collect(CollectorsUtil.toImmutableList());
        assertThat("There should be 1 parent issues", targetParentIssues, Matchers.contains(parentSecured1_1.getKey()));

        // updated security levels
        Collection<String> targetSecurityLevels = CollectionUtil.toList(moveIssuesConfirmContainerIterable).stream().map(c -> c.getUpdateFields())
                .map(CollectionUtil::toList).flatMap(Collection::stream).filter(field -> field.getField().equals("Security Level")).map(f -> f.getValue()).
                        collect(CollectorsUtil.toImmutableList());
        assertThat("There should be 1 security level", targetSecurityLevels,
                Matchers.contains(securityLevelNameSecured12Level1)
        );

        confirmationPage.confirm().acknowledge();

        //check if issues have moved to subtasks
        Collection<IssueDto> subtasksToMove = subtasks.stream().filter(st -> st.getParentKey().equals(parentNotSecured1_1.getKey())).collect(CollectorsUtil.toImmutableList());
        final Collection<Issue> subtasksAfter = subtasksToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                subtasksAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                subtasksAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));

        assertThat(String.format("New parent should be set to %s", parentSecured1_1.getKey()),
                subtasksAfter, everyItem(IssueMatchers.issueWithParentWithKey(parentSecured1_1.getKey())));

        //security levels
        assertThat(String.format("Security Level for subtask should be set to %s", securityLevelNameSecured12Level1), subtasksAfter, everyItem(IssueMatchers.issueWithSecurityLevelName(securityLevelNameSecured12Level1)));

        //current parent issues should have no subtasks
        Issue oldParentIssueAfter = backdoor.issues().getIssue(parentNotSecured1_1.getKey());
        assertThat("Issue should have no subtasks", oldParentIssueAfter.fields.subtasks.size(), equalTo(0));

        //new parent issues should have new subtasks
        Issue newParentIssueAfter = backdoor.issues().getIssue(parentSecured1_1.getKey());
        assertThat("New parent should have 4 links", newParentIssueAfter.fields.subtasks.size(), equalTo(4));

        // test history tab
        IssueDto movedIssueBefore = subtasksToMove.iterator().next();
        Issue movedIssueAfter = backdoor.issues().getIssue(movedIssueBefore.getKey());
        HistoryModule historyTab = jira.goToViewIssue(movedIssueAfter.key).getActivitySection().historyModule();
        Iterable<HistoryModule.IssueHistoryItem> historyItems = historyTab.getHistoryItemContainers(2).stream()
            .flatMap(container -> container.getHistoryDataElements().stream())
            .collect(Collectors.toList());
        assertThat("There should be 4 properties changed", Iterables.size(historyItems), equalTo(4));
        assertThat("Project should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Project", "notsecured1 [ 10010 ]", "secured1 [ 10012 ]")));
        assertThat("Parent should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Parent", "NS1-1 [ 10000 ]", "SS1-1 [ 10012 ]")));
        assertThat("Issue key should change when moving between projects", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Key", movedIssueBefore.getKey(), movedIssueAfter.key)));
        assertThat("Security should be copied from new parent", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Security", "", "Secured12Level1 [ 10000 ]")));
    }

    @Test
    public void testSubtaskNotSecuredToSubtaskNotSecured() {
        //secured1_1 to secured2_1 in different project
        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery(String.format("parent = %s", parentNotSecured1_1.getKey())).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();
        //wait for JS to execute
        moveDetails.waitForJavaScriptToProcessParentIssuePicker();

        assertThat("Use the above project and issue type pair for all other combinations checkbox is present", moveDetails.isSameAllCheckboxPresent().now(), equalTo(true));

        //secured1_1 to secured1_1 in different project
        final String targetIssueType = "Sub-task";
        final String targetProject = PROJECT_NOT_SECURED2;
        Collection<MoveIssuesContainer> containerForSubtasks = moveDetails.getMoveIssuesContainers();
        assertThat("There should be one group of issues", containerForSubtasks.size(), equalTo(1));

        //test if error is raised when no parent is selected
        testMoveIssueContainerForSubtaskToSubtask(containerForSubtasks.iterator().next(), targetProject, targetIssueType, "");
        moveDetails = moveDetails.next(MoveDetails.class);

        testMoveIssueContainerForSubtaskToSubtask(containerForSubtasks.iterator().next(), targetProject, targetIssueType, parentNotSecured2_1.getKey());

        // retained
        MoveSetFields setFields = moveDetails.next();
        assertThat("All fields should be retained", setFields.isAllFieldsRetained(), equalTo(true));

        MoveConfirmationPage confirmationPage = setFields.next(MoveConfirmationPage.class);
        Iterable<MoveConfirmationPage.MoveIssuesConfirmContainer> moveIssuesConfirmContainerIterable = confirmationPage.getMoveIssuesConfirmContainers();
        assertThat("There should be one group of issues", Iterables.size(moveIssuesConfirmContainerIterable), equalTo(1));
        assertThat(String.format("All target projects should be %s", targetProject), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetProject", equalTo(targetProject))));
        assertThat(String.format("All target issue types should be %s", targetIssueType), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetIssueType", equalTo(targetIssueType))));
        // parent issue
        Collection<String> targetParentIssues = CollectionUtil.toList(moveIssuesConfirmContainerIterable).stream().map(c -> c.getTargetParentIssue()).collect(CollectorsUtil.toImmutableList());
        assertThat("There should be 1 parent issues", targetParentIssues, Matchers.contains(parentNotSecured2_1.getKey()));

        //updated security levels
        MoveConfirmationPage.MoveIssuesConfirmContainer micc = moveIssuesConfirmContainerIterable.iterator().next();
        assertThat("No fields should be updated", micc.getUpdateFieldsTabPresent().now(), is(false));

        confirmationPage.confirm().acknowledge();

        //check if issues have moved to subtasks
        Collection<IssueDto> subtasksToMove = subtasks.stream().filter(st -> st.getParentKey().equals(parentNotSecured1_1.getKey())).collect(CollectorsUtil.toImmutableList());
        final Collection<Issue> subtasksAfter = subtasksToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                subtasksAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                subtasksAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));

        assertThat(String.format("New parent should be set to %s", parentNotSecured2_1.getKey()),
                subtasksAfter, everyItem(IssueMatchers.issueWithParentWithKey(parentNotSecured2_1.getKey())));

        //security levels
        assertThat(String.format("Security Level for subtask should be set to %s", securityLevelNameSecured12Level1), subtasksAfter, everyItem(IssueMatchers.issueWithoutSecurityLevel()));

        //current parent issues should have no subtasks
        Issue oldParentIssueAfter = backdoor.issues().getIssue(parentNotSecured1_1.getKey());
        assertThat("Issue should have no subtasks", oldParentIssueAfter.fields.subtasks.size(), equalTo(0));

        //new parent issues should have new subtasks
        Issue newParentIssueAfter = backdoor.issues().getIssue(parentNotSecured2_1.getKey());
        assertThat("New parent should have 4 links", newParentIssueAfter.fields.subtasks.size(), equalTo(4));

        // test history tab
        IssueDto movedIssueBefore = subtasksToMove.iterator().next();
        Issue movedIssueAfter = backdoor.issues().getIssue(movedIssueBefore.getKey());
        HistoryModule historyTab = jira.goToViewIssue(movedIssueAfter.key).getActivitySection().historyModule();
        Iterable<HistoryModule.IssueHistoryItem> historyItems = historyTab.getHistoryItemContainers(2).stream()
            .flatMap(container -> container.getHistoryDataElements().stream())
            .collect(Collectors.toList());
        assertThat("There should be 3 properties changed", Iterables.size(historyItems), equalTo(3));
        assertThat("Project should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Project", "notsecured1 [ 10010 ]", "notsecured2 [ 10011 ]")));
        assertThat("Parent should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Parent", "NS1-1 [ 10000 ]", "NS2-1 [ 10006 ]")));
        assertThat("Issue key should change when moving between projects", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Key", movedIssueBefore.getKey(), movedIssueAfter.key)));
    }

    @Test
    public void testSubtaskSecuredToSubtaskNotSecured() {
        //secured1_1 to secured2_1 in different project
        AdvancedSearch searchPage = jira.visit(AdvancedSearch.class);
        searchPage = searchPage.enterQuery(String.format("parent = %s", parentSecured1_1.getKey())).submit();
        BulkEdit bulkEdit = searchPage.toolsMenu().open().bulkChange();

        ChooseOperation chooseOperation = bulkEdit.selectAllIssues().chooseOperation();
        MoveDetails moveDetails = chooseOperation.moveIssues();
        //wait for JS to execute
        moveDetails.waitForJavaScriptToProcessParentIssuePicker();

        assertThat("Use the above project and issue type pair for all other combinations checkbox is present", moveDetails.isSameAllCheckboxPresent().now(), equalTo(true));

        //secured1_1 to secured1_1 in different project
        final String targetIssueType = "Sub-task";
        final String targetProject = PROJECT_NOT_SECURED1;
        Collection<MoveIssuesContainer> containerForSubtasks = moveDetails.getMoveIssuesContainers();
        assertThat("There should be one group of issues", containerForSubtasks.size(), equalTo(1));
        testMoveIssueContainerForSubtaskToSubtask(containerForSubtasks.iterator().next(), targetProject, targetIssueType, parentNotSecured1_1.getKey());

        MoveSetFields setFields = moveDetails.next();
        checkIfSecurityLevelWarningIsPresent(setFields, "The value of this field must be changed to be valid in the target project, but you are not able to update this field in the target project. It will be set to the field's default value for the affected issues.", false, false);

        MoveConfirmationPage confirmationPage = setFields.next(MoveConfirmationPage.class);
        Iterable<MoveConfirmationPage.MoveIssuesConfirmContainer> moveIssuesConfirmContainerIterable = confirmationPage.getMoveIssuesConfirmContainers();
        assertThat("There should be one group of issues", Iterables.size(moveIssuesConfirmContainerIterable), equalTo(1));
        assertThat(String.format("All target projects should be %s", targetProject), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetProject", equalTo(targetProject))));
        assertThat(String.format("All target issue types should be %s", targetIssueType), moveIssuesConfirmContainerIterable, everyItem(Matchers.hasProperty("targetIssueType", equalTo(targetIssueType))));
        // parent issue
        Collection<String> targetParentIssues = CollectionUtil.toList(moveIssuesConfirmContainerIterable).stream().map(c -> c.getTargetParentIssue()).collect(CollectorsUtil.toImmutableList());
        assertThat("There should be 1 parent issues", targetParentIssues, Matchers.contains(parentNotSecured1_1.getKey()));

        // updated security levels
        Collection<String> targetSecurityLevels = CollectionUtil.toList(moveIssuesConfirmContainerIterable).stream().map(c -> c.getUpdateFields())
                .map(CollectionUtil::toList).flatMap(Collection::stream).filter(field -> field.getField().equals("Security Level")).map(f -> f.getValue()).
                        collect(CollectorsUtil.toImmutableList());
        assertThat("There should be 1 security level change - to none", targetSecurityLevels,
                Matchers.contains("None")
        );

        confirmationPage.confirm().acknowledge();

        //check if issues have moved to subtasks
        Collection<IssueDto> subtasksToMove = subtasks.stream().filter(st -> st.getParentKey().equals(parentSecured1_1.getKey())).collect(CollectorsUtil.toImmutableList());
        final Collection<Issue> subtasksAfter = subtasksToMove.stream().map(dto -> backdoor.issues().getIssue(dto.getKey())).collect(CollectorsUtil.toImmutableList());
        assertThat(String.format("Target issue project should be %s", targetProject),
                subtasksAfter, everyItem(IssueMatchers.issueWithProjectName(targetProject)));
        assertThat(String.format("Target issue type should be %s", targetIssueType),
                subtasksAfter, everyItem(IssueMatchers.issueWithIssueType(targetIssueType)));

        assertThat(String.format("New parent should be set to %s", parentNotSecured1_1.getKey()),
                subtasksAfter, everyItem(IssueMatchers.issueWithParentWithKey(parentNotSecured1_1.getKey())));

        assertThat("Security Level for subtask should be cleared", subtasksAfter, everyItem(IssueMatchers.issueWithoutSecurityLevel()));

        //current parent issues should have no subtasks
        Issue oldParentIssueAfter = backdoor.issues().getIssue(parentSecured1_1.getKey());
        assertThat("Issue should have no subtasks", oldParentIssueAfter.fields.subtasks.size(), equalTo(0));

        //new parent issues should have new subtasks
        Issue newParentIssueAfter = backdoor.issues().getIssue(parentNotSecured1_1.getKey());
        assertThat("New parent should have 4 links", newParentIssueAfter.fields.subtasks.size(), equalTo(4));

        // test history tab
        IssueDto movedIssueBefore = subtasksToMove.iterator().next();
        Issue movedIssueAfter = backdoor.issues().getIssue(movedIssueBefore.getKey());
        HistoryModule historyTab = jira.goToViewIssue(movedIssueAfter.key).getActivitySection().historyModule();
        Iterable<HistoryModule.IssueHistoryItem> historyItems = historyTab.getHistoryItemContainers(2).stream()
            .flatMap(container -> container.getHistoryDataElements().stream())
            .collect(Collectors.toList());
        assertThat("There should be 4 properties changed", Iterables.size(historyItems), equalTo(4));
        assertThat("Project should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Project", "secured1 [ 10012 ]", "notsecured1 [ 10010 ]")));
        assertThat("Parent should change", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Parent", "SS1-1 [ 10012 ]", "NS1-1 [ 10000 ]")));
        assertThat("Issue key should change when moving between projects", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Key", movedIssueBefore.getKey(), movedIssueAfter.key)));
        assertThat("Security should be copied from new parent", historyItems, Matchers.hasItem(IssueHistoryDataMatchers.containsHistoryRow("Security", "Secured12Level1 [ 10000 ]", "")));
    }

    public Iterable<String> transformComponents(Iterable<Component> components) {
        return Iterables.transform(components, c -> c.name);
    }

    public Iterable<String> transformVersions(Iterable<Version> versions) {
        return Iterables.transform(versions, v -> v.name);
    }

    private void checkIfSecurityLevelSelectIsPresent(final MoveSetFields setFields, final String content) {
        Iterable<MoveSetFields.FieldUpdateRow> fieldUpdateRows = setFields.getFieldUpdateRows();
        //check for text that warns that security field will be cleared
        fieldUpdateRows.forEach(row -> {
            assertThat("There should be only Security Level field", row.getField(), Matchers.containsString("Security Level"));
            assertThat("There should be select box with list of security levels", row.getContent(), Matchers.containsString(content));
        });
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

    private void testMoveIssueContainerForSubtaskToSubtask(final MoveIssuesContainer mic, final String targetProject, final String targetIssueType, final String parentIssueKey) {
        assertTrue("Project selector should be present for subtasks", mic.isProjectSelectPresent().now());
        assertTrue("Parent selector should be present for subtasks", mic.isParentSelectPresent().now());
        assertThat("Target project selected should not be present", mic.getTargetProjectSelectedEl().isPresent().now(), is(false));

        mic.selectProject(targetProject);

        //all types available in issue type selector
        Iterable<String> issueTypes = mic.getAvailableIssueTypeSuggestions();
        assertThat("There should be 5 issue type suggestions", Iterables.size(issueTypes), Matchers.equalTo(5));
        issueTypes.forEach(s -> assertThat("All issue types should be available", s, Matchers.isIn(ImmutableSet.of("New Feature", "Task", "Improvement", "Bug", "Sub-task"))));
        //select target issue type
        mic.selectIssueType(targetIssueType);

        String targetProjectKey = mapProjectNameToProjectKey(targetProject);
        //check that only homosapien parent issue suggestions are returned
        Iterable<String> parentIssueSuggestions = mic.typeParentIssueAndReturnSuggestions(targetProjectKey);
        parentIssueSuggestions.forEach(
                s -> assertThat(
                        String.format("Should only allow to select parent issues from %s project", targetProjectKey),
                        s,
                        anyOf(startsWith(targetProjectKey), equalTo(targetProjectKey))
                ));
        mic.selectParentIssue(parentIssueKey);
    }

    private void testMoveIssueContainerForSubtaskToIssue(final MoveIssuesContainer mic, final String targetProject, final String targetIssueType, final String parentIssueKey) {
        assertTrue("Project selector should be present for subtasks", mic.isProjectSelectPresent().now());
        assertTrue("Parent selector should be present for subtasks", mic.isParentSelectPresent().now());
        assertThat("Target project selected should not be present", mic.getTargetProjectSelectedEl().isPresent().now(), is(false));

        mic.selectProject(targetProject);

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

        public TestBulkMoveSubtaskToSubtask.IssueDto createIssueDto() {
            return new TestBulkMoveSubtaskToSubtask.IssueDto(id, key, projectKey, projectId, issueTypeId, assignee, parentId, parentKey, securityLevelId);
        }
    }
}
