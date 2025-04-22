package meteordevelopment.meteorclient.mixin;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.SplashOverlay;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private static void init(CallbackInfo ci) throws IOException {
        ci.cancel(); // allow resource packs to override mojang logo
    }
}
