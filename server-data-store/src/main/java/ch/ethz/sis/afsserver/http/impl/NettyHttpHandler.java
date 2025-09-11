/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.afsserver.http.impl;

import ch.ethz.sis.afsserver.exception.HTTPExceptions;
import ch.ethz.sis.afsserver.http.HttpResponse;
import ch.ethz.sis.afsserver.http.HttpServerHandler;
import ch.ethz.sis.shared.log.standard.LogManager;
import ch.ethz.sis.shared.log.standard.Logger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.stream.ChunkedStream;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.OPTIONS;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static io.netty.handler.codec.http.HttpMethod.valueOf;

public class NettyHttpHandler extends ChannelInboundHandlerAdapter
{

    private static final Logger logger = LogManager.getLogger(NettyHttpServer.class);

    private static final byte[] NOT_FOUND = "404 NOT FOUND".getBytes();

    private static final ByteBuf NOT_FOUND_BUFFER = Unpooled.wrappedBuffer(NOT_FOUND);

    private static final Set<HttpMethod> allowedMethods = Set.of(GET, POST, PUT, DELETE, OPTIONS);


    private final Map<String, HttpServerHandler> httpServerHandler;

    public NettyHttpHandler(String uri, HttpServerHandler[] httpServerHandlersAux)
    {
        httpServerHandler = new ConcurrentHashMap<>(httpServerHandlersAux.length);
        for (HttpServerHandler httpServerHandler:httpServerHandlersAux) {
            this.httpServerHandler.put(uri + "/" + httpServerHandler.getPath(), httpServerHandler);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof FullHttpRequest)
        {
            final FullHttpRequest request = (FullHttpRequest) msg;
            QueryStringDecoder queryStringDecoderForPath = new QueryStringDecoder(request.uri(), true);

            HttpServerHandler httpServerHandler = this.httpServerHandler.get(queryStringDecoderForPath.path());
            if (httpServerHandler != null &&
                    allowedMethods.contains(request.method()))
            {
                if (OPTIONS.equals(request.method()))
                {
                    final String requestMethod = request.headers().get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD);

                    final HttpResponseStatus responseStatus;
                    if (requestMethod == null)
                    {
                        responseStatus = HttpResponseStatus.BAD_REQUEST;
                    } else if (!allowedMethods.contains(valueOf(requestMethod)))
                    {
                        responseStatus = HttpResponseStatus.METHOD_NOT_ALLOWED;
                    } else
                    {
                        responseStatus = HttpResponseStatus.OK;
                    }

                    final FullHttpResponse response = getHttpResponse(
                            responseStatus,
                            Map.of(HttpResponse.CONTENT_TYPE_HEADER, HttpResponse.CONTENT_TYPE_TEXT),
                            new EmptyByteBuf(ByteBufAllocator.DEFAULT),
                            0);
                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                } else
                {
                    ByteBuf content = request.content();
                    try
                    {
                        Map<String, List<String>> parameters =
                                queryStringDecoderForPath.parameters();

                        byte[] requestBody = new byte[content.readableBytes()];
                        content.readBytes(requestBody);

                        HttpResponse apiResponse = httpServerHandler.process(request.method(),
                                parameters, requestBody);

                        HttpResponseStatus status = null;
                        switch (apiResponse.getStatus()) {
                            case HttpResponse.OK:
                                status = HttpResponseStatus.OK;
                                break;
                            case HttpResponse.BAD_REQUEST:
                                status = HttpResponseStatus.BAD_REQUEST;
                                break;
                            case HttpResponse.NOT_FOUND:
                                status = HttpResponseStatus.NOT_FOUND;
                                break;
                            case HttpResponse.INTERNAL_SERVER_ERROR:
                                status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
                                break;
                            default:
                                HTTPExceptions.throwInstance(HTTPExceptions.UNKNOWN);
                        }

                        if (apiResponse.getStatus() == HttpResponse.OK &&
                                HttpResponse.CONTENT_TYPE_ZIP.equals(apiResponse.getHeaders().get(HttpResponse.CONTENT_TYPE_HEADER))) {
                            ctx.writeAndFlush(new ChunkedStream(apiResponse.getInput()))
                                    .addListener(ChannelFutureListener.CLOSE);
                        } else {
                            byte[] bytes = apiResponse.getInput().readAllBytes();
                            final FullHttpResponse response = getHttpResponse(
                                    status,
                                    apiResponse.getHeaders(),
                                    Unpooled.wrappedBuffer(bytes),
                                    bytes.length);
                            ctx.writeAndFlush(response)
                                    .addListener(ChannelFutureListener.CLOSE);
                        }
                    } finally
                    {
                        content.release();
                    }
                }
            } else
            {
                FullHttpResponse response = getHttpResponse(
                        HttpResponseStatus.NOT_FOUND,
                        Map.of(HttpResponse.CONTENT_TYPE_HEADER, HttpResponse.CONTENT_TYPE_TEXT),
                        NOT_FOUND_BUFFER,
                        NOT_FOUND.length);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        } else
        {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
    {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        logger.catching(cause);
        byte[] causeBytes = cause.getMessage().getBytes();
        FullHttpResponse response = getHttpResponse(
                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                Map.of(HttpResponse.CONTENT_TYPE_HEADER, HttpResponse.CONTENT_TYPE_TEXT),
                Unpooled.wrappedBuffer(causeBytes),
                causeBytes.length
        );
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public FullHttpResponse getHttpResponse(
            HttpResponseStatus status,
            Map<String, String> headers,
            ByteBuf content,
            int contentLength)
    {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                content
        );
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS,
                String.join(", ", allowedMethods.stream().map(HttpMethod::name).collect(Collectors.toUnmodifiableList())));
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");

        for (Map.Entry<String, String> header: headers.entrySet()) {
            response.headers().set(header.getKey(), header.getValue());
        }

        response.headers().set(HttpHeaderNames.CONNECTION, "close");
        return response;
    }

    public static Map<String, List<String>> getBodyParameters(byte[] array) {
        QueryStringDecoder queryStringDecoderForParameters =
                new QueryStringDecoder(new String(array, StandardCharsets.UTF_8), StandardCharsets.UTF_8, false);
        return  queryStringDecoderForParameters.parameters();
    }
}
