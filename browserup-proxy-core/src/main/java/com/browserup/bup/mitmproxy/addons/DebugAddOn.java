package com.browserup.bup.mitmproxy.addons;

import java.io.File;
import java.util.Optional;

public class DebugAddOn extends AbstractAddon {
  private static final String DEBUG_ADDON_FILE_NAME = "remote_debug.py";

  private File harDumpFile = null;

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath()
    };
  }

  @Override
  public String getAddOnFileName() {
    return DEBUG_ADDON_FILE_NAME;
  }

  public Optional<File> getHarDumpFile() {

    return Optional.ofNullable(harDumpFile);
  }
}
