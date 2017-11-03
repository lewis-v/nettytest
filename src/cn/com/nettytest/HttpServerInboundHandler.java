package cn.com.nettytest;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;  
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;  
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;  
import static io.netty.handler.codec.http.HttpResponseStatus.OK;  
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;  
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;  
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;  
import io.netty.channel.ChannelInboundHandlerAdapter;  
import io.netty.handler.codec.http.DefaultFullHttpResponse;  
import io.netty.handler.codec.http.FullHttpResponse;  
import io.netty.handler.codec.http.HttpContent;  
import io.netty.handler.codec.http.HttpHeaders;  
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MixedFileUpload;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;

public class HttpServerInboundHandler extends ChannelInboundHandlerAdapter {  
	private HttpRequest request;  
	private String Log = "----------start----------\n";

	@Override  
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {  
		String boundary = "";
		request = (HttpRequest)msg;
		//信息处理
		FullHttpRequest fullReq = (FullHttpRequest)msg;
		Map<String, String> parmMap = new HashMap<>();
		Map<String, File> parmMapFile = new HashMap<>();
		HttpMethod method = fullReq.method();
		if (HttpMethod.GET == method) {
			// 是GET请求
			Log = Log + "GET:\n";
			QueryStringDecoder decoder = new QueryStringDecoder(fullReq.uri());
			decoder.parameters().entrySet().forEach( entry -> {
				// entry.getValue()是一个List, 只取第一个元素
				parmMap.put(entry.getKey(), entry.getValue().get(0));
				Log = Log + entry.getKey()+":"+entry.getValue().get(0)+"\n";
			});
		} else if (HttpMethod.POST == method) {
			// 是POST请求
			Log = Log + "POST:\n";
			HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(fullReq);
			decoder.offer(fullReq);
			List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
			for (InterfaceHttpData parm : parmList) {
				if (parm instanceof Attribute) {
					Attribute data = (Attribute) parm;
					parmMap.put(data.getName(), data.getValue());
					Log = Log + data.getName()+":"+data.getValue()+"\n";
				}else if(parm instanceof MixedFileUpload) {
					MixedFileUpload upload = (MixedFileUpload)parm;
					parmMapFile.put(upload.getName(), upload.getFile());
					Log = Log + upload.getName()+":"+upload.getFile()+"\n";
				}
			}

		} 

		//返回信息
		HttpContent content = (HttpContent) msg;  
		ByteBuf buf = content.content();  
		String postContent = buf.toString(io.netty.util.CharsetUtil.UTF_8);
		Log = Log + "postContent:"+postContent + "\n";
		buf.release();  

		String res = "{data:JSON}";  
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK,  
				Unpooled.wrappedBuffer(res.getBytes("UTF-8")));  
		response.headers().set(CONTENT_TYPE,Values.APPLICATION_JSON);  
		response.headers().set(CONTENT_LENGTH, response.content().readableBytes());  
		if (HttpHeaders.isKeepAlive(request)) {  
			response.headers().set(CONNECTION, Values.KEEP_ALIVE);  
		}
		Log = Log +response.toString()+"\n";
		ctx.write(response);  
		ctx.flush();  

	}  

	@Override  
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {  
		ctx.flush();  

		//		ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);

		System.out.println(Log+"----------end----------\n");
	}  

	@Override  
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {  
		System.out.println("ERR:"+cause.getMessage());  
		String result = "{message:\""+cause.getMessage()+"\"}";
		FullHttpResponse response;
		try {
			response = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND,  
					Unpooled.wrappedBuffer(result.getBytes("UTF-8")));

			response.headers().set(CONTENT_TYPE,Values.APPLICATION_JSON);  
			response.headers().set(CONTENT_LENGTH, response.content().readableBytes());  
			if (HttpHeaders.isKeepAlive(request)) {  
				response.headers().set(CONNECTION, Values.KEEP_ALIVE);  
			}
			Log = Log +response.toString()+"\n";
			ctx.write(response);  
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		ctx.flush();   
		System.out.println(Log+"----------end----------\n");
	}  
}  
