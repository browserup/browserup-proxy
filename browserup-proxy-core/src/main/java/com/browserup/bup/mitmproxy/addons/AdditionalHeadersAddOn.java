package com.browserup.bup.mitmproxy.addons;

public class AdditionalHeadersAddOn extends AbstractAddon {
  private static final String ADDITIONAL_HEADERS_ADDON_FILE = "additional_headers.py";

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
