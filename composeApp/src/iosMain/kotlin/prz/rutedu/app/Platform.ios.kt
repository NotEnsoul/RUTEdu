package prz.rutedu.app

import platform.UIKit.UIDevice

/** iOS implementation of [Platform]. Reports `"<systemName> <systemVersion>"` as the platform name. */
class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

/**
 * Instantiates the iOS platform info provider.
 *
 * @return An instance of [IOSPlatform] reporting iOS details.
 */
actual fun getPlatform(): Platform = IOSPlatform()