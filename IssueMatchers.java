package com.atlassian.jira.webtest.webdriver.tests.bulk;

import com.atlassian.jira.testkit.client.restclient.Issue;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import javax.annotation.Nullable;
import java.util.Optional;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Matchers for the {@link Issue} domain object.
 *
 * @since v7.0
 */
public class IssueMatchers {

    private IssueMatchers() {
        throw new AssertionError("Don't instantiate me");
    }

    public static Matcher<Issue> issueWithIssueType(final String issueType) {
        notNull("issueType", issueType);
        return new TypeSafeMatcher<Issue>() {
            @Override
            protected boolean matchesSafely(Issue issue) {
                return issueType.equals(issue.fields.issuetype.name);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Issue with issue type=").appendValue(issueType);
            }
        };
    }

    public static Matcher<Issue> issueWithParentWithKey(final String parentKey) {
        notNull("parentKey", parentKey);
        return new TypeSafeMatcher<Issue>() {
            @Override
            protected boolean matchesSafely(Issue issue) {
                return parentKey.equals(issue.fields.parent.key());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Issue with parent with key=").appendValue(parentKey);
            }
        };
    }

    public static Matcher<Issue> issueWithProjectName(final String projectName) {
        notNull("projectName", projectName);
        return new TypeSafeMatcher<Issue>() {
            @Override
            protected boolean matchesSafely(Issue issue) {
                return projectName.equals(issue.fields.project.name);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Issue with project name=").appendValue(projectName);
            }
        };
    }

    public static Matcher<Issue> issueWithProjectId(final String projectId) {
        notNull("projectId", projectId);
        return new TypeSafeMatcher<Issue>() {
            @Override
            protected boolean matchesSafely(Issue issue) {
                return projectId.equals(issue.fields.project.id);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Issue with project id=").appendValue(projectId);
            }
        };
    }

    public static Matcher<Issue> issueWithSubtaskNum(final int subtaskNum) {
        notNull("subtaskNum", subtaskNum);
        return new TypeSafeMatcher<Issue>() {
            @Override
            protected boolean matchesSafely(Issue issue) {
                return subtaskNum == issue.fields.subtasks.size();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Issue with subtask number=").appendValue(subtaskNum);
            }
        };
    }

    public static Matcher<Issue> issueWithSecurityLevelName(@Nullable final String securityLevelName) {
        return new TypeSafeMatcher<Issue>() {
            @Override
            protected boolean matchesSafely(Issue issue) {
                return Optional.ofNullable(securityLevelName).equals(Optional.ofNullable(issue.fields.security).map(s -> s.name));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Issue with security level Name=").appendValue(securityLevelName);
            }
        };
    }

    public static Matcher<Issue> issueWithoutSecurityLevel() {
        return new TypeSafeMatcher<Issue>() {
            @Override
            protected boolean matchesSafely(Issue issue) {
                return issue.fields.security == null;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Issue without security level");
            }
        };
    }

    public static Matcher<Issue> issueWithParentKey(@Nullable final String parentKey) {
        return new TypeSafeMatcher<Issue>() {
            @Override
            protected boolean matchesSafely(Issue issue) {
                return Optional.ofNullable(parentKey).equals(Optional.ofNullable(issue.fields.parent).map(s -> s.key()));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Issue with parentKey=").appendValue(parentKey);
            }
        };
    }
}
