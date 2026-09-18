package com.dingdongji.mod.init;

import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
   public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, "dingdongji");
   public static final Supplier<SimpleParticleType> IONOCRAFT_BOOTS_EXHAUST = PARTICLES.register("ionocraft_boots_exhaust", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> NEUTRON_BARRIER_REPEL = PARTICLES.register("neutron_barrier_repel", () -> new SimpleParticleType(false));
   public static final Supplier<SimpleParticleType> NEUTRON_BARRIER_REPEL_BIG = PARTICLES.register(
      "neutron_barrier_repel_big", () -> new SimpleParticleType(false)
   );
   public static final Supplier<SimpleParticleType> NEUTRON_BARRIER_ABSORB = PARTICLES.register("neutron_barrier_absorb", () -> new SimpleParticleType(false));
}
