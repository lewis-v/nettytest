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
			sb.group(group) // ���̳߳�
			.channel(NioServerSocketChannel.class) // ָ��ʹ�õ�channel
			.localAddress(this.port)// �󶨼����˿�
			.childHandler(new ChannelInitializer<SocketChannel>() { // �󶨿ͻ�������ʱ�򴥷�����

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					System.out.println("connected...; Client:" + ch.remoteAddress());
					//                                    ch.pipeline().addLast(new EchoServerHandler()); // �ͻ��˴�������

					ch.pipeline()
//					.addLast(new ObjectEncoder())
//					.addLast(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.weakCachingConcurrentResolver(null))) // ��󳤶�
					//����ѹ��
					.addLast(new HttpContentCompressor())
					// server�˽��յ�����httpRequest������Ҫʹ��HttpRequestDecoder���н���  
					.addLast("decoder",new HttpRequestDecoder(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE))
					.addLast("aggregator",new HttpObjectAggregator(port))
					// server�˷��͵���httpResponse������Ҫʹ��HttpResponseEncoder���б���  
					.addLast("encoder",new HttpResponseEncoder())
					//������ļ�����,�鴫��
					.addLast("chunkedWriter",new ChunkedWriteHandler())
					.addLast("serverHandler", new HttpServerInboundHandler());  
				}
			});
			ChannelFuture cf = sb.bind().sync(); // �������첽������
			System.out.println(EchoServer.class + " started and listen on " + cf.channel().localAddress());
			cf.channel().closeFuture().sync(); // �رշ�����ͨ��
		} finally {
			group.shutdownGracefully().sync(); // �ͷ��̳߳���Դ
		}
	}

	public static void main(String[] args) throws Exception {
		new EchoServer(47423).start(); // ����
	}
}