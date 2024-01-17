package settingdust.hoconresourceloader

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigSyntax
import java.io.InputStream
import java.lang.reflect.Constructor
import net.minecraft.resource.InputSupplier
import net.minecraft.resource.NamespaceResourceManager
import net.minecraft.resource.Resource
import net.minecraft.resource.ResourceFinder
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourcePack
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import settingdust.hoconresourceloader.mixin.ResourceFinderAccessor

var currentResourceFinder = ThreadLocal<ResourceFinder?>()

val ResourceFinder.directoryName
    get() = (this as ResourceFinderAccessor).directoryName!!
val ResourceFinder.fileExtension
    get() = (this as ResourceFinderAccessor).fileExtension!!

private const val HOCON_SUFFIX = ".hocon"
private const val JSON_SUFFIX = ".json"

fun Identifier.toHocon() = Identifier(namespace, "${path.removeSuffix(JSON_SUFFIX)}$HOCON_SUFFIX")

fun Identifier.toJson() = Identifier(namespace, "${path.removeSuffix(HOCON_SUFFIX)}$JSON_SUFFIX")

fun ResourcePack.openHoconResource(
    identifier: Identifier,
    type: ResourceType,
    manager: ResourceManager
): InputSupplier<InputStream>? {
    val resourceSupplier = open(type, identifier) ?: return null
    // Json file has no mcmeta. Needn't read
    val inputStream = resourceSupplier.get()
    val directoryName = currentResourceFinder.get()?.directoryName
    return InputSupplier {
        ConfigFactory.parseReader(
                inputStream.reader(),
                ConfigParseOptions.defaults()
                    .setSyntax(ConfigSyntax.CONF)
                    .setIncluder(SimpleIncluder(manager, directoryName ?: ""))
            )
            .root()
            .render(ConfigRenderOptions.concise())
            .encodeToByteArray()
            .inputStream()
    }
}

fun ResourcePack.readResource(inputSupplier: InputSupplier<InputStream>) =
    Resource(this, inputSupplier)

val ResultClass =
    Class.forName("net.minecraft.resource.NamespaceResourceManager\$Result") as Class<out Record>
val ResultConstructor = ResultClass.constructors.single() as Constructor<out Record>

fun Result(pack: ResourcePack, supplier: InputSupplier<InputStream>, packIndex: Int): Record =
    ResultConstructor.newInstance(pack, supplier, packIndex)

fun ResourcePack.findHoconResources(
    type: ResourceType,
    namespace: String,
    index: Int,
    map: MutableMap<Identifier, Record>,
    manager: ResourceManager
) {
    val resourceFinder = currentResourceFinder.get()!!
    return findResources(
        type,
        namespace,
        resourceFinder.directoryName,
    ) { id, _ ->
        if (!id.path.endsWith(HOCON_SUFFIX)) return@findResources
        val inputSupplier = openHoconResource(id, type, manager)
        if (inputSupplier != null) map[id.toJson()] = Result(this, inputSupplier, index)
    }
}

fun ResourcePack.findAndAddHoconResources(
    type: ResourceType,
    namespace: String,
    map: MutableMap<Identifier, NamespaceResourceManager.EntryList>,
    manager: ResourceManager
) {
    val resourceFinder = currentResourceFinder.get()!!
    return findResources(
        type,
        namespace,
        resourceFinder.directoryName,
    ) { id, _ ->
        if (!id.path.endsWith(HOCON_SUFFIX)) return@findResources
        val inputSupplier = openHoconResource(id, type, manager)
        if (inputSupplier != null)
            map.computeIfAbsent(id.toJson()) { NamespaceResourceManager.EntryList(it) }
                .fileSources += NamespaceResourceManager.FileSource(this, inputSupplier)
    }
}
