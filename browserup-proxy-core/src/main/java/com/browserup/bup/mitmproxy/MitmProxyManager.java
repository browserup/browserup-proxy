package com.browserup.bup.mitmproxy;

import com.browserup.bup.mitmproxy.addons.AbstractAddon;
import com.browserup.bup.mitmproxy.addons.HarCaptureFilterAddOn;
import com.browserup.harreader.model.Har;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MitmProxyManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(MitmProxyManager.class);

  private StartedProcess startedProcess = null;
  private HarCaptureFilterAddOn harCaptureFilterAddOn = new HarCaptureFilterAddOn();
  private Integer port = 0;
  private Har har = new Har();
  private Process process;

  private PipedInputStream pipedInputStream;

  private MitmProxyManager() {}

  public static MitmProxyManager getInstance() {
    return new MitmProxyManager();
  }

  public void start(int port) {
    try {
      startProxy(port);
      this.port = port;
    } catch (Exception ex) {
      LOGGER.error("Failed to start proxy", ex);
      stop();
      throw ex;
    }
  }

  public Integer getPort() {
    return port;
  }

  public void stop() {
    try {
      pipedInputStream.close();
    } catch (IOException e) {
      LOGGER.warn("Couldn't close piped input stream", e);
    }
    startedProcess.getProcess().destroy();
    //startedProcess.getProcess().destroy();
  }

  private long getPidOfProcess(Process p) {
    long pid = -1;

    try {
      if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
        Field f = p.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        pid = f.getLong(p);
        f.setAccessible(false);
      }
    } catch (Exception e) {
      pid = -1;
    }
    return pid;
  }

  private void startProxy(int port) {
    List<String> command = new ArrayList<String>() {{
      add("mitmdump");
      add("-p");
      add(String.valueOf(port));
    }};
    command.addAll(Arrays.asList(harCaptureFilterAddOn.getCommandParams()));

    LOGGER.info("Starting proxy using command: " + String.join(" ", command));

    this.pipedInputStream = new PipedInputStream();
    ProcessExecutor processExecutor;
    try {
      processExecutor = new ProcessExecutor(command)
          .readOutput(true)
          .redirectOutput(Slf4jStream.ofCaller().asInfo())
          .redirectOutputAlsoTo(new PipedOutputStream(pipedInputStream));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      startedProcess = processExecutor.start();
    } catch (Exception ex) {
      throw new RuntimeException("Couldn't start mitmproxy process", ex);
    }

    StringBuilder output = new StringBuilder();
    readOutputOfMimtproxy(pipedInputStream, output);

    try {
      Awaitility.await().
          atMost(5, TimeUnit.SECONDS).
          until(() -> output.toString().contains("Proxy server listening"));
    } catch (ConditionTimeoutException ex) {
      LOGGER.error("MitmProxy haven't started properly, output: " + output);
      throw new RuntimeException("Mitmproxy haven't started properly, output: " + output);
    }
  }

  private void readOutputOfMimtproxy(PipedInputStream pipedInputStream, StringBuilder output) {
    new Thread(() -> {
      BufferedReader reader = new BufferedReader(new InputStreamReader(pipedInputStream));
      Awaitility.await().atMost(5, TimeUnit.SECONDS).until(reader::ready);

      while (true) {
        try {
          if (!reader.ready())
            break;
          output.append(reader.readLine());
        } catch (IOException e) {
          LOGGER.error("Error while reading output of mitmproxy", e);
        }
      }
    }).start();
  }

  public Har getHar() {
    stop();

    File harFile = harCaptureFilterAddOn.getHarDumpFile().get();

    Har har;
    try {
      har = new ObjectMapper().readerFor(Har.class).readValue(harFile);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't read HAR file", e);
    }

    start(port);

    return har;
  }
}
