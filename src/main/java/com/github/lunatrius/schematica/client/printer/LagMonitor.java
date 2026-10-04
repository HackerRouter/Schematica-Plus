// Printer lag check after the behavior of litematica-printer: pause while no packet arrives from the server, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.network.NetworkManager;

import com.github.lunatrius.schematica.reference.Reference;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public final class LagMonitor {
    private static volatile long lastPacket;

    private LagMonitor() {}

    /** Watches the incoming packets of a new connection; without the hook the check never pauses. */
    public static void install(NetworkManager manager) {
        lastPacket = 0;
        try {
            manager.channel().pipeline().addBefore("packet_handler", "schematica_plus:lag_monitor", new ChannelInboundHandlerAdapter() {
                @Override public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                    lastPacket = System.nanoTime();
                    super.channelRead(context, message);
                }
            });
        } catch (RuntimeException error) {
            Reference.logger.debug("Could not watch the connection for the printer lag check", error);
        }
    }

    /** Client ticks since the last packet from the server, 0 before the first one. */
    public static long ticksSincePacket() {
        long last = lastPacket;
        return last == 0 ? 0 : (System.nanoTime() - last) / 50_000_000L;
    }
}
