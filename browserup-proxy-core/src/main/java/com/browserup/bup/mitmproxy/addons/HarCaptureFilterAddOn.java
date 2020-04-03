package com.browserup.bup.mitmproxy.addons;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class HarCaptureFilterAddOn extends AbstractAddon {
  private static final String HAR_DUMP_ADD_ON_FILE_NAME = "har_dump.py";

  private File harDumpFile = null;

  @Override
  public String[] getCommandParams() {
    initHarDumpFile();

    String[] command = new String[]{
        "-s", "/home/kirill/dev/epic/bu/browserup-proxy/browserup-proxy-core/src/main/resources/mitmproxy/har_dump.py",
        "--set", "hardump=" + harDumpFile.getAbsolutePath()
    };

    return command;
  }

  @Override
  public String getAddOnFileName() {
    return HAR_DUMP_ADD_ON_FILE_NAME;
  }

  public Optional<File> getHarDumpFile() {
    return Optional.ofNullable(harDumpFile);
  }

  private void initHarDumpFile() {
    try {
      harDumpFile = File.createTempFile("har_dump.json", ".tmp");
    } catch (IOException ex) {
      throw new RuntimeException("Couldn't create har dump file", ex);
    }
  }
}
