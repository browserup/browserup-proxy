package net.lightbody.bmp.filters;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import net.lightbody.bmp.exception.JavascriptCompilationException;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Convenience class that executes arbitrary javascript code as a {@link RequestFilter} or {@link ResponseFilter}.
 */
public class JavascriptRequestResponseFilter implements RequestFilter, ResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(JavascriptRequestResponseFilter.class);

    private static final ScriptEngine JAVASCRIPT_ENGINE = new ScriptEngineManager().getEngineByName("JavaScript");

    private CompiledScript compiledRequestFilterScript;
    private CompiledScript compiledResponseFilterScript;

    public void setRequestFilterScript(String script) {
        Compilable compilable = (Compilable) JAVASCRIPT_ENGINE;
        try {
            compiledRequestFilterScript = compilable.compile(script);
        } catch (ScriptException e) {
            throw new JavascriptCompilationException("Unable to compile javascript. Script in error:\n" + script, e);
        }
    }

    public void setResponseFilterScript(String script) {
        Compilable compilable = (Compilable) JAVASCRIPT_ENGINE;
        try {
            compiledResponseFilterScript = compilable.compile(script);
        } catch (ScriptException e) {
            throw new JavascriptCompilationException("Unable to compile javascript. Script in error:\n" + script, e);
        }
    }

    @Override
    public HttpResponse filterRequest(HttpRequest request, HttpMessageContents contents, HttpMessageInfo messageInfo) {
        if (compiledRequestFilterScript == null) {
            return null;
        }

        Bindings bindings = JAVASCRIPT_ENGINE.createBindings();
        bindings.put("request", request);
        bindings.put("contents", contents);
        bindings.put("messageInfo", messageInfo);
        bindings.put("log", log);

        try {
            Object retVal = compiledRequestFilterScript.eval(bindings);
            // avoid implicit javascript returns
            if (retVal instanceof HttpResponse) {
                return (HttpResponse) retVal;
            } else {
                return null;
            }
        } catch (ScriptException e) {
            log.error("Could not invoke filterRequest using supplied javascript", e);

            return null;
        }
    }

    @Override
    public void filterResponse(HttpResponse response, HttpMessageContents contents, HttpMessageInfo messageInfo) {
        if (compiledResponseFilterScript == null) {
            return;
        }

        Bindings bindings = JAVASCRIPT_ENGINE.createBindings();
        bindings.put("response", response);
        bindings.put("contents", contents);
        bindings.put("messageInfo", messageInfo);
        bindings.put("log", log);
        try {
            compiledResponseFilterScript.eval(bindings);
        } catch (ScriptException e) {
            log.error("Could not invoke filterResponse using supplied javascript", e);
        }
    }
}
