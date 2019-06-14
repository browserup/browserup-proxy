package com.browserup.bup.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Path("/hello")
public class FooResource {

    public FooResource() {
        System.out.println();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() {
        return "served by jersey";
    }
}