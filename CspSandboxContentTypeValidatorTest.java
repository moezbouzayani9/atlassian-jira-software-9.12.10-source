package com.atlassian.jira.web.filters.steps.security.csp;

import com.atlassian.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CspSandboxContentTypeValidatorTest {

    CspSandboxContentTypeValidator validator;

    @BeforeEach
    void setup() {
        validator = new CspSandboxContentTypeValidator();
    }

    @Test
    void emptyIsValidatedAsCorrect() {
        Validator.Result result = validator.validate("");
        assertTrue(result.isValid());
    }

    @Test
    void mimeTypeIsValidatedAsCorrect() {
        Validator.Result result = validator.validate("application/json");
        assertTrue(result.isValid());
    }

    @Test
    void mimeTypeListIsValidatedAsCorrect() {
        Validator.Result result = validator.validate("application/json;text/html");
        assertTrue(result.isValid());
    }

    @Test
    void mimeTypeListIsValidatedAsIncorrect() {
        Validator.Result result = validator.validate("application/json;text/html;");
        assertFalse(result.isValid());
    }

    @Test
    void mimeTypeListIsValidatedAsIncorrect_2() {
        Validator.Result result = validator.validate("application/json;texthtml");
        assertFalse(result.isValid());
    }

    @Test
    void mimeTypeDuplicatedValidatedAsIncorrect() {
        Validator.Result result = validator.validate("application/json;application/json");
        assertFalse(result.isValid());
    }

    @Test
    void badMimeTypeIsValidatedAsIncorrect() {
        Validator.Result result = validator.validate("applicationjson");
        assertFalse(result.isValid());
    }

    @Test
    void badMimeTypeIsValidatedAsIncorrect_2() {
        Validator.Result result = validator.validate("application/.json");
        assertFalse(result.isValid());
    }
}