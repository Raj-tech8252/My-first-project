package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.BikeId
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class SoundEngine {
    private val sampleRate = 22050
    private val bufferSamples = 512

    @Volatile private var running = false
    @Volatile private var enabled = true
    @Volatile private var masterVolume = 0.85f

    @Volatile private var bikeId = BikeId.ROYAL_ENFIELD_BULLET
    @Volatile private var engineOn = false
    @Volatile private var rpmNorm = 0f
    @Volatile private var throttle = false
    @Volatile private var braking = false
    @Volatile private var speedKmh = 0f
    @Volatile private var nitroActive = false
    @Volatile private var hornActive = false
    @Volatile private var rainIntensity = 0f
    @Volatile private var nightFactor = 0f
    @Volatile private var nearRailwayBell = false
    @Volatile private var sirenActive = false

    @Volatile private var crashImpulse = 0f
    @Volatile private var starterImpulse = 0f
    @Volatile private var revPreviewTimer = 0f

    private var workerThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    fun start() {
        if (running) return
        running = true
        workerThread = thread(start = true, isDaemon = true, name = "IndianBikeAudio") {
            val minBufBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(bufferSamples * 2)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                running = false
                return@thread
            }

            audioTrack = track
            try {
                track.play()
            } catch (_: Exception) {
                running = false
                return@thread
            }

            val pcm = ShortArray(bufferSamples)
            var enginePhase = 0.0
            var subPhase = 0.0
            var hornPhase1 = 0.0
            var hornPhase2 = 0.0
            var bellPhase = 0.0
            var sirenPhase = 0.0
            var lfoPhase = 0.0
            var noiseState = 1234567
            var lpFilter = 0f
            var revTimer = 0f

            val dt = 1.0 / sampleRate

            while (running) {
                val vol = if (enabled) masterVolume else 0f
                val activeBike = bikeId

                // Handle rev preview in showroom/garage
                var effectiveEngineOn = engineOn
                var effectiveRpm = rpmNorm
                var effectiveThrottle = throttle
                if (revPreviewTimer > 0f) {
                    revPreviewTimer = (revPreviewTimer - bufferSamples * dt.toFloat()).coerceAtLeast(0f)
                    revTimer += bufferSamples * dt.toFloat()
                    effectiveEngineOn = true
                    effectiveThrottle = true
                    effectiveRpm = (0.25f + 0.7f * abs(sin(revTimer * 4.5f))).coerceIn(0f, 1f)
                } else {
                    revTimer = 0f
                }

                val freq = activeBike.baseFreqHz +
                    (activeBike.maxFreqHz - activeBike.baseFreqHz) * effectiveRpm
                val thump = activeBike.thumpFactor
                val isBrakingFast = braking && speedKmh > 18f

                for (i in 0 until bufferSamples) {
                    noiseState = noiseState * 1664525 + 1013904223
                    val white = ((noiseState ushr 16) / 32768f) - 1f
                    lpFilter = lpFilter * 0.86f + white * 0.14f

                    var sample = 0f

                    // 1. Engine combustion synthesis tailored per bike
                    if (effectiveEngineOn) {
                        enginePhase += freq * dt
                        if (enginePhase >= 1.0) enginePhase -= 1.0
                        subPhase += (freq * 0.5) * dt
                        if (subPhase >= 1.0) subPhase -= 1.0

                        val fundamental = sin(enginePhase * 2.0 * PI).toFloat()
                        val h2 = sin(enginePhase * 4.0 * PI).toFloat()
                        val h3 = sin(enginePhase * 6.0 * PI).toFloat()
                        // Royal Enfield irregular thump pulse
                        val pulseWindow = if (enginePhase < 0.35) {
                            sin((enginePhase / 0.35) * PI).toFloat()
                        } else {
                            0.18f
                        }
                        val subThump = sin(subPhase * 2.0 * PI).toFloat()

                        val sportyTone = fundamental * 0.55f + h2 * 0.30f + h3 * 0.18f
                        val thumpTone = (subThump * 0.65f + fundamental * 0.45f) * (0.4f + 0.9f * pulseWindow)
                        val engineMix = sportyTone * (1f - thump * 0.65f) + thumpTone * (thump * 0.85f)
                        val gain = if (effectiveThrottle) 0.34f else 0.18f
                        sample += engineMix * gain
                    }

                    // 2. Starter crank
                    if (starterImpulse > 0.01f) {
                        val crank = sin(lfoPhase * 2.0 * PI * 14.0).toFloat()
                        sample += (crank * 0.25f + lpFilter * 0.2f) * starterImpulse
                        starterImpulse *= 0.9994f
                    }

                    // 3. Dual-tone Indian motorcycle horn
                    if (hornActive) {
                        hornPhase1 += 415.0 * dt
                        hornPhase2 += 522.0 * dt
                        if (hornPhase1 >= 1.0) hornPhase1 -= 1.0
                        if (hornPhase2 >= 1.0) hornPhase2 -= 1.0
                        val h1 = if (hornPhase1 < 0.5) 0.8f else -0.8f
                        val h2 = if (hornPhase2 < 0.5) 0.7f else -0.7f
                        sample += (h1 * 0.22f + h2 * 0.18f)
                    }

                    // 4. Brake screech & tire skid
                    if (isBrakingFast) {
                        val screech = sin(lfoPhase * 2.0 * PI * 920.0).toFloat() * 0.08f
                        sample += (white * 0.12f + screech) * (speedKmh / 140f).coerceIn(0.2f, 0.8f)
                    }

                    // 5. Nitro turbine boost
                    if (nitroActive && effectiveEngineOn) {
                        sample += (white * 0.18f + lpFilter * 0.22f)
                    }

                    // 6. Crash impact
                    if (crashImpulse > 0.01f) {
                        sample += (lpFilter * 0.7f + white * 0.35f) * crashImpulse
                        crashImpulse *= 0.9985f
                    }

                    // 7. Ambient wind + rain + night crickets
                    val windGain = (speedKmh / 180f).coerceIn(0f, 1f) * 0.12f
                    sample += lpFilter * (windGain + rainIntensity * 0.11f)

                    lfoPhase += dt
                    if (lfoPhase >= 100.0) lfoPhase -= 100.0

                    if (nightFactor > 0.4f && speedKmh < 35f) {
                        val chirpEnv = ((sin(lfoPhase * 2.0 * PI * 2.3) + 1.0) * 0.5).toFloat()
                        val cricket = sin(lfoPhase * 2.0 * PI * 3600.0).toFloat()
                        sample += cricket * chirpEnv * nightFactor * 0.025f
                    }

                    // 8. Railway crossing warning chime
                    if (nearRailwayBell) {
                        bellPhase += dt
                        val pingEnv = (1.0 - (bellPhase % 0.65) / 0.65).toFloat().coerceAtLeast(0f)
                        val bell = sin(bellPhase * 2.0 * PI * 880.0).toFloat()
                        sample += bell * pingEnv * pingEnv * 0.14f
                    }

                    // 9. Police PCR siren
                    if (sirenActive) {
                        sirenPhase += dt
                        val sweep = 620.0 + 260.0 * sin(sirenPhase * 2.0 * PI * 1.4)
                        val sirenWave = sin(sirenPhase * 2.0 * PI * sweep).toFloat()
                        sample += sirenWave * 0.12f
                    }

                    val clamped = (sample * vol).coerceIn(-0.95f, 0.95f)
                    pcm[i] = (clamped * 32767f).toInt().toShort()
                }

                try {
                    track.write(pcm, 0, bufferSamples)
                } catch (_: Exception) {
                    break
                }
            }

            try {
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
            audioTrack = null
        }
    }

    fun stop() {
        running = false
        workerThread?.interrupt()
        workerThread = null
    }

    fun updateSettings(soundEnabled: Boolean, volume: Float) {
        enabled = soundEnabled
        masterVolume = volume.coerceIn(0f, 1f)
    }

    fun updateTelemetry(
        bike: BikeId,
        isEngineOn: Boolean,
        rpmNormalized: Float,
        isThrottle: Boolean,
        isBraking: Boolean,
        currentSpeedKmh: Float,
        isNitro: Boolean,
        isHorn: Boolean,
        rain: Float,
        night: Float,
        railwayBell: Boolean,
        policeSiren: Boolean
    ) {
        bikeId = bike
        engineOn = isEngineOn
        rpmNorm = rpmNormalized.coerceIn(0f, 1f)
        throttle = isThrottle
        braking = isBraking
        speedKmh = currentSpeedKmh
        nitroActive = isNitro
        hornActive = isHorn
        rainIntensity = rain.coerceIn(0f, 1f)
        nightFactor = night.coerceIn(0f, 1f)
        nearRailwayBell = railwayBell
        sirenActive = policeSiren
    }

    fun triggerCrash(intensity: Float = 0.8f) {
        crashImpulse = intensity.coerceIn(0.2f, 1.0f)
    }

    fun triggerStarter() {
        starterImpulse = 0.7f
    }

    fun triggerEngineRevPreview(bike: BikeId) {
        bikeId = bike
        revPreviewTimer = 1.6f
    }
}
