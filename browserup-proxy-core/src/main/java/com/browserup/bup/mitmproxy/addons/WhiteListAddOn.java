package com.browserup.bup.mitmproxy.addons;

public class WhiteListAddOn extends AbstractAddon {
  private static final String WHITE_LIST_ADDON_FILE = "white_list.py";

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath()
    };
  }

  @Override
  public String getAddOnFileName() {
    return WHITE_LIST_ADDON_FILE;
  }
}
