package com.browserup.bup.proxy.mitmproxy.validation

import com.browserup.bup.rest.validation.LongPositiveConstraint
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

import javax.validation.ConstraintValidatorContext

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock

class LongPositiveConstraintTest {

    @Test
    void validLongPositive() {
        def validator = new LongPositiveConstraint.LongPositiveValidator()
        def value = "10000"
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)

        def result = validator.isValid(value, mockedContext)

        Assert.assertTrue("Expected long positive validation to pass", result)
    }

    @Test
    void invalidLongNegative() {
        def validator = new LongPositiveConstraint.LongPositiveValidator()
        def value = "-1"
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)

        def result = validator.isValid(value, mockedContext)

        Assert.assertFalse("Expected long positive validation to fail", result)
    }

    @Test
    void invalidLongFormat() {
        def validator = new LongPositiveConstraint.LongPositiveValidator()
        def value = "invalid_format"
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)

        def result = validator.isValid(value, mockedContext)

        Assert.assertFalse("Expected long positive validation to fail", result)
    }

}
