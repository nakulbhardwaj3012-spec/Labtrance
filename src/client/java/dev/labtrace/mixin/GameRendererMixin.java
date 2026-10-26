package dev.labtrace.mixin;

import dev.labtrace.LabTraceClient;
import dev.labtrace.event.RenderEvent;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void labtrace$renderStart(CallbackInfo ci) {
        LabTraceClient.post(new RenderEvent(LabTraceClient.tick(), RenderEvent.Phase.START));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void labtrace$renderEnd(CallbackInfo ci) {
        LabTraceClient.post(new RenderEvent(LabTraceClient.tick(), RenderEvent.Phase.END));
    }
}
