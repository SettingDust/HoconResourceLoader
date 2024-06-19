package settingdust.hoconresourceloader.mixin.client;

import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import settingdust.hoconresourceloader.HooksKt;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableResourceManagerImpl.class)
public class ReloadableResourceManagerImplMixin {
    @Inject(method = "reload", at = @At("HEAD"))
    private void hoconresourceloader$reload(
        final Executor prepareExecutor,
        final Executor applyExecutor,
        final CompletableFuture<Unit> initialStage,
        final List<ResourcePack> packs,
        final CallbackInfoReturnable<ResourceReload> cir
    ) {
        HooksKt.getPackMetaCache().invalidateAll();
    }
}
