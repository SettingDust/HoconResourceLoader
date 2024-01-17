package settingdust.hoconresourceloader

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigSyntax
import java.io.InputStream
import java.lang.reflect.Constructor
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resource.InputSupplier
import net.minecraft.resource.NamespaceResourceManager
import net.minecraft.resource.Resource
import net.minecraft.resource.ResourceFinder
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourcePack
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import settingdust.hoconresourceloader.mixin.ResourceFinderAccessor

val currentResourceFinder = ThreadLocal<ResourceFinder?>()
val abortParsing = ThreadLocal<Boolean>()

fun canParsing() = !(abortParsing.get() ?: false)

val ResourceFinder.directoryName
    get() = (this as ResourceFinderAccessor).directoryName!!
val ResourceFinder.fileExtension
    get() = (this as ResourceFinderAccessor).fileExtension!!

private const val HOCON_SUFFIX = ".hocon"
private const val JSON_SUFFIX = ".json"

fun Identifier.toHocon() = Identifier(namespace, "${path.removeSuffix(JSON_SUFFIX)}$HOCON_SUFFIX")

fun Identifier.toJson() = Identifier(namespace, "${path.removeSuffix(HOCON_SUFFIX)}$JSON_SUFFIX")

@JvmOverloads
fun ResourcePack.openHoconResource(
    identifier: Identifier,
    type: ResourceType,
    manager: ResourceManager,
    resourceFinder: ResourceFinder? = currentResourceFinder.get()
): InputSupplier<InputStream>? {
    val resourceSupplier = open(type, identifier) ?: return null
    // Json file has no mcmeta. Needn't read
    val inputStream = resourceSupplier.get()
    val directoryName = resourceFinder?.directoryName
    return InputSupplier {
        ConfigFactory.parseReader(
                inputStream.reader(),
                ConfigParseOptions.defaults()
                    .setSyntax(ConfigSyntax.CONF)
                    .setIncluder(SimpleIncluder(manager, identifier, directoryName ?: ""))
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
    Class.forName(
        FabricLoader.getInstance()
            .mappingResolver
            .mapClassName("intermediary", "net.minecraft.class_3294\$class_7681")
    )
val ResultConstructor = ResultClass.declaredConstructors.single() as Constructor<out Record>

fun Result(pack: ResourcePack, supplier: InputSupplier<InputStream>, packIndex: Int): Record =
    ResultConstructor.newInstance(pack, supplier, packIndex)

@JvmOverloads
fun ResourcePack.findHoconResources(
    type: ResourceType,
    namespace: String,
    index: Int,
    map: MutableMap<Identifier, Record>,
    manager: ResourceManager,
    resourceFinder: ResourceFinder = currentResourceFinder.get()!!
) {
    return findResources(
        type,
        namespace,
        resourceFinder.directoryName,
    ) { id, _ ->
        if (!id.path.endsWith(HOCON_SUFFIX)) return@findResources
        val inputSupplier = openHoconResource(id, type, manager, resourceFinder)
        if (inputSupplier != null) map[id.toJson()] = Result(this, inputSupplier, index)
    }
}

@JvmOverloads
fun ResourcePack.findAndAddHoconResources(
    type: ResourceType,
    namespace: String,
    map: MutableMap<Identifier, NamespaceResourceManager.EntryList>,
    manager: ResourceManager,
    resourceFinder: ResourceFinder = currentResourceFinder.get()!!
) {
    return findResources(
        type,
        namespace,
        resourceFinder.directoryName,
    ) { id, _ ->
        if (!id.path.endsWith(HOCON_SUFFIX)) return@findResources
        val inputSupplier = openHoconResource(id, type, manager, resourceFinder)
        if (inputSupplier != null)
            map.computeIfAbsent(id.toJson()) { NamespaceResourceManager.EntryList(it) }
                .fileSources += NamespaceResourceManager.FileSource(this, inputSupplier)
    }
}
