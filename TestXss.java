package com.atlassian.jira.webtest.webdriver.tests.security;

import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.config.CreateUser;
import com.atlassian.jira.pageobjects.config.LoginAs;
import com.atlassian.jira.pageobjects.navigator.AgnosticIssueNavigator;
import com.atlassian.jira.pageobjects.pages.EditProfilePage;
import com.atlassian.jira.pageobjects.pages.ViewProfilePage;
import com.atlassian.jira.pageobjects.pages.admin.EditApplicationPropertiesPage;
import com.atlassian.jira.pageobjects.pages.admin.configuration.ViewGeneralConfigurationPage;
import com.atlassian.jira.pageobjects.pages.admin.roles.UserRoleActorActionPage;
import com.atlassian.jira.pageobjects.pages.admin.user.AddUserPage;
import com.atlassian.jira.pageobjects.pages.admin.user.UserBrowserPage;
import com.atlassian.pageobjects.elements.PageElement;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import javax.inject.Inject;
import java.util.NoSuchElementException;
import java.util.UUID;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@WebTest({com.atlassian.jira.functest.framework.suite.Category.WEBDRIVER_TEST})
public class TestXss extends BaseJiraWebTest {
    private static final String DEVELOPER = "developer";

    private static final String XSS_JS_VARIABLE = "window.xssString";
    private static final String XSS_JS_EXPRESSION = format("%s='hacked';", XSS_JS_VARIABLE);
    private static final String XSS_JS_STRING = format("'\"><script>alert(1);%s</script>", XSS_JS_EXPRESSION);

    @BeforeClass
    public static void setUp() {
        backdoor.restoreBlankInstance();
    }

    @Inject
    private JavascriptExecutor javascriptExecutor;

    @Test
    @CreateUser(username = DEVELOPER, password = DEVELOPER)
    @LoginAs(user = DEVELOPER, password = DEVELOPER)
    public void testRoleActorActionsXSS() throws Exception {
        pageBinder.navigateToAndBind(UserRoleActorActionPage.class,
                "10002f843c%3Cscript%3Ealert%281%29%3C/script%3Ee156c7382b7");
        assertSourceNoXSS();
        pageBinder.navigateToAndBind(UserRoleActorActionPage.class,
                "10002&projectId=10010f843c%3Cscript%3Ealert%281%29%3C/script%3Ee156c7382b7");
        assertSourceNoXSS();
        pageBinder.navigateToAndBind(UserRoleActorActionPage.class,
                "100021442d<script>alert(1)</script>42df75ab185&projectId=10020");
        assertSourceNoXSS();
        pageBinder.navigateToAndBind(UserRoleActorActionPage.class,
                format("100021442d<script>alert(1);%s</script>42df75ab185&projectId=10020", XSS_JS_EXPRESSION));
        assertSourceNoXSS();
        assertJsNoXSS();
        pageBinder.navigateToAndBind(UserRoleActorActionPage.class,
                "10002f843c%3Cscript%3Ealert%281%29%3C/script%3Ee156c7382b7");
        assertSourceNoXSS();
        pageBinder.navigateToAndBind(UserRoleActorActionPage.class,
                "10002&projectId=10010b4927<script>alert(1)</script>fa5f1a0dfb");
        assertSourceNoXSS();
        pageBinder.navigateToAndBind(UserRoleActorActionPage.class,
                format("10002&projectId=10010b4927<script>alert(1);%s</script>fa5f1a0dfb", XSS_JS_EXPRESSION));
        assertSourceNoXSS();
        assertJsNoXSS();
    }

    /**
     * Tests against XSS in EditProfile!default.jspa (JST-3617)
     */
    @Test
    @CreateUser(username = DEVELOPER, password = DEVELOPER)
    @LoginAs(user = DEVELOPER, password = DEVELOPER, targetPage = ViewProfilePage.class)
    public void testEditProfileXSS() {
        final ViewProfilePage profilePage = pageBinder.bind(ViewProfilePage.class);
        final EditProfilePage editPage = profilePage.edit();
        editPage.setFullname(format("\"><script>alert(\"JST-3617\");%s</script>", XSS_JS_EXPRESSION)).setPassword(DEVELOPER);
        editPage.submit();
        assertSourceNoXSS();
        assertJsNoXSS();
    }

    @Test
    @CreateUser(username = DEVELOPER, password = DEVELOPER)
    @LoginAs(user = DEVELOPER, password = DEVELOPER, targetPage = ViewProfilePage.class)
    public void testEditProfileEmail() {
        final ViewProfilePage profilePage = pageBinder.bind(ViewProfilePage.class);
        final EditProfilePage editPage = profilePage.edit();
        editPage.setEmail("\"><script>alert(1)</script>").setPassword(DEVELOPER);
        PageElement errorMessage = editPage.submitBadEmailAndReturnErrorElement();
        assertThat(errorMessage.getText(), containsString("Invalid email address format."));
    }

    /**
     * Tests against XSS in EditApplicationProperties.jspa (JST-3790)
     */
    @Test
    @CreateUser(username = DEVELOPER, password = DEVELOPER)
    @LoginAs(sysadmin = true, targetPage = EditApplicationPropertiesPage.class)
    public void testEditApplicationPropertiesXSS() {
        EditApplicationPropertiesPage editPage = pageBinder.bind(EditApplicationPropertiesPage.class);
        final String originalEmailFrom = editPage.getEmailFromHeaderFormat();
        final String originalAppTitle = editPage.getApplicationTitle();

        try {
            editPage.setEmailFromHeaderFormat(format("\"><script>alert(3790);%s</script>", XSS_JS_EXPRESSION));
            editPage.submit();
            pageBinder.bind(ViewGeneralConfigurationPage.class);
            assertJsNoXSS();

            editPage = pageBinder.navigateToAndBind(EditApplicationPropertiesPage.class);
            editPage.setTitle(format("votest.jira.com'\"><script>alert(3790);%s</script>d5c2734e173b21b9c", XSS_JS_EXPRESSION));
            editPage.submit();
            pageBinder.bind(ViewGeneralConfigurationPage.class);
            assertJsNoXSS();
        } finally {
            editPage = pageBinder.navigateToAndBind(EditApplicationPropertiesPage.class);
            editPage.setTitle(originalAppTitle).setEmailFromHeaderFormat(originalEmailFrom);
            editPage.submit();
            pageBinder.bind(ViewGeneralConfigurationPage.class);
        }
    }

    /**
     * Tests against XSS in AddUser.jspa (JST-3797)
     */
    @Test
    @CreateUser(username = DEVELOPER, password = DEVELOPER)
    @LoginAs(sysadmin = true, targetPage = AddUserPage.class)
    public void testAddUserXSS() {
        String testUserUsername = null;
        try {
            testUserUsername = "a" + System.currentTimeMillis();
            final AddUserPage addUserPage = pageBinder.bind(AddUserPage.class);
            final UserBrowserPage userBrowser = addUserPage.addUser(testUserUsername, XSS_JS_STRING,
                    XSS_JS_STRING, testUserUsername + "@example.com", false).createUser(
                    UserBrowserPage.class);
            try {
                userBrowser.findRow(testUserUsername);
            } catch (final NoSuchElementException e) {
                throw new AssertionError("User " + testUserUsername + " not found on browser page.");
            }
            assertJsNoXSS();
        } finally {
            jira.backdoor().getTestkit().rawRestApiControl().rootResource().path("user")
                    .queryParam("username", testUserUsername).delete();
        }
    }

    public static class IssueNavigatorWithPrototypePollution extends AgnosticIssueNavigator {

        private final String injectedUuid = UUID.randomUUID().toString();

        @Override
        public String getUrl() {
            return format("/issues/?filter=-4&__proto__.preventDefault=x&__proto__.handleObj.=x&__proto__.delegateTarget=%s", maliciousJs());
        }

        private String maliciousJs() {
            return format("%%3Cimg%%2Fsrc%%3Dxyz%%20onerror%%3Ddocument.body.appendChild(document.createTextNode('%s'))%%3E", injectedUuid);
        }

        public boolean hasNoInjectedUuid() {
            return body.findAll(By.xpath(format("//*[text()[contains(.,'%s')]]", injectedUuid))).isEmpty();
        }
    }

    @Test
    @LoginAs(admin = true)
    public void testPrototypePollutionInIssueNavigator() {
        final IssueNavigatorWithPrototypePollution searchPage = pageBinder.navigateToAndBind(IssueNavigatorWithPrototypePollution.class);
        assertTrue("Prototype pollution injected a random UUID in the DOM", searchPage.hasNoInjectedUuid());
    }

    private void assertSourceNoXSS() {
        assertThat(jira.getTester().getDriver().getPageSource(), not(containsString("<script>alert")));
    }

    private void assertJsNoXSS() {
        assertThat(javascriptExecutor.executeScript(format("return %s;", XSS_JS_VARIABLE)), is(nullValue()));
    }
}
