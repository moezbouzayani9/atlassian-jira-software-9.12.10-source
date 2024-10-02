package com.atlassian.jira.webtest.webdriver.tests.plugin.auditing.events;

import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.webtest.webdriver.tests.plugin.auditing.models.Restmodels;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.List;

import static com.atlassian.jira.webtest.webdriver.tests.plugin.auditing.Unique.unique;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

@WebTest({com.atlassian.jira.functest.framework.suite.Category.WEBDRIVER_TEST})
public class TestPermissionEvents extends TestAbstractSharedEntityEvents {

    private static final String ADMIN_USER_KEY = "admin";

    public TestPermissionEvents() throws URISyntaxException {
    }

    @Test
    public void testPermissionSchemeCopy() {
        Long id = backdoor.permissionSchemes().copyDefaultScheme("New copy");
        try {
            backdoor.permissionSchemes().removeGroupPermission(id, ProjectPermissions.ADMINISTER_PROJECTS, "jira-administrators");
            backdoor.permissionSchemes().addGroupPermission(id, ProjectPermissions.ADMINISTER_PROJECTS, "jira-administrators");
            backdoor.permissionSchemes().addUserPermission(id, ProjectPermissions.CREATE_ISSUES, "admin");
        } finally {
            backdoor.permissionSchemes().deleteScheme(id);
        }

        final List<Restmodels.RecordResponse> auditEventsSchemeUpdated = waitUntilEventsAreFoundInLog("Permission scheme updated", 4);

        Restmodels.RecordResponse schemeUpdatedAuditEvent = auditEventsSchemeUpdated.get(2);
        assertThat(schemeUpdatedAuditEvent.objectItem.objectName, startsWith("New copy"));
        assertThat(schemeUpdatedAuditEvent.category, equalTo("permissions"));
        assertThat(schemeUpdatedAuditEvent.summary, equalTo("Permission scheme updated"));
        assertThat(schemeUpdatedAuditEvent.changedValues, hasSize(3));
        assertThat(schemeUpdatedAuditEvent.changedValues, hasItem(new Restmodels.ChangedValueResponse("Permission", "Administer Projects", "")));
        assertThat(schemeUpdatedAuditEvent.changedValues, hasItem(new Restmodels.ChangedValueResponse("Type", "Group", "")));
        assertThat(schemeUpdatedAuditEvent.changedValues, hasItem(new Restmodels.ChangedValueResponse("Value", "jira-administrators", "")));

        final Restmodels.RecordResponse schemeCopiedAuditEvent = waitUntilEventIsFoundInLog("Permission scheme copied");
        assertThat(schemeCopiedAuditEvent.objectItem.objectName, startsWith("Copy of Default Permission Scheme"));
        assertThat(schemeCopiedAuditEvent.category, equalTo("permissions"));
        assertThat(schemeCopiedAuditEvent.summary, equalTo("Permission scheme copied"));
        assertThat(schemeCopiedAuditEvent.objectItem.objectType, equalTo("SCHEME"));
        assertThat(schemeCopiedAuditEvent.author, notNullValue());
        assertThat(schemeCopiedAuditEvent.author.username, equalTo(ADMIN_USER_KEY));
        assertThat(schemeCopiedAuditEvent.changedValues, hasSize(2));
        assertThat(schemeCopiedAuditEvent.changedValues.get(1).name, equalTo("Name"));
        assertThat(schemeCopiedAuditEvent.changedValues.get(1).to, startsWith("Copy of Default Permission Scheme"));
        assertThat(schemeCopiedAuditEvent.associatedItems, hasSize(1));
        assertThat(schemeCopiedAuditEvent.associatedItems.get(0).objectName, equalTo("Default Permission Scheme"));
        assertThat(schemeCopiedAuditEvent.associatedItems.get(0).objectType, equalTo("SCHEME"));
    }

    @Test
    public void testPermissionsSchemeChanges() {
        Long id = backdoor.permissionSchemes().createScheme(unique("Testing scheme"), "this is a description");
        try {
            backdoor.permissionSchemes().addGroupPermission(id, ProjectPermissions.ADMINISTER_PROJECTS, "jira-administrators");
            backdoor.permissionSchemes().addUserPermission(id, ProjectPermissions.CREATE_ISSUES, "admin");
            backdoor.permissionSchemes().removeGroupPermission(id, ProjectPermissions.ADMINISTER_PROJECTS, "jira-administrators");

            // we expect three events when the test is run without Software! If Software is installed then there's six events
            final List<Restmodels.RecordResponse> auditEventsSchemeUpdated = waitUntilEventsAreFoundInLog("Permission scheme updated", 3);

            Restmodels.RecordResponse record = auditEventsSchemeUpdated.get(0);
            assertThat(record.objectItem.objectName, startsWith("Testing scheme"));
            assertThat(record.category, equalTo("permissions"));
            assertThat(record.summary, equalTo("Permission scheme updated"));
            assertThat(record.changedValues, hasSize(3));
            assertThat(record.changedValues, hasItem(new Restmodels.ChangedValueResponse("Permission", "Administer Projects", "")));
            assertThat(record.changedValues, hasItem(new Restmodels.ChangedValueResponse("Type", "Group", "")));
            assertThat(record.changedValues, hasItem(new Restmodels.ChangedValueResponse("Value", "jira-administrators", "")));

            record = auditEventsSchemeUpdated.get(1);
            assertThat(record.objectItem.objectName, startsWith("Testing scheme"));
            assertThat(record.category, equalTo("permissions"));
            assertThat(record.summary, equalTo("Permission scheme updated"));
            assertThat(record.changedValues, hasSize(3));
            assertThat(record.changedValues.get(0).to, equalTo("Create Issues"));
            assertThat(record.changedValues.get(1).to, equalToIgnoringCase("Single User"));
            assertThat(record.changedValues.get(2).to, equalTo("Administrator"));

            record = auditEventsSchemeUpdated.get(2);
            assertThat(record.objectItem.objectName, startsWith("Testing scheme"));
            assertThat(record.category, equalTo("permissions"));
            assertThat(record.summary, equalTo("Permission scheme updated"));
            assertThat(record.changedValues, hasSize(3));
            assertThat(record.changedValues, hasItem(new Restmodels.ChangedValueResponse("Permission", "", "Administer Projects")));
            assertThat(record.changedValues, hasItem(new Restmodels.ChangedValueResponse("Type", "", "Group")));
            assertThat(record.changedValues, hasItem(new Restmodels.ChangedValueResponse("Value", "", "jira-administrators")));

            final Restmodels.RecordResponse auditEventSchemeCreated = waitUntilEventsAreFoundInLog("Permission scheme created", 1).get(0);

            assertThat(auditEventSchemeCreated.objectItem.objectName, startsWith("Testing scheme"));
            assertThat(auditEventSchemeCreated.category, equalTo("permissions"));
            assertThat(auditEventSchemeCreated.summary, equalTo("Permission scheme created"));

        } finally {
            backdoor.permissionSchemes().deleteScheme(id);
        }
    }

    @Test
    public void testPermissionSchemeEvents() {
        Long id = backdoor.permissionSchemes().createScheme("Testing scheme " + DateTime.now().toInstant().getMillis(), "this is a description");
        try {
            final Restmodels.RecordResponse auditEventSchemeCreated = waitUntilEventsAreFoundInLog("Permission scheme created", 1).get(0);

            assertThat(auditEventSchemeCreated.objectItem.objectName, startsWith("Testing scheme"));
            assertThat(auditEventSchemeCreated.category, equalTo("permissions"));
            assertThat(auditEventSchemeCreated.summary, equalTo("Permission scheme created"));
            assertThat(auditEventSchemeCreated.objectItem.objectType, equalTo("SCHEME"));
            assertThat(auditEventSchemeCreated.author, notNullValue());
            assertThat(auditEventSchemeCreated.author.username, equalTo(ADMIN_USER_KEY));
            assertThat(auditEventSchemeCreated.changedValues, hasSize(2));
            assertThat(auditEventSchemeCreated.changedValues.get(0).name, equalTo("Description"));
            assertThat(auditEventSchemeCreated.changedValues.get(0).to, startsWith("this is a description"));
            assertThat(auditEventSchemeCreated.changedValues.get(1).name, equalTo("Name"));
            assertThat(auditEventSchemeCreated.changedValues.get(1).to, startsWith("Testing scheme"));
        } finally {
            testkit.auditing().clearAllRecords();
            backdoor.permissionSchemes().deleteScheme(id);
        }

        final Restmodels.RecordResponse auditEventSchemeDeleted = waitUntilEventsAreFoundInLog("Permission scheme deleted", 1).get(0);

        assertThat(auditEventSchemeDeleted.objectItem.objectName, startsWith("Testing scheme"));
        assertThat(auditEventSchemeDeleted.category, equalTo("permissions"));
        assertThat(auditEventSchemeDeleted.summary, equalTo("Permission scheme deleted"));
        assertThat(auditEventSchemeDeleted.objectItem.objectType, equalTo("SCHEME"));
        assertThat(auditEventSchemeDeleted.author, notNullValue());
        assertThat(auditEventSchemeDeleted.author.username, equalTo(ADMIN_USER_KEY));
    }

    @Test
    public void testGlobalPermissionsEvents() {
        backdoor.usersAndGroups().addGroup("new-group");
        backdoor.permissions().addAnyoneGlobalPermission(Permissions.BULK_CHANGE);
        backdoor.permissions().removeAnyoneGlobalPermission(Permissions.BULK_CHANGE);
        backdoor.permissions().addGlobalPermission(Permissions.ADMINISTER, "new-group");
        backdoor.usersAndGroups().deleteGroup("new-group");

        final Restmodels.RecordResponse auditEventGroupDeleted = waitUntilEventsAreFoundInLog("Group deleted", 1).get(0);

        assertThat(auditEventGroupDeleted.objectItem.objectName, equalTo("new-group"));
        assertThat(auditEventGroupDeleted.category, equalTo("group management"));
        assertThat(auditEventGroupDeleted.summary, equalTo("Group deleted"));

        final List<Restmodels.RecordResponse> auditEventsPermissionAdded = waitUntilEventsAreFoundInLog("Global permission added", 2);
        final Restmodels.RecordResponse firstAuditEventPermissionAdded = auditEventsPermissionAdded.get(0);
        assertThat(firstAuditEventPermissionAdded.objectItem.objectName, equalTo("Global Permissions"));
        assertThat(firstAuditEventPermissionAdded.category, equalTo("permissions"));
        assertThat(firstAuditEventPermissionAdded.summary, equalTo("Global permission added"));
        assertThat(firstAuditEventPermissionAdded.changedValues, hasSize(2));
        assertThat(firstAuditEventPermissionAdded.changedValues, hasItem(new Restmodels.ChangedValueResponse("Group", "", "new-group")));
        assertThat(firstAuditEventPermissionAdded.changedValues, hasItem(new Restmodels.ChangedValueResponse("Permission", "", "Jira Administrators")));

        final Restmodels.RecordResponse auditEventPermissionDeleted = waitUntilEventsAreFoundInLog("Global permission deleted", 1).get(0);

        assertThat(auditEventPermissionDeleted.objectItem.objectName, equalTo("Global Permissions"));
        assertThat(auditEventPermissionDeleted.category, equalTo("permissions"));
        assertThat(auditEventPermissionDeleted.summary, equalTo("Global permission deleted"));
        assertThat(auditEventPermissionDeleted.changedValues, hasSize(2));
        assertThat(auditEventPermissionDeleted.changedValues, hasItem(new Restmodels.ChangedValueResponse("Group", "Anyone on the web", "")));
        assertThat(auditEventPermissionDeleted.changedValues, hasItem(new Restmodels.ChangedValueResponse("Permission", "Bulk Change", "")));

        final Restmodels.RecordResponse secondAuditEventPermissionAdded = auditEventsPermissionAdded.get(1);

        assertThat(secondAuditEventPermissionAdded.objectItem.objectName, equalTo("Global Permissions"));
        assertThat(secondAuditEventPermissionAdded.category, equalTo("permissions"));
        assertThat(secondAuditEventPermissionAdded.summary, equalTo("Global permission added"));
        assertThat(secondAuditEventPermissionAdded.changedValues, hasSize(2));
        assertThat(secondAuditEventPermissionAdded.changedValues, hasItem(new Restmodels.ChangedValueResponse("Group", "", "Anyone on the web")));
        assertThat(secondAuditEventPermissionAdded.changedValues, hasItem(new Restmodels.ChangedValueResponse("Permission", "", "Bulk Change")));

        final Restmodels.RecordResponse auditEventGroupCreated = waitUntilEventsAreFoundInLog("Group created", 1).get(0);

        assertThat(auditEventGroupCreated.objectItem.objectName, equalTo("new-group"));
        assertThat(auditEventGroupCreated.category, equalTo("group management"));
        assertThat(auditEventGroupCreated.summary, equalTo("Group created"));
    }
}
