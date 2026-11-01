package com.edith.assistant.data

enum class NovaOrbStyle(val displayName: String, val description: String) {
    NOVA("Nova", "Signature harmonic energy core with orbital rings"),
    AURORA("Aurora", "Fluid multi-hue ethereal atmospheric glow"),
    PLASMA("Plasma", "High-energy swirling vortex particles"),
    GLASS("Glass", "Crystalline translucent sphere with specular caustics"),
    ENERGY("Energy", "Intense hyper-charged radiant photon field"),
    MINIMAL("Minimal", "Clean, refined minimalist zen glow"),
    ARCHER("Archer", "Geometric Fibonacci spiral golden lattice");

    companion object {
        fun fromName(name: String): NovaOrbStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NOVA
        }
    }
}
