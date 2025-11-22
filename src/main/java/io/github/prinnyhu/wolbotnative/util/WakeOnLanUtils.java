package io.github.prinnyhu.wolbotnative.util;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class WakeOnLanUtils {

    private static final int DEFAULT_BROADCAST_PORT = 9;

    public static boolean checkMacAddress(String macAddress) {
        String[] macAddressParts = macAddress.split(":");
        if (macAddressParts.length != 6) {
            return false;
        }
        for (String part : macAddressParts) {
            if (part.length() != 2) {
                return false;
            }
            if (!part.matches("[0-9a-fA-F]+")) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkBroadcastAddress(String broadcastAddress) {
        String[] ipParts = broadcastAddress.split("\\.");
        if (ipParts.length != 4) {
            return false;
        }
        for (String part : ipParts) {
            if (part.length() > 3) {
                return false;
            }
            if (!part.matches("[0-9]+")) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkBroadcastPort(Integer broadcastPort) {
        if (broadcastPort == null) {
            return false;
        }
        if (broadcastPort < 0 || broadcastPort > 65535) {
            return false;
        }
        return true;
    }

    public static void sendWolPacket(String macAddress, String broadcastAddress, Integer broadcastPort) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] magicData = buildMagicPacketData(getMacAddressBytes(macAddress));
            socket.send(new DatagramPacket(magicData, magicData.length, InetAddress.getByName(broadcastAddress), broadcastPort == null ? DEFAULT_BROADCAST_PORT : broadcastPort));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] getMacAddressBytes(String macAddress) {
        String[] macAddressParts = macAddress.split(":");
        assert macAddressParts.length == 6;
        byte[] macAddressBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            macAddressBytes[i] = (byte) Integer.parseInt(macAddressParts[i], 16);
        }
        return macAddressBytes;
    }

    private static byte[] buildMagicPacketData(byte[] macAddressBytes) {
        byte[] packet = new byte[6 + 16 * 6];
        for (int i = 0; i < 6; i++) {
            packet[i] = (byte) 0xff;
        }
        for (int i = 0; i < 16; i++) {
            System.arraycopy(macAddressBytes, 0, packet, 6 + i * 6, 6);
        }
        return packet;
    }

}
