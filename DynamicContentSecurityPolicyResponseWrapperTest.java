package com.atlassian.jira.web.filters.steps.security.csp;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.junit.extensions.MockComponentContainerExtension;
import com.atlassian.jira.junit.rules.AvailableInContainer;
import io.atlassian.util.concurrent.LazyReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Stream;

import static com.atlassian.jira.JiraFeatureFlagRegistrar.ENABLE_SANDBOX_CONTENT_SECURITY_POLICY;
import static com.atlassian.jira.config.properties.APKeys.ContentSecurityPolicySandbox.BROWSER_DIFFERENTIATED_CLAUSES;
import static com.atlassian.jira.config.properties.APKeys.ContentSecurityPolicySandbox.BROWSER_DIFFERENTIATED_PATHS;
import static com.atlassian.jira.config.properties.APKeys.ContentSecurityPolicySandbox.EXCLUDED_CONTENT_TYPES;
import static com.atlassian.jira.config.properties.APKeys.ContentSecurityPolicySandbox.INCLUDED_CONTENT_DISPOSITION;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.USER_AGENT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, MockComponentContainerExtension.class})
public class DynamicContentSecurityPolicyResponseWrapperTest {

    private static final String ATTACHMENT_PATH = "/secure/attachment";
    private static final String NON_ATTACHMENT_PATH = "/secure/resource";

    private static final String FIREFOX_DESKTOP_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:89.0) Gecko/20100101 Firefox/89.0";
    private static final String CHROME_DESKTOP_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36";
    private static final String CHROME_IOS_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/86.0.4240.93 Mobile/15E148 Safari/604.1";
    private static final String CHROME_ANDROID_UA = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.105 Mobile Safari/537.36";
    private static final String SAFARI_DESKTOP_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Safari/605.1.15";
    private static final String SAFARI_IOS_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.1 Mobile/15E148 Safari/604.1";
    private static final String EDGE_LEGACY_DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTLM, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.18363";
    private static final String UNKNOWN_UA = "Mozilla/5.0 (Unknown OS) UnknownBrowser/21.37";


    private static final String DEFAULT_DIFFERENTIATED_CLAUSES = "CHROME_DESKTOP:allow-same-origin;CHROME_ANDROID:allow-same-origin;CHROME_IOS:allow-scripts;SAFARI_DESKTOP:allow-scripts;SAFARI_IOS:allow-scripts";
    private static final String DEFAULT_ADDITIONAL_PATHS = "";

    private static final String EMPTY_SANDBOX_CLAUSE = "";
    private static final String DEFAULT_SANDBOX_CLAUSE = "sandbox";

    private static final String DEFAULT_EXCLUDED_CONTENT_TYPE = "";

    private static final String DEFAULT_INCLUDED_CONTENT_DISPOSITION = "attachment";

    private static final String MP4_MIME = "video/mp4";
    private static final String JS_MIME = "application/javascript";
    private static final String XML_MIME = "text/xml";
    private static final String HTML_MIME = "text/html";
    private static final String JSON_MIME = "application/json;charset=UTF-8";
    private static final String PNG_MIME = "image/png;charset=UTF-8";
    private static final String JPEG_MIME = "image/jpeg";

    private static final String TEST_DESCRIPTION = "{index}. Sandbox enabled: {0},\n" +
            "UA: {1}, \n" +
            "Servlet path: {2}, \n" +
            "Header {3} set to {4}, \n" +
            "deniedContents: {5}, \n" +
            "includedDispositions: {6}, \n" +
            "browserDifferentiatedPaths: {7}, \n" +
            "browserDifferentiatedClauses: {8}, \n" +
            "resulting sandbox clause: {9}";

    @Mock
    @AvailableInContainer
    private FeatureManager featureManager;

    @Mock
    @AvailableInContainer
    private ApplicationProperties applicationProperties;

    private DynamicContentSecurityPolicyResponseWrapper cspWrapper;
    private HttpServletResponse response;
    private HttpServletRequest request;

    @BeforeEach
    public void setUp() {
        response = Mockito.mock(HttpServletResponse.class);
        request = Mockito.mock(HttpServletRequest.class);
    }

    @AfterEach
    public void tearDown() {
        cspWrapper = null;
        response = null;
    }


    private static Stream<Arguments> testCases() {
        return Stream.of(

                //Feature Flag is off, no sandbox should be set
                Arguments.of(false, // is feature enabled
                        FIREFOX_DESKTOP_UA, // user-agent header value
                        ATTACHMENT_PATH, //request destination servlet path
                        CONTENT_TYPE, XML_MIME, // header name and value
                        DEFAULT_EXCLUDED_CONTENT_TYPE, // excluded content types
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION, // included content dispositions
                        DEFAULT_ADDITIONAL_PATHS, // additional browser differentiated paths
                        DEFAULT_DIFFERENTIATED_CLAUSES, // browser differentiated clauses
                        EMPTY_SANDBOX_CLAUSE //expected CSP header value
                ),

                Arguments.of(false,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, HTML_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        EMPTY_SANDBOX_CLAUSE),


                // Sandbox set according to content type with no additional exclusions
                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, HTML_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        EMPTY_SANDBOX_CLAUSE),

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, JS_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        EMPTY_SANDBOX_CLAUSE),

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, JSON_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        DEFAULT_SANDBOX_CLAUSE),

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, PNG_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        DEFAULT_SANDBOX_CLAUSE),

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, XML_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        DEFAULT_SANDBOX_CLAUSE),

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, JPEG_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        DEFAULT_SANDBOX_CLAUSE),


                //Testing excluded mime type is not sandboxed
                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, JS_MIME,
                        XML_MIME,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        EMPTY_SANDBOX_CLAUSE),

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, XML_MIME,
                        XML_MIME,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        EMPTY_SANDBOX_CLAUSE),


                //Testing Content-Disposition is correctly sandboxed
                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_DISPOSITION, "attachment;others",
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        DEFAULT_SANDBOX_CLAUSE),

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_DISPOSITION, "others;attachment",
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        DEFAULT_SANDBOX_CLAUSE),

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_DISPOSITION, "inline;other",
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        EMPTY_SANDBOX_CLAUSE),

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_DISPOSITION, "other;inline",
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        EMPTY_SANDBOX_CLAUSE),


                // Testing content-disposition can be included/excluded
                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_DISPOSITION, "attachment;other",
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        "",
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        EMPTY_SANDBOX_CLAUSE),

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_DISPOSITION, "inline;other",
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        "inline",
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        DEFAULT_SANDBOX_CLAUSE),


                // Testing other browser add postfixes to attachments
                Arguments.of(true,
                        CHROME_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        "sandbox allow-same-origin"),

                Arguments.of(true,
                        CHROME_ANDROID_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        "sandbox allow-same-origin"),

                Arguments.of(true,
                        CHROME_IOS_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        "sandbox allow-scripts"),

                Arguments.of(true,
                        SAFARI_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        "sandbox allow-scripts"),

                Arguments.of(true,
                        SAFARI_IOS_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        "sandbox allow-scripts"),


                // Testing postfixes are not applied for other paths

                Arguments.of(true,
                        CHROME_DESKTOP_UA,
                        NON_ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        DEFAULT_SANDBOX_CLAUSE),

                Arguments.of(true,
                        SAFARI_DESKTOP_UA,
                        NON_ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        DEFAULT_SANDBOX_CLAUSE),


                // Testing reaction on browser clause property changed

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        "CHROME_DESKTOP:allow-same-origin;FIREFOX_DESKTOP:allow-popups",
                        "sandbox allow-popups"),

                Arguments.of(true,
                        SAFARI_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        "CHROME_DESKTOP:allow-same-origin",
                        DEFAULT_SANDBOX_CLAUSE),


                // Testing reaction on new differentiated path added

                Arguments.of(true,
                        CHROME_DESKTOP_UA,
                        NON_ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        NON_ATTACHMENT_PATH,
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        "sandbox allow-same-origin"),
                Arguments.of(true,
                        CHROME_DESKTOP_UA,
                        "/path/two",
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        "/path/one;/path/two",
                        DEFAULT_DIFFERENTIATED_CLAUSES,
                        "sandbox allow-same-origin"),


                // Testing it works with more User-Agents
                Arguments.of(true,
                        EDGE_LEGACY_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        "EDGE_LEGACY_DESKTOP:allow-same-origin",
                        "sandbox allow-same-origin"),

                Arguments.of(true,
                        UNKNOWN_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        "OTHER:allow-same-origin",
                        "sandbox allow-same-origin"),


                // Testing if it doesn't explode with malformed browser differentiation
                Arguments.of(true,
                        EDGE_LEGACY_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        "",
                        DEFAULT_SANDBOX_CLAUSE),

                Arguments.of(true,
                        EDGE_LEGACY_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        "CHROME_DESKTOP:;EDGE_LEGACY_DESKTOP:allow-same-origin",
                        "sandbox allow-same-origin"),

                Arguments.of(true,
                        EDGE_LEGACY_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        "FIREFOX_DESKTOP;EDGE_LEGACY_DESKTOP:allow-same-origin",
                        "sandbox allow-same-origin"),

                Arguments.of(true,
                        FIREFOX_DESKTOP_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        "FIREFOX;EDGE_LEGACY:allow-same-origin",
                        DEFAULT_SANDBOX_CLAUSE),

                Arguments.of(true,
                        UNKNOWN_UA,
                        ATTACHMENT_PATH,
                        CONTENT_TYPE, MP4_MIME,
                        DEFAULT_EXCLUDED_CONTENT_TYPE,
                        DEFAULT_INCLUDED_CONTENT_DISPOSITION,
                        DEFAULT_ADDITIONAL_PATHS,
                        "STH:allow-same-origin;NOT_KNOWN:allow-popups",
                        "sandbox allow-same-origin allow-popups")
        );
    }

    private static Stream<Arguments> testCasesContentTypeOnly() {
        return testCases().filter(argument -> argument.get()[3].equals(CONTENT_TYPE));
    }

    @ParameterizedTest(name = TEST_DESCRIPTION)
    @MethodSource("testCases")
    void setHeadersTest(boolean enabled,
                        String userAgent,
                        String servletPath,
                        String header,
                        String value,
                        String excludedContentTypes,
                        String includedContentDisposition,
                        String browserDifferentiatedPaths,
                        String browserDifferentiatedClauses,
                        String sandboxValue) {
        prepareStubs(enabled, userAgent, servletPath, excludedContentTypes, includedContentDisposition, browserDifferentiatedPaths, browserDifferentiatedClauses);
        cspWrapper = DynamicContentSecurityPolicyResponseWrapperFactory.getWrapper(response, request);


        cspWrapper.setHeader(header, value);

        verify(response).setHeader(header, value);
        verifySandboxIsSetCorrectly(sandboxValue);
    }

    @ParameterizedTest(name = TEST_DESCRIPTION)
    @MethodSource("testCases")
    void addHeadersTest(boolean enabled,
                        String userAgent,
                        String servletPath,
                        String header,
                        String value,
                        String excludedContentTypes,
                        String includedContentDisposition,
                        String browserDifferentiatedPaths,
                        String browserDifferentiatedClauses,
                        String sandboxValue) {
        prepareStubs(enabled, userAgent, servletPath, excludedContentTypes, includedContentDisposition, browserDifferentiatedPaths, browserDifferentiatedClauses);
        cspWrapper = DynamicContentSecurityPolicyResponseWrapperFactory.getWrapper(response, request);


        cspWrapper.addHeader(header, value);

        verify(response).addHeader(header, value);
        verifySandboxIsSetCorrectly(sandboxValue);
    }

    @ParameterizedTest(name = TEST_DESCRIPTION)
    @MethodSource("testCasesContentTypeOnly")
    void setContentTypeTest(boolean enabled,
                            String userAgent,
                            String servletPath,
                            String header,
                            String value,
                            String excludedContentTypes,
                            String includedContentDisposition,
                            String browserDifferentiatedPaths,
                            String browserDifferentiatedClauses,
                            String sandboxValue) {
        prepareStubs(enabled, userAgent, servletPath, excludedContentTypes, includedContentDisposition, browserDifferentiatedPaths, browserDifferentiatedClauses);
        cspWrapper = DynamicContentSecurityPolicyResponseWrapperFactory.getWrapper(response, request);


        cspWrapper.setContentType(value);

        verify(response).setContentType(value);
        verifySandboxIsSetCorrectly(sandboxValue);
    }

    private void prepareStubs(boolean enabled,
                              String userAgent,
                              String servletPath,
                              String excludedContentTypes,
                              String includedContentDisposition,
                              String browserDifferentiatedPaths,
                              String browserDifferentiatedClauses) {
        when(featureManager.isEnabled(ENABLE_SANDBOX_CONTENT_SECURITY_POLICY)).thenReturn(enabled);
        lenient().when(request.getServletPath()).thenReturn(servletPath);
        lenient().when(request.getHeader(USER_AGENT)).thenReturn(userAgent);
        doReturn(excludedContentTypes).when(applicationProperties).getDefaultBackedString(EXCLUDED_CONTENT_TYPES);
        doReturn(includedContentDisposition).when(applicationProperties).getDefaultBackedString(INCLUDED_CONTENT_DISPOSITION);
        doReturn(browserDifferentiatedPaths).when(applicationProperties).getDefaultBackedString(BROWSER_DIFFERENTIATED_PATHS);
        doReturn(browserDifferentiatedClauses).when(applicationProperties).getDefaultBackedString(BROWSER_DIFFERENTIATED_CLAUSES);
    }

    private void verifySandboxIsSetCorrectly(String sandboxValue) {
        if (sandboxValue.isEmpty()) {
            verify(response, never()).setHeader(eq(CONTENT_SECURITY_POLICY), anyString());
        } else {
            verify(response).setHeader(CONTENT_SECURITY_POLICY, sandboxValue);
        }
    }

    @Test
    public void worksWhenApplicationPropertiesThrowExceptionValueTest() {
        when(featureManager.isEnabled(ENABLE_SANDBOX_CONTENT_SECURITY_POLICY)).thenReturn(true);
        when(request.getServletPath()).thenReturn(ATTACHMENT_PATH);
        when(applicationProperties.getDefaultBackedString(EXCLUDED_CONTENT_TYPES))
                .thenThrow(LazyReference.InitializationException.class);

        cspWrapper = DynamicContentSecurityPolicyResponseWrapperFactory.getWrapper(response, request);

        cspWrapper.setContentType(XML_MIME);

        verify(response).setContentType(XML_MIME);
        verify(response).setHeader(CONTENT_SECURITY_POLICY, "sandbox");
    }
}
