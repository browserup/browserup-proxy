package com.browserup.bup.mitmproxy.addons;

public class AllowListAddOn extends AbstractAddon {
  private static final String ALLOW_LIST_ADDON_FILE = "allow_list.py";

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath()
    };
  }

  @Override
  public String getAddOnFileName() {
    return ALLOW_LIST_ADDON_FILE;
  }
}
