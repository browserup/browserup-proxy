package net.lightbody.bmp.core.har;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HarContent {
    private volatile long size;
    private volatile Long compression;
    private volatile String mimeType = "";
    private volatile String text;
    private volatile String encoding;
    private volatile String comment = "";

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Long getCompression() {
        return compression;
    }

    public void setCompression(Long compression) {
        this.compression = compression;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
