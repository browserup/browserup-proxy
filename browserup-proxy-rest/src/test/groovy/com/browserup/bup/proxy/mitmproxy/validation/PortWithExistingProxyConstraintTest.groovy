package com.browserup.bup.proxy.mitmproxy.validation

import com.browserup.bup.BrowserUpProxyServer
import com.browserup.bup.MitmProxyServer
import com.browserup.bup.proxy.MitmProxyManager
import com.browserup.bup.proxy.ProxyManager
import com.browserup.bup.rest.validation.PortWithExistingProxyConstraint
import org.junit.Assert
import org.junit.Test

import javax.validation.ConstraintValidatorContext

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PortWithExistingProxyConstraintTest {

    @Test
    void validPort() {
        def port = 10
        def mockedProxyManager = mock(MitmProxyManager)
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)
        def validator = new PortWithExistingProxyConstraint.PortWithExistingProxyConstraintValidator(mockedProxyManager)

        when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)
        when(mockedProxyManager.get(eq(port))).thenReturn(mock(MitmProxyServer))

        def result = validator.isValid(port, mockedContext)

        Assert.assertTrue("Expected port validation to pass", result)
    }

    @Test
    void invalidPort() {
        def port = 10
        def nonExistingProxyPort = 100
        def mockedProxyManager = mock(MitmProxyManager)
        def validator = new PortWithExistingProxyConstraint.PortWithExistingProxyConstraintValidator(mockedProxyManager)
        def mockedContext = mock(ConstraintValidatorContext)
        def mockedBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder)
        def mockedCustomContext = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext)

        when(mockedContext.buildConstraintViolationWithTemplate(any(String))).thenReturn(mockedBuilder)
        when(mockedBuilder.addPropertyNode(anyString())).thenReturn(mockedCustomContext)
        when(mockedProxyManager.get(eq(nonExistingProxyPort))).thenReturn(mock(MitmProxyServer))

        def result = validator.isValid(port, mockedContext)

        Assert.assertFalse("Expected port validation to fail", result)
    }

}
