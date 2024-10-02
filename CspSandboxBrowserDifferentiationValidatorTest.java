package com.atlassian.jira.web.filters.steps.security.csp;

import com.atlassian.validation.Validator.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CspSandboxBrowserDifferentiationValidatorTest {

    CspSandboxBrowserDifferentiationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CspSandboxBrowserDifferentiationValidator();
    }

    private static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of("", true),
                Arguments.of("CHROME_DESKTOP:allow-same-origin", true),
                Arguments.of("CHROME_DESKTOP:allow-same-origin allow-scripts", true),
                Arguments.of("CHROME_DESKTOP:allow-scripts;CHROME_ANDROID:allow-scripts", true),
                Arguments.of("CHROME_DESKTOP:allow-same-origin allow-scripts;CHROME_IOS:allow-scripts allow-popups", true),
                Arguments.of("ANDROID:allow-same-origin;CHROME_IOS:allow-scripts allow-popups", true),
                Arguments.of("FIREFOX_DESKTOP:allow-popups;FIREFOX_DESKTOP:allow-same-origin", true),
                Arguments.of("OTHER:allow-popups", true),
                Arguments.of("EDGE_LEGACY_DESKTOP:allow-forms", true),
                Arguments.of("CHROME_DESKTOP:allow-same-origin       allow-scripts;SAFARI_IOS:allow-scripts allow-popups", false),
                Arguments.of("CHROME_DESKTOP:allow-same-origin;;SAFARI_DESKTOP:allow-scripts allow-popups", false),
                Arguments.of("CHROME_DESKTOP: ", false),
                Arguments.of("CHROME_DESKTOP:", false),
                Arguments.of("CHROME_IOS CHROME_DESKTOP:allow-same-origin", false),
                Arguments.of("CHROME_DESKTOP: ;SAFARI_DESKTOP:allow-popups", false),
                Arguments.of("CHROME_DESKTOP:;SAFARI_IOS:allow-popups", false),
                Arguments.of("SOMETHING:allow-same-origin", false),
                Arguments.of("SAFARI_DESKTOP:crazy-clause", false)
        );
    }

    @ParameterizedTest(name = "{index}. Is parameter value {0} valid? {1}")
    @MethodSource("testCases")
    void UnknownClauseIsIncorrect(String clause, boolean valid) {
        Result result = validator.validate(clause);

        assertEquals(valid, result.isValid());
    }
}