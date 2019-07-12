package com.browserup.bup.rest.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

public class LoggingFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        LOG.info(String.format("%s /%s", requestContext.getMethod().toUpperCase(), requestContext.getUriInfo().getPath()));
    }
}
