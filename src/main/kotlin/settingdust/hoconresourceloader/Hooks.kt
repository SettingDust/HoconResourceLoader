package settingdust.hoconresourceloader

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.gson.JsonObject
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigSyntax
import java.io.InputStream
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resource.InputSupplier
import net.minecraft.resource.NamespaceResourceManager
import net.minecraft.resource.Resource
import net.minecraft.resource.ResourceFinder
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourcePack
import net.minecraft.resource.ResourceType
import net.minecraft.resource.metadata.ResourceMetadataReader
import net.minecraft.util.Identifier
import settingdust.hoconresourceloader.mixin.ResourceFinderAccessor

object MetadataParser : ResourceMetadataReader<Boolean> {
    override fun getKey() = "hoconresourceloader"

    override fun fromJson(json: JsonObject): Boolean {
        return json.getAsJsonPrimitive("enabled").asBoolean
    }
}

val packMetaCache: LoadingCache<ResourcePack, Boolean> =
    CacheBuilder.newBuilder()
        .build(
            object : CacheLoader<ResourcePack, Boolean>() {
                override fun load(key: ResourcePack): Boolean {
                    return try {
                        val enabled = key.parseMetadata(MetadataParser)
                        if (enabled == true)
                            HoconResourceLoader.LOGGER.info("Enabled for ${key.info.id}")
                        enabled
                    } catch (e: Throwable) {
                        HoconResourceLoader.LOGGER.warn("Failed to parse metadata", e)
                        false
                    } ?: false
                }
            }
        )

val currentResourceFinder = ThreadLocal<ResourceFinder?>()
val abortParsing = ThreadLocal<Boolean>()

fun canParsing() = !(abortParsing.get() ?: false)

val ResourceFinder.directoryName
    get() = (this as ResourceFinderAccessor).directoryName!!
val ResourceFinder.fileExtension
    get() = (this as ResourceFinderAccessor).fileExtension!!

const val HOCON_SUFFIX = ".hocon"
const val JSON_SUFFIX = ".json"

fun Identifier.toHocon() = Identifier.of(namespace, "${path.removeSuffix(JSON_SUFFIX)}$HOCON_SUFFIX")

fun Identifier.toJson() = Identifier.of(namespace, "${path.removeSuffix(HOCON_SUFFIX)}$JSON_SUFFIX")

@JvmOverloads
fun ResourcePack.openHoconResource(
    identifier: Identifier,
    type: ResourceType,
    manager: ResourceManager,
    resourceFinder: ResourceFinder? = currentResourceFinder.get()
): InputSupplier<InputStream>? {
    val resourceSupplier = open(type, identifier) ?: return null
    // Json file has no mcmeta. Needn't read
    val directoryName = resourceFinder?.directoryName

    return InputSupplier {
        ConfigFactory.parseReader(
                resourceSupplier.get().reader(),
                ConfigParseOptions.defaults()
                    .setSyntax(ConfigSyntax.CONF)
                    .setIncluder(SimpleIncluder(manager, identifier, directoryName ?: ""))
            )
            .resolve()
            .root()
            .render(ConfigRenderOptions.concise())
            .encodeToByteArray()
            .inputStream()
    }
}

fun ResourcePack.readResource(inputSupplier: InputSupplier<InputStream>) =
    Resource(this, inputSupplier)

val ResultClass: Class<*> by lazy {
    Class.forName(
        FabricLoader.getInstance()
            .mappingResolver
            .mapClassName("intermediary", "net.minecraft.class_3294\$class_7681")
    )
}

val ResultConstructorType: MethodType by lazy {
    MethodType.methodType(
        Void.TYPE,
        ResourcePack::class.java,
        InputSupplier::class.java,
        Int::class.javaPrimitiveType
    )
}

val ResultConstructorHandle: MethodHandle by lazy {
    MethodHandles.privateLookupIn(ResultClass, MethodHandles.lookup())
        .findConstructor(ResultClass, ResultConstructorType)
}

@Suppress("FunctionName")
fun Result(pack: ResourcePack, supplier: InputSupplier<InputStream>, packIndex: Int): Record =
    ResultConstructorHandle.invoke(pack, supplier, packIndex) as Record

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
