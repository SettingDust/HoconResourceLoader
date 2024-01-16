package settingdust.hoconresourceloader.mixin;

import net.minecraft.resource.ResourceFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResourceFinder.class)
public interface ResourceFinderAccessor {
    @Accessor
    String getDirectoryName();

    @Accessor
    String getFileExtension();
}
