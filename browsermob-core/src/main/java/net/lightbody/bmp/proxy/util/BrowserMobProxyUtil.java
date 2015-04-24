package net.lightbody.bmp.proxy.util;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;
import net.lightbody.bmp.core.har.HarPage;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.service.UADetectorServiceFactory;

import java.util.HashSet;
import java.util.Set;

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

    /**
     * Copies {@link HarEntry} and {@link HarPage} references from the specified har to a new har copy, up to and including
     * the specified pageRef. Does not perform a "deep copy", so any subsequent modification to the entries or pages will
     * be reflected in the copied har.
     *
     * @param har existing har to copy
     * @param pageRef last page ID to copy
     * @return copy of a {@link Har} with entries and pages from the original har, or null if the input har is null
     */
    public static Har copyHarThroughPageRef(Har har, String pageRef) {
        if (har == null) {
            return null;
        }

        if (har.getLog() == null) {
            return new Har();
        }

        // collect the page refs that need to be copied to new har copy.
        Set<String> pageRefsToCopy = new HashSet<String>();

        for (HarPage page : har.getLog().getPages()) {
            pageRefsToCopy.add(page.getId());

            if (pageRef.equals(page.getId())) {
                break;
            }
        }

        HarLog logCopy = new HarLog();

        // copy every entry and page in the HarLog that matches a pageRefToCopy. since getEntries() and getPages() return
        // lists, we are guaranteed that we will iterate through the pages and entries in the proper order
        for (HarEntry entry : har.getLog().getEntries()) {
            if (pageRefsToCopy.contains(entry.getPageref())) {
                logCopy.addEntry(entry);
            }
        }

        for (HarPage page : har.getLog().getPages()) {
            if (pageRefsToCopy.contains(page.getId())) {
                logCopy.addPage(page);
            }
        }

        Har harCopy = new Har();
        harCopy.setLog(logCopy);

        return harCopy;
    }

}
