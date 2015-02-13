package net.lightbody.bmp.proxy.util;

import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.service.UADetectorServiceFactory;

/**
 * General utility class for functionality and classes used mostly internally by BrowserMob Proxy.
 */
public class BrowserMobProxyUtil {
    /**
     * Singleton User Agent parser.
     */
    private static volatile UserAgentStringParser parser;

    private static final Object PARSER_INIT_LOCK = new Object();

    /**
     * Retrieve the User Agent String Parser. Create the parser if it has not yet been initialized.
     * 
     * @return singleton UserAgentStringParser object
     */
    public static UserAgentStringParser getUserAgentStringParser() {
        if (parser == null) {
            synchronized (PARSER_INIT_LOCK) {
                if (parser == null) {
                    // using resourceModuleParser for now because user-agent-string.info no longer exists. the updating
                    // parser will get incorrect data and wipe out its entire user agent repository.
                    parser = UADetectorServiceFactory.getResourceModuleParser();
                }
            }
        }

        return parser;
    }

}
