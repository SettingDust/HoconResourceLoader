package settingdust.hoconresourceloader

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import java.io.InputStream
import java.lang.reflect.Constructor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import net.minecraft.resource.InputSupplier
import net.minecraft.resource.NamespaceResourceManager
import net.minecraft.resource.Resource
import net.minecraft.resource.ResourceFinder
import net.minecraft.resource.ResourcePack
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import settingdust.hoconresourceloader.mixin.ResourceFinderAccessor

@OptIn(ExperimentalSerializationApi::class) val hocon = Hocon {}

var currentResourceFinder = ThreadLocal<ResourceFinder?>()

val ResourceFinder.directoryName
    get() = (this as ResourceFinderAccessor).directoryName!!
val ResourceFinder.fileExtension
    get() = (this as ResourceFinderAccessor).fileExtension!!

fun Identifier.toHocon() = Identifier(namespace, "${path.removeSuffix(".json")}.hocon")

fun Identifier.toJson() = Identifier(namespace, "${path.removeSuffix(".hocon")}.json")

fun ResourcePack.openHoconResource(
    identifier: Identifier,
    type: ResourceType
): InputSupplier<InputStream>? {
    val resourceSupplier = open(type, identifier) ?: return null
    // Json file has no mcmeta. Needn't read
    val inputStream = resourceSupplier.get()
    return InputSupplier {
        ConfigFactory.parseReader(inputStream.reader())
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
    map: MutableMap<Identifier, Record>
) =
    findResources(
        type,
        namespace,
        currentResourceFinder.get()!!.directoryName,
    ) { id, _ ->
        if (!id.path.endsWith(".hocon")) return@findResources
        val inputSupplier = openHoconResource(id, type)
        if (inputSupplier != null) map[id.toJson()] = Result(this, inputSupplier, index)
    }

fun ResourcePack.findAndAddHoconResources(
    type: ResourceType,
    namespace: String,
    map: MutableMap<Identifier, NamespaceResourceManager.EntryList>
) =
    findResources(
        type,
        namespace,
        currentResourceFinder.get()!!.directoryName,
    ) { id, _ ->
        if (!id.path.endsWith(".hocon")) return@findResources
        val inputSupplier = openHoconResource(id, type)
        if (inputSupplier != null)
            map.computeIfAbsent(id.toJson()) { NamespaceResourceManager.EntryList(it) }
                .fileSources += NamespaceResourceManager.FileSource(this, inputSupplier)
    }
