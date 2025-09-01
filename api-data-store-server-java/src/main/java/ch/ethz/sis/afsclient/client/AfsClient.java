package ch.ethz.sis.afsclient.client;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.api.PublicAPI;
import ch.ethz.sis.afsapi.dto.ApiResponse;
import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.dto.FreeSpace;
import ch.ethz.sis.afsclient.client.exception.ClientExceptions;
import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsjson.jackson.JacksonObjectMapper;
import lombok.NonNull;

public final class AfsClient implements PublicAPI, ClientAPI
{

    private static final int DEFAULT_PACKAGE_SIZE_IN_BYTES = 1024;

    private static final int DEFAULT_TIMEOUT_IN_MILLIS = 30000;

    private static final String MD5 = "MD5";

    private final int maxReadSizeInBytes;

    private final int timeout;

    private String sessionToken;

    private String interactiveSessionKey;

    private String transactionManagerKey;

    private final URI serverUri;

    private static final JsonObjectMapper jsonObjectMapper = new JacksonObjectMapper();

    public AfsClient(final URI serverUri)
    {
        this(serverUri, DEFAULT_PACKAGE_SIZE_IN_BYTES, DEFAULT_TIMEOUT_IN_MILLIS);
    }

    public AfsClient(final URI serverUri, final int timeoutInMillis){
        this(serverUri, DEFAULT_PACKAGE_SIZE_IN_BYTES, timeoutInMillis);
    }

    public AfsClient(final URI serverUri, final int maxReadSizeInBytes, final int timeoutInMillis)
    {
        this.maxReadSizeInBytes = maxReadSizeInBytes;
        this.timeout = timeoutInMillis;
        this.serverUri = serverUri;
    }

    public URI getServerUri()
    {
        return serverUri;
    }

    public int getMaxReadSizeInBytes()
    {
        return maxReadSizeInBytes;
    }

    public String getSessionToken()
    {
        return sessionToken;
    }

    public void setSessionToken(final String sessionToken)
    {
        this.sessionToken = sessionToken;
    }

    public String getInteractiveSessionKey()
    {
        return interactiveSessionKey;
    }

    public void setInteractiveSessionKey(String interactiveSessionKey)
    {
        this.interactiveSessionKey = interactiveSessionKey;
    }

    public String getTransactionManagerKey()
    {
        return transactionManagerKey;
    }

    public void setTransactionManagerKey(String transactionManagerKey)
    {
        this.transactionManagerKey = transactionManagerKey;
    }

    private static String urlEncode(final String s)
    {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Override
    public @NonNull String login(@NonNull final String userId, @NonNull final String password)
            throws Exception
    {
        String result = request("POST",
                "login", String.class, Map.of("userId", userId, "password", password));
        setSessionToken(result);
        return result;
    }

    @Override
    public @NonNull Boolean isSessionValid() throws Exception
    {
        validateSessionToken();
        return request("GET", "isSessionValid", Boolean.class, Map.of());
    }

    @Override
    public @NonNull Boolean logout() throws Exception
    {
        validateSessionToken();
        Boolean result = request("POST", "logout", Boolean.class, Map.of());
        setSessionToken(null);
        return result;
    }

    @Override
    public @NonNull File[] list(@NonNull final String owner, @NonNull final String source,
            @NonNull final Boolean recursively) throws Exception
    {
        validateSessionToken();
        return request("GET", "list", File[].class,
                Map.of("owner", owner, "source", source, "recursively",
                        recursively.toString()));
    }

    @Override
    public byte[] read(@NonNull String owner, @NonNull String source, @NonNull Long offset, @NonNull Integer limit) throws Exception {
        Chunk[] chunks = read(new Chunk[] { new Chunk(owner, source, offset, limit, ChunkEncoderDecoder.EMPTY_ARRAY)});
        return chunks[0].getData();
    }

    @Override
    public @NonNull Chunk[] read(@NonNull final Chunk[] chunks) throws Exception
    {
        validateSessionToken();
        return request("POST",
                "read",
                Chunk[].class,
                Map.of() ,
                ChunkEncoderDecoder.encodeChunksAsBytes(chunks),
                false );
    }


    @Override
    public @NonNull Boolean write(@NonNull final String owner, @NonNull final String source,
                                  @NonNull final Long offset, @NonNull final byte[] data) throws Exception
    {
        Chunk[] chunks = new Chunk[] {new Chunk(owner, source, offset, data.length, data) } ;
        return write(chunks);
    }

    @Override
    public @NonNull Boolean write(@NonNull Chunk[] chunks) throws Exception {
        validateSessionToken();
        return request("POST",
                "write",
                Boolean.class,
                Map.of() ,
                ChunkEncoderDecoder.encodeChunksAsBytes(chunks),
                false );
    }

    @Override
    public @NonNull Boolean delete(@NonNull final String owner, @NonNull final String source)
            throws Exception
    {
        validateSessionToken();
        return request("DELETE", "delete", Boolean.class, Map.of("owner", owner, "source", source));
    }

    @Override
    public @NonNull Boolean copy(@NonNull final String sourceOwner, @NonNull final String source,
            @NonNull final String targetOwner,
            @NonNull final String target)
            throws Exception
    {
        validateSessionToken();
        return request("POST", "copy", Boolean.class,
                Map.of("sourceOwner", sourceOwner, "source", source,
                        "targetOwner", targetOwner, "target", target));
    }

    @Override
    public @NonNull Boolean move(@NonNull final String sourceOwner, @NonNull final String source,
            @NonNull final String targetOwner,
            @NonNull final String target)
            throws Exception
    {
        validateSessionToken();
        return request("POST", "move", Boolean.class,
                Map.of("sourceOwner", sourceOwner, "source", source,
                        "targetOwner", targetOwner, "target", target));
    }

    @Override
    public @NonNull Boolean create(@NonNull final String owner, @NonNull final String source, @NonNull final Boolean directory)
            throws Exception
    {
        validateSessionToken();
        return request("POST", "create", Boolean.class, Map.of("owner", owner, "source", source, "directory", String.valueOf(directory)));
    }

    @Override
    public @NonNull FreeSpace free(@NonNull final String owner, @NonNull final String source) throws Exception
    {
        validateSessionToken();
        return request("GET", "free", FreeSpace.class, Map.of("owner", owner, "source", source));
    }

    @Override
    public void begin(final UUID transactionId) throws Exception
    {
        validateSessionToken();
        request("POST", "begin", null, Map.of("transactionId", transactionId.toString()));
    }

    @Override
    public Boolean prepare() throws Exception
    {
        validateSessionToken();
        return request("POST", "prepare", Boolean.class, Map.of());
    }

    @Override
    public void commit() throws Exception
    {
        validateSessionToken();
        request("POST", "commit", null, Map.of());
    }

    @Override
    public void rollback() throws Exception
    {
        validateSessionToken();
        request("POST", "rollback", null, Map.of());
    }

    @Override
    public List<UUID> recover() throws Exception
    {
        validateSessionToken();
        return request("POST", "recover", List.class, Map.of());
    }


    public String getName(String path) {
        int indexOf = path.lastIndexOf('/');
        if(indexOf == -1) {
            return path;
        } else {
            return path.substring(indexOf + 1);
        }
    }

    @Override
    @NonNull public Boolean upload(@NonNull Path sourcePath, @NonNull String destinationOwner, @NonNull Path destinationPath, @NonNull FileCollisionListener fileCollisionListener, @NonNull TransferMonitorListener transferMonitorListener) throws Exception {
        return AfsClientUploadHelper.upload(this, sourcePath, destinationOwner, destinationPath, fileCollisionListener, transferMonitorListener);
    }

    @Override
    @NonNull public Boolean download(@NonNull String sourceOwner, @NonNull Path sourcePath, @NonNull Path destinationPath, @NonNull FileCollisionListener fileCollisionListener, @NonNull TransferMonitorListener transferMonitorListener) throws Exception{
        return AfsClientDownloadHelper.download(this, sourceOwner, sourcePath, destinationPath, fileCollisionListener, transferMonitorListener);
    }

    private static byte[] getMD5(final byte[] data)
    {
        try
        {
            return MessageDigest.getInstance(MD5).digest(data);
        } catch (Exception exception)
        {
            throw new RuntimeException(exception);
        }
    }

    private static String getQueryString(String apiMethod, Map<String, String> params, boolean encode){
        return Stream.concat(
                        Stream.of(new AbstractMap.SimpleImmutableEntry<>("method", apiMethod)),
                        params.entrySet().stream())
                .map(entry -> (encode ? urlEncode(entry.getKey()) : entry.getKey()) + "=" + (encode ? urlEncode(entry.getValue()) : entry.getValue()))
                .collect(Collectors.joining("&"));
    }


    private <T> T request(@NonNull final String httpMethod, @NonNull final String apiMethod,
            Class<T> responseType,
            @NonNull Map<String, String> params)
            throws Exception
    {
        return request(httpMethod,apiMethod, responseType, params, null, true);
    }

    private <T> T request(@NonNull final String httpMethod, @NonNull final String apiMethod,
            Class<T> responseType,
            @NonNull Map<String, String> paramsP, byte[] body, boolean sendParamsInBodyPostAndDelete)
            throws Exception
    {

        HashMap<String, String> mutableParams = new HashMap<>(paramsP);

        if (sessionToken != null)
        {
            mutableParams.put("sessionToken", sessionToken);
        }

        if (interactiveSessionKey != null)
        {
            mutableParams.put("interactiveSessionKey", interactiveSessionKey);
        }

        if (transactionManagerKey != null)
        {
            mutableParams.put("transactionManagerKey", transactionManagerKey);
        }

        byte[] bodyBytes = (body != null) ? body : new byte[0];;
        String queryParameters = null;
        if (httpMethod.equals("GET") || !sendParamsInBodyPostAndDelete)
        {
            queryParameters = getQueryString(apiMethod, mutableParams, false);

        } else if (httpMethod.equals("POST") || httpMethod.equals("DELETE"))
        {
            bodyBytes = getQueryString(apiMethod, mutableParams, true).getBytes(StandardCharsets.UTF_8);
        }

        final URI uri =
                new URI(serverUri.getScheme(), null, serverUri.getHost(),
                        serverUri.getPort(), serverUri.getPath() + "/api",
                        queryParameters, null);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofMillis(timeout))
                .header("Content-Type", "application/octet-stream")
                .method(httpMethod, HttpRequest.BodyPublishers.ofByteArray(bodyBytes));

        final HttpRequest request = builder.build();
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(timeout));

        HttpClient client = clientBuilder.build();

        final HttpResponse<byte[]> httpResponse =
                client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        final int statusCode = httpResponse.statusCode();
        if (statusCode >= 200 && statusCode < 300)
        {
            if (!httpResponse.headers().map().containsKey("content-type"))
            {
                throw new IllegalArgumentException(
                        "Server error HTTP response. Missing content-type");
            }
            String contentType = httpResponse.headers().map().get("content-type").get(0);
            byte[] responseBody = httpResponse.body();

            return getResponseResult(responseType, contentType, responseBody);
        } else if (statusCode >= 400 && statusCode < 500)
        {
            // jsonObjectMapper can't deserialize immutable lists sent in the error message.
            String res = new String(httpResponse.body(), StandardCharsets.UTF_8);
            throw ClientExceptions.API_ERROR.getInstance(res);
        } else if (statusCode >= 500 && statusCode < 600)
        {
            throw ClientExceptions.SERVER_ERROR.getInstance(String.valueOf(statusCode));
        } else
        {
            throw ClientExceptions.OTHER_ERROR.getInstance(String.valueOf(statusCode));
        }
    }

    public static <T> T getResponseResult(Class<T> responseType, String contentType,
            byte[] responseBody)
            throws Exception
    {
        switch (contentType)
        {
            case "text/plain":
                return parseFormDataResponse(responseType, responseBody);
            case "application/json":
                return parseJsonResponse(responseBody);
            case "application/octet-stream":
                return parseOctetStreamResponse(responseType, responseBody);
            default:
                throw new IllegalArgumentException(
                        "Client error HTTP response. Unsupported content-type received.");
        }
    }

    private static <T> T parseFormDataResponse(Class<T> responseType, byte[] responseBody)
    {
        if (responseType == null)
        {
            return null;
        } else if (responseType == String.class)
        {
            return responseType.cast(new String(responseBody, StandardCharsets.UTF_8));
        } else if (responseType == Boolean.class)
        {
            return responseType.cast(
                    Boolean.parseBoolean(new String(responseBody, StandardCharsets.UTF_8)));
        }

        throw new IllegalStateException("Unreachable statement!");
    }

    private static <T> T parseJsonResponse(byte[] responseBody) throws Exception
    {
        final ApiResponse response =
                jsonObjectMapper.readValue(new ByteArrayInputStream(responseBody),
                        ApiResponse.class);

        if (response.getError() != null)
        {
            throw ClientExceptions.API_ERROR.getInstance(response.getError());
        } else
        {
            return (T) response.getResult();
        }
    }

    private static <T> T parseOctetStreamResponse(Class<T> responseType, byte[] responseBody) throws Exception
    {
        if (Chunk[].class.equals(responseType)) {
            return responseType.cast(ChunkEncoderDecoder.decodeChunks(responseBody));
        } else if (File[].class.equals(responseType)) {
            return responseType.cast(FileEncoderDecoder.decodeFiles(responseBody));
        }
        throw new IllegalArgumentException("Client error HTTP response. Unsupported content-type received for expected responseType");
    }

    private void validateSessionToken()
    {
        if (getSessionToken() == null)
        {
            throw new IllegalStateException("No session information detected!");
        }
    }

    private static class ChannelWriteCompletionHandler implements CompletionHandler<Integer, ByteBuffer>
    {

        private final CountDownLatch latch;

        private final AtomicBoolean hasError;

        public ChannelWriteCompletionHandler(final CountDownLatch latch, final AtomicBoolean hasError)
        {
            this.latch = latch;
            this.hasError = hasError;
        }

        @Override
        public void completed(final Integer result, final ByteBuffer attachment)
        {
            latch.countDown();
        }

        @Override
        public void failed(final Throwable exc, final ByteBuffer attachment)
        {
            hasError.set(true);
            latch.countDown();
        }

    }

    private class ChannelReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer>
    {

        private final @NonNull String owner;

        private final @NonNull String destination;

        private final CountDownLatch latch;

        private final AtomicBoolean hasError;

        private final Long offset;

        public ChannelReadCompletionHandler(final @NonNull String owner, final @NonNull String destination, final Long offset,
                final CountDownLatch latch, final AtomicBoolean hasError)
        {
            this.owner = owner;
            this.destination = destination;
            this.offset = offset;
            this.latch = latch;
            this.hasError = hasError;
        }

        @Override
        public void completed(final Integer result, final ByteBuffer attachment)
        {
            final byte[] fullBuffer = attachment.array();
            final byte[] data = result < fullBuffer.length ? Arrays.copyOf(fullBuffer, result) : fullBuffer;
            try
            {
                final Boolean writeSuccessful = write(owner, destination, this.offset, data);
                if (!writeSuccessful)
                {
                    hasError.set(true);
                }
            } catch (final Exception e)
            {
                hasError.set(true);
            } finally
            {
                latch.countDown();
            }
        }

        @Override
        public void failed(final Throwable exc, final ByteBuffer attachment)
        {
            hasError.set(true);
            latch.countDown();
        }

    }

}