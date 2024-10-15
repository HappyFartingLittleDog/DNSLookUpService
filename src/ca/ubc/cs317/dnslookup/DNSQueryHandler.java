package ca.ubc.cs317.dnslookup;

import java.io.IOException;
import java.io.SyncFailedException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DNSQueryHandler {

    private static final int DEFAULT_DNS_PORT = 53;
    private static DatagramSocket socket;
    private static boolean verboseTracing = false;
    private static int index;

    private static final Random random = new Random();

    /**
     * Sets up the socket and set the timeout to 5 seconds
     *
     * @throws SocketException if the socket could not be opened, or if there was an
     *                         error with the underlying protocol
     */
    public static void openSocket() throws SocketException {
        socket = new DatagramSocket();
        socket.setSoTimeout(5000);
    }

    /**
     * Closes the socket
     */
    public static void closeSocket() {
        socket.close();
    }

    /**
     * Set verboseTracing to tracing
     */
    public static void setVerboseTracing(boolean tracing) {
        verboseTracing = tracing;
    }

    /**
     * Builds the query, sends it to the server, and returns the response.
     *
     * @param message Byte array used to store the query to DNS servers.
     * @param server  The IP address of the server to which the query is being sent.
     * @param node    Host and record type to be used for search.
     * @return A DNSServerResponse Object containing the response buffer and the
     * transaction ID.
     * @throws IOException if an IO Exception occurs
     */
    public static DNSServerResponse buildAndSendQuery(byte[] message, InetAddress server,
                                                      DNSNode node) throws IOException {
        try {
            short id = (short) random.nextInt(Short.MAX_VALUE + 1);
            byte[] idArray = ByteBuffer.allocate(2).putShort(id).array();
            message[0] = idArray[0];
            message[1] = idArray[1];
            for (int i = 2; i < 12; i++) {
                message[i] = 0x00;
                if (i == 5) // QDCount
                    message[i] = 0x01;
            }

            int currIndex = 12;
            String[] domainNameArray = node.getHostName().split("[.]");
            for (String domainName : domainNameArray) {

                message[currIndex++] = (byte) domainName.length();
                byte[] name = domainName.getBytes();
                for (int i = 0; i < name.length; i++) {
                    message[currIndex++] = name[i];
                }
            }

            message[currIndex++] = 0x00;

            byte[] typeArray = ByteBuffer.allocate(2).putShort((byte) node.getType().getCode()).array();
            message[currIndex++] = typeArray[0];
            message[currIndex++] = typeArray[1];
            message[currIndex++] = 0x00;
            message[currIndex++] = 0x01;

            if (verboseTracing) {
                System.out.println();
                System.out.println("Query ID     " + id + " " + node.getHostName() + "  " + node.getType() + " --> "
                        + server.getHostAddress());
            }
            DatagramPacket dnsRequest = new DatagramPacket(message, currIndex, server, DEFAULT_DNS_PORT);
            openSocket();
            socket.send(dnsRequest);
            byte[] responseBuf = new byte[1024];
            DatagramPacket dnsResponse = new DatagramPacket(responseBuf, responseBuf.length);
            socket.receive(dnsResponse);

            ByteBuffer response = ByteBuffer.wrap(responseBuf);
            int responseID = bytesToInt(responseBuf[0], responseBuf[1]);
            byte[] byteArray = new byte[]{responseBuf[3], responseBuf[4]};
            int RCODE = ByteBuffer.wrap(byteArray).getShort() & 0xFF;
            if (RCODE == 3 || RCODE == 5) {
                throw new IOException();
            }
            return new DNSServerResponse(response, responseID);
        }
        catch (SocketTimeoutException ste) {
            throw new IOException();
        }

    }

    /**
     * Decodes the DNS server response and caches it.
     *
     * @param transactionID  Transaction ID of the current communication with the
     *                       DNS server
     * @param responseBuffer DNS server's response
     * @param cache          To store the decoded server's response
     * @return A set of resource records corresponding to the name servers of the
     * response.
     */
    public static Set<ResourceRecord> decodeAndCacheResponse(int transactionID, ByteBuffer responseBuffer,
                                                             DNSCache cache) {

        boolean isAuthoritative = ( responseBuffer.get(2) & 0x4) == 4;
        int ANcount = bytesToInt(responseBuffer.get(6), responseBuffer.get(7));
        int NScount = bytesToInt(responseBuffer.get(8), responseBuffer.get(9));
        int ARcount = bytesToInt(responseBuffer.get(10), responseBuffer.get(11));

        Set<ResourceRecord> responses = new HashSet<>();
        int currindex = 12;
        String Qname = getDomain(currindex, responseBuffer);
        int Qtype = bytesToInt(responseBuffer.get(index++), responseBuffer.get(index++));
        int Qclass = bytesToInt(responseBuffer.get(index++), responseBuffer.get(index++));

        if (verboseTracing) {
            System.out.println("Response ID: " + transactionID + " " + "Authoritative" + " = " + isAuthoritative);
        }
        if (verboseTracing) {
            System.out.println("  Answers (" + ANcount + ")");
        }
        decodeMessage(responseBuffer, cache, responses, ANcount);
        if (verboseTracing) {
            System.out.println("  Name Servers (" + NScount + ")");
        }
        decodeMessage(responseBuffer, cache, responses, NScount);
        if (verboseTracing) {
            System.out.println("  Additional Information (" + ARcount + ")");
        }
        decodeMessage(responseBuffer, cache, responses, ARcount);

        return responses;
    }

    /**
     * decode response buffer, cache into cache, and return qualified answer to response
     *
     * @param responseBuffer
     * @param cache
     * @param responses
     * @param count
     */
    private static void decodeMessage(ByteBuffer responseBuffer, DNSCache cache, Set<ResourceRecord> responses,
                                      int count) {
        while (count > 0) {
            String name = getDomain(index, responseBuffer);
            int type = bytesToInt(responseBuffer.get(index++), responseBuffer.get(index++));
            int dataClass = bytesToInt(responseBuffer.get(index++), responseBuffer.get(index++));
            byte[] byteArray = new byte[]{responseBuffer.get(index++), responseBuffer.get(index++),
                    responseBuffer.get(index++), responseBuffer.get(index++)};
            int ttl = ByteBuffer.wrap(byteArray).getInt();

            int rdLength = bytesToInt(responseBuffer.get(index++), responseBuffer.get(index++));

            ResourceRecord rr = null;
            if (type == RecordType.A.getCode()) {
                String address = getAddressIPv4(index, responseBuffer);
                try {
                    rr = new ResourceRecord(name, RecordType.getByCode(type), (long) ttl,
                            InetAddress.getByName(address));
                } catch (UnknownHostException uhe) {
                    System.out.println(uhe);
                }
            } else if (type == RecordType.NS.getCode()) {
                String textResult = getDomain(index, responseBuffer);
                rr = new ResourceRecord(name, RecordType.getByCode(type), (long) ttl, textResult);
                responses.add(rr);
            } else if (type == RecordType.CNAME.getCode()) {
                String textResult = getDomain(index, responseBuffer);
                rr = new ResourceRecord(name, RecordType.getByCode(type), (long) ttl, textResult);
            } else if (type == RecordType.AAAA.getCode()) {
                String address = getAddressIPv6(index, responseBuffer);
                try {
                    rr = new ResourceRecord(name, RecordType.getByCode(type), (long) ttl,
                            InetAddress.getByName(address));
                } catch (UnknownHostException uhe) {
                    System.out.println(uhe);
                }
            } else if (type == RecordType.SOA.getCode()) {
                rr = new ResourceRecord(name, RecordType.getByCode(type), (long) ttl,
                        "----");
            } else {
                return;
            }
            cache.addResult(rr);
            verbosePrintResourceRecord(rr, type);
            count--;
        }
    }

    /**
     * parse IPv4 Address from buffer
     *
     * @param currIndex
     * @param responseBuffer
     * @return
     */
    public static String getAddressIPv4(int currIndex, ByteBuffer responseBuffer) {
        StringBuilder sb = new StringBuilder();
        int addressLength = 4;
        for (int i = 0; i < addressLength; i++) {
            sb.append(responseBuffer.get(currIndex + i) & 0xFF);
            if (i < addressLength - 1)
                sb.append('.');
        }
        index = currIndex + addressLength;
        return sb.toString();
    }

    /**
     * parse IPv6 Address from buffer
     *
     * @param currIndex
     * @param responseBuffer
     * @return
     */
    public static String getAddressIPv6(int currIndex, ByteBuffer responseBuffer) {
        StringBuilder sb = new StringBuilder();
        int addressLength = 16;
        for (int i = 0; i < addressLength; i += 2) {
            byte[] temp = new byte[]{responseBuffer.get(currIndex + i), responseBuffer.get(currIndex + i + 1)};
            sb.append(Integer.toHexString(ByteBuffer.wrap(temp).getShort() & 0xFFFF));
            if (i < addressLength - 3)
                sb.append(':');
        }
        index = currIndex + addressLength;
        return sb.toString();
    }

    /**
     * parse domain from buffer
     *
     * @param currIndex
     * @param responseBuffer
     * @return
     */
    public static String getDomain(int currIndex, ByteBuffer responseBuffer) {
        StringBuilder QNameSb = new StringBuilder();
        int recordIndex = currIndex;
        while (responseBuffer.get(currIndex) != 0) {
            if (responseBuffer.get(currIndex) >> 6 == -1) {
                recordIndex = currIndex;
                byte[] bytes = new byte[]{(byte) (responseBuffer.get(currIndex) & 0x3F),
                        responseBuffer.get(currIndex + 1)};
                int newIndex = ByteBuffer.wrap(bytes).getShort();
                QNameSb.append(getDomain(newIndex, responseBuffer));
                break;
            } else {
                byte[] string = new byte[responseBuffer.get(currIndex)];
                for (int i = currIndex + 1; i < currIndex + responseBuffer.get(currIndex) + 1; i++) {
                    string[i - currIndex - 1] = responseBuffer.get(i);
                }
                String s = new String(string, StandardCharsets.UTF_8);
                currIndex = currIndex + responseBuffer.get(currIndex) + 1;
                QNameSb.append(s + ".");
            }
        }
        if (responseBuffer.get(recordIndex) >> 6 != -1) {
            if (QNameSb.length() > 0) {
                QNameSb.deleteCharAt(QNameSb.length() - 1);
            }
            index = currIndex + 1;
        } else {
            index = recordIndex + 2;
        }
        return QNameSb.toString();
    }

    /**
     * convert Bytes to Integer
     *
     * @param byteOne
     * @param byteTwo
     * @return
     */
    public static int bytesToInt(byte byteOne, byte byteTwo) {
        byte[] byteArray = new byte[]{byteOne, byteTwo};
        int integer = ByteBuffer.wrap(byteArray).getShort() & 0xFFFF;
        return integer;
    }

    /**
     * Formats and prints record details (for when trace is on)
     *
     * @param record The record to be printed
     * @param rtype  The type of the record to be printed
     */
    private static void verbosePrintResourceRecord(ResourceRecord record, int rtype) {
        if (verboseTracing)
            System.out.format("       %-30s %-10d %-4s %s\n", record.getHostName(),
                    record.getTTL(),
                    record.getType() == RecordType.OTHER ? rtype : record.getType(),
                    record.getTextResult());
    }
}
