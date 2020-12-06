package com.browserup.bup.mitmproxy.addons;

public class BlockListAddOn extends AbstractAddon {
  private static final String BLOCK_LIST_ADDON_FILE = "block_list.py";

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath()
    };
  }

  @Override
  public String getAddOnFileName() {
    return BLOCK_LIST_ADDON_FILE;
  }
}
