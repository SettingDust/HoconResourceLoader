package settingdust.hoconresourceloader

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigIncludeContext
import com.typesafe.config.ConfigIncluder
import com.typesafe.config.ConfigIncluderClasspath
import com.typesafe.config.ConfigIncluderFile
import com.typesafe.config.ConfigIncluderURL
import com.typesafe.config.ConfigObject
import com.typesafe.config.impl.Parseable
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import kotlin.jvm.optionals.getOrNull
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier

class SimpleIncluder(
    private val manager: ResourceManager,
    private val hoconId: Identifier,
    private val startingPath: String = ""
) : ConfigIncluder, ConfigIncluderFile, ConfigIncluderURL, ConfigIncluderClasspath {

    override fun withFallback(fallback: ConfigIncluder) = this

    override fun include(context: ConfigIncludeContext, what: String): ConfigObject {
        Identifier.tryParse(what)
            ?.let {
                abortParsing.set(true)
                manager.getResource(it).getOrNull()
                    ?: manager
                        .getResource(Identifier(it.namespace, "$startingPath/${it.path}"))
                        .getOrNull()
                        .also { abortParsing.remove() }
            }
            ?.also {
                return ConfigFactory.parseReader(it.reader).root()
            }

        // the heuristic is valid URL then URL, else relative to including file;
        // relativeTo in a file falls back to classpath inside relativeTo().
        try {
                URL(what)
            } catch (e: MalformedURLException) {
                null
            }
            ?.let {
                return includeURL(context, it)
            }

        val parseable =
            (context.relativeTo(what)
                ?: Parseable.newNotFound(
                    what,
                    "include was not found: '$what'",
                    context.parseOptions()
                ))
        return parseable.parse(context.parseOptions())
    }

    override fun includeFile(context: ConfigIncludeContext, what: File): ConfigObject {
        return ConfigFactory.parseFile(what, context.parseOptions()).root()
    }

    override fun includeURL(context: ConfigIncludeContext, what: URL): ConfigObject {
        return ConfigFactory.parseURL(what, context.parseOptions()).root()
    }

    override fun includeResources(context: ConfigIncludeContext, what: String): ConfigObject {
        return ConfigFactory.parseResources(what, context.parseOptions()).root()
    }
}
