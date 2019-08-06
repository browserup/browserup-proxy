package com.browserup.bup.rest.openapi;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Customizes generation of the following OpenAPI data:
 * - OperationID: generates operation id using scheme: {last URL path element of resource}{resource method}
 * - Info version: uses version properties file generated while gradle build to get current version of the project
 */
public class CustomOpenApiReader extends Reader {
    private static final String VERSION_PROPERTIES_FILE_NAME = "browserup-proxy-rest-version.properties";
    private static final String DEFAULT_VERSION = "2.0.0";
    private static final Logger LOG = LoggerFactory.getLogger(CustomOpenApiReader.class);

    private static String version;

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

    @Override
    public OpenAPI read(Class<?> cls, String parentPath, String parentMethod, boolean isSubresource, RequestBody parentRequestBody, ApiResponses parentResponses, Set<String> parentTags, List<Parameter> parentParameters, Set<Class<?>> scannedResources) {
        OpenAPI result = super.read(cls, parentPath, parentMethod, isSubresource, parentRequestBody, parentResponses, parentTags, parentParameters, scannedResources);
        result.getInfo().setVersion(getVersion());
        return result;
    }

    private Optional<String> createOperationIdPrefixByPathAnnotation(String pathAnnoValue) {
        String[] pathElements = pathAnnoValue.split("/");
        if (pathElements.length > 0) {
            return Optional.of(pathElements[pathElements.length - 1]);
        }
        return Optional.empty();
    }

    private String getVersion() {
        if (version == null) {
            synchronized (CustomOpenApiReader.class) {
                if (version == null) {
                    version = readVersion().orElse(DEFAULT_VERSION);
                }
            }
        }
        return version;
    }

    private Optional<String> readVersion() {
        InputStream in = CustomOpenApiReader.class.getClassLoader().getResourceAsStream(VERSION_PROPERTIES_FILE_NAME);
        if (in == null) {
            LOG.warn("Couldn't read version properties, resource not found by path: " + VERSION_PROPERTIES_FILE_NAME);
            return Optional.empty();
        }

        Properties properties = new Properties();
        try {
            properties.load(in);
            Object version = properties.get("version");
            if (version == null) {
                LOG.warn("Couldn't read version properties (version is null)");
            } else if (!(version instanceof String)) {
                LOG.warn("Couldn't read version properties (version is not String)");
            } else if (StringUtils.isEmpty((CharSequence) version)) {
                LOG.warn("Couldn't read version properties (version is empty)");
            } else {
                return Optional.of((String) version);
            }
        } catch (IOException e) {
            LOG.warn("Couldn't read version properties", e);
        }
        return Optional.empty();
    }
}
