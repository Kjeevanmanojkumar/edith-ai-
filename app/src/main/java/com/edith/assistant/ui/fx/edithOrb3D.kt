package com.edith.assistant.ui.fx

import android.content.Context
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.edith.assistant.data.NovaOrbStyle
import com.edith.assistant.ui.theme.NovaTheme
import com.edith.assistant.voice.NovaOrbState

private const val ORB_AGSL_SHADER = """
uniform float2 uResolution;
uniform float uTime;
uniform float uAmplitude;
uniform float2 uTilt;
uniform float3 uColorCore;
uniform float3 uColorRim;
uniform float3 uColorHalo;

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * noise(p);
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 center = uResolution * 0.5;
    float minDim = min(uResolution.x, uResolution.y);
    float radius = minDim * 0.35 * (1.0 + uAmplitude * 0.22);
    
    float2 p = (fragCoord - center) / radius;
    float d = length(p);
    
    // Halo outside the sphere
    if (d > 1.0) {
        float haloDist = (d - 1.0);
        float haloAlpha = exp(-haloDist * 3.8) * 0.65;
        return half4(uColorHalo * haloAlpha, haloAlpha);
    }
    
    // Normal calculation
    float z = sqrt(max(0.0, 1.0 - d * d));
    float3 normal = float3(p.x, p.y, z);
    
    // Light vector with device tilt
    float3 lightDir = normalize(float3(0.35 + uTilt.x * 0.5, -0.45 - uTilt.y * 0.5, 0.8));
    float diff = max(dot(normal, lightDir), 0.0);
    
    // Fresnel rim effect
    float fresnel = pow(1.0 - z, 2.8);
    
    // Internal FBM energy swirl
    float2 uvNoise = p * 2.2 + float2(uTime * 0.15, uTime * 0.1);
    float energy = fbm(uvNoise);
    
    // Diffuse + Specular lighting
    float3 viewDir = float3(0.0, 0.0, 1.0);
    float3 halfDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfDir), 0.0), 32.0) * 0.75;
    
    float3 col = mix(uColorCore, uColorRim, fresnel);
    col += uColorHalo * energy * 0.45;
    col = col * (0.35 + 0.65 * diff) + float3(spec);
    
    float alpha = smoothstep(1.0, 0.96, d);
    return half4(col * alpha, alpha);
}
"""

@Composable
fun edithOrb3D(
    state: NovaOrbState,
    size: Dp = 220.dp,
    amplitude: Float = 0f,
    style: NovaOrbStyle = NovaOrbStyle.NOVA,
    modifier: Modifier = Modifier,
    assistantName: String = "edith"
) {
    val reduced = NovaTheme.reducedMotion
    val colors = NovaTheme.colors

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || reduced) {
        NovaOrb(
            state = state,
            size = size,
            amplitude = amplitude,
            style = style,
            modifier = modifier,
            assistantName = assistantName
        )
        return
    }

    val context = LocalContext.current
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.values.size >= 2) {
                    // Low pass filter
                    tiltX = tiltX * 0.85f + (-event.values[0] / 9.8f) * 0.15f
                    tiltY = tiltY * 0.85f + (event.values[1] / 9.8f) * 0.15f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        accel?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose {
            sm?.unregisterListener(listener)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "agsl_orb")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb_time"
    )

    // Palette mapping
    val (coreCol, rimCol, haloCol) = when (state) {
        NovaOrbState.IDLE -> Triple(
            floatArrayOf(0.72f, 0.35f, 0.85f),
            floatArrayOf(1.0f, 0.43f, 0.68f),
            floatArrayOf(0.55f, 0.3f, 0.95f)
        )
        NovaOrbState.LISTENING -> Triple(
            floatArrayOf(0.15f, 0.65f, 0.75f),
            floatArrayOf(0.25f, 0.88f, 0.82f),
            floatArrayOf(0.1f, 0.85f, 0.75f)
        )
        NovaOrbState.THINKING -> Triple(
            floatArrayOf(0.2f, 0.7f, 0.85f),
            floatArrayOf(1.0f, 0.71f, 0.28f),
            floatArrayOf(0.35f, 0.9f, 0.85f)
        )
        NovaOrbState.SPEAKING -> Triple(
            floatArrayOf(1.0f, 0.55f, 0.24f),
            floatArrayOf(1.0f, 0.71f, 0.28f),
            floatArrayOf(1.0f, 0.43f, 0.68f)
        )
        NovaOrbState.CONNECTING -> Triple(
            floatArrayOf(0.85f, 0.45f, 0.1f),
            floatArrayOf(1.0f, 0.71f, 0.28f),
            floatArrayOf(0.9f, 0.6f, 0.2f)
        )
        NovaOrbState.ERROR -> Triple(
            floatArrayOf(0.85f, 0.15f, 0.15f),
            floatArrayOf(1.0f, 0.32f, 0.32f),
            floatArrayOf(0.9f, 0.2f, 0.2f)
        )
    }

    val shader = remember { RuntimeShader(ORB_AGSL_SHADER) }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "$assistantName status: ${state.name.lowercase()}" }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            shader.setFloatUniform("uResolution", this.size.width, this.size.height)
            shader.setFloatUniform("uTime", time)
            shader.setFloatUniform("uAmplitude", amplitude)
            shader.setFloatUniform("uTilt", tiltX, tiltY)
            shader.setFloatUniform("uColorCore", coreCol[0], coreCol[1], coreCol[2])
            shader.setFloatUniform("uColorRim", rimCol[0], rimCol[1], rimCol[2])
            shader.setFloatUniform("uColorHalo", haloCol[0], haloCol[1], haloCol[2])

            val brush = ShaderBrush(shader)
            drawRect(brush = brush)
        }
    }
}
