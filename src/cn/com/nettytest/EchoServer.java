package cn.com.nettytest;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

public class EchoServer {
	private final int port;

	public EchoServer(int port) {
		this.port = port;
	}

	public void start() throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			ServerBootstrap sb = new ServerBootstrap();
			sb.group(group) // 绑定线程池
			.channel(NioServerSocketChannel.class) // 指定使用的channel
			.localAddress(this.port)// 绑定监听端口
			.childHandler(new ChannelInitializer<SocketChannel>() { // 绑定客户端连接时候触发操作

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					System.out.println("connected...; Client:" + ch.remoteAddress());
					//                                    ch.pipeline().addLast(new EchoServerHandler()); // 客户端触发操作

					ch.pipeline()
//					.addLast(new ObjectEncoder())
//					.addLast(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.weakCachingConcurrentResolver(null))) // 最大长度
					//内容压缩
					.addLast(new HttpContentCompressor())
					// server端接收到的是httpRequest，所以要使用HttpRequestDecoder进行解码  
					.addLast("decoder",new HttpRequestDecoder(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE))
					.addLast("aggregator",new HttpObjectAggregator(port))
					// server端发送的是httpResponse，所以要使用HttpResponseEncoder进行编码  
					.addLast("encoder",new HttpResponseEncoder())
					//处理大文件传输,块传输
					.addLast("chunkedWriter",new ChunkedWriteHandler())
					.addLast("serverHandler", new HttpServerInboundHandler());  
				}
			});
			ChannelFuture cf = sb.bind().sync(); // 服务器异步创建绑定
			System.out.println(EchoServer.class + " started and listen on " + cf.channel().localAddress());
			cf.channel().closeFuture().sync(); // 关闭服务器通道
		} finally {
			group.shutdownGracefully().sync(); // 释放线程池资源
		}
	}

	public static void main(String[] args) throws Exception {
		new EchoServer(47423).start(); // 启动
	}
}