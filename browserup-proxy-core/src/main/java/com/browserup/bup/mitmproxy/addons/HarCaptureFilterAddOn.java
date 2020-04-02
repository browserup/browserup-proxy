package com.browserup.bup.mitmproxy.addons;

import com.browserup.bup.mitmproxy.MitmProxyManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class HarCaptureFilterAddOn implements Addon {
  private static final Object LOCK = new Object();
  private static final String HAR_DUMP_ADD_ON_FILE_PATH = "/mitmproxy/har_dump.py";

  private static File harDumpAddOn = null;
  private File harDumpFile = null;

  public HarCaptureFilterAddOn() {
    try {
      if (harDumpAddOn == null) {
        synchronized (LOCK) {
          if (harDumpAddOn == null) {
            loadHarDumpAddOn();
          }
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException("Couldn't initialize mitm proxy manager", ex);
    }
  }

  private static void loadHarDumpAddOn() throws Exception {
    String harDumpAddOnString = new String(Files.readAllBytes(
        Paths.get(MitmProxyManager.class.getResource(HAR_DUMP_ADD_ON_FILE_PATH).toURI())));
    harDumpAddOn = File.createTempFile("har_dump.py", ".tmp");

    Files.write(harDumpAddOn.toPath(), harDumpAddOnString.getBytes());
  }

  @Override
  public String getCommandParam() {
    try {
      harDumpFile = File.createTempFile("har_dump.json", ".tmp");
    } catch (IOException ex) {
      throw new RuntimeException("Couldn't create har dump file", ex);
    }

    String[] command = new String[]{
        " -s", harDumpAddOn.getAbsolutePath(),
        "--set", "hardump=" + harDumpFile.getAbsolutePath()
    };

    return String.join("", command);
  }

  public Optional<File> getHarDumpFile() {
    return Optional.ofNullable(harDumpFile);
  }
}
