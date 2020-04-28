package com.browserup.bup.mitmproxy.addons;

public class RewriteUrlAddOn extends AbstractAddon {
  private static final String REWRITE_URL_ADDON_FILE = "rewrite_url.py";

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath()
    };
  }

  @Override
  public String getAddOnFileName() {
    return REWRITE_URL_ADDON_FILE;
  }
}
