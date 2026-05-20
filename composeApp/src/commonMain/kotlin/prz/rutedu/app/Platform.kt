package prz.rutedu.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform