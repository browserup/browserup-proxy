package net.lightbody.bmp.proxy;

import java.util.regex.Pattern;

public class BlacklistEntry {
	private final Pattern pattern;
	private final int responseCode;
	private final Pattern method;

	/**
	 * Creates a new BlacklistEntry with no HTTP method matching (i.e. all methods will match).
	 * 
	 * @param pattern URL pattern to blacklist
	 * @param responseCode response code to return for blacklisted URL
	 */
	public BlacklistEntry(String pattern, int responseCode) {
		this(pattern, responseCode, null);
	}
	
	/**
	 * Creates a new BlacklistEntry which will match both a URL and an HTTP method
	 * 
	 * @param pattern URL pattern to blacklist
	 * @param responseCode response code to return for blacklisted URL
	 * @param method HTTP method to match (e.g. GET, PUT, PATCH, etc.)
	 */
	public BlacklistEntry(String pattern, int responseCode, String method) {
		this.pattern = Pattern.compile(pattern);
		this.responseCode = responseCode;
		if (method == null || method.isEmpty()) {
			this.method = null;
		} else {
			this.method = Pattern.compile(method);
		}
	}
	
	/**
	 * Determines if this BlacklistEntry matches the given URL. Attempts to match both the URL and the 
	 * HTTP method.
	 * 
	 * @param url possibly-blacklisted URL
	 * @param httpMethod HTTP method this URL is being accessed with
	 * @return true if the URL matches this BlacklistEntry
	 */
	public boolean matches(String url, String httpMethod) {
		if (method != null) {
			return pattern.matcher(url).matches() && method.matcher(httpMethod).matches();
		} else {
			return pattern.matcher(url).matches();
		}
	}

	public Pattern getPattern() {
		return this.pattern;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public Pattern getMethod() {
		return method;
	}

}