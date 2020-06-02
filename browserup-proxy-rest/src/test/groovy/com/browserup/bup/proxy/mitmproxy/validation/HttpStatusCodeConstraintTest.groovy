package com.browserup.bup.proxy.mitmproxy.validation

import com.browserup.bup.rest.validation.HttpStatusCodeConstraint
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

import javax.validation.ConstraintValidatorContext

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock

class HttpStatusCodeConstraintTest  {

    @Test
    void validStatus() {
        def validator = new HttpStatusCodeConstraint.HttpStatusCodeValidator()
        def status = "400"
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)

        def result = validator.isValid(status, mockedContext)

        Assert.assertTrue("Expected http status validation to pass", result)
    }

    @Test
    void inValidStatusFormat() {
        def validator = new HttpStatusCodeConstraint.HttpStatusCodeValidator()
        def status = "400invalid"
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)

        def result = validator.isValid(status, mockedContext)

        Assert.assertFalse("Expected http status validation to fail", result)
    }

    @Test
    void inValidStatusRange() {
        def validator = new HttpStatusCodeConstraint.HttpStatusCodeValidator()
        def status = "699"
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)

        Mockito.when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)

        def result = validator.isValid(status, mockedContext)

        Assert.assertFalse("Expected http status validation to fail", result)
    }

}
