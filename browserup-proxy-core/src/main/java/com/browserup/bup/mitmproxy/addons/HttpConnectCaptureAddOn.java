package com.browserup.bup.mitmproxy.addons;

public class HttpConnectCaptureAddOn extends AbstractAddon {
  private static final String HTTP_CONNECT_CAPTURE_ADDON_FILE = "http_connect_capture.py";

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath()
    };
  }

  @Override
  public String getAddOnFileName() {
    return HTTP_CONNECT_CAPTURE_ADDON_FILE;
  }
}
