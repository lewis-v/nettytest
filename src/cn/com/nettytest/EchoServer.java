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

                                    // server�˽��յ�����httpRequest������Ҫʹ��HttpRequestDecoder���н���  
                                    ch.pipeline().addLast(new HttpRequestDecoder());
                                    ch.pipeline().addLast(new HttpObjectAggregator(port));
                                    // server�˷��͵���httpResponse������Ҫʹ��HttpResponseEncoder���б���  
                                    ch.pipeline().addLast(new HttpResponseEncoder());  
                                    //������ļ�����,�鴫��
                                    ch.pipeline().addLast(new ChunkedWriteHandler());
                                    ch.pipeline().addLast(new HttpContentCompressor());
                                    ch.pipeline().addLast(new HttpServerInboundHandler());  
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