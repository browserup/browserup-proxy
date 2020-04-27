package com.browserup.bup.mitmproxy.addons;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class HarCaptureAddOn extends AbstractAddon {
  private static final String HAR_DUMP_ADD_ON_FILE_NAME = "har_dump.py";

  private File harDumpFile = null;

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath()
    };
  }

  @Override
  public String getAddOnFileName() {
    return HAR_DUMP_ADD_ON_FILE_NAME;
  }

  public Optional<File> getHarDumpFile() {

    return Optional.ofNullable(harDumpFile);
  }
}
