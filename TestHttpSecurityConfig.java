package com.atlassian.jira.web.filters.steps.security;

import com.atlassian.jira.config.properties.JiraProperties;
import com.atlassian.ozymandias.PluginPointFunction;
import com.atlassian.ozymandias.SafeAccessViaPluginAccessor;
import com.atlassian.plugin.PluginAccessor;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @since 7.6
 */
public class TestHttpSecurityConfig {
    private static final String DISABLE_CLICKJACKING_PROTECTION_PROPERTY = "com.atlassian.jira.clickjacking.protection.disabled";
    private static final String CLICKJACKING_PROTECTION_EXCLUDE_PROPERTY = "com.atlassian.jira.clickjacking.protection.exclude";

    @Rule
    public MethodRule initMockito = MockitoJUnit.rule();
    @Mock
    private JiraProperties jiraProperties;
    @Mock
    private PluginAccessor pluginAccessor;
    @Mock
    private SafeAccessViaPluginAccessor safeAccessViaPluginAccessor;

    private HttpSecurityConfig tested;


    @Before
    public void before() {
        when(jiraProperties.getProperty(CLICKJACKING_PROTECTION_EXCLUDE_PROPERTY, "")).thenReturn("");
        when(jiraProperties.getBoolean(DISABLE_CLICKJACKING_PROTECTION_PROPERTY)).thenReturn(false);

        tested = new HttpSecurityConfig(jiraProperties, pluginAccessor);
    }

    @Test
    public void shouldExcludeIssueCollectorByDefault() {
        verifyExcluded("/rest/collectors/1.0/template/form/a0b1c2");
        verifyExcluded("/rest/collectors/1.0/template/custom/a0b1c2?os");
        verifyExcluded("/rest/collectors/1.0/template/feedback/a0b1c2?os=1&query=abc");

        verifyNotExcluded("/rest/collectors/1.0/template/other/");
    }

    @Test
    public void shouldExcludeAppLinksByDefault() {
        verifyExcluded("/plugins/servlet/applinks/auth/conf/trusted/something");
        verifyExcluded("/plugins/servlet/applinks/auth/conf/basic/something?param=1");
        verifyExcluded("/plugins/servlet/applinks/auth/conf/oauth/something#hash");

        verifyNotExcluded("/plugins/servlet/applinks/auth/conf/untrusted");
    }

    @Test
    public void differentPathShouldNotBeExcluded() {
        setExcludedPaths("/one,/two,/four");

        verifyNotExcluded("/three");
    }

    @Test
    public void exactMatchShouldBeExcluded() {
        setExcludedPaths("/one");

        verifyExcluded("/one");
    }

    @Test
    public void subpathShouldBeExcluded() {
        setExcludedPaths("/one,/two/");

        verifyExcluded("/two/three");
    }

    @Test
    public void slashShouldBeValidPathPart() {
        setExcludedPaths("/one/two/,/four");

        verifyNotExcluded("/one/two");
    }

    @Test
    public void pathsShouldBeTrimmed() {
        setExcludedPaths("  /one,\n/one/two  ,/three");

        verifyExcluded("/one/two");
    }

    @Test
    public void shouldNotBeAllowedToExcludeEveryPath() {
        setExcludedPaths("");

        verifyNotExcluded("/one/two");
    }

    @Test
    public void isExcludedShouldHandleNull() {
        verifyNotExcluded(null);
    }

    @Test
    public void clickjackingProtectionIsOnByDefault() {
        assertFalse(tested.isClickjackingProtectionDisabled());
    }

    @Test
    public void shouldBeAbleToDisableClickjackingProtection() {
        disableClickjackingProtection();

        assertTrue(tested.isClickjackingProtectionDisabled());
    }

    @Test
    public void shouldReadExcludedPathFromPlugins() {
        mockPluginAccessor();

        verifyExcluded("/rest/collectors/2.0/template/form/a0b1c2");
        verifyExcluded("/rest/collectors/2.0/template/custom/a0b1c2?os");
        verifyExcluded("/rest/collectors/2.0/template/feedback/a0b1c2?os=1&query=abc");

        verifyNotExcluded("/rest/collectors/2.0/template/other/");
        verifyNotExcluded("");
    }


    private void verifyExcluded(final String path) {
        assertTrue(tested.isExcluded(path));
    }

    private void verifyNotExcluded(final String path) {
        assertFalse(tested.isExcluded(path));
    }

    private void setExcludedPaths(final String excludedPaths) {
        final String returnValue = (excludedPaths == null) ? "" : excludedPaths;
        when(jiraProperties.getProperty(CLICKJACKING_PROTECTION_EXCLUDE_PROPERTY, "")).thenReturn(returnValue);
    }

    private void disableClickjackingProtection() {
        when(jiraProperties.getBoolean(DISABLE_CLICKJACKING_PROTECTION_PROPERTY)).thenReturn(true);
    }

    private void mockPluginAccessor() {
        PathExclusionImpl pathExclusion1 = new PathExclusionImpl(ImmutableList.of("/rest/collectors/2.0/template/form/"));
        PathExclusionImpl pathExclusion2 = new PathExclusionImpl(ImmutableList.of(
                "/rest/collectors/2.0/template/custom/",
                "/rest/collectors/2.0/template/feedback/"));

        tested = spy(tested);
        doReturn(safeAccessViaPluginAccessor).when(tested).getSafeAccessViaPluginAccessor();

        when(safeAccessViaPluginAccessor.forType(eq(PathExclusionModuleDescriptor.class),
                Matchers.<PluginPointFunction<PathExclusionModuleDescriptor, PathExclusion, Stream<String>>>any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    PluginPointFunction<PathExclusionModuleDescriptor, PathExclusion, Stream<String>> visitor =
                            invocation.getArgument(1);
                    return ImmutableList.of(
                            visitor.onModule(mock(PathExclusionModuleDescriptor.class), pathExclusion1),
                            visitor.onModule(mock(PathExclusionModuleDescriptor.class), pathExclusion2)
                    );
                });
    }
}
