package settingdust.hoconresourceloader.mixin;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import settingdust.hoconresourceloader.HooksKt;

import java.util.Map;

@Mixin(ResourceFinder.class)
public class ResourceFinderMixin {
    @Inject(
            method = {"findResources", "findAllResources"},
            at = @At("HEAD"))
    private void recordCurrentResourceFinder(
            final ResourceManager resourceManager, final CallbackInfoReturnable<Map<Identifier, Resource>> cir) {
        HooksKt.getCurrentResourceFinder().set((ResourceFinder) (Object) this);
    }

    @Inject(
            method = {"findResources", "findAllResources"},
            at = @At("RETURN"))
    private void clearCurrentResourceFinder(
            final ResourceManager resourceManager, final CallbackInfoReturnable<Map<Identifier, Resource>> cir) {
        HooksKt.getCurrentResourceFinder().remove();
    }
}
