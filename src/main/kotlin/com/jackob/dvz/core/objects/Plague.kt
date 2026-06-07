package com.jackob.dvz.core.objects

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.sync
import com.jackob.dvz.util.toPlayer
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.joml.Matrix4f
import java.util.UUID
import kotlin.random.Random

class Plague(allDwarves: List<UUID>, volunteers: List<UUID>, private val plagueResult: (Player) -> Unit) : Listener {

    private var victimsCount: Int = allDwarves.size / calculateProportion(allDwarves.size)

    private val victims: MutableSet<UUID> = HashSet(victimsCount)

    init {
        if (victimsCount >= 1) {
            victims.addAll(volunteers)

            val applicableDwarves =
                allDwarves.filter { !KitsManager.isHero(it.toPlayer()!!) }.shuffled().toMutableList()

            while (victims.size != victimsCount) {
                victims.add(applicableDwarves.removeLast())
            }

            startPlague()
        }
    }

    private fun startPlague() {
        Bukkit.broadcast("<b><dark_green>Plague started!".mm())
        // play global sound

        for (id in victims) {
            id.toPlayer()?.let { playStartEffect(it) }
        }

        sync(delay = TimeUnit.SECONDS(5)) {
            val iterator = victims.iterator()

            while (iterator.hasNext()) {
                val id = iterator.next()
                val player = id.toPlayer()

                if (player != null) {
                    plagueResult(player)
                    playEndEffect(player)
                    iterator.remove()
                }
            }

            if (!victims.isEmpty()) {
                DvZ.INSTANCE.server.pluginManager.registerEvents(this@Plague, DvZ.INSTANCE)
            }
        }
    }

    private fun playStartEffect(player: Player) = with(player) {
        sendMessage("<i><dark_green>You have been infected".mm())
        playSound(location, Sound.ENTITY_ZOMBIE_INFECT, 1f, 1f)
        addPotionEffect(PotionEffect(PotionEffectType.NAUSEA, Int.MAX_VALUE, 0))
        addPotionEffect(PotionEffect(PotionEffectType.POISON, Int.MAX_VALUE, 0))

        Particle.DUST_COLOR_TRANSITION.builder()
            .location(eyeLocation)
            .offset(0.5, 0.5, 0.5)
            .count(10)
            .extra(0.1)
            .colorTransition(Color.LIME, Color.GREEN)
            .receivers(20, true)
            .spawn()
    }

    private fun playEndEffect(player: Player) = with(player) {
        world.playSound(location, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 1f)
        Particle.EXPLOSION.builder()
            .location(player.location.clone().add(0.0, 1.0, 0.0))
            .offset(1.0, 0.0, 0.0)
            .count(2)
            .receivers(32, true)
            .spawn()

        playRotatingLightsEffect(location)
    }

    private fun playRotatingLightsEffect(location: Location) {
        val beamLights = generateBeamLights(location)
        var repetitions = 5
        val duration = 20

        sync(delay = TimeUnit.TICKS(20), period = TimeUnit.TICKS(duration.toLong())) {
            for (beam in beamLights) {
                val display = beam.display

                display.setTransformationMatrix(
                    beam.matrix.rotateY(
                        (Math.toRadians(180.0).toFloat() + 0.1f) * beam.rotationDir
                    )
                )
                display.interpolationDelay = 0
                display.interpolationDuration = duration
            }

            repetitions--
            if (repetitions <= 0) {
                cancel()
                for (beam in beamLights) {
                    beam.display.remove()
                }
            }
        }
    }

    private fun generateBeamLights(originLoc: Location): List<BeamLight> {
        val count = 6
        val beamLights = mutableListOf<BeamLight>()

        for (i in 0..count) {
            val dirModifier = if (Random.nextBoolean()) 1 else -1
            val dirModifier2 = if (Random.nextBoolean()) 1 else -1
            val locModifier = Random.nextDouble(0.5, 0.7)
            val length = Random.nextDouble(5.0, 20.0)
            val pitchModifier = Random.nextInt(10, 30) * dirModifier

            val loc = originLoc.clone().apply {
                x += locModifier * dirModifier
                z += locModifier * dirModifier2
                y -= 1
                pitch = pitchModifier.toFloat()
            }

            val beamBlock = if (i % 2 == 0) Material.ORANGE_STAINED_GLASS else Material.RED_STAINED_GLASS

            val display = (originLoc.world.spawnEntity(loc, EntityType.BLOCK_DISPLAY) as BlockDisplay).apply {
                block = beamBlock.createBlockData()
                viewRange = 50.0f
                brightness = Display.Brightness(15, 15)
            }

            beamLights.add(BeamLight(display, dirModifier, Matrix4f().scale(0.7f, length.toFloat(), 0.7f)))
        }

        return beamLights
    }

    private fun calculateProportion(allDwarvesCount: Int): Int {
        if (allDwarvesCount >= 21) return 3

        return 2
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!victims.contains(player.uniqueId)) return

        plagueResult(player)
        playEndEffect(player)
        victims.remove(player.uniqueId)

        if (victims.isEmpty()) {
            HandlerList.unregisterAll(this)
        }
    }

    data class BeamLight(val display: BlockDisplay, val rotationDir: Int, val matrix: Matrix4f)
}