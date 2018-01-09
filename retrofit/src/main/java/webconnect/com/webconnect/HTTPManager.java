package webconnect.com.webconnect;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by clickapps on 28/12/17.
 */

public class HTTPManager {
    int cacheSize = 10 * 1024 * 1024; // 10 MB
    private static HTTPManager sManager = new HTTPManager();
    private final OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
    private Dispatcher dispatcher = new Dispatcher();
    final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
    OkHttpClient okHttpClient;
    private final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private HTTPManager() {
    }

    /**
     * Get web connect.
     *
     * @return the web connect
     */
    static HTTPManager get() {
        if (sManager == null) {
            synchronized (HTTPManager.class) {
                if (sManager == null) {
                    sManager = new HTTPManager();
                }
            }
        }
        return sManager;
    }

    OkHttpClient getDefaultOkHttpClient(@NonNull WebParam webParam) {
        if (okHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            Cache cache = new Cache(webParam.getContext().getCacheDir(), cacheSize);
            builder.cache(cache);
            builder.connectTimeout(ApiConfiguration.INSTANCE.getConnectTimeOut(), TimeUnit.MILLISECONDS);
            builder.writeTimeout(ApiConfiguration.INSTANCE.getConnectTimeOut(), TimeUnit.MILLISECONDS);
            builder.readTimeout(ApiConfiguration.INSTANCE.getReadTimeOut(), TimeUnit.MILLISECONDS);
            dispatcher.setMaxRequestsPerHost(2);
            dispatcher.setMaxRequests(10);
            builder.dispatcher(dispatcher);
            interceptor.setLevel(ApiConfiguration.INSTANCE.isDebug() ?
                    HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
            builder.addInterceptor(interceptor);
            okHttpClient = builder.build();
        }
        if (webParam.getConnectTimeOut() > 0 || webParam.getReadTimeOut() > 0) {
            return okHttpClient.newBuilder()
                    .connectTimeout(webParam.getConnectTimeOut(), TimeUnit.MILLISECONDS)
                    .writeTimeout(webParam.getConnectTimeOut(), TimeUnit.MILLISECONDS)
                    .readTimeout(webParam.getReadTimeOut(), TimeUnit.MILLISECONDS)
                    .build();
        }
        return okHttpClient;
    }

    /**
     * @param webParam
     * @param <T>
     * @return
     */
    <T> Observable<Response> performSimpleRequest(final WebParam webParam) {
        String baseUrl = ApiConfiguration.INSTANCE.getBaseUrl();
        if (!TextUtils.isEmpty(webParam.getBaseUrl())) {
            baseUrl = webParam.getBaseUrl();
        }
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + webParam.getUrl()).newBuilder();
        if (webParam.getRequestParam() != null && webParam.getRequestParam().size() > 0) {
            Set<? extends Map.Entry<String, ?>> entries = webParam.getRequestParam().entrySet();
            for (Map.Entry<String, ?> entry : entries) {
                String name = entry.getKey();
                List<String> list = (List<String>) entry.getValue();
                for (String value : list) {
                    urlBuilder.addQueryParameter(name, value);
                }
            }
        }
        builder.url(urlBuilder.build().toString());

        if (webParam.getHeaderParam() != null && webParam.getHeaderParam().size() > 0) {
            Headers.Builder headerBuilder = new Headers.Builder();
            for (Map.Entry<String, String> entry : webParam.getHeaderParam().entrySet()) {
                headerBuilder.add(entry.getKey(), entry.getValue());
            }
            builder.headers(headerBuilder.build());
        }

        RequestBody requestBody;
        switch (webParam.getHttpType()) {
            case GET: {
                builder = builder.get();
                break;
            }
            case POST: {
                requestBody = RequestBody.create(JSON_MEDIA_TYPE, new Gson().toJson(webParam.getRequestParam()));
                builder = builder.post(requestBody);
                break;
            }
            case PUT: {
                requestBody = RequestBody.create(JSON_MEDIA_TYPE, new Gson().toJson(webParam.getRequestParam()));
                builder = builder.put(requestBody);
                break;
            }
            case DELETE: {
                requestBody = RequestBody.create(JSON_MEDIA_TYPE, new Gson().toJson(webParam.getRequestParam()));
                builder = builder.delete(requestBody);
                break;
            }
            case HEAD: {
                builder = builder.head();
                break;
            }
            case OPTIONS: {
                builder = builder.method("OPTIONS", null);
                break;
            }
            case PATCH: {
                requestBody = RequestBody.create(JSON_MEDIA_TYPE, new Gson().toJson(webParam.getRequestParam()));
                builder = builder.patch(requestBody);
                break;
            }
            case MULTIPART: {

            }
        }
        if (webParam.isCacheEnabled()) {
            builder.cacheControl(CacheControl.FORCE_CACHE);
        } else {
            builder.cacheControl(CacheControl.FORCE_NETWORK);
        }
        Request okHttpRequest = builder.build();
        Call call = getDefaultOkHttpClient(webParam).newCall(okHttpRequest);
        return new RxObservable.SimpleANObservable<>(webParam, call);
    }

    /**
     * @param webParam
     * @param <T>
     * @return
     */
    <T> Observable<T> generateDownloadObservable(final WebParam webParam) {
        String baseUrl = ApiConfiguration.INSTANCE.getBaseUrl();
        if (!TextUtils.isEmpty(webParam.getBaseUrl())) {
            baseUrl = webParam.getBaseUrl();
        }
        OkHttpClient client = getDefaultOkHttpClient(webParam);
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + webParam.getUrl()).newBuilder();
        if (webParam.getRequestParam() != null && webParam.getRequestParam().size() > 0) {
            Set<? extends Map.Entry<String, ?>> entries = webParam.getRequestParam().entrySet();
            for (Map.Entry<String, ?> entry : entries) {
                String name = entry.getKey();
                List<String> list = (List<String>) entry.getValue();
                for (String value : list) {
                    urlBuilder.addQueryParameter(name, value);
                }
            }
        }
        builder.url(urlBuilder.build().toString());
        if (webParam.getHeaderParam() != null && webParam.getHeaderParam().size() > 0) {
            Headers.Builder headerBuilder = new Headers.Builder();
            for (Map.Entry<String, String> entry : webParam.getHeaderParam().entrySet()) {
                headerBuilder.add(entry.getKey(), entry.getValue());
            }
            builder.headers(headerBuilder.build());
        }

        if (webParam.isCacheEnabled()) {
            builder.cacheControl(CacheControl.FORCE_CACHE);
        } else {
            builder.cacheControl(CacheControl.FORCE_NETWORK);
        }
        Request okHttpRequest = builder.build();
        client = client.newBuilder().addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
                return originalResponse.newBuilder()
                        .body(new HTTPInternalNetworking.ProgressResponseBody(originalResponse.body(), webParam))
                        .build();
            }
        }).build();
        Call call = client.newCall(okHttpRequest);
        return new RxObservable.DownloadANObservable<>(webParam, call);
    }
}