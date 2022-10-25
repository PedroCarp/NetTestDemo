package com.rt.nettestdemo.wifi;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtils {
    /**
     * Convert a IPv4 address from an integer to an InetAddress.
     *
     * @param hostAddress
     *            an int corresponding to the IPv4 address in network byte order
     */
    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte) (0xff & hostAddress), (byte) (0xff & (hostAddress >> 8)),
                (byte) (0xff & (hostAddress >> 16)), (byte) (0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

    /**
     * Convert a IPv4 address from an InetAddress to an integer
     *
     * @param inetAddr
     *            is an InetAddress corresponding to the IPv4 address
     * @return the IP address as an integer in network byte order
     */
    public static int inetAddressToInt(InetAddress inetAddr) throws IllegalArgumentException {
        byte[] addr = inetAddr.getAddress();
        return ((addr[3] & 0xff) << 24) | ((addr[2] & 0xff) << 16) | ((addr[1] & 0xff) << 8) | (addr[0] & 0xff);
    }
    @TargetApi(Build.VERSION_CODES.M)
    public static boolean isNetworkOnline(Context context) {
        boolean isOnline = false;
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());  // need ACCESS_NETWORK_STATE permission
            isOnline = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isOnline;
    }

}
