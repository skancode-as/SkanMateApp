package dk.skancode.skanmate.haptics

import dk.skancode.skanmate.util.Haptic

class StubHaptic: Haptic {
    override fun start() {
        println("Haptics are not supported by hardware")
    }
}