package com.atlassian.jira.webtest.webdriver.tests.admin.customfields;

import com.atlassian.jira.functest.framework.FunctTestConstants;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.config.EnableAUIFlags;
import com.atlassian.jira.pageobjects.config.ResetData;
import com.atlassian.jira.pageobjects.elements.AuiFlag;
import com.atlassian.jira.pageobjects.elements.GlobalFlags;
import com.atlassian.jira.pageobjects.pages.admin.customfields.AddConfigurationSchemeContext;
import com.atlassian.jira.pageobjects.pages.admin.customfields.ConfigureCustomField;
import com.atlassian.jira.pageobjects.pages.admin.customfields.EditCustomFieldDetails;
import com.atlassian.jira.pageobjects.pages.admin.customfields.EditCustomFieldOptions;
import com.atlassian.jira.pageobjects.pages.admin.customfields.ViewCustomFields;
import com.atlassian.jira.webtest.webdriver.util.AUIFlags;
import com.atlassian.pageobjects.elements.query.Poller;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.atlassian.jira.JiraFeatureFlagRegistrar.CUSTOMFIELDS_BETTER_MULTI_SELECTS;
import static com.atlassian.jira.functest.framework.admin.CustomFields.numericCfId;

/**
 *
 * This test checks the "We recommend a re-index" notification shown after adding a custom field from the View Custom Fields page.
 * The notification should pop-up only if adding custom field has affected issues, regardless if it was added in the global context or only for issues in selected projects / issue types.
 * E.g. the notification should not be present if cf was added for a project that has no issues.
 *
 * @since v8.14
 */

@EnableAUIFlags
@ResetData
@WebTest({Category.WEBDRIVER_TEST, Category.ADMINISTRATION})
public class TestReindexNotification extends BaseJiraWebTest {

    private static final String LABELS_NAME = "Labels";

    private static final String PROJECT_KEY = "HSP";
    private static final String PROJECT_NAME = "homosapien";
    private static final String SECOND_PROJECT_KEY = "MKY";
    private static final String SECOND_PROJECT_NAME = "monkey";
    private static final String PROJECT_NAME_NOT_IN_CONTEXT = "Third project";
    private static final String PROJECT_KEY_NOT_IN_CONTEXT = "THI";
    private static final String ISSUE_SUMMARY = "Issue summary";
    private static final String CUSTOM_FIELD_NAME = "CF";
    private static final String CUSTOM_FIELD_DESCRIPTION = "Description";
    private static final String ADMIN_USERNAME = "admin";
    private static final String IMPROVEMENT_ISSUE_TYPE = "Improvement";
    private static final String BUG_ISSUE_TYPE = "Bug";
    private static final String GLOBAL_CONTEXT_ID = "10110";
    private static final String CUSTOM_CONTEXT_ID = "10111";
    private static final String SELECT_FIELD_TYPE = FunctTestConstants.BUILT_IN_CUSTOM_FIELD_KEY + ":" + FunctTestConstants.CUSTOM_FIELD_TYPE_SELECT;
    private static final String SEARCHER_KEY_NUMBER_RANGE = FunctTestConstants.BUILT_IN_CUSTOM_FIELD_KEY + ":" + FunctTestConstants.CUSTOM_FIELD_NUMBER_RANGE;
    private static final String SEARCHER_KEY_EXACT_NUMBER = FunctTestConstants.BUILT_IN_CUSTOM_FIELD_KEY + ":" + FunctTestConstants.CUSTOM_FIELD_EXACT_NUMBER;
    private static final String SEARCHER_KEY_MULTI_SELECT= FunctTestConstants.BUILT_IN_CUSTOM_FIELD_KEY + ":" + FunctTestConstants.CUSTOM_FIELD_MULTI_SELECT_SEARCHER;
    private static final String SEARCHER_KEY_NO_SEARCHER = "-1";

    private static final String REINDEX_MESSAGE = "We recommend that you perform a re-index";

    private final GlobalFlags flags = pageBinder.bind(GlobalFlags.class);
    private final AUIFlags auiFlags = pageBinder.bind(AUIFlags.class);

    private String customFieldId;

    @Before
    public void setUp(){
        // ViewCustomFields.isAt() relies on ViewCustomFields#customFieldsTable element.
        // When there are no custom fields, the table is absent and the page object cannot be bound.
        // Hence, we need to create a dummy field, just so that the page object works.
        this.customFieldId = createCustomField();
    }

    @Test
    public void testAddCustomFieldGlobalWithOneIssueType() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);

        ViewCustomFields viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsNotDisplayedWhenAddedCustomFieldSpecificContextWithNoIssueInContext(viewCustomFields, new HashSet<>(), Collections.singleton(IMPROVEMENT_ISSUE_TYPE));

        this.customFieldId = createCustomField();
        viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsDisplayedWhenAddedCustomFieldSpecificContextWithIssueInContext(viewCustomFields, new HashSet<>(), Collections.singleton(IMPROVEMENT_ISSUE_TYPE), SECOND_PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);
    }

    @Test
    public void testAddCustomFieldGlobalWithTwoIssueTypes() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);

        ViewCustomFields viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsNotDisplayedWhenAddedCustomFieldSpecificContextWithNoIssueInContext(viewCustomFields, new HashSet<>(), new HashSet<>(Arrays.asList(IMPROVEMENT_ISSUE_TYPE, BUG_ISSUE_TYPE)));

        this.customFieldId = createCustomField();
        viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsDisplayedWhenAddedCustomFieldSpecificContextWithIssueInContext(viewCustomFields, new HashSet<>(), new HashSet<>(Arrays.asList(IMPROVEMENT_ISSUE_TYPE, BUG_ISSUE_TYPE)), SECOND_PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);
    }

    @Test
    public void testAddCustomFieldOneProjectAndAllIssueTypes() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);

        ViewCustomFields viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsNotDisplayedWhenAddedCustomFieldSpecificContextWithNoIssueInContext(viewCustomFields, Collections.singleton(PROJECT_KEY), new HashSet<>());

        this.customFieldId = createCustomField();
        viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsDisplayedWhenAddedCustomFieldSpecificContextWithIssueInContext(viewCustomFields, Collections.singleton(PROJECT_KEY), new HashSet<>(), PROJECT_KEY, BUG_ISSUE_TYPE);
    }

    @Test
    public void testAddCustomFieldTwoProjectAndAllIssueTypes() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);

        ViewCustomFields viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsNotDisplayedWhenAddedCustomFieldSpecificContextWithNoIssueInContext(viewCustomFields, new HashSet<>(Arrays.asList(PROJECT_KEY, SECOND_PROJECT_KEY)), new HashSet<>());

        this.customFieldId = createCustomField();
        viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsDisplayedWhenAddedCustomFieldSpecificContextWithIssueInContext(viewCustomFields, new HashSet<>(Arrays.asList(PROJECT_KEY, SECOND_PROJECT_KEY)), new HashSet<>(), PROJECT_KEY, BUG_ISSUE_TYPE);
    }

    @Test
    public void testAddCustomFieldOneProjectAndOneIssueType() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);

        ViewCustomFields viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsNotDisplayedWhenAddedCustomFieldSpecificContextWithNoIssueInContext(viewCustomFields, Collections.singleton(PROJECT_KEY), Collections.singleton(IMPROVEMENT_ISSUE_TYPE));

        this.customFieldId = createCustomField();
        viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsDisplayedWhenAddedCustomFieldSpecificContextWithIssueInContext(viewCustomFields, Collections.singleton(PROJECT_KEY), Collections.singleton(IMPROVEMENT_ISSUE_TYPE), PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);
    }

    @Test
    public void testAddCustomFieldTwoProjectsAndTwoIssueTypes() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);

        ViewCustomFields viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsNotDisplayedWhenAddedCustomFieldSpecificContextWithNoIssueInContext(viewCustomFields, new HashSet<>(Arrays.asList(PROJECT_KEY, SECOND_PROJECT_KEY)), new HashSet<>(Arrays.asList(IMPROVEMENT_ISSUE_TYPE, BUG_ISSUE_TYPE)));

        this.customFieldId = createCustomField();
        viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        assertFlagIsDisplayedWhenAddedCustomFieldSpecificContextWithIssueInContext(viewCustomFields, new HashSet<>(Arrays.asList(PROJECT_KEY, SECOND_PROJECT_KEY)), new HashSet<>(Arrays.asList(IMPROVEMENT_ISSUE_TYPE, BUG_ISSUE_TYPE)), PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);
    }

    @Test
    public void testAddCustomFieldConfigurationContextWithGlobalAlreadyPresent() {
        createIssue(SECOND_PROJECT_KEY, BUG_ISSUE_TYPE);

        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(SECOND_PROJECT_NAME), new HashSet<>());

        assertNotificationIsNotShown();
    }

    @Test
    public void testAddCustomFieldConfigurationContextWithProjectContextAlreadyPresentAndIssueNotInNewContext() {
        final ViewCustomFields viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);

        addCustomFieldWithProjectsAndIssueTypesContext(viewCustomFields, Collections.singleton(SECOND_PROJECT_KEY), new HashSet<>());
        backdoor.customFields().deleteCustomField(this.customFieldId);

        createIssue(SECOND_PROJECT_KEY, BUG_ISSUE_TYPE);

        final String newCustomFieldId = backdoor.customFields().getCustomFields().get(0).id;

        addConfigurationSchemeContext(newCustomFieldId, Collections.singleton(PROJECT_NAME), new HashSet<>());

        assertNotificationIsNotShown();
    }

    @Test
    public void testAddCustomFieldConfigurationContextWithProjectContextAlreadyPresentAndIssueInNewContext() {
        final ViewCustomFields viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);

        addCustomFieldWithProjectsAndIssueTypesContext(viewCustomFields, Collections.singleton(SECOND_PROJECT_KEY), new HashSet<>());
        backdoor.customFields().deleteCustomField(this.customFieldId);

        createIssue(PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);

        final String newCustomFieldId = backdoor.customFields().getCustomFields().get(0).id;

        addConfigurationSchemeContext(newCustomFieldId, Collections.singleton(PROJECT_NAME), new HashSet<>());

        assertNotificationIsShown();
    }

    @Test
    public void testAddCustomFieldConfigurationContextWithGlobalContextAndIssueInNewContext() {
        final ViewCustomFields viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);

        addCustomFieldWithProjectsAndIssueTypesContext(viewCustomFields, Collections.singleton(SECOND_PROJECT_KEY), new HashSet<>());
        backdoor.customFields().deleteCustomField(this.customFieldId);

        createIssue(SECOND_PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);

        final String newCustomFieldId = backdoor.customFields().getCustomFields().get(0).id;

        addConfigurationSchemeContext(newCustomFieldId, new HashSet<>(), new HashSet<>());

        assertNotificationIsShown();
    }

    @Test
    public void testRemoveCustomFieldConfigurationWithGlobalContextStillInPlace() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);

        final ViewCustomFields viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);

        addCustomFieldWithProjectsAndIssueTypesContext(viewCustomFields, Collections.singleton(SECOND_PROJECT_KEY), Collections.singleton(BUG_ISSUE_TYPE));
        backdoor.customFields().deleteCustomField(this.customFieldId);
        final String newCustomFieldId = backdoor.customFields().getCustomFields().get(0).id;
        addConfigurationSchemeContext(newCustomFieldId, new HashSet<>(), new HashSet<>());
        createIssue(SECOND_PROJECT_KEY, BUG_ISSUE_TYPE);

        backdoor.indexing().reindexAll();

        final ConfigureCustomField configureCustomFields = pageBinder.navigateToAndBind(ConfigureCustomField.class, newCustomFieldId, Long.parseLong(numericCfId(newCustomFieldId)));
        configureCustomFields.deleteCustomFieldContext(CUSTOM_CONTEXT_ID);

        assertNotificationIsNotShown();
    }

    @Test
    public void testRemoveCustomFieldConfigurationWithNoGlobalContextStillInPlaceAndIssueInContext() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);
        ConfigureCustomField configureCustomFields = pageBinder.navigateToAndBind(ConfigureCustomField.class, this.customFieldId, Long.parseLong(numericCfId(this.customFieldId)));
        configureCustomFields.deleteCustomFieldContext(GLOBAL_CONTEXT_ID);
        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(SECOND_PROJECT_NAME), Collections.singleton(BUG_ISSUE_TYPE));
        createIssue(SECOND_PROJECT_KEY, BUG_ISSUE_TYPE);

        backdoor.indexing().reindexAll();

        configureCustomFields = pageBinder.navigateToAndBind(ConfigureCustomField.class, this.customFieldId, Long.parseLong(numericCfId(this.customFieldId)));
        configureCustomFields.deleteCustomFieldContext(CUSTOM_CONTEXT_ID);

        assertNotificationIsShown();
    }

    @Test
    public void testRemoveCustomFieldConfigurationWithNoGlobalContextStillInPlaceAndNoIssueInContext() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);
        ConfigureCustomField configureCustomFields = pageBinder.navigateToAndBind(ConfigureCustomField.class, this.customFieldId, Long.parseLong(numericCfId(this.customFieldId)));
        configureCustomFields.deleteCustomFieldContext(GLOBAL_CONTEXT_ID);
        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(SECOND_PROJECT_NAME), Collections.singleton(BUG_ISSUE_TYPE));

        backdoor.indexing().reindexAll();

        configureCustomFields = pageBinder.navigateToAndBind(ConfigureCustomField.class, this.customFieldId, Long.parseLong(numericCfId(this.customFieldId)));
        configureCustomFields.deleteCustomFieldContext(CUSTOM_CONTEXT_ID);

        assertNotificationIsNotShown();
    }

    @Test
    public void testRemoveCustomFieldConfigurationWithGlobalContextAndNoIssueInContext() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);
        final ConfigureCustomField configureCustomFields = pageBinder.navigateToAndBind(ConfigureCustomField.class, this.customFieldId, Long.parseLong(numericCfId(this.customFieldId)));
        configureCustomFields.deleteCustomFieldContext(GLOBAL_CONTEXT_ID);

        assertNotificationIsNotShown();
    }

    @Test
    public void testRemoveCustomFieldConfigurationWithGlobalContextAndIssueInContext() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);
        createIssue(SECOND_PROJECT_KEY, BUG_ISSUE_TYPE);

        final ConfigureCustomField configureCustomFields = pageBinder.navigateToAndBind(ConfigureCustomField.class, this.customFieldId, Long.parseLong(numericCfId(this.customFieldId)));
        configureCustomFields.deleteCustomFieldContext(GLOBAL_CONTEXT_ID);

        assertNotificationIsShown();
    }

    @Test
    public void testRemoveCustomFieldConfigurationWithGlobalContextAndCustomContextsInPlaceAndNoIssueInContext() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);
        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(SECOND_PROJECT_NAME), Collections.singleton(BUG_ISSUE_TYPE));
        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(PROJECT_NAME), Collections.singleton(IMPROVEMENT_ISSUE_TYPE));

        final ConfigureCustomField configureCustomFields = pageBinder.navigateToAndBind(ConfigureCustomField.class, this.customFieldId, Long.parseLong(numericCfId(this.customFieldId)));
        configureCustomFields.deleteCustomFieldContext(GLOBAL_CONTEXT_ID);

        assertNotificationIsNotShown();
    }

    @Test
    public void testRemoveCustomFieldConfigurationWithGlobalContextAndCustomContextsInPlaceAndIssueInCustomContext() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);
        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(SECOND_PROJECT_NAME), Collections.singleton(BUG_ISSUE_TYPE));
        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(PROJECT_NAME), Collections.singleton(IMPROVEMENT_ISSUE_TYPE));

        createIssue(SECOND_PROJECT_KEY, BUG_ISSUE_TYPE);

        final ConfigureCustomField configureCustomFields = pageBinder.navigateToAndBind(ConfigureCustomField.class, this.customFieldId, Long.parseLong(numericCfId(this.customFieldId)));
        configureCustomFields.deleteCustomFieldContext(GLOBAL_CONTEXT_ID);

        assertNotificationIsNotShown();
    }

    @Test
    public void testRemoveCustomFieldConfigurationWithGlobalContextAndCustomContextsInPlaceAndIssuesInTwoDifferentCustomContext() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);
        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(SECOND_PROJECT_NAME), Collections.singleton(BUG_ISSUE_TYPE));
        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(PROJECT_NAME), Collections.singleton(IMPROVEMENT_ISSUE_TYPE));

        createIssue(SECOND_PROJECT_KEY, BUG_ISSUE_TYPE);
        createIssue(PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);

        final ConfigureCustomField configureCustomFields = pageBinder.navigateToAndBind(ConfigureCustomField.class, this.customFieldId, Long.parseLong(numericCfId(this.customFieldId)));
        configureCustomFields.deleteCustomFieldContext(GLOBAL_CONTEXT_ID);

        assertNotificationIsNotShown();
    }

    @Test
    public void testRemoveCustomFieldConfigurationWithGlobalContextAndCustomContextsInPlaceAndIssuesInTwoDifferentCustomContextAndOneIssueOutsideOfCustomContext() {
        backdoor.project().addProject(PROJECT_NAME_NOT_IN_CONTEXT, PROJECT_KEY_NOT_IN_CONTEXT, ADMIN_USERNAME);
        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(SECOND_PROJECT_NAME), Collections.singleton(BUG_ISSUE_TYPE));
        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(PROJECT_NAME), Collections.singleton(IMPROVEMENT_ISSUE_TYPE));

        createIssue(SECOND_PROJECT_KEY, BUG_ISSUE_TYPE);
        createIssue(PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);
        createIssue(PROJECT_KEY, BUG_ISSUE_TYPE);

        final ConfigureCustomField configureCustomFields = pageBinder.navigateToAndBind(ConfigureCustomField.class, this.customFieldId, Long.parseLong(numericCfId(this.customFieldId)));
        configureCustomFields.deleteCustomFieldContext(GLOBAL_CONTEXT_ID);

        assertNotificationIsShown();
    }

    @Test
    public void testEditCustomFieldConfigurationWithGlobalStillInPlace() {
        addConfigurationSchemeContext(this.customFieldId, Collections.singleton(SECOND_PROJECT_NAME), Collections.singleton(BUG_ISSUE_TYPE));
        createIssue(SECOND_PROJECT_KEY, BUG_ISSUE_TYPE);

        editConfigurationSchemeContext(this.customFieldId, CUSTOM_CONTEXT_ID, new HashSet<>(), Collections.singleton(IMPROVEMENT_ISSUE_TYPE));

        assertNotificationIsNotShown();
    }

    @Test
    public void testSetIssueTypeInGlobalContextWithoutExistingIssue() {
        editConfigurationSchemeContext(this.customFieldId, GLOBAL_CONTEXT_ID, new HashSet<>(), Collections.singleton(BUG_ISSUE_TYPE));

        assertNotificationIsNotShown();
    }

    @Test
    public void testSetIssueTypeInGlobalContextWithExistingIssue() {
        createIssue(SECOND_PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);
        editConfigurationSchemeContext(this.customFieldId, GLOBAL_CONTEXT_ID, new HashSet<>(), Collections.singleton(BUG_ISSUE_TYPE));

        assertNotificationIsShown();
    }

    @Test
    public void testConvertCustomContextIntoGlobalContextWithExistingIssue() {
        editConfigurationSchemeContext(this.customFieldId, GLOBAL_CONTEXT_ID, new HashSet<>(), Collections.singleton(BUG_ISSUE_TYPE));
        createIssue(SECOND_PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);
        applyConfigurationSchemeContextToAllIssueTypes(this.customFieldId);

        assertNotificationIsShown();
    }

    @Test
    public void testChangeIssueTypeWithoutExistingIssue() {
        editConfigurationSchemeContext(this.customFieldId, GLOBAL_CONTEXT_ID, new HashSet<>(), Collections.singleton(BUG_ISSUE_TYPE));
        editConfigurationSchemeContext(this.customFieldId, GLOBAL_CONTEXT_ID, new HashSet<>(), Collections.singleton(IMPROVEMENT_ISSUE_TYPE));

        assertNotificationIsNotShown();
    }

    @Test
    public void testChangeIssueTypeWithIssueCreatedInContextBeforeChangeWasApplied() {
        editConfigurationSchemeContext(this.customFieldId, GLOBAL_CONTEXT_ID, new HashSet<>(), Collections.singleton(IMPROVEMENT_ISSUE_TYPE));
        createIssue(SECOND_PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);
        editConfigurationSchemeContext(this.customFieldId, GLOBAL_CONTEXT_ID, new HashSet<>(), Collections.singleton(BUG_ISSUE_TYPE));

        assertNotificationIsShown();
    }

    @Test
    public void testChangeIssueTypeWithIssueCreatedInContextAfterChangeWasApplied() {
        editConfigurationSchemeContext(this.customFieldId, GLOBAL_CONTEXT_ID, new HashSet<>(), Collections.singleton(IMPROVEMENT_ISSUE_TYPE));
        editConfigurationSchemeContext(this.customFieldId, GLOBAL_CONTEXT_ID, new HashSet<>(), Collections.singleton(BUG_ISSUE_TYPE));
        createIssue(SECOND_PROJECT_KEY, IMPROVEMENT_ISSUE_TYPE);
        editConfigurationSchemeContext(this.customFieldId, GLOBAL_CONTEXT_ID, new HashSet<>(), Collections.singleton(IMPROVEMENT_ISSUE_TYPE));

        assertNotificationIsShown();
    }

    @Test
    public void testShouldShowFlagWhenSearcherTypeHasBeenChanged() {
        final String numericCustomFieldId = numericCfId(this.customFieldId);
        setCustomFieldSearcher(numericCustomFieldId, SEARCHER_KEY_NUMBER_RANGE);

        assertNotificationIsShown();
    }

    @Test
    public void testShouldNotShowFlagWhenSearcherTypeHasBeenChangedToNone() {
        final String numericCustomFieldId = numericCfId(this.customFieldId);
        setCustomFieldSearcher(numericCustomFieldId, SEARCHER_KEY_NO_SEARCHER);

        assertNotificationIsNotShown();
    }

    @Test
    public void testShouldShowFlagWhenSearcherTypeHasBeenChangedFromNoneToSpecificOne() {
        final String numericCustomFieldId = numericCfId(this.customFieldId);
        setCustomFieldSearcher(numericCustomFieldId, SEARCHER_KEY_NO_SEARCHER);
        backdoor.indexing().reindexAll();
        setCustomFieldSearcher(numericCustomFieldId, SEARCHER_KEY_EXACT_NUMBER);

        assertNotificationIsShown();
    }

    @Test
    public void testShouldNotShowFlagWhenSearcherTypeHasNotBeenChanged() {
        setCustomFieldSearcher(numericCfId(this.customFieldId), SEARCHER_KEY_EXACT_NUMBER);

        assertNotificationIsNotShown();
    }

    @Test
    public void testShouldShowFlagWhenCustomFieldOptionHasBeenRemoved() {
        createSelectCustomField();

        for (final String option : new String[] {"bob", "joe", "jill"}) {
            backdoor.customFields().addOption("customfield_10001", option);
        }

        backdoor.indexing().reindexAll();

        for (final String optionId : new String[] {"10000", "10001"}) {
            final EditCustomFieldOptions editCustomFieldOptions = pageBinder.navigateToAndBind(EditCustomFieldOptions.class, CUSTOM_CONTEXT_ID);
            editCustomFieldOptions.removeOption(optionId);

            assertNotificationIsShown();

            backdoor.indexing().reindexAll();
        }
    }

    @Test
    public void testFlagIsDisplayedProperlyWhenUserHasPermissionsToCreateIssueWithinProjectButHasNotPermissionsToSeeTheProject() {
        // Admin can't see project monkey but can create issues. This test is checking whether the search to find issues
        // in context when adding the custom field overrides security correctly or not.
        backdoor.restoreDataFromResource("TestReindexMessagesIssueCountOverridesSecurity.xml");
        backdoor.flags().enableFlags();
        createCustomField();
        ViewCustomFields viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        auiFlags.closeAllFlags();
        addCustomFieldWithIssueTypesAndAllProjectsContext(viewCustomFields, new HashSet<>());

        assertNotificationIsNotShown();

        createIssueWithoutAssignee(SECOND_PROJECT_KEY);
        final String newCustomFieldId = backdoor.customFields().getCustomFields().get(1).id;
        backdoor.customFields().deleteCustomField(newCustomFieldId);
        backdoor.indexing().reindexAll();

        viewCustomFields = pageBinder.navigateToAndBind(ViewCustomFields.class);
        addCustomFieldWithIssueTypesAndAllProjectsContext(viewCustomFields, new HashSet<>());

        assertNotificationIsShown();
    }

    private void assertNotificationIsShown(){
        Poller.waitUntilTrue(flags.flagContainerPresent());
        final AuiFlag reindexFlag = flags.getFlagWithText(REINDEX_MESSAGE);
        Assert.assertNotNull(reindexFlag);
        reindexFlag.dismiss();
        Assert.assertTrue(flags.doesNotContainFlagWithText(REINDEX_MESSAGE));
    }

    private void assertNotificationIsNotShown(){
        Assert.assertTrue(!flags.isPresent() || flags.doesNotContainFlagWithText(REINDEX_MESSAGE));
    }

    private void addCustomFieldWithProjectsAndIssueTypesContext(final ViewCustomFields viewCustomFields, final Set<String> projectKeys, final Set<String> issueTypeNames){
        viewCustomFields.addCustomField()
                .select(LABELS_NAME)
                .next()
                .name(CUSTOM_FIELD_NAME)
                .description(CUSTOM_FIELD_DESCRIPTION)
                .next()
                .selectMultipleProjectsByKey(projectKeys)
                .selectMultipleIssueTypesByName(issueTypeNames)
                .nextAndThenAssociate();
    }

    private void addCustomFieldWithProjectsAndAllIssueTypesContext(final ViewCustomFields viewCustomFields, final Set<String> projectKeys){
        viewCustomFields.addCustomField()
                .select(LABELS_NAME)
                .next()
                .name(CUSTOM_FIELD_NAME)
                .description(CUSTOM_FIELD_DESCRIPTION)
                .next()
                .selectMultipleProjectsByKey(projectKeys)
                .nextAndThenAssociate();
    }

    private void addCustomFieldWithIssueTypesAndAllProjectsContext(final ViewCustomFields viewCustomFields, final Set<String> issueTypeNames){
        viewCustomFields.addCustomField()
                .select(LABELS_NAME)
                .next()
                .name(CUSTOM_FIELD_NAME)
                .description(CUSTOM_FIELD_DESCRIPTION)
                .next()
                .setGlobalContext()
                .selectMultipleIssueTypesByName(issueTypeNames)
                .nextAndThenAssociate();
    }

    private void addConfigurationSchemeContext(final String customFieldId, final Set<String> projects, final Set<String> issuetypes) {
        final AddConfigurationSchemeContext configureCustomFields = pageBinder.navigateToAndBind(AddConfigurationSchemeContext.class, customFieldId, this.shouldUseLegacyUi());

        configureCustomFields
                .setName("Test")
                .setProjectsByName(projects)
                .setIssueTypesByName(issuetypes)
                .addConfigurationSchemeContext();
    }

    private void editConfigurationSchemeContext(final String customFieldId, final String fieldConfigSchemeId, final Set<String> projects, final Set<String> issueTypes) {
        final AddConfigurationSchemeContext configureCustomFields = pageBinder.navigateToAndBind(AddConfigurationSchemeContext.class, customFieldId, fieldConfigSchemeId, this.shouldUseLegacyUi());

        configureCustomFields
                .setName("Test")
                .setProjectsByName(projects)
                .setIssueTypesByName(issueTypes)
                .addConfigurationSchemeContext();
    }

    private void applyConfigurationSchemeContextToAllIssueTypes(final String customFieldId) {
        final AddConfigurationSchemeContext configureCustomFields = pageBinder.navigateToAndBind(AddConfigurationSchemeContext.class, customFieldId, GLOBAL_CONTEXT_ID, this.shouldUseLegacyUi());

        configureCustomFields
                .setAllIssueTypes()
                .addConfigurationSchemeContext();
    }

    private void createIssueNotInContext(){
        backdoor.getTestkit().issues().createIssue(PROJECT_KEY_NOT_IN_CONTEXT, ISSUE_SUMMARY, ADMIN_USERNAME, FunctTestConstants.PRIORITY_MAJOR, FunctTestConstants.ISSUE_TYPE_TASK);
    }
    private void createIssue(final String project, final String issueType) {
        backdoor.getTestkit().issues().createIssue(project, ISSUE_SUMMARY, ADMIN_USERNAME, FunctTestConstants.PRIORITY_MAJOR, issueType);
    }

    private void createIssueWithoutAssignee(final String project) {
        backdoor.getTestkit().issues().createIssue(project, ISSUE_SUMMARY, null, null, FunctTestConstants.PRIORITY_MAJOR, BUG_ISSUE_TYPE);
    }

    private String createCustomField() {
        return backdoor.customFields().createCustomField(
                CUSTOM_FIELD_NAME,
                CUSTOM_FIELD_DESCRIPTION,
                FunctTestConstants.BUILT_IN_CUSTOM_FIELD_KEY + ":" + FunctTestConstants.CUSTOM_FIELD_TYPE_FLOAT,
                SEARCHER_KEY_EXACT_NUMBER);
    }

    private void createSelectCustomField(){
        backdoor.customFields().createCustomField(
                CUSTOM_FIELD_NAME,
                CUSTOM_FIELD_DESCRIPTION,
                SELECT_FIELD_TYPE,
                SEARCHER_KEY_MULTI_SELECT);
    }

    private void addCustomField(final ViewCustomFields viewCustomFields, final Set<String> projects, final Set<String> issueTypes) {
        if (projects.isEmpty()) {
            addCustomFieldWithIssueTypesAndAllProjectsContext(viewCustomFields, issueTypes);
        } else if (issueTypes.isEmpty()) {
            addCustomFieldWithProjectsAndAllIssueTypesContext(viewCustomFields, projects);
        } else {
            addCustomFieldWithProjectsAndIssueTypesContext(viewCustomFields, projects, issueTypes);
        }
    }

    private void setCustomFieldSearcher(final String customFieldId, final String searcherKey) {
        pageBinder.navigateToAndBind(EditCustomFieldDetails.class, customFieldId)
                .setSearcher(searcherKey)
                .submit();
    }

    private void assertFlagIsNotDisplayedWhenAddedCustomFieldSpecificContextWithNoIssueInContext(final ViewCustomFields viewCustomFields, final Set<String> projectKeys, final Set<String> issueTypeNames) {
        createIssueNotInContext();
        clearEnvironmentPreTest();
        addCustomField(viewCustomFields, projectKeys, issueTypeNames);

        assertNotificationIsNotShown();
    }

    private void assertFlagIsDisplayedWhenAddedCustomFieldSpecificContextWithIssueInContext(final ViewCustomFields viewCustomFields, final Set<String> projectKeys, final Set<String> issueTypeNames, final String projectKeyForIssue, final String issueTypeNameForIssue) {
        createIssue(projectKeyForIssue, issueTypeNameForIssue);
        clearEnvironmentPreTest();
        addCustomField(viewCustomFields, projectKeys, issueTypeNames);

        assertNotificationIsShown();
    }

    private void clearEnvironmentPreTest() {
        backdoor.customFields().deleteCustomField(this.customFieldId);
        auiFlags.closeAllFlags();
    }

    private boolean shouldUseLegacyUi() {
        return !backdoor.darkFeatures().isGlobalEnabled(CUSTOMFIELDS_BETTER_MULTI_SELECTS.featureKey());
    }
}
