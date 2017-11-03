package cn.com.nettytest;

/**
 * ����HTTP�ļ��ϴ��Ĵ�����
 */
import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.COOKIE;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Set;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;
import net.sf.json.JSONObject;

public class HttpFileHandler extends SimpleChannelInboundHandler<HttpObject>{
    private HttpRequest request;
    private boolean readingChunks;
    private final StringBuilder responseContent = new StringBuilder();
    
    private static final HttpDataFactory factory =
            new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk if size exceed

    private HttpPostRequestDecoder decoder;

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
                                                         // on exit (in normal
                                                         // exit)
        DiskFileUpload.baseDirectory = null; // system temp directory
        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
                                                        // exit (in normal exit)
        DiskAttribute.baseDirectory = null; // system temp directory
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        // TODO Auto-generated method stub
        JSONObject jObject = new JSONObject();
        jObject.put("code", "404");
        jObject.put("msg", "page not found");
        
        if (msg instanceof HttpRequest) {
            this.request = (HttpRequest) msg;
            URI uri = new URI(request.getUri());
            String path = uri.getPath();
            
            setCookie(this.request);
            
            /* url���ֵ���������� */
            if (!path.startsWith("/feed") || request.getMethod().equals(HttpMethod.GET)) {
                writeResponseString(ctx, jObject.toString());
                return;
            } else {
                try {
                decoder = new HttpPostRequestDecoder(factory, request);
                } catch (ErrorDataDecoderException e1) {
                    e1.printStackTrace();
                    responseContent.append(e1.getMessage());
                    writeResponseString(ctx, jObject.toString());
                    ctx.channel().close();
                    return;
                } 
//                catch (IncompatibleDataDecoderException e1) {
//                    // ��ȻGet����Ҫ�������������Լ�ʵ��ҵ���ʱ�����̲�Ҫ����������
//                    // GET Method: should not try to create a HttpPostRequestDecoder
//                    // So OK but stop here
//                    writeResponseString(ctx, jObject.toString());
//                    return;
//                }
                
                readingChunks = HttpHeaders.isTransferEncodingChunked(request);
                //������Կ����Ƿ����ϴ��ļ�����
                if (readingChunks) {
                    // Chunk version
                    readingChunks = true;
                }
            }
        } else if (msg instanceof HttpContent) {
            if (decoder != null && readingChunks) {
                HttpContent chunk = (HttpContent) msg;
                try{
                    decoder.offer(chunk);
                } catch (ErrorDataDecoderException e1) {
                    writeResponseString(ctx, jObject.toString());
                    reset();
                    ctx.channel().close();
                    return;
                }
                readHttpDataChunkByChunk(); //�ӽ�����decoder�ж�������
                if (chunk instanceof LastHttpContent) {
                    writeResponseString(ctx, "{\"code\":0,\"msg\":\"ok\"}");
                    readingChunks = false;
                    reset();
                }
            } else {
                writeResponseString(ctx, jObject.toString());
                ctx.channel().close();
                return;
            }
        }
    }

    /**
     * Example of reading request by chunk and getting values from chunk to chunk
     * ��decoder�ж������ݣ�д����ʱ����Ȼ��д��...���
     * �����װ��Ҫ��Ϊ���ͷ���ʱ����
     */
    private void readHttpDataChunkByChunk() {
        try {
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data != null) {
                    try {
                        // new value
                        writeHttpData(data);
                    } finally {
                        data.release();
                    }
                }
            }
        } catch (EndOfDataDecoderException e1) {
            // end
            responseContent.append("\r\n\r\nEND OF CONTENT CHUNK BY CHUNK\r\n\r\n");
        }
    }
    
    /**
     * ����cookie
     */
    private void setCookie(HttpRequest request) {
        Set<Cookie> cookies;
        String value = request.headers().get(COOKIE);
        if (value == null) {
            cookies = Collections.emptySet();
        } else {
            cookies = ServerCookieDecoder.LAX.decode(value);
        }
        for (Cookie cookie : cookies) {
            responseContent.append("COOKIE: " + cookie + "\r\n");
        }
        responseContent.append("\r\n\r\n");
    }
    /**
     * ��װӦ��Ļ�д
     * @param ctx
     * @param message String����Ϣ��Header���Ѿ�����ΪApplication/json
     */
    private void writeResponseString(ChannelHandlerContext ctx, String message) {
        // Convert the response content to a ChannelBuffer.
        responseContent.setLength(0);
        responseContent.append(message);

        ByteBuf buf = copiedBuffer(responseContent.toString(), CharsetUtil.UTF_8);
        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);

        response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(CONTENT_LENGTH, buf.readableBytes());

        // Write the response.
        ctx.channel().writeAndFlush(response);
    }

    private void reset() {
        request = null;

        // destroy the decoder to release all resources
        decoder.destroy();
        decoder = null;
    }
    
    private void writeHttpData(InterfaceHttpData data) {
        // Attribute����form������ĸ��� name= ������
        if (data.getHttpDataType() == HttpDataType.Attribute) {
        } else if (data.getHttpDataType() == HttpDataType.InternalAttribute){
        }else{
            String uploadFileName = getUploadFileName(data);
            FileUpload fileUpload = (FileUpload) data;
            if (fileUpload.isCompleted()) {
                // fileUpload.isInMemory();// tells if the file is in Memory
                // or on File
                // fileUpload.renameTo(dest); // enable to move into another
                // File dest
                // decoder.removeFileUploadFromClean(fileUpload); //remove
                // the File of to delete file
                File dir = new File(Property.getSaveFileDir() + "download" + File.separator);
                if (!dir.exists()) {
                    dir.mkdir();
                }
                File dest = new File(dir, uploadFileName);
                try {
                    fileUpload.renameTo(dest);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    private String getUploadFileName(InterfaceHttpData data) {
        String content = data.toString();
        String temp = content.substring(0, content.indexOf("\n"));
        content = temp.substring(temp.lastIndexOf("=") + 2, temp.lastIndexOf("\""));
        return content;
    }
}
