package com.browserup.bup.proxy.rest.validation

import com.browserup.bup.rest.validation.PatternConstraint
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

import javax.validation.ConstraintValidatorContext

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock

class PatternConstraintTest {

    @Test
    void validPattern() {
        def validator = new PatternConstraint.PatternValidator()
        def pattern = ".*"
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)

        def result = validator.isValid(pattern, mockedContext)

        Assert.assertTrue("Expected pattern validation to pass", result)
    }

    @Test
    void invalidPattern() {
        def validator = new PatternConstraint.PatternValidator()
        def pattern = "["
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)

        def result = validator.isValid(pattern, mockedContext)

        Assert.assertFalse("Expected pattern validation to fail", result)
    }

}
