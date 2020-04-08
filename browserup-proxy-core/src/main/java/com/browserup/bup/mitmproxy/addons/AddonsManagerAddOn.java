package com.browserup.bup.mitmproxy.addons;

public class AddonsManagerAddOn extends AbstractAddon {
  private static final String HAR_DUMP_ADD_ON_FILE_NAME = "bu_addons_manager.py";
  private final int port;

  public AddonsManagerAddOn(int port) {
    this.port = port;
  }

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath(),
            "--set", "addons_management_port=" + port
    };
  }

  @Override
  public String getAddOnFileName() {
    return HAR_DUMP_ADD_ON_FILE_NAME;
  }
}
