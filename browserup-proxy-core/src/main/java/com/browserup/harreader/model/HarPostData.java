package com.browserup.harreader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Information about POST data.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/#postData">specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarPostData {

    private String mimeType;
    private List<HarPostDataParam> params = new ArrayList<>();
    private String text;
    private String comment;

    /**
     * @return MIME type of posted data, null if not present.
     */
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return List of posted params.
     */
    public List<HarPostDataParam> getParams() {
        if (params == null) {
            params = new ArrayList<>();
        }
        return params;
    }

    public void setParams(List<HarPostDataParam> params) {
        this.params = params;
    }

    /**
     * @return Plain text posted data, null if not present.
     */
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return Comment provided by the user or application, null if not present.
     */
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HarPostData that = (HarPostData) o;
        return Objects.equals(mimeType, that.mimeType) &&
                Objects.equals(params, that.params) &&
                Objects.equals(text, that.text) &&
                Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mimeType, params, text, comment);
    }
}
