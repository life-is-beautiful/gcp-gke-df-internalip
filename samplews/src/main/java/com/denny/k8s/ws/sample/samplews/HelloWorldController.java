package com.denny.k8s.ws.sample.samplews;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

@Controller
@RequestMapping("/hello-world")
public class HelloWorldController {
    private static final String template = "Hello, %s! Server IP is: %s and Local Hostname is: %s";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping(method=RequestMethod.GET)
    public @ResponseBody Greeting sayHello(@RequestParam(value="name", required=false, defaultValue="Stranger") String name) {
        Enumeration e = null;
        String ip = "n/a";
        String hostname = "n/a";
        try {
            e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while(ee.hasMoreElements()) {
                    InetAddress i = (InetAddress) ee.nextElement();

                    if (i instanceof Inet6Address) continue;

                    ip = i.getHostAddress();
                    hostname = i.getLocalHost().getHostName();
                    break;
                }
            }

        } catch (Exception e1) {
            e1.printStackTrace();
        }


        return new Greeting(counter.incrementAndGet(), String.format(template, name, ip, hostname));
    }

}
