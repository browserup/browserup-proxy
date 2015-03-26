package net.lightbody.bmp.proxy;

import java.util.regex.Pattern;

public class RewriteRule {
    private final Pattern match;
    private final String replace;

    public RewriteRule(String match, String replace) {
        this.match = Pattern.compile(match);
        this.replace = replace;
    }

    public Pattern getMatch() {
        return match;
    }

    public String getReplace() {
        return replace;
    }
}
