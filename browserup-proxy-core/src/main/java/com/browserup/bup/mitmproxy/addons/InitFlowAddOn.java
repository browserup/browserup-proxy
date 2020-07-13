package com.browserup.bup.mitmproxy.addons;

public class InitFlowAddOn extends AbstractAddon {
  private static final String INIT_FLOW_ADDON_FILE = "init_flow.py";

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath()
    };
  }

  @Override
  public String getAddOnFileName() {
    return INIT_FLOW_ADDON_FILE;
  }
}
