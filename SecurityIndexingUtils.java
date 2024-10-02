package com.atlassian.jira.issue.index;

import com.atlassian.annotations.Internal;
import com.atlassian.jira.issue.Issue;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.util.BytesRef;

import static com.atlassian.jira.issue.index.DocumentConstants.ISSUE_LEVEL_PERMISSIONS_FIELD;
import static com.atlassian.jira.issue.index.DocumentConstants.PROJECT_PERMISSIONS_FIELD;

/**
 * Set of utility methods for indexing and querying project and issue security permissions.
 *
 * Permissions can be based on a combination of
 * <ul>
 * <li>project id</li>
 * <li>project id + field (name + value)</li>
 * <li>issue security level id</li>
 * <li>issue security level id + project id</li>
 * <li>issue security level id + field (name + value)</li>
 * </ul>
 *
 * Each field which can be used in project security scheme or issue security scheme (i.e. one that is a user or group field)
 * has to be indexed with {@link #indexPermissions(Document, Issue, String, String)}.
 *
 * @since 8.0
 */
@Internal
public class SecurityIndexingUtils {

    /**
     * Used to concatenate parts of the permission field content.
     * It's an arbitrary delimiter and has nothing to do with {@link org.apache.lucene.search.BooleanClause.Occur#FILTER}.
     *
     * <h3>Clash with field value</h3>
     * We are safe against attacks of values that contain #, because we allow free text only after the last delimiter,
     * i.e. we control the prefix of the concatenated string. The string will always match the regex:
     * <pre>(p|s)#[0-9]+#(issue_assignee|issue_author|customfield_[0-9]+)#.*</pre>
     */
    private static final String DELIMITER = "#";

    private static final String PROJECT_PREFIX = "p";

    private static final String SECURITY_LEVEL_PREFIX = "s";

    /**
     * Index top level permissions - ones that depend only on the project and issue security level and not
     * on a specific field value. For example "project lead" is satisfied for all issues in a given project,
     * regardless of the issues' values.
     *
     * @param doc   Document to which the indexed field will be added
     * @param issue Issue that is indexed in the document
     */
    public static void indexPermissions(final Document doc, final Issue issue) {
        final Long projectId = issue.getProjectObject().getId();
        final Long issueSecurityLevel = issue.getSecurityLevelId();
        indexProjectPermissionField(doc, generateProjectPermissionFieldContents(projectId));
        if (issueSecurityLevel == null) {
            indexIssueLevelPermissionField(doc, generateIssueLevelPermissionContents(-1L));
        } else {
            indexIssueLevelPermissionField(doc, generateIssueLevelPermissionContents(issueSecurityLevel));
            indexIssueLevelPermissionField(doc, generateIssueLevelPermissionContents(issueSecurityLevel, projectId));
        }
    }

    /**
     * Index field level permissions - ones that depend on specific field value.
     * For example "assignee" is satisfied only by issues which have the assignee field set to the current searcher.
     *
     * @param doc        Document to which the indexed field will be added
     * @param issue      Issue that is indexed in the document
     * @param fieldName  The name of the field that is being indexed
     * @param fieldValue The value of the field that is being indexed
     */
    public static void indexPermissions(final Document doc, final Issue issue, final String fieldName, final String fieldValue) {
        final Long projectId = issue.getProjectObject().getId();
        final Long issueSecurityLevel = issue.getSecurityLevelId();
        indexProjectPermissionField(doc, generateProjectPermissionFieldContents(projectId, fieldName, fieldValue));
        if (issueSecurityLevel != null) {
            indexIssueLevelPermissionField(doc, generateIssueLevelPermissionContents(issueSecurityLevel == null ? -1L : issueSecurityLevel, fieldName, fieldValue));
        }
    }

    /**
     * Generate query string that corresponds to a project permission that is satisfied by the project.
     *
     * @param projectId Id of the project for which we generate permission field content
     * @return          Permission field content for the project id
     */
    public static BytesRef generateProjectPermissionFieldContents(final Long projectId) {
        return new BytesRef(PROJECT_PREFIX + DELIMITER + projectId);
    }

    /**
     * Generate query string that corresponds to a project permission that is satisfied by a concrete value of a concrete field.
     *
     * @param projectId  Id of the project for which we generate field content
     * @param fieldName  Field name for which we generate permission field content
     * @param fieldValue Value of the field for which we generate permission field content
     * @return           Permission field content for the value of the field in the specified project
     */
    public static BytesRef generateProjectPermissionFieldContents(final Long projectId, final String fieldName, final String fieldValue) {
        return new BytesRef(PROJECT_PREFIX + DELIMITER + projectId + DELIMITER + fieldName + DELIMITER + fieldValue);
    }

    /**
     * Generate query string that corresponds to a issue security level permission that is satisfied by the issue security level.
     *
     * @param issueSecurityLevel Id of the issue security level for which we generate field content
     * @return                   Permission field content for the issue security level
     */
    public static BytesRef generateIssueLevelPermissionContents(final Long issueSecurityLevel) {
        return new BytesRef(SECURITY_LEVEL_PREFIX + DELIMITER + issueSecurityLevel);
    }

    /**
     * Generate query string that corresponds to a issue security level permission that is satisfied by the issue security level in the project.
     *
     * @param issueSecurityLevel Id of the issue security level for which we generate field content
     * @param projectId          Id of the project for which we generate field content
     * @return                   Permission field content for the issue security level and project
     */
    public static BytesRef generateIssueLevelPermissionContents(final Long issueSecurityLevel, final Long projectId) {
        return new BytesRef(SECURITY_LEVEL_PREFIX + DELIMITER + issueSecurityLevel + DELIMITER + PROJECT_PREFIX + DELIMITER + projectId);
    }

    /**
     * Generate query string that corresponds to a issue security level permission that is satisfied by a concrete value of a concrete field.
     *
     * @param issueSecurityLevel Id of the issue security level for which we generate field content
     * @param fieldName          Field name for which we generate permission field content
     * @param fieldValue         Value of the field for which we generate permission field content
     * @return
     */
    public static BytesRef generateIssueLevelPermissionContents(final Long issueSecurityLevel, final String fieldName, final String fieldValue) {
        return new BytesRef(SECURITY_LEVEL_PREFIX + DELIMITER + issueSecurityLevel + DELIMITER + fieldName + DELIMITER + fieldValue);
    }

    //adding fields
    private static void indexIssueLevelPermissionField(final Document doc, final BytesRef value) {
        doc.add(new StringField(ISSUE_LEVEL_PERMISSIONS_FIELD, value, Field.Store.NO));
    }

    private static void indexProjectPermissionField(final Document doc, final BytesRef value) {
        doc.add(new StringField(PROJECT_PERMISSIONS_FIELD, value, Field.Store.NO));
    }
}
