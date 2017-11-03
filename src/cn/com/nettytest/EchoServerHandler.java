package cn.com.nettytest;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    	 ByteBuf in = (ByteBuf) msg;  
         byte[] req = new byte[in.readableBytes()];  
         in.readBytes(req);  
         String body = new String(req,"utf-8");  
         System.out.println("�յ��ͻ�����Ϣ:"+body); 
        System.out.println("server channelRead...; received:" + msg);
        ctx.write(Unpooled.copiedBuffer(body.getBytes()));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("server channelReadComplete..");
        // ��һ�ַ�����дһ���յ�buf����ˢ��д��������ɺ�ر�sock channel���ӡ�
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        //ctx.flush(); // �ڶ��ַ�������client�˹ر�channel���ӣ������Ļ����ᴥ������channelReadComplete������
        //ctx.flush().close().sync(); // �����֣��ĳ�����д��Ҳ���ԣ���������д����û�е�һ�ַ����ĺá�
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("server occur exception:" + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // �رշ����쳣������
    }
}