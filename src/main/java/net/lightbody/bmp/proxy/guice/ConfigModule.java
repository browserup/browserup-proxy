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
import net.sf.uadetector.service.UADetectorServiceFactory;

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
        
        ArgumentAcceptingOptionSpec<Integer> proxyPortRange =
                parser.accepts("proxyPortRange", "The range of ports to use for proxies")
                      .withOptionalArg()
                      .ofType(Integer.class)
                      .defaultsTo(8081, 8581)
                      .withValuesSeparatedBy('-');

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
        binder.bind(Key.get(Integer.class, new NamedImpl("minPort"))).toInstance(minPort);
        binder.bind(Key.get(Integer.class, new NamedImpl("maxPort"))).toInstance(maxPort);   

        /*
         * Init User Agent String Parser, update of the UAS datastore will run in background.
         */
        UADetectorServiceFactory.getCachingAndUpdatingParser();
    }
}
