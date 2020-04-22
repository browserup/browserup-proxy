package com.browserup.bup.mitmproxy.addons;

public class BlackListAddOn extends AbstractAddon {
  private static final String BLACK_LIST_ADDON_FILE = "black_list.py";

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath()
    };
  }

  @Override
  public String getAddOnFileName() {
    return BLACK_LIST_ADDON_FILE;
  }
}
