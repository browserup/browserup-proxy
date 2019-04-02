package com.browserup.bup.proxy.rest.validation

import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.proxy.ProxyManager
import com.browserup.bup.rest.validation.PatternConstraint
import com.browserup.bup.rest.validation.PortWithExistingProxyConstraint
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

import javax.validation.ConstraintValidatorContext

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PortWithExistingProxyConstraintTest {

    @Test
    void validPort() {
        def port = 10
        def mockedProxyManager = mock(ProxyManager)
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)
        def validator = new PortWithExistingProxyConstraint.PortWithExistingProxyConstraintValidator(mockedProxyManager)

        when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)
        when(mockedProxyManager.get(eq(port))).thenReturn(mock(BrowserUpProxyServer))

        def result = validator.isValid(port, mockedContext)

        Assert.assertTrue("Expected port validation to pass", result)
    }

    @Test
    void invalidPort() {
        def port = 10
        def nonExistingProxyPort = 100
        def mockedProxyManager = mock(ProxyManager)
        def validator = new PortWithExistingProxyConstraint.PortWithExistingProxyConstraintValidator(mockedProxyManager)
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)
        def mockedCustomContext = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext)

        when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)
        when(mockedBuilder.addPropertyNode(anyString())).thenReturn(mockedCustomContext)
        when(mockedProxyManager.get(eq(nonExistingProxyPort))).thenReturn(mock(BrowserUpProxyServer))

        def result = validator.isValid(port, mockedContext)

        Assert.assertFalse("Expected port validation to fail", result)
    }

}
