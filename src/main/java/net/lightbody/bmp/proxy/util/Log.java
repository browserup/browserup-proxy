package net.lightbody.bmp.proxy.util;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log {
    protected Logger logger;
    private String className;

    public Log() {
        Exception e = new Exception();
        className = e.getStackTrace()[1].getClassName();
        logger = Logger.getLogger(className);
    }

    public Log(Class clazz) {
        className = clazz.getName();
        logger = Logger.getLogger(className);
    }

    public void severe(String msg, Throwable e) {
        log(Level.SEVERE, msg, e);
    }

    public void severe(String msg, Object... args) {
        log(Level.SEVERE, msg, args);
    }

    public void severe(String msg, Throwable e, Object... args) {
        log(Level.SEVERE, msg, e, args);
    }

    public RuntimeException severeAndRethrow(String msg, Throwable e, Object... args) {
        log(Level.SEVERE, msg, e, args);

        //noinspection ThrowableInstanceNeverThrown
        return new RuntimeException(new java.util.Formatter().format(msg, args).toString());
    }

    public void warn(String msg, Throwable e) {
        log(Level.WARNING, msg, e);
    }

    public void warn(String msg, Object... args) {
        log(Level.WARNING, msg, args);
    }

    public void warn(String msg, Throwable e, Object... args) {
        log(Level.WARNING, msg, e, args);
    }

    public void info(String msg, Throwable e) {
        log(Level.INFO, msg, e);
    }

    public void info(String msg, Object... args) {
        log(Level.INFO, msg, args);
    }

    public void info(String msg, Throwable e, Object... args) {
        log(Level.INFO, msg, e, args);
    }

    public void fine(String msg, Throwable e) {
        log(Level.FINE, msg, e);
    }

    public void fine(String msg, Object... args) {
        log(Level.FINE, msg, args);
    }

    public void fine(String msg, Throwable e, Object... args) {
        log(Level.FINE, msg, e, args);
    }

    private void log(Level level, String msg, Throwable e) {
        logger.log(level, msg, e);
    }

    private void log(Level level, String msg, Object... args) {
        logger.log(level, msg, args);
    }

    private void log(Level level, String msg, Throwable e, Object... args) {
        LogRecord lr = new LogRecord(level, msg);
        lr.setThrown(e);
        lr.setParameters(args);
        lr.setSourceMethodName("");
        lr.setSourceClassName(className);
        lr.setLoggerName(className);
        logger.log(lr);
    }
}