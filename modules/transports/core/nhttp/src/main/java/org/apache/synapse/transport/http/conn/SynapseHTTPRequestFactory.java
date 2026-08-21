package org.apache.synapse.transport.http.conn;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;

public class SynapseHTTPRequestFactory implements HttpRequestFactory {

    private static final String[] RFC2616_COMMON_METHODS = {
            "GET"
    };

    private static final String[] RFC2616_ENTITY_ENC_METHODS = {
            "POST",
            "PUT",
            "PATCH"
    };

    private static final String[] RFC2616_SPECIAL_METHODS = {
            "HEAD",
            "TRACE",
            "CONNECT"
    };

    /**
     * Methods that may or may not carry a request body. DELETE is included to provide payload
     * support for HTTP DELETE requests. OPTIONS is included because, per RFC 9112 6, message
     * framing (Content-Length/Transfer-Encoding) is independent of method semantics - a client
     * may declare a body on OPTIONS even though RFC 9110 9.3.7 defines no use for it - and
     * framing that body correctly here (the same way DELETE's optional payload already is)
     * ensures it is read off the wire via httpcore's normal entity-enclosing path instead of
     * being left unconsumed and misparsed as the start of the next request.
    **/
    private static final String[] RFC2616_POSSIBLE_ENTITY_ENC_METHODS = {
            "DELETE",
            "OPTIONS"
    };

    public static final String ENDPOINT_URL = "endPointURI";

    public SynapseHTTPRequestFactory() {
        super();
    }

    private static boolean isOneOf(final String[] methods, final String method) {
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }

    public HttpRequest newHttpRequest(final RequestLine requestline)
            throws MethodNotSupportedException {
        if (requestline == null) {
            throw new IllegalArgumentException("Request line may not be null");
        }
        String method = requestline.getMethod();
        if (isOneOf(RFC2616_COMMON_METHODS, method)) {
            return new BasicHttpRequest(requestline);
        } else if (isOneOf(RFC2616_POSSIBLE_ENTITY_ENC_METHODS, method)) {
            return new BasicHttpEntityPossibleEnclosingRequest(requestline);
        } else if (isOneOf(RFC2616_ENTITY_ENC_METHODS, method)) {
            return new BasicHttpEntityEnclosingRequest(requestline);
        } else if (isOneOf(RFC2616_SPECIAL_METHODS, method)) {
            return new BasicHttpRequest(requestline);
        } else {
            throw new MethodNotSupportedException(method +  " method not supported");
        }
    }

    public HttpRequest newHttpRequest(final String method, final String uri)
            throws MethodNotSupportedException {
        if (isOneOf(RFC2616_COMMON_METHODS, method)) {
            return new BasicHttpRequest(method, uri);
        } else if (isOneOf(RFC2616_POSSIBLE_ENTITY_ENC_METHODS, method)) {
            return new BasicHttpEntityPossibleEnclosingRequest(method, uri);
        } else if (isOneOf(RFC2616_ENTITY_ENC_METHODS, method)) {
            return new BasicHttpEntityEnclosingRequest(method, uri);
        } else if (isOneOf(RFC2616_SPECIAL_METHODS, method)) {
            return new BasicHttpRequest(method, uri);
        } else {
            throw new MethodNotSupportedException(method
                    + " method not supported");
        }
    }

}