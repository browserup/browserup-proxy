package net.lightbody.bmp.proxy.guice;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import java.io.IOException;
import static java.util.Arrays.asList;
import java.util.List;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class ConfigModule implements Module {
    private String[] args;

    public ConfigModule(String[] args) {
        this.args = args;
    }

    @Override
    public void configure(Binder binder) {
        OptionParser parser = new OptionParser();

        ArgumentAcceptingOptionSpec<Integer> portSpec =
                parser.accepts("port", "The port to listen on")
                        .withOptionalArg().ofType(Integer.class).defaultsTo(8080);
        
        ArgumentAcceptingOptionSpec<String> addressSpec =
                parser.accepts("address", "The address to bind to")
                      .withOptionalArg()
                      .ofType(String.class)
                      .defaultsTo("0.0.0.0");
        
        ArgumentAcceptingOptionSpec<Integer> proxyPortRange =
                parser.accepts("proxyPortRange", "The range of ports to use for proxies")
                      .withOptionalArg()
                      .ofType(Integer.class)
                      .defaultsTo(8081, 8581)
                      .withValuesSeparatedBy('-');

        ArgumentAcceptingOptionSpec<Integer> ttlSpec =
                parser.accepts("ttl", "Time in seconds until an unused proxy is deleted")
                      .withOptionalArg()
                      .ofType(Integer.class)
                      .defaultsTo(0);

        parser.acceptsAll(asList("help", "?"), "This help text");

        OptionSet options = parser.parse(args);

        if (options.has("?")) {
            try {
                parser.printHelpOn(System.out);
                System.exit(0);
            } catch (IOException e) {
                // should never happen, but...
                e.printStackTrace();
            }
            return;
        }
        
        List<Integer> ports = options.valuesOf(proxyPortRange); 
        if(ports.size() < 2){
            throw new IllegalArgumentException();
        }
        Integer minPort;
        Integer maxPort;        
        if(ports.get(1) > ports.get(0)){
            minPort = ports.get(0);
            maxPort = ports.get(1);
        }else{
            minPort = ports.get(1);
            maxPort = ports.get(0);
        }   
        Integer port = portSpec.value(options);
        if(port >= minPort && port <= maxPort){
            int num = maxPort - minPort;
            minPort = port + 1;
            maxPort = minPort + num;
        }

        binder.bind(Key.get(Integer.class, new NamedImpl("port"))).toInstance(port);
        binder.bind(Key.get(String.class, new NamedImpl("address"))).toInstance(addressSpec.value(options));
        binder.bind(Key.get(Integer.class, new NamedImpl("minPort"))).toInstance(minPort);
        binder.bind(Key.get(Integer.class, new NamedImpl("maxPort"))).toInstance(maxPort);                 
        binder.bind(Key.get(Integer.class, new NamedImpl("ttl"))).toInstance(ttlSpec.value(options));
        
        /*
         * Init User Agent String Parser, update of the UAS datastore will run in background.
         */
        // temporarily disabled because user-agent-string.info no longer exists
        //BrowserMobHttpClient.getUserAgentStringParser();
    }
}
