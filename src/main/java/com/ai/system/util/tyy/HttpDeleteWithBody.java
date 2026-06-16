package com.ai.system.util.tyy;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

/**
 * 支持携带请求体的 HTTP DELETE 方法
 */
public class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {

    public HttpDeleteWithBody() {
        super();
    }

    public HttpDeleteWithBody(final URI uri) {
        super();
        setURI(uri);
    }

    public HttpDeleteWithBody(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return "DELETE";
    }
}
