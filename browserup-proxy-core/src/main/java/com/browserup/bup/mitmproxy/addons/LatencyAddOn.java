package com.browserup.bup.mitmproxy.addons;

public class LatencyAddOn extends AbstractAddon {
  private static final String ADDITIONAL_HEADERS_ADDON_FILE = "latency.py";

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath()
    };
  }

  @Override
  public String getAddOnFileName() {
    return ADDITIONAL_HEADERS_ADDON_FILE;
  }
}
