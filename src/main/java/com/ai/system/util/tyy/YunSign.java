package com.ai.system.util.tyy;

import com.ai.system.model.pojo.TyyResponse;
import com.ai.system.model.pojo.TyyResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

public class YunSign {
    private String url;
    private String body;
    private Map dataMap;
    private String ak;
    private String sk;
    private String uuId;
    private int temp;
    private String contentType;
    private String queryStr;
    private Map<String, Object> headerMap;
    private String afterQuery;
    private Map<String, Object> fileMap;

    public YunSign() {
    }

    public YunSign(String url, String ak, String sk, String uuId, String body, int temp, String contentType, String queryStr, Map<String, Object> headerMap) {
        this.url = url;
        if (body.equals("{}")) {
            body = "";
        }

        this.body = body;
        this.ak = ak;
        this.sk = sk;
        this.uuId = uuId;
        this.temp = temp;
        this.contentType = contentType;
        this.queryStr = queryStr;
        this.headerMap = headerMap;
    }

    public YunSign(String url, String ak, String sk, String uuId, Map<String, Object> fileMap, int temp, String queryStr, Map<String, Object> headerMap, Map dataMap) {
        this.url = url;
        this.fileMap = fileMap;
        this.ak = ak;
        this.sk = sk;
        this.uuId = uuId;
        this.temp = temp;
        this.queryStr = queryStr;
        this.headerMap = headerMap;
        this.dataMap = dataMap;
    }

    public TyyResponse toDo(String method) {
        TyyResponse response = null;
        switch (method.toUpperCase()) {
            case "POST":
                response = this.doPost();
                break;
            case "GET":
                if (StringUtils.isNotEmpty(this.body)) {
                    response = this.sendJsonByGetReq();
                } else {
                    response = this.doGet();
                }
                break;
            case "DELETE":
                if (StringUtils.isNotEmpty(this.body)) {
                    response = this.sendJsonByDeleteReq();
                } else {
                    response = this.doDelete();
                }
                break;
            case "PUT":
                response = this.doPut();
                break;
            case "PATCH":
                response = this.doPatch();
                break;
            case "HEAD":
                response = this.doHead();
                break;
            case "OPTIONS":
                response = this.doOptions();
        }

        return response;
    }

    private String getSign(Date eopDate) {
        String calculateContentHash = this.getSHA256(this.body);
        System.out.println("加密的 body:" + calculateContentHash);
        SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd");
        String singerDate = TIME_FORMATTER.format(eopDate);
        String singerDd = DATE_FORMATTER.format(eopDate);

        try {
            String CampmocalHeader = String.format("ctyun-eop-request-id:%s\neop-date:%s\n", this.uuId, singerDate);
            String sigture = CampmocalHeader + "\n" + this.afterQuery + "\n" + calculateContentHash;
            System.out.println("sigture:" + sigture);
            byte[] ktime = this.HmacSHA256(singerDate.getBytes(), this.sk.getBytes());
            System.out.println("ktime:" + HexUtils.bytes2Hex(ktime));
            byte[] kak = this.HmacSHA256(this.ak.getBytes(), ktime);
            System.out.println("kAk:" + HexUtils.bytes2Hex(kak));
            byte[] kdate = this.HmacSHA256(singerDd.getBytes(), kak);
            System.out.println("kdate:" + HexUtils.bytes2Hex(kdate));
            String signature = Base64.getEncoder().encodeToString(this.HmacSHA256(sigture.getBytes("UTF-8"), kdate));
            System.out.println("---Signature:" + signature);
            String signHeader = String.format("%s Headers=ctyun-eop-request-id;eop-date Signature=%s", this.ak, signature);
            System.out.println("---signHeader:" + signHeader);
            return signHeader;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getSignByByMultipartFormDataBoundary(Date eopDate, byte[] bodyArr) {
        String calculateContentHash = this.getSHA256(bodyArr);
        System.out.println("calculateContentHash:" + calculateContentHash);
        SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd");
        String singerDate = TIME_FORMATTER.format(eopDate);
        String singerDd = DATE_FORMATTER.format(eopDate);

        try {
            String CampmocalHeader = String.format("ctyun-eop-request-id:%s\neop-date:%s\n", this.uuId, singerDate);
            String sigture = CampmocalHeader + "\n" + this.afterQuery + "\n" + calculateContentHash;
            System.out.println("sigture:" + sigture);
            byte[] ktime = this.HmacSHA256(singerDate.getBytes(), this.sk.getBytes());
            System.out.println("ktime:" + HexUtils.bytes2Hex(ktime));
            byte[] kAk = this.HmacSHA256(this.ak.getBytes(), ktime);
            System.out.println("kAk:" + HexUtils.bytes2Hex(kAk));
            byte[] kdate = this.HmacSHA256(singerDd.getBytes(), kAk);
            System.out.println("kdate:" + HexUtils.bytes2Hex(kdate));
            String Signature = Base64.getEncoder().encodeToString(this.HmacSHA256(sigture.getBytes("UTF-8"), kdate));
            System.out.println("---Signature:" + Signature);
            String signHeader = String.format("%s Headers=ctyun-eop-request-id;eop-date Signature=%s", this.ak, Signature);
            System.out.println("---signHeader:" + signHeader);
            return signHeader;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static CloseableHttpClient sslClient() {
        try {
            X509TrustManager trustManager = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] xcs, String str) {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String str) {
                }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init((KeyManager[])null, new TrustManager[]{trustManager}, (SecureRandom)null);
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(ctx, NoopHostnameVerifier.INSTANCE);
            RequestConfig requestConfig = RequestConfig.custom().setCookieSpec("standard-strict").setExpectContinueEnabled(Boolean.TRUE).setTargetPreferredAuthSchemes(Arrays.asList("NTLM", "Digest")).setProxyPreferredAuthSchemes(Arrays.asList("Basic")).build();
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.INSTANCE).register("https", socketFactory).build();
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            CloseableHttpClient closeableHttpClient = HttpClients.custom().setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig).build();
            return closeableHttpClient;
        } catch (KeyManagementException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public TyyResponse doGet() {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        TyyResponse result = new TyyResponse();

        try {
            if (this.temp == 0) {
                httpClient = sslClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            String query = this.queryStr;
            this.afterQuery = this.encodeQueryStr(query);
            HttpGet httpGet;
            if (StringUtils.isNotEmpty(this.afterQuery)) {
                httpGet = new HttpGet(this.url + "?" + this.afterQuery);
            } else {
                httpGet = new HttpGet(this.url);
            }

            SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            Date eopDate = new Date();
            httpGet.setHeader("ctyun-eop-request-id", this.uuId);
            httpGet.setHeader("Eop-Authorization", this.getSign(eopDate));
            httpGet.setHeader("Eop-date", TIME_FORMATTER.format(eopDate));
            if (this.headerMap != null) {
                for(String key : this.headerMap.keySet()) {
                    if (StringUtils.isNotEmpty(key)) {
                        httpGet.setHeader(key, this.headerMap.get(key).toString());
                    }
                }
            }

            System.out.println("请求头部 ----- ");

            for(Header header : httpGet.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }

            System.out.println();
            RequestConfig requestConfig = RequestConfig.custom().build();
            httpGet.setConfig(requestConfig);
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            result.setBody(EntityUtils.toString(entity, "UTF-8"));
            result.setStatusCode(response.getStatusLine().getStatusCode());
            Map<String, String> headerMap = new HashMap();

            for(Header header : response.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }

            //result.setHeaders(headerMap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }

    public TyyResponse sendJsonByGetReq() {
        TyyResponse result = new TyyResponse();
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;

        try {
            if (this.temp == 0) {
                client = sslClient();
            } else {
                client = HttpClients.createDefault();
            }

            String query = this.queryStr;
            this.afterQuery = this.encodeQueryStr(query);
            HttpGetWithEntity httpGetWithEntity;
            if (StringUtils.isNotEmpty(this.afterQuery)) {
                httpGetWithEntity = new HttpGetWithEntity(this.url + "?" + this.afterQuery);
            } else {
                httpGetWithEntity = new HttpGetWithEntity(this.url);
            }

            RequestConfig requestConfig = RequestConfig.custom().build();
            httpGetWithEntity.setConfig(requestConfig);
            SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            Date eopDate = new Date();
            httpGetWithEntity.setHeader("Content-Type", this.contentType);
            httpGetWithEntity.setHeader("ctyun-eop-request-id", this.uuId);
            httpGetWithEntity.setHeader("Eop-Authorization", this.getSign(eopDate));
            httpGetWithEntity.setHeader("Eop-date", TIME_FORMATTER.format(eopDate));
            if (this.headerMap != null) {
                for(String key : this.headerMap.keySet()) {
                    if (StringUtils.isNotEmpty(key)) {
                        httpGetWithEntity.setHeader(key, this.headerMap.get(key).toString());
                    }
                }
            }

            System.out.println("请求头部 ----- ");

            for(Header header : httpGetWithEntity.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }

            System.out.println();
            HttpEntity httpEntity = new StringEntity(this.body, Charset.forName("UTF-8"));
            httpGetWithEntity.setEntity(httpEntity);
            response = client.execute(httpGetWithEntity);
            HttpEntity entity = response.getEntity();
            result.setBody(EntityUtils.toString(entity, "UTF-8"));
            result.setStatusCode(response.getStatusLine().getStatusCode());
            Map<String, String> headerMap = new HashMap();

            for(Header header : response.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }

            result.setHeaders(headerMap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != client) {
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }

    public TyyResponse doPost() {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        TyyResponse result = new TyyResponse();

        try {
            if (this.temp == 0) {
                httpClient = sslClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            String query = this.queryStr;
            this.afterQuery = this.encodeQueryStr(query);
            HttpPost httpPost;
            if (StringUtils.isNotEmpty(this.afterQuery)) {
                httpPost = new HttpPost(this.url + "?" + this.afterQuery);
            } else {
                httpPost = new HttpPost(this.url);
            }

            RequestConfig requestConfig = RequestConfig.custom().build();
            httpPost.setConfig(requestConfig);
            SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            Date eopDate = new Date();
            httpPost.setHeader("Content-Type", this.contentType);
            httpPost.setHeader("ctyun-eop-request-id", this.uuId);
            httpPost.setHeader("Eop-Authorization", this.getSign(eopDate));
            httpPost.setHeader("Eop-date", TIME_FORMATTER.format(eopDate));
            if (this.headerMap != null) {
                for(String key : this.headerMap.keySet()) {
                    if (StringUtils.isNotEmpty(key)) {
                        httpPost.setHeader(key, this.headerMap.get(key).toString());
                    }
                }
            }

            System.out.println("请求头部 ----- ");

            for(Header header : httpPost.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }

            System.out.println();
            StringEntity data = new StringEntity(this.body, Charset.forName("UTF-8"));
            httpPost.setEntity(data);
            httpResponse = httpClient.execute(httpPost);
            HttpEntity entity = httpResponse.getEntity();
            result.setBody(EntityUtils.toString(entity, "UTF-8"));
            result.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            Map<String, String> headerMap = new HashMap();

            for(Header header : httpResponse.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }

            //result.setHeaders(headerMap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }

    public TyyResponse doPostByMultipartFormDataBoundary() {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        TyyResponse result = new TyyResponse();

        try {
            if (this.temp == 0) {
                httpClient = sslClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            String query = this.queryStr;
            this.afterQuery = this.encodeQueryStr(query);
            HttpPost httpPost;
            if (StringUtils.isNotEmpty(this.afterQuery)) {
                httpPost = new HttpPost(this.url + "?" + this.afterQuery);
            } else {
                httpPost = new HttpPost(this.url);
            }

            RequestConfig requestConfig = RequestConfig.custom().build();
            httpPost.setConfig(requestConfig);
            HttpEntity data = null;
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            for(String key : this.fileMap.keySet()) {
                String fileNameVal = this.fileMap.get(key).toString();
                File file = new File(fileNameVal);
                if (file.isFile() && file.exists()) {
                    FileBody fileBody = new FileBody(file);
                    builder.addPart(key, fileBody);
                }
            }

            for(Object key : this.dataMap.keySet()) {
                builder.addTextBody(key.toString(), this.dataMap.get(key).toString());
            }

            data = builder.build();
            List<byte[]> byteList = new ArrayList();
            String contentType = data.getContentType().getValue();
            String boundary = contentType.substring(contentType.indexOf("=") + 1);
            boundary = boundary.split(";")[0];
            boundary = "--" + boundary;

            for(String key : this.fileMap.keySet()) {
                String fileNameVal = this.fileMap.get(key).toString();
                File file = new File(fileNameVal);
                if (file.isFile() && file.exists()) {
                    long size = file.length();
                    if (size >= 1048576L) {
                        System.out.println("----------------文件过大----------------");
                    }

                    String body1 = boundary + "\r\nContent-Disposition: form-data; name=\"" + key + "\"; filename=\"" + file.getName() + "\"\r\nContent-Type: application/octet-stream\r\n\r\n";
                    String body3 = "\r\n";
                    byte[] order1 = body1.getBytes();
                    byte[] order2 = readByte(fileNameVal);
                    byte[] order3 = body3.getBytes();
                    byte[] bodyArr = new byte[order1.length + order2.length + order3.length];

                    for(int i = 0; i < order1.length; ++i) {
                        bodyArr[i] = order1[i];
                    }

                    for(int i = 0; i < order2.length; ++i) {
                        bodyArr[order1.length + i] = order2[i];
                    }

                    for(int i = 0; i < order3.length; ++i) {
                        bodyArr[order1.length + order2.length + i] = order3[i];
                    }

                    byteList.add(bodyArr);
                }
            }

            Integer lengths = 0;

            for(byte[] b : byteList) {
                lengths = lengths + b.length;
            }

            byte[] bodyArrs = new byte[lengths];
            int num = 0;

            for(byte[] list : byteList) {
                for(int j = 0; j < list.length; ++j) {
                    bodyArrs[num] = list[j];
                    ++num;
                }
            }

            System.out.println("bodyArrs 总长度:" + bodyArrs.length);
            StringBuffer dataStr = new StringBuffer();
            if (this.dataMap != null && this.dataMap.size() > 0) {
                for(Object key : this.dataMap.keySet()) {
                    String body = "Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n";
                    String nameVal = this.dataMap.get(key).toString() + "\r\n";
                    dataStr.append(boundary + "\r\n");
                    dataStr.append(body);
                    dataStr.append(nameVal);
                }

                System.out.println("dataStr长度:" + dataStr.length());
            }

            byte[] dataStrByte = dataStr.toString().getBytes();
            String body4 = boundary + "--\r\n";
            byte[] order4 = body4.getBytes();
            System.out.println("order4 长度:" + order4.length);
            byte[] lastBodyArr = new byte[bodyArrs.length + dataStrByte.length + order4.length];

            for(int i = 0; i < bodyArrs.length; ++i) {
                lastBodyArr[i] = bodyArrs[i];
            }

            for(int i = 0; i < dataStrByte.length; ++i) {
                lastBodyArr[bodyArrs.length + i] = dataStrByte[i];
            }

            for(int i = 0; i < order4.length; ++i) {
                lastBodyArr[bodyArrs.length + dataStrByte.length + i] = order4[i];
            }

            System.out.println("lastBodyArr 长度:" + lastBodyArr.length);
            SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            Date eopDate = new Date();
            httpPost.setHeader("ctyun-eop-request-id", this.uuId);
            httpPost.setHeader("Eop-Authorization", this.getSignByByMultipartFormDataBoundary(eopDate, lastBodyArr));
            httpPost.setHeader("Eop-date", TIME_FORMATTER.format(eopDate));
            if (this.headerMap != null) {
                for(String key : this.headerMap.keySet()) {
                    if (StringUtils.isNotEmpty(key)) {
                        httpPost.setHeader(key, this.headerMap.get(key).toString());
                    }
                }
            }

            System.out.println("请求头部 ----- ");

            for(Header header : httpPost.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }

            System.out.println();
            httpPost.setEntity(data);
            httpResponse = httpClient.execute(httpPost);
            HttpEntity entity = httpResponse.getEntity();
            result.setBody(EntityUtils.toString(entity, "UTF-8"));
            result.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            Map<String, String> headerMap = new HashMap();

            for(Header header : httpResponse.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }

            //result.setHeaders(headerMap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }

    public TyyResponse doPut() {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        TyyResponse result = new TyyResponse();

        try {
            if (this.temp == 0) {
                httpClient = sslClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            String query = this.queryStr;
            this.afterQuery = this.encodeQueryStr(query);
            HttpPut httpPut;
            if (StringUtils.isNotEmpty(this.afterQuery)) {
                httpPut = new HttpPut(this.url + "?" + this.afterQuery);
            } else {
                httpPut = new HttpPut(this.url);
            }

            RequestConfig requestConfig = RequestConfig.custom().build();
            httpPut.setConfig(requestConfig);
            SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            Date eopDate = new Date();
            httpPut.setHeader("ctyun-eop-request-id", this.uuId);
            httpPut.setHeader("Eop-Authorization", this.getSign(eopDate));
            httpPut.setHeader("Eop-date", TIME_FORMATTER.format(eopDate));
            if (this.headerMap != null) {
                for(String key : this.headerMap.keySet()) {
                    if (StringUtils.isNotEmpty(key)) {
                        httpPut.setHeader(key, this.headerMap.get(key).toString());
                    }
                }
            }

            System.out.println("请求头部 ----- ");

            for(Header header : httpPut.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }

            System.out.println();
            StringEntity data = new StringEntity(this.body, Charset.forName("UTF-8"));
            httpPut.setEntity(data);
            httpResponse = httpClient.execute(httpPut);
            HttpEntity entity = httpResponse.getEntity();
            result.setBody(EntityUtils.toString(entity, "UTF-8"));
            result.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            Map<String, String> headerMap = new HashMap();

            for(Header header : httpResponse.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }

            //result.setHeaders(headerMap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }

    public TyyResponse doPatch() {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        TyyResponse result = new TyyResponse();

        try {
            if (this.temp == 0) {
                httpClient = sslClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            String query = this.queryStr;
            this.afterQuery = this.encodeQueryStr(query);
            HttpPatch httpPatch;
            if (StringUtils.isNotEmpty(this.afterQuery)) {
                httpPatch = new HttpPatch(this.url + "?" + this.afterQuery);
            } else {
                httpPatch = new HttpPatch(this.url);
            }

            RequestConfig requestConfig = RequestConfig.custom().build();
            httpPatch.setConfig(requestConfig);
            SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            Date eopDate = new Date();
            httpPatch.setHeader("Content-Type", this.contentType);
            httpPatch.setHeader("ctyun-eop-request-id", this.uuId);
            httpPatch.setHeader("Eop-Authorization", this.getSign(eopDate));
            httpPatch.setHeader("Eop-date", TIME_FORMATTER.format(eopDate));
            if (this.headerMap != null) {
                for(String key : this.headerMap.keySet()) {
                    if (StringUtils.isNotEmpty(key)) {
                        httpPatch.setHeader(key, this.headerMap.get(key).toString());
                    }
                }
            }

            System.out.println("请求头部 ----- ");

            for(Header header : httpPatch.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }

            System.out.println();
            StringEntity data = new StringEntity(this.body, Charset.forName("UTF-8"));
            httpPatch.setEntity(data);
            httpResponse = httpClient.execute(httpPatch);
            HttpEntity entity = httpResponse.getEntity();
            result.setBody(EntityUtils.toString(entity, "UTF-8"));
            result.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            Map<String, String> headerMap = new HashMap();

            for(Header header : httpResponse.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }

           // result.setHeaders(headerMap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != httpResponse) {
                try {
                    httpResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }

    public TyyResponse doDelete() {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        TyyResponse result = new TyyResponse();

        try {
            if (this.temp == 0) {
                httpClient = sslClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            String query = this.queryStr;
            this.afterQuery = this.encodeQueryStr(query);
            HttpDelete httpDelete;
            if (StringUtils.isNotEmpty(this.afterQuery)) {
                httpDelete = new HttpDelete(this.url + "?" + this.afterQuery);
            } else {
                httpDelete = new HttpDelete(this.url);
            }

            SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            Date eopDate = new Date();
            httpDelete.setHeader("Content-Type", this.contentType);
            httpDelete.setHeader("ctyun-eop-request-id", this.uuId);
            httpDelete.setHeader("Eop-Authorization", this.getSign(eopDate));
            httpDelete.setHeader("Eop-date", TIME_FORMATTER.format(eopDate));
            if (this.headerMap != null) {
                for(String key : this.headerMap.keySet()) {
                    if (StringUtils.isNotEmpty(key)) {
                        httpDelete.setHeader(key, this.headerMap.get(key).toString());
                    }
                }
            }

            System.out.println("请求头部 ----- ");

            for(Header header : httpDelete.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }

            System.out.println();
            RequestConfig requestConfig = RequestConfig.custom().build();
            httpDelete.setConfig(requestConfig);
            response = httpClient.execute(httpDelete);
            HttpEntity entity = response.getEntity();
            result.setBody(EntityUtils.toString(entity, "UTF-8"));
            result.setStatusCode(response.getStatusLine().getStatusCode());
            Map<String, String> headerMap = new HashMap();

            for(Header header : response.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }

            //result.setHeaders(headerMap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }

    public TyyResponse sendJsonByDeleteReq() {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        TyyResponse result = new TyyResponse();

        try {
            if (this.temp == 0) {
                httpClient = sslClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            String query = this.queryStr;
            this.afterQuery = this.encodeQueryStr(query);
            HttpDeleteWithBody delete;
            if (StringUtils.isNotEmpty(this.afterQuery)) {
                delete = new HttpDeleteWithBody(this.url + "?" + this.afterQuery);
            } else {
                delete = new HttpDeleteWithBody(this.url);
            }

            SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            Date eopDate = new Date();
            delete.setHeader("Content-Type", this.contentType);
            delete.setHeader("ctyun-eop-request-id", this.uuId);
            delete.setHeader("Eop-Authorization", this.getSign(eopDate));
            delete.setHeader("Eop-date", TIME_FORMATTER.format(eopDate));
            if (this.headerMap != null) {
                for(String key : this.headerMap.keySet()) {
                    if (StringUtils.isNotEmpty(key)) {
                        delete.setHeader(key, this.headerMap.get(key).toString());
                    }
                }
            }

            System.out.println("请求头部 ----- ");

            for(Header header : delete.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }

            System.out.println();
            HttpEntity httpEntity = new StringEntity(this.body, Charset.forName("UTF-8"));
            delete.setEntity(httpEntity);
            RequestConfig requestConfig = RequestConfig.custom().build();
            delete.setConfig(requestConfig);
            response = httpClient.execute(delete);
            HttpEntity entity = response.getEntity();
            result.setBody(EntityUtils.toString(entity, "UTF-8"));
            result.setStatusCode(response.getStatusLine().getStatusCode());
            Map<String, String> headerMap = new HashMap();

            for(Header header : response.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }

            //result.setHeaders(headerMap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }

    public TyyResponse doOptions() {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        TyyResponse result = new TyyResponse();

        try {
            if (this.temp == 0) {
                httpClient = sslClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            String query = this.queryStr;
            this.afterQuery = this.encodeQueryStr(query);
            HttpOptions httpOptions;
            if (StringUtils.isNotEmpty(this.afterQuery)) {
                httpOptions = new HttpOptions(this.url + "?" + this.afterQuery);
            } else {
                httpOptions = new HttpOptions(this.url);
            }

            SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            Date eopDate = new Date();
            httpOptions.setHeader("Content-Type", this.contentType);
            httpOptions.setHeader("ctyun-eop-request-id", this.uuId);
            httpOptions.setHeader("Eop-Authorization", this.getSign(eopDate));
            httpOptions.setHeader("Eop-date", TIME_FORMATTER.format(eopDate));
            if (this.headerMap != null) {
                for(String key : this.headerMap.keySet()) {
                    if (StringUtils.isNotEmpty(key)) {
                        httpOptions.setHeader(key, this.headerMap.get(key).toString());
                    }
                }
            }

            System.out.println("请求头部 ----- ");

            for(Header header : httpOptions.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }

            System.out.println();
            RequestConfig requestConfig = RequestConfig.custom().build();
            httpOptions.setConfig(requestConfig);
            response = httpClient.execute(httpOptions);
            HttpEntity entity = response.getEntity();
            result.setBody(EntityUtils.toString(entity, "UTF-8"));
            result.setStatusCode(response.getStatusLine().getStatusCode());
            Map<String, String> headerMap = new HashMap();

            for(Header header : response.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }

            //result.setHeaders(headerMap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }

    public TyyResponse doHead() {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        TyyResponse result = new TyyResponse();

        try {
            if (this.temp == 0) {
                httpClient = sslClient();
            } else {
                httpClient = HttpClients.createDefault();
            }

            String query = this.queryStr;
            this.afterQuery = this.encodeQueryStr(query);
            HttpHead httpHead;
            if (StringUtils.isNotEmpty(this.afterQuery)) {
                httpHead = new HttpHead(this.url + "?" + this.afterQuery);
            } else {
                httpHead = new HttpHead(this.url);
            }

            SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            Date eopDate = new Date();
            httpHead.setHeader("Content-Type", this.contentType);
            httpHead.setHeader("ctyun-eop-request-id", this.uuId);
            httpHead.setHeader("Eop-Authorization", this.getSign(eopDate));
            httpHead.setHeader("Eop-date", TIME_FORMATTER.format(eopDate));
            if (this.headerMap != null) {
                for(String key : this.headerMap.keySet()) {
                    if (StringUtils.isNotEmpty(key)) {
                        httpHead.setHeader(key, this.headerMap.get(key).toString());
                    }
                }
            }

            System.out.println("请求头部 ----- ");

            for(Header header : httpHead.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }

            System.out.println();
            RequestConfig requestConfig = RequestConfig.custom().build();
            httpHead.setConfig(requestConfig);
            response = httpClient.execute(httpHead);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result.setBody(EntityUtils.toString(entity, "UTF-8"));
            }

            result.setStatusCode(response.getStatusLine().getStatusCode());
            Map<String, String> headerMap = new HashMap();

            for(Header header : response.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }

            //result.setHeaders(headerMap);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != httpClient) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }

    private String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);

        for(byte b : data) {
            String hex = Integer.toHexString(b);
            if (hex.length() == 1) {
                sb.append("0");
            } else if (hex.length() == 8) {
                hex = hex.substring(6);
            }

            sb.append(hex);
        }

        return sb.toString().toLowerCase(Locale.getDefault());
    }

    private String getSHA256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes());
            return this.toHex(md.digest());
        } catch (NoSuchAlgorithmException var3) {
            return null;
        } catch (Exception var4) {
            return null;
        }
    }

    private String getSHA256(byte[] byteArr) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(byteArr);
            return this.toHex(md.digest());
        } catch (NoSuchAlgorithmException var3) {
            return null;
        } catch (Exception var4) {
            return null;
        }
    }

    public byte[] HmacSHA256(byte[] data, byte[] key) throws Exception {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception var4) {
            return null;
        }
    }

    public String encodeQueryStr(String query) {
        String afterQuery = "";

        try {
            if (StringUtils.isNotEmpty(query)) {
                String[] param = query.split("&");
                Arrays.sort(param);

                for(String str : param) {
                    if (afterQuery.length() < 1) {
                        String[] s = str.split("=");
                        if (s.length >= 2) {
                            String encodeStr = null;
                            encodeStr = URLEncoder.encode(s[1], "UTF-8");
                            (new StringBuilder()).append(s[0]).append("=").append(encodeStr).toString();
                        } else {
                            afterQuery = afterQuery + str;
                            if (s[0].contains("=")) {
                                String encodeStr = "";
                                str = s[0] + "=" + encodeStr;
                                afterQuery = afterQuery + str;
                            } else {
                                afterQuery = s[0];
                            }
                        }
                    } else {
                        String[] s = str.split("=");
                        if (s.length >= 2) {
                            String encodeStr = URLEncoder.encode(s[1], "UTF-8");
                            str = s[0] + "=" + encodeStr;
                            afterQuery = afterQuery + "&" + str;
                        } else {
                            String encodeStr = "";
                            str = s[0] + "=" + encodeStr;
                            afterQuery = afterQuery + "&" + str;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return afterQuery;
    }

    private static String readString1(String fileName) {
        StringBuffer sb = new StringBuffer();
        File file = new File(fileName);
        InputStream in = null;

        try {
            System.out.println("以字节为单位读取文件内容，一次读一个字节：");
            in = new FileInputStream(file);

            int tempbyte;
            while((tempbyte = in.read()) != -1) {
                sb.append((char)tempbyte);
            }

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static String readString2(String fileName) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder("");
        File file = new File(fileName);

        try {
            InputStream in = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(in));

            String str;
            while((str = br.readLine()) != null) {
                sb.append(str);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return sb.toString();
    }

    private static String readString3(String fileName) {
        String str = "";
        File file = new File(fileName);

        try {
            FileInputStream in = new FileInputStream(file);
            int size = in.available();
            byte[] buffer = new byte[size];
            in.read(buffer);
            in.close();
            str = new String(buffer, "utf-8");
            return str;
        } catch (IOException var6) {
            return null;
        }
    }

    private static byte[] readByte(String fileName) {
        String str = "";
        File file = new File(fileName);

        try {
            FileInputStream in = new FileInputStream(file);
            int size = in.available();
            byte[] buffer = new byte[size];
            in.read(buffer);
            in.close();
            new String(buffer, "utf-8");
            return buffer;
        } catch (IOException var6) {
            return null;
        }
    }

    public static String byteToHex(byte[] by) {
        StringBuffer sb = new StringBuffer();

        for(byte b : by) {
            String hex = Integer.toHexString(b & 255);
            hex = hex.toUpperCase();
            if (hex.length() < 2) {
                hex = "0" + hex;
            }

            hex = "0x" + hex;
            sb.append(hex + ",");
        }

        return sb.toString();
    }
}