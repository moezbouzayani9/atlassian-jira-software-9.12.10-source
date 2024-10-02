package com.atlassian.jira.kubernetes.test;

import com.atlassian.jira.webtests.ztests.admin.security.xsrf.TestXsrfAttachments;
import com.atlassian.jira.webtests.ztests.bulk.TestBulkOperationsIndexing;
import com.atlassian.jira.webtests.ztests.bundledplugins2.rest.TestClusterUpgradeStateResource;
import com.atlassian.jira.webtests.ztests.database.TestDatabaseLargeInQueries;
import com.atlassian.jira.webtests.ztests.database.TestDatabaseSystemTimeReader;
import com.atlassian.jira.webtests.ztests.database.TestDropIndexHelper;
import org.junit.runner.RunWith;

/**
 * @since 8.15
 */
@RunWith(TestSuite.class)
@TestSuite.SuiteClasses(classes = {TestXsrfAttachments.class, TestClusterUpgradeStateResource.class,
        TestBulkOperationsIndexing.class, TestDropIndexHelper.class, TestDatabaseLargeInQueries.class, TestDatabaseSystemTimeReader.class})
public class JiraFuncTestSuite {
}
