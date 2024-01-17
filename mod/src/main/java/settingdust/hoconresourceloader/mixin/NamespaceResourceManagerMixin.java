package settingdust.hoconresourceloader.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resource.*;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import settingdust.hoconresourceloader.HooksKt;

@Mixin(NamespaceResourceManager.class)
public abstract class NamespaceResourceManagerMixin {
    @Shadow
    @Final
    private ResourceType type;

    @Shadow
    private static InputSupplier<InputStream> wrapForDebug(
            final Identifier id, final ResourcePack pack, final InputSupplier<InputStream> supplier) {
        return null;
    }

    @Shadow
    @Final
    private String namespace;

    @Inject(
            method = {"getResource", "getAllResources"},
            at = @At(value = "HEAD"))
    private void getResource$determineJson(
            final Identifier identifier,
            final CallbackInfoReturnable<Optional<Resource>> cir,
            @Share("isJson") LocalBooleanRef isJson) {
        if (identifier.getPath().endsWith(".json")) isJson.set(true);
    }

    @Inject(
            method = "getResource",
            at =
                    @At(
                            value = "INVOKE",
                            shift = At.Shift.BEFORE,
                            target = "Lnet/minecraft/resource/NamespaceResourceManager$FilterablePack;isFiltered"
                                    + "(Lnet/minecraft/util/Identifier;)Z"),
            cancellable = true)
    private void getResource$readHoconResource(
            final Identifier identifier,
            final CallbackInfoReturnable<Optional<Resource>> cir,
            @Local ResourcePack resourcePack,
            @Share("isJson") LocalBooleanRef isJson) {
        if (resourcePack == null || !isJson.get()) return;
        final var hoconId = HooksKt.toHocon(identifier);
        final var inputSupplier = HooksKt.openHoconResource(resourcePack, hoconId, type, (ResourceManager) this);
        if (inputSupplier != null)
            cir.setReturnValue(Optional.of(
                    HooksKt.readResource(resourcePack, wrapForDebug(hoconId, resourcePack, inputSupplier))));
    }

    @Inject(
            method = "getAllResources",
            at =
                    @At(
                            value = "INVOKE",
                            shift = At.Shift.BEFORE,
                            ordinal = 0,
                            target = "Lnet/minecraft/resource/NamespaceResourceManager$FilterablePack;isFiltered"
                                    + "(Lnet/minecraft/util/Identifier;)Z"))
    private void getAllResources$readHoconResources(
            final Identifier id,
            final CallbackInfoReturnable<List<Resource>> cir,
            @Local(ordinal = 0) ResourcePack resourcePack,
            @Share("isJson") LocalBooleanRef isJson,
            @Local List<Resource> list) {
        if (resourcePack == null || !isJson.get()) return;
        final var hoconId = HooksKt.toHocon(id);
        final var inputSupplier = HooksKt.openHoconResource(resourcePack, hoconId, type, (ResourceManager) this);
        if (inputSupplier != null)
            list.add(HooksKt.readResource(resourcePack, wrapForDebug(hoconId, resourcePack, inputSupplier)));
    }

    @Inject(method = "findResources", at = @At("HEAD"))
    private void findResources$determineJson(
            final String startingPath,
            final Predicate<Identifier> allowedPathPredicate,
            final CallbackInfoReturnable<Map<Identifier, Resource>> cir,
            @Share("isJson") LocalBooleanRef isJson) {
        final var resourceFinder = HooksKt.getCurrentResourceFinder().get();
        if (resourceFinder != null && HooksKt.getFileExtension(resourceFinder).equals(".json")) {
            isJson.set(true);
        }
    }

    @Inject(
            method = "findResources",
            at =
                    @At(
                            value = "INVOKE",
                            shift = At.Shift.AFTER,
                            target =
                                    "Lnet/minecraft/resource/ResourcePack;findResources(Lnet/minecraft/resource/ResourceType;"
                                            + "Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/resource/ResourcePack$ResultConsumer;)V"))
    private void findResources$findHocon(
            final CallbackInfoReturnable<Map<Identifier, Resource>> cir,
            @Share("isJson") LocalBooleanRef isJson,
            @Local ResourcePack resourcePack,
            @Local(ordinal = 1) int index,
            @Local(ordinal = 0) Map<Identifier, Record> map) {
        if (!isJson.get()) return;
        HooksKt.findHoconResources(resourcePack, type, namespace, index, map, (ResourceManager) this);
    }

    @Inject(method = "findAndAdd", at = @At("HEAD"))
    private void findAndAdd$determineJson(final CallbackInfo ci, @Share("isJson") LocalBooleanRef isJson) {
        final var resourceFinder = HooksKt.getCurrentResourceFinder().get();
        if (resourceFinder != null && HooksKt.getFileExtension(resourceFinder).equals(".json")) {
            isJson.set(true);
        }
    }

    @Inject(
            method = "findAndAdd",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/resource/ResourcePack;findResources(Lnet/minecraft/resource/ResourceType;"
                                            + "Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/resource/ResourcePack$ResultConsumer;)V"))
    private void findAndAdd$findHocon(
            final CallbackInfo ci,
            @Share("isJson") LocalBooleanRef isJson,
            @Local ResourcePack resourcePack,
            @Local Map<Identifier, NamespaceResourceManager.EntryList> idToEntryList) {
        if (!isJson.get()) return;
        HooksKt.findAndAddHoconResources(resourcePack, type, namespace, idToEntryList, (ResourceManager) this);
    }
}
