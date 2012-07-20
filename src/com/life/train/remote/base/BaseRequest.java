package com.life.train.remote.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.life.train.App;
import com.life.train.Const;
import com.life.train.err.AppException;
import com.life.train.util.AppLog;

public abstract class BaseRequest<Result> {

    private static final String TAG = "BaseRequest";

    private static final int DEFAULT_TIMEOUT_SEC = 30;
    private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;

    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String ENCODING_GZIP = "gzip";

    private URI uri;

    public BaseRequest(URI uri){
        this.uri = uri;
        AppLog.debug(TAG, uri.toString());
    }

    /**
     * Executes actions which specified in the derived class
     * @return object that is specified in the derived class
     * @throws AppException throws when there are problems with access to the server
     */
    public Result execute(ArrayList<NameValuePair> params) throws AppException{
        String responseText = "";

        final Context context = App.getContext();

        HttpRequestBase request = createPostWithParameters(params);

        final HttpClient httpClient = getHttpClient(context); /*new DefaultHttpClient()*/;

        request.setURI(uri);
        try {
            responseText = retreive(httpClient, request);
        } catch (ClientProtocolException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }


        if (TextUtils.isEmpty(responseText)){
            throw new AppException("Returned result string is empty");
        }

        return parse(responseText);
    }

    public Result parse(String responseText) throws AppException{

        Result result;
        try {

            JSONObject json = new JSONObject(responseText);

            checkJsonException(json);
            result = parseJson(json);

        } catch (JSONException e) {
            throw new AppException("Unable to parse response", e);
        }

        return result;
    }

    public Result parseJson(JSONObject json) throws JSONException, AppException{
        return null;
    }

    public Result parseJackson(String json) throws AppException{
        return null;
    }

    public static URI buildURI(String url) throws URISyntaxException{
        return new URI(Const.BASE_URL+App.getLang()+url);
    }

    protected HttpPost createPostWithParameters(ArrayList<NameValuePair> params) throws AppException {
        HttpPost post = new HttpPost();
        post.setHeader( "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8" );
        if (params == null){
            return post;
        }

        try {
            post.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new AppException("Failed to encode parameters", e);
        }

        return post;
    }

    private static String retreive(HttpClient httpClient, HttpRequestBase request) throws ClientProtocolException, IOException{
        String result = null;
        result = httpClient.execute(request, new BasicResponseHandler());
        return result;
    }

    private void checkJsonException(JSONObject json) throws AppException{

        String error = json.optString("error", null);

        if (error == null || "null".equals(error) || "false".equals(error)){
            return;
        }

        throw new AppException("0", error);

    }

    /**
     * Generate and return a {@link HttpClient} configured for general use,
     * including setting an application-specific user-agent string.
     */
    public static HttpClient getHttpClient(Context context) {
        final HttpParams params = new BasicHttpParams();

        // Use generous timeouts for slow mobile networks
        HttpConnectionParams.setConnectionTimeout(params, DEFAULT_TIMEOUT_SEC * SECOND_IN_MILLIS);
        HttpConnectionParams.setSoTimeout(params, DEFAULT_TIMEOUT_SEC * SECOND_IN_MILLIS);

        HttpConnectionParams.setSocketBufferSize(params, 8192);
        HttpProtocolParams.setUserAgent(params, buildUserAgent(context));

        final DefaultHttpClient client = new DefaultHttpClient(params);

        client.addRequestInterceptor(new HttpRequestInterceptor() {

            @Override
            public void process(HttpRequest request, HttpContext context) {
                // Add header to accept gzip content
                if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
                    request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
                }
            }
        });

        client.addResponseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext context) {
                // Inflate any responses compressed with gzip
                final HttpEntity entity = response.getEntity();
                final Header encoding = entity.getContentEncoding();
                if (encoding != null) {
                    for (HeaderElement element : encoding.getElements()) {
                        if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
                            response.setEntity(new InflatingEntity(response.getEntity()));
                            break;
                        }
                    }
                }
            }
        });

        return client;
    }

    /**
     * Build and return a user-agent string that can identify this application
     * to remote servers. Contains the package name and version code.
     */
    private static String buildUserAgent(Context context) {
        try {
            final PackageManager manager = context.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);

            // Some APIs require "(gzip)" in the user-agent string.
            return info.packageName + "/" + info.versionName
                    + " (" + info.versionCode + ") (gzip)";
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Simple {@link HttpEntityWrapper} that inflates the wrapped
     * {@link HttpEntity} by passing it through {@link GZIPInputStream}.
     */
    private static class InflatingEntity extends HttpEntityWrapper {
        public InflatingEntity(HttpEntity wrapped) {
            super(wrapped);
        }

        @Override
        public InputStream getContent() throws IOException {
            return new GZIPInputStream(wrappedEntity.getContent());
        }

        @Override
        public long getContentLength() {
            return -1;
        }
    }



    private static void throwServerException(Exception ex) throws AppException{
        throw new AppException("Unable to create HTTP client", ex);
    }
}
