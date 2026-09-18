package com.dingdongji.mod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class IonocraftBootsExhaustParticle extends TextureSheetParticle {
   private final SpriteSet sprites;
   private final float baseSize;

   protected IonocraftBootsExhaustParticle(ClientLevel level, double x, double y, double z, double speedX, double speedY, double speedZ, SpriteSet sprites) {
      super(level, x, y, z);
      this.sprites = sprites;
      this.gravity = 0.0F;
      this.friction = 0.98F;
      this.xd = speedX;
      this.yd = speedY;
      this.zd = speedZ;
      this.rCol = 1.0F;
      this.gCol = 1.0F;
      this.bCol = 1.0F;
      this.baseSize = 0.08F;
      this.quadSize = this.baseSize;
      this.lifetime = (int)(12.0 / ((double)this.random.nextFloat() * 0.4 + 0.6));
      this.setSpriteFromAge(sprites);
      this.alpha = 0.6F;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public void tick() {
      super.tick();
      this.setSpriteFromAge(this.sprites);
      float progress = (float)this.age / (float)this.lifetime;
      this.alpha = 0.6F * (1.0F - progress);
      this.quadSize = this.baseSize * (1.0F - progress);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double speedX, double speedY, double speedZ) {
         return new IonocraftBootsExhaustParticle(level, x, y, z, speedX, speedY, speedZ, this.sprites);
      }
   }
}
