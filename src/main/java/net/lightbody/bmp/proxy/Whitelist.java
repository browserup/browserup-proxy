package net.lightbody.bmp.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A URL whitelist. An empty whitelist is disabled by default. This object is immutable and the list of matching
 * patterns is unmodifiable after creation. Enabling, disabling, or modifying the whitelist can be safely and easily
 * accomplished by updating the whitelist reference to a new whitelist.
 */
public class Whitelist {
    private final Collection<Pattern> patterns;
    private final int responseCode;
    private final boolean enabled;

    /**
     * A disabled Whitelist.
     */
    public static final Whitelist WHITELIST_DISABLED = new Whitelist();
    
    /**
     * Creates an empty, disabled Whitelist.
     */
    public Whitelist() {
    	this.patterns = Collections.emptyList();
		this.responseCode = -1;
    	this.enabled = false;
    }
    
	/**
	 * Creates an empty, enabled whitelist with the specified response code.
	 * 
	 * @param responseCode the response code that the (enabled) Whitelist will return for all URLs.
	 */
	public Whitelist(int responseCode) {
		this.patterns = Collections.emptyList();
		this.responseCode = responseCode;
		this.enabled = true;
	}
	
	/**
	 * Creates an whitelist for the specified patterns, returning the given responseCode when a URL does not match one of the patterns.
	 *  
	 * @param patterns
	 * @param responseCode
	 */
	public Whitelist(String[] patterns, int responseCode) {
		List<Pattern> patternList = new ArrayList<Pattern>(patterns.length);
		
        for (String pattern : patterns) {
            patternList.add(Pattern.compile(pattern));
        }
        
        this.patterns = Collections.unmodifiableList(patternList);
        
        this.responseCode = responseCode;
        
        this.enabled = true;
    }
	
    public boolean isEnabled() {
		return enabled;
	}

    public Collection<Pattern> getPatterns() {
        return this.patterns;
    }

    public int getResponseCode() {
        return this.responseCode;
    }
}
