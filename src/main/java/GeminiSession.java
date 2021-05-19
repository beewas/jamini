import javax.net.ssl.*;
import java.io.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class GeminiSession {

    private static final String algorithm = "TLSv1.2";
    private final int port;
    private String currentAddress = "";
    private final Stack<String> history = new Stack<>();

    public GeminiSession (int port) {
        this.port = port;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }

    public String peekPrevSite() {
        return history.peek();
    }

    public List<String> getContent(String address) {
        history.push(currentAddress);
        System.out.println("Current: " + currentAddress + " Address: " + address);
        StringBuilder proceed = new StringBuilder();
        if (address.startsWith("gemini://")) {
            String[] lines = address.substring(9).split("/+");
            proceed.append(lines[0]).append("/");
            for (int i = 1; i < lines.length; i++){
                if (!lines[i].contains(".")) {
                    proceed.append(lines[i]).append("/");
                } else proceed.append(lines[i]);
            }
        } else if (address.startsWith("./")) {
            String[] lines = currentAddress.split("/+");
            lines[lines.length - 1] = address.substring(2);
            for (int i = 1; i < lines.length; i++){
                if (!lines[i].contains(".")) {
                    proceed.append(lines[i]).append("/");
                } else proceed.append(lines[i]);
            }
        } else if (address.startsWith("/")) {
            proceed.append(currentAddress).append(address.substring(1));
        } else {
            proceed.append(currentAddress).append(address);
        }

        address = proceed.toString();
        System.out.println("Address: " + address);
        currentAddress = address;
        List<String> output = new ArrayList<>();

        try {
            SSLSocketFactory factory = getSSLContextWithoutCert().getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(address.split("/+")[0], port);
            socket.setEnabledProtocols(new String[]{algorithm});
            socket.startHandshake();

            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));

            out.println("gemini://" + address + "\r\n");
            out.flush();

            if (out.checkError())
                System.out.println("SSLSocketClient:  java.io.PrintWriter error");

            /* read response */
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) if (!inputLine.startsWith("20")) {
                output.add(inputLine);
            }

            in.close();
            out.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    public String getPrevSite() {
        String out = "gemini://" + history.pop();
        currentAddress = history.pop();
        return out;
    }

    private static SSLContext getSSLContextWithoutCert() throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext context = SSLContext.getInstance(algorithm);
        TrustManager[] trustManager = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certificate, String str) {}

                    public void checkServerTrusted(X509Certificate[] certificate, String str) {}
                }
        };
        context.init(null, trustManager, new SecureRandom());
        return context;
    }
}