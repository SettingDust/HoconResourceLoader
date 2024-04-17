package settingdust.hoconresourceloader

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.apache.logging.log4j.LogManager

object HoconResourceLoader {
    val LOGGER = LogManager.getLogger()
}

fun init() {
    ServerLifecycleEvents.START_DATA_PACK_RELOAD.register { _, _ -> packMetaCache.invalidateAll() }
}
