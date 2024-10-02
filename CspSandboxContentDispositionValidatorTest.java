package com.atlassian.jira.web.filters.steps.security.csp;

import com.atlassian.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CspSandboxContentDispositionValidatorTest {

    CspSandboxContentDispositionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CspSandboxContentDispositionValidator();
    }

    @Test
    void emptyContentDispositionIsCorrect() {
        Validator.Result result = validator.validate("");

        assertTrue(result.isValid());
    }

    @Test
    void attachmentContentDispositionIsCorrect() {
        Validator.Result result = validator.validate("attachment");

        assertTrue(result.isValid());
    }

    @Test
    void inlineContentDispositionIsCorrect() {
        Validator.Result result = validator.validate("inline");

        assertTrue(result.isValid());
    }

    @Test
    void bothContentDispositionIsCorrect() {
        Validator.Result result = validator.validate("inline;attachment");

        assertTrue(result.isValid());
    }

    @Test
    void contentDispositionBadListIsIncorrect() {
        Validator.Result result = validator.validate("inline;attachment;");

        assertFalse(result.isValid());
    }

    @Test
    void contentDispositionPartiallyInvalidListIsIncorrect() {
        Validator.Result result = validator.validate("inline;invalid");

        assertFalse(result.isValid());
    }

    @Test
    void contentDispositionDuplicatesIsIncorrect() {
        Validator.Result result = validator.validate("inline;inline");

        assertFalse(result.isValid());
    }

    @Test
    void invalidContentDispositionIsIncorrect() {
        Validator.Result result = validator.validate("invalid");

        assertFalse(result.isValid());
    }
}