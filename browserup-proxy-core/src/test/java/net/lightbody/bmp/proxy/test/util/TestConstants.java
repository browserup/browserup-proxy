package net.lightbody.bmp.proxy.test.util;

import com.google.common.collect.ImmutableList;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Convenience class for test constants.
 */
public class TestConstants {
    public static final InetAddress addressOnes;
    public static final InetAddress addressTwos;
    public static final List<InetAddress> addressOnesList;
    public static final List<InetAddress> addressTwosList;

    static {
        try {
            addressOnes = InetAddress.getByName("1.1.1.1");
            addressTwos = InetAddress.getByName("2.2.2.2");
            addressOnesList = ImmutableList.of(TestConstants.addressOnes);
            addressTwosList = ImmutableList.of(TestConstants.addressTwos);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
