package org.java_bandwidthlimiter;

import java.io.IOException;

public class MaximumTransferExceededException extends IOException {    
	private final boolean isUpstream;
    private final long limit;
    
    public boolean isUpstream() {
        return isUpstream;
    }

	public long getLimit() {
		return limit;
	}
    
	public MaximumTransferExceededException(long limit, boolean isUpstream) {
		super("Maximum " + (isUpstream? "upstream" : "downstream") + " transfer allowance of " + limit + " KB exceeded.");
        this.isUpstream = isUpstream;
		this.limit = limit;
	}
}
