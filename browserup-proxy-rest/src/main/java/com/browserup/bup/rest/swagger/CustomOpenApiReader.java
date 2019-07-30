package com.browserup.bup.rest.swagger;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.lang.reflect.Method;
import java.util.*;

public class CustomOpenApiReader extends Reader {
    @Override
    protected String getOperationId(String operationId) {
        return super.getOperationId(operationId);
    }

    @Override
    public Operation parseMethod(Method method, List<Parameter> globalParameters, Produces methodProduces, Produces classProduces, Consumes methodConsumes, Consumes classConsumes, List<SecurityRequirement> classSecurityRequirements, Optional<ExternalDocumentation> classExternalDocs, Set<String> classTags, List<Server> classServers, boolean isSubresource, RequestBody parentRequestBody, ApiResponses parentResponses, JsonView jsonViewAnnotation, ApiResponse[] classResponses, AnnotatedMethod annotatedMethod) {
        Operation operation = super.parseMethod(method, globalParameters, methodProduces, classProduces, methodConsumes, classConsumes, classSecurityRequirements, classExternalDocs, classTags, classServers, isSubresource, parentRequestBody, parentResponses, jsonViewAnnotation, classResponses, annotatedMethod);

        Arrays.stream(method.getDeclaringClass().getAnnotations())
                .filter(Path.class::isInstance)
                .filter(path -> StringUtils.isNotEmpty(((Path) path).value()))
                .findFirst()
                .map(path -> ((Path) path).value())
                .flatMap(this::createOperationIdPrefixByPathAnnotation)
                .map(operationIdPrefix -> {
                    return operationIdPrefix.equals(method.getName()) ?
                            method.getName() :
                            operationIdPrefix + StringUtils.capitalize(method.getName());
                })
                .ifPresent(operation::setOperationId);

        return operation;
    }

    private Optional<String> createOperationIdPrefixByPathAnnotation(String pathAnnoValue) {
        String[] pathElements = pathAnnoValue.split("/");
        if (pathElements.length > 0) {
            return Optional.of(pathElements[pathElements.length - 1]);
        }
        return Optional.empty();
    }
}
