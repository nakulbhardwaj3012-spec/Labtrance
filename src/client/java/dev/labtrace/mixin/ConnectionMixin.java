package dev.labtrace.mixin;

import dev.labtrace.LabTraceClient;
import dev.labtrace.event.PacketReceiveEvent;
import dev.labtrace.event.PacketSendEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes packet class names only. It does not cancel, alter, replay, or forge packets. */
@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void labtrace$onPacketSend(Packet<?> packet, CallbackInfo ci) {
        LabTraceClient.post(new PacketSendEvent(
                LabTraceClient.tick(), packet.getClass().getName(), Thread.currentThread().getName()));
    }

    @Inject(method = "channelRead0", at = @At("HEAD"))
    private void labtrace$onPacketReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        LabTraceClient.post(new PacketReceiveEvent(
                LabTraceClient.tick(), packet.getClass().getName(), Thread.currentThread().getName()));
    }
}
