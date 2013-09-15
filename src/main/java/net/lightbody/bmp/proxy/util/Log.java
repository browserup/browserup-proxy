package net.lightbody.bmp.proxy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log {
    protected Logger logger;
    private String className;

    public Log() {
        Exception e = new Exception();
        className = e.getStackTrace()[1].getClassName();
        logger = LoggerFactory.getLogger(className);
    }

    public Log(Class clazz) {
        className = clazz.getName();
        logger = LoggerFactory.getLogger(className);
    }

    public void severe(String msg, Throwable e) {
        logger.error(msg, e);
    }

    public void severe(String msg, Object... args) {
        logger.error(msg, args);
    }

    public void severe(String msg, Throwable e, Object... args) {
        logger.error(msg, e, args);
    }

    public RuntimeException severeAndRethrow(String msg, Throwable e, Object... args) {
        logger.error(msg, e, args);

        //noinspection ThrowableInstanceNeverThrown
        return new RuntimeException(new java.util.Formatter().format(msg, args).toString());
    }

    public void warn(String msg, Throwable e) {
        logger.warn(msg, e);
    }

    public void warn(String msg, Object... args) {
        logger.warn(msg, args);
    }

    public void warn(String msg, Throwable e, Object... args) {
        logger.warn(msg, e, args);
    }

    public void info(String msg, Throwable e) {
        logger.info(msg, e);
    }

    public void info(String msg, Object... args) {
        logger.info(msg, args);
    }

    public void info(String msg, Throwable e, Object... args) {
        logger.info(msg, e, args);
    }

    public void fine(String msg, Throwable e) {
        logger.debug(msg, e);
    }

    public void fine(String msg, Object... args) {
        logger.debug(msg, args);
    }

    public void fine(String msg, Throwable e, Object... args) {
        logger.debug(msg, e, args);
    }
}