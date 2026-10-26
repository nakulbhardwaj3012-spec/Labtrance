package dev.labtrace.mixin;

import dev.labtrace.LabTraceClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.KeyboardHandler;
import org.lwjgl.glfw.GLFW;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress(JIIII)V", at = @At("HEAD"))
    private void labtrace$moduleKeybinds(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS) {
            LabTraceClient.handleModuleKey(key);
        }
    }
}
