package com.yeespec.microscope.master.service.server.socket;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Created by virgilyan on 16/2/3.
 */
public class NettySocketServer {

    public NettySocketServer(final int port) {
        // TODO Auto-generated method stub
        final EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            final ChannelPipeline pipeline = ch.pipeline();
                            /**
                             * 使用LengthFieldBasedFrameDecoder作为decoder实现，LengthFieldBasedFrameDecoder构造函数，第一个参数为信息最大长度，超过这个长度回报异常，第二参数为长度属性的起始（偏移）位，我们的协议中长度是0到第3个字节，所以这里写0，第三个参数为“长度属性”的长度，我们是4个字节，所以写4，第四个参数为长度调节值，在总长被定义为包含包头长度时，修正信息长度，第五个参数为跳过的字节数，根据需要我们跳过前4个字节，以便接收端直接接受到不含“长度属性”的内容。
                             */
                            pipeline.addFirst(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            pipeline.addFirst(new LengthFieldPrepender(4, false));
                            pipeline.addLast(
                                    new DiscardServerHandler(),
                                    new LengthFieldPrepender(8, 4, true),
                                    new ChunkedWriteHandler());
                        }
                    })
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.ALLOW_HALF_CLOSURE, true)
                    .option(ChannelOption.WRITE_SPIN_COUNT, 16)
                    .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
                    .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 64 * 1024)
                    .option(ChannelOption.SO_BACKLOG, 128)          // 连接数
                    .option(ChannelOption.SO_LINGER, 100)
                    .option(ChannelOption.SO_BROADCAST, true)       // 广播
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // 长连接


            // Bind and start to accept incoming connections.
            ChannelFuture f = null; // (7)
            try {
                f = b.bind(port).sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            try {
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static final class DiscardServerHandler extends ChannelInboundHandlerAdapter {

        private static final Collection<ChannelHandlerContext> connections = new HashSet<ChannelHandlerContext>();

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            addConnection(ctx);
            super.channelRegistered(ctx);
            System.out.println("有一新客户端注册");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // TODO Auto-generated method stub
            ByteBuf buf = (ByteBuf) msg;
            String recieved = getMessage(buf);
            System.out.println("服务器接收到消息：" + recieved);
            sendToAll("Receive message".getBytes());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            // TODO Auto-generated method stub
            System.err.println("exceptionCaught 远程连接断开后是这里抛出的错误...");
            cause.printStackTrace();
            ctx.close();
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            removeConnection(ctx);
            super.channelUnregistered(ctx);
            System.out.println("有一客户端注销");
        }

        private String getMessage(ByteBuf buf) {
            byte[] con = new byte[buf.readableBytes()];
            buf.readBytes(con);
            try {
                return new String(con, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected boolean addConnection(ChannelHandlerContext ctx) {
            synchronized (connections) {
                return connections.add(ctx);
            }
        }

        protected boolean removeConnection(ChannelHandlerContext ctx) {
            synchronized (connections) {
                for (ChannelHandlerContext ctx0 : connections) {
                    if (ctx0.name().equals(ctx.name()))
                        return connections.remove(ctx);
                }
                return false;
            }
        }

        public static void sendToAll(byte[] content) {
            synchronized (connections) {
                for (ChannelHandlerContext ctx : connections) {
                    System.out.println("发送广播-" + ctx.name() + "-" + content.length);
                    ByteBuf byteBuf = Unpooled.wrappedBuffer(content);
                    ctx.writeAndFlush(byteBuf);

                }
            }
        }

    }

}
