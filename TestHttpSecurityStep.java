package com.atlassian.jira.web.filters.steps.security;

import com.atlassian.jira.junit.rules.AvailableInContainer;
import com.atlassian.jira.junit.rules.MockitoContainer;
import com.atlassian.jira.junit.rules.MockitoMocksInContainer;
import com.atlassian.jira.web.filters.steps.FilterCallContext;
import com.atlassian.jira.web.filters.steps.FilterCallContextImpl;
import com.google.common.net.HttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 7.6
 */
public class TestHttpSecurityStep {
    private static final String ASSETS_SERVLET_PATH = "/s";
    private static final String STRICT_ORIGIN_WHEN_CROSS_ORIGIN = "strict-origin-when-cross-origin";
    private static final String X_XSS_PROTECTION_VALUE = "1; mode=block";
    private static final String X_CONTENT_TYPE_OPTIONS_VALUE = "nosniff";
    private static final String X_FRAME_OPTIONS_VALUE = "SAMEORIGIN";
    private static final String CONTENT_SECURITY_POLICY_VALUE = "frame-ancestors 'self'";
    private static final String STRICT_TRANSPORT_SECURITY_DEFAULT_VALUE = "max-age=31536000";

    @Rule
    public MockitoContainer container = MockitoMocksInContainer.rule(this);

    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse httpResponse;
    @Mock
    private FilterConfig filterConfig;
    @Mock
    private FilterChain filterChain;
    @Mock
    @AvailableInContainer
    private HttpSecurityConfig httpSecurityConfig;


    @Before
    public void mockHttpSecurityConfig() {
        when(httpSecurityConfig.isClickjackingProtectionDisabled()).thenReturn(false);
        when(httpSecurityConfig.isExcluded(any())).thenReturn(false);
        when(httpSecurityConfig.isStrictTransportSecurityDisabled()).thenReturn(false);
        when(httpSecurityConfig.getStrictTransportSecurityMaxAge()).thenReturn(null);
    }

    @Test
    public void allHeadersShouldBeSetByDefault() throws Exception {
        runTest("/somepath", null);
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersAreSet();
        verifyReferrerPolicyIsSet();
        verifyStrictTransportSecurityHeaderIsSet();
    }

    @Test
    public void exactMatchShouldBeExcluded() throws Exception {
        excludePath("/one");

        runTest("/one", null);
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersNotSet();
        verifyStrictTransportSecurityHeaderIsSet();
    }

    @Test
    public void subpathShouldBeExcluded() throws Exception {
        excludePath("/abc");

        runTest("/abc/two", null);
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersNotSet();
        verifyStrictTransportSecurityHeaderIsSet();
    }

    @Test
    public void disableClickjackingProtectionShouldWork() throws Exception {
        disableClickjackingProtection();

        runTest("/abc", null);
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersNotSet();
        verifyStrictTransportSecurityHeaderIsSet();
    }

    @Test
    public void disableStrictTransportSecurityShouldWork() throws Exception {
        when(httpSecurityConfig.isStrictTransportSecurityDisabled()).thenReturn(true);

        runTest("/abc", null);
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersAreSet();
        verifyStrictTransportSecurityHeaderNotSet();
    }

    @Test
    public void maxAgeOfStrictTransportSecurityShouldBeAdjustable() throws Exception {
        when(httpSecurityConfig.getStrictTransportSecurityMaxAge()).thenReturn(0L);

        runTest("/abc", null);
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersAreSet();
        verify(httpResponse).setHeader(HttpHeaders.STRICT_TRANSPORT_SECURITY, "max-age=0");
    }

    @Test
    public void includeSubDomainsOfStrictTransportSecurityShouldBeAdjustable() throws Exception {
        when(httpSecurityConfig.getStrictTransportSecurityMaxAge()).thenReturn(0L);
        when(httpSecurityConfig.isStrictTransportSecurityIncludeSubDomainsEnabled()).thenReturn(true);

        runTest("/abc", null);
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersAreSet();
        verify(httpResponse).setHeader(HttpHeaders.STRICT_TRANSPORT_SECURITY, "max-age=0; includeSubDomains");
    }

    @Test
    public void preloadOfStrictTransportSecurityShouldBeAdjustable() throws Exception {
        when(httpSecurityConfig.getStrictTransportSecurityMaxAge()).thenReturn(0L);
        when(httpSecurityConfig.isStrictTransportSecurityPreloadEnabled()).thenReturn(true);

        runTest("/abc", null);
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersAreSet();
        verify(httpResponse).setHeader(HttpHeaders.STRICT_TRANSPORT_SECURITY, "max-age=0; includeSubDomains; preload");
    }

    @Test
    public void itIsPossibleToAddAdditionalParamsToStrictTransportSecurity() throws Exception {
        when(httpSecurityConfig.getStrictTransportSecurityMaxAge()).thenReturn(0L);
        when(httpSecurityConfig.getStrictTransportSecurityAdditionalParams()).thenReturn("param");

        runTest("/abc", null);
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersAreSet();
        verify(httpResponse).setHeader(HttpHeaders.STRICT_TRANSPORT_SECURITY, "max-age=0; param");
    }

    @Test
    public void allHeadersShouldBeSetWhenHttpSecurityConfigIsNotAvailable() throws Exception {
        removeHttpSecurityConfig();
        excludePath("/one");

        runTest("/one", null);
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersAreSet();
        verifyStrictTransportSecurityHeaderIsSet();
    }

    @Test
    public void assetsShouldNotBeExcluded() throws Exception {
        excludePath("/s/download/logo");

        runTest(ASSETS_SERVLET_PATH, "/download/logo");
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersAreSet();
    }

    @Test
    public void pathsShouldBeCombined() throws Exception {
        excludePath("/one/two");

        runTest("/one", "/two/sub-path");
        verifyCommonHeadersAreSet();
        verifyClickjackingHeadersNotSet();
    }


    private void verifyCommonHeadersAreSet() {
        verify(httpResponse).setHeader(HttpHeaders.X_CONTENT_TYPE_OPTIONS, X_CONTENT_TYPE_OPTIONS_VALUE);
        verify(httpResponse).setHeader(HttpHeaders.X_XSS_PROTECTION, X_XSS_PROTECTION_VALUE);
    }

    private void verifyClickjackingHeadersAreSet() {
        verify(httpResponse).setHeader(HttpHeaders.X_FRAME_OPTIONS, X_FRAME_OPTIONS_VALUE);
        verify(httpResponse).setHeader(HttpHeaders.CONTENT_SECURITY_POLICY, CONTENT_SECURITY_POLICY_VALUE);
    }

    private void verifyClickjackingHeadersNotSet() {
        verify(httpResponse, never()).setHeader(eq(HttpHeaders.X_FRAME_OPTIONS), any());
        verify(httpResponse, never()).setHeader(eq(HttpHeaders.CONTENT_SECURITY_POLICY), any());
    }

    private void verifyStrictTransportSecurityHeaderIsSet() {
        verify(httpResponse).setHeader(HttpHeaders.STRICT_TRANSPORT_SECURITY, STRICT_TRANSPORT_SECURITY_DEFAULT_VALUE);
    }

    private void verifyStrictTransportSecurityHeaderNotSet() {
        verify(httpResponse, never()).setHeader(eq(HttpHeaders.STRICT_TRANSPORT_SECURITY), any());
    }

    private void verifyReferrerPolicyIsSet() {
        verify(httpResponse).setHeader(HttpHeaders.REFERRER_POLICY, STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
    }

    private void runTest(final String servletPath, final String pathInfo) throws IOException, ServletException {
        when(httpRequest.getServletPath()).thenReturn(servletPath);
        when(httpRequest.getPathInfo()).thenReturn(pathInfo);

        FilterCallContext context = new FilterCallContextImpl(httpRequest, httpResponse, filterChain, filterConfig);

        final HttpSecurityStep tested = new HttpSecurityStep();
        tested.beforeDoFilter(context);
    }

    private void removeHttpSecurityConfig() {
        container.getMockComponentContainer().addMock(HttpSecurityConfig.class, null);
    }

    private void disableClickjackingProtection() {
        when(httpSecurityConfig.isClickjackingProtectionDisabled()).thenReturn(true);
    }

    private void excludePath(String path) {
        when(httpSecurityConfig.isExcluded(startsWith(path))).thenReturn(true);
    }
}
