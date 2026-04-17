package com.huobao.drama.service.seedance;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class VolcSignUtil {

    public static Map<String, String> sign(String method, String host, String path,
                                            Map<String, String> queryParams,
                                            byte[] body, String ak, String sk,
                                            String region, String service) throws Exception {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String xDate = sdf.format(now);
        String shortDate = xDate.substring(0, 8);

        if (body == null) body = new byte[0];
        String xContentSha256 = sha256Hex(body);
        String contentType = "application/json";

        // Sorted query string
        TreeMap<String, String> sortedQuery = new TreeMap<>(queryParams);
        StringBuilder querySB = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedQuery.entrySet()) {
            if (querySB.length() > 0) querySB.append("&");
            querySB.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
        }

        // Canonical request
        String signedHeaders = "content-type;host;x-content-sha256;x-date";
        String canonicalRequest = method + "\n" + path + "\n" + querySB + "\n"
                + "content-type:" + contentType + "\n"
                + "host:" + host + "\n"
                + "x-content-sha256:" + xContentSha256 + "\n"
                + "x-date:" + xDate + "\n"
                + "\n"
                + signedHeaders + "\n"
                + xContentSha256;

        // String to sign
        String credentialScope = shortDate + "/" + region + "/" + service + "/request";
        String stringToSign = "HMAC-SHA256\n" + xDate + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        // Signing key
        byte[] kDate = hmacSha256(sk.getBytes(StandardCharsets.UTF_8), shortDate);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        byte[] kSigning = hmacSha256(kService, "request");

        String signature = HexFormat.of().formatHex(hmacSha256(kSigning, stringToSign));

        String authorization = "HMAC-SHA256 Credential=" + ak + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", host);
        headers.put("X-Date", xDate);
        headers.put("X-Content-Sha256", xContentSha256);
        headers.put("Content-Type", contentType);
        headers.put("Authorization", authorization);
        return headers;
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(data));
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }
}
