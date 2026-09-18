package com.dingdongji.mod.client.particle;

import com.dingdongji.mod.init.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class NeutronBarrierParticle extends TextureSheetParticle {
   private final boolean isRepel;
   private final float maxSize;
   private final UUID playerUUID;
   private final double offsetX;
   private final double offsetZ;
   private final double startY;

   protected NeutronBarrierParticle(ClientLevel level, double x, double y, double z, boolean isRepel, boolean isBig) {
      super(level, x, y, z);
      this.isRepel = isRepel;
      this.gravity = 0.0F;
      this.friction = 1.0F;
      this.lifetime = 16;
      this.quadSize = 0.0F;
      if (isRepel) {
         this.maxSize = isBig ? 2.4F : 2.0F;
      } else {
         this.maxSize = 1.2F;
      }

      this.alpha = 0.0F;
      this.rCol = 1.0F;
      this.gCol = 1.0F;
      this.bCol = 1.0F;
      this.startY = y;
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         this.playerUUID = player.getUUID();
         this.offsetX = x - player.getX();
         this.offsetZ = z - player.getZ();
      } else {
         this.playerUUID = null;
         this.offsetX = 0.0;
         this.offsetZ = 0.0;
      }
   }

   public void tick() {
      super.tick();
      float t = (float)this.age / (float)this.lifetime;
      Player player = this.playerUUID != null ? Minecraft.getInstance().level.getPlayerByUUID(this.playerUUID) : null;
      this.quadSize = this.maxSize * t;
      this.alpha = 1.0F - t;
      if (this.isRepel) {
         if (player != null) {
            double groundY = player.getY();
            double chestY = groundY + 1.2;
            this.setPos(player.getX(), chestY - 1.2 * (double)t, player.getZ());
         } else {
            this.setPos(this.x, this.startY - 1.2 * (double)t, this.z);
         }
      } else if (player != null) {
         this.setPos(player.getX() + this.offsetX, this.y, player.getZ() + this.offsetZ);
      }
   }

   public void render(VertexConsumer buffer, Camera camera, float partialTick) {
      Vec3 camPos = camera.getPosition();
      float cx = (float)(Mth.lerp((double)partialTick, this.xo, this.x) - camPos.x);
      float cy = (float)(Mth.lerp((double)partialTick, this.yo, this.y) - camPos.y);
      float cz = (float)(Mth.lerp((double)partialTick, this.zo, this.z) - camPos.z);
      float size = this.getQuadSize(partialTick);
      float u0 = this.getU0();
      float u1 = this.getU1();
      float v0 = this.getV0();
      float v1 = this.getV1();
      int light = this.getLightColor(partialTick);
      float r = this.rCol;
      float g = this.gCol;
      float b = this.bCol;
      float a = this.alpha;
      if (!this.isRepel) {
         Vector3f look = camera.getLookVector();
         Vector3f upV = camera.getUpVector();
         Vector3f rightV = new Vector3f(look).cross(upV).normalize();
         Vector3f upV2 = new Vector3f(rightV).cross(look).normalize();
         float rotAngle = -((float)this.age + partialTick) * 0.6F;
         float cR = (float)Math.cos((double)rotAngle);
         float sR = (float)Math.sin((double)rotAngle);
         float rxx = rightV.x * cR + upV2.x * sR;
         float ryy = rightV.y * cR + upV2.y * sR;
         float rzz = rightV.z * cR + upV2.z * sR;
         float upx = -rightV.x * sR + upV2.x * cR;
         float upy = -rightV.y * sR + upV2.y * cR;
         float upz = -rightV.z * sR + upV2.z * cR;
         float[][] corners = new float[][]{
            {cx + (rxx + upx) * size, cy + (ryy + upy) * size, cz + (rzz + upz) * size},
            {cx + (-rxx + upx) * size, cy + (-ryy + upy) * size, cz + (-rzz + upz) * size},
            {cx + (-rxx - upx) * size, cy + (-ryy - upy) * size, cz + (-rzz - upz) * size},
            {cx + (rxx - upx) * size, cy + (ryy - upy) * size, cz + (rzz - upz) * size}
         };
         float[][] uvs = new float[][]{{u1, v1}, {u0, v1}, {u0, v0}, {u1, v0}};

         for (int i = 0; i < 4; i++) {
            buffer.addVertex(corners[i][0], corners[i][1], corners[i][2]).setUv(uvs[i][0], uvs[i][1]).setColor(r, g, b, a).setLight(light);
         }

         for (int i = 0; i < 4; i++) {
            int j = 3 - i;
            buffer.addVertex(corners[j][0], corners[j][1], corners[j][2]).setUv(uvs[i][0], uvs[i][1]).setColor(r, g, b, a).setLight(light);
         }
      } else {
         float[][] raw = new float[][]{{-size, -size}, {size, -size}, {size, size}, {-size, size}};
         float[][] uv = new float[][]{{u1, v1}, {u1, v0}, {u0, v0}, {u0, v1}};

         for (int i = 0; i < 4; i++) {
            buffer.addVertex(cx + raw[i][0], cy, cz + raw[i][1]).setUv(uv[i][0], uv[i][1]).setColor(r, g, b, a).setLight(light);
         }

         for (int i = 0; i < 4; i++) {
            int j = 3 - i;
            buffer.addVertex(cx + raw[j][0], cy, cz + raw[j][1]).setUv(uv[i][0], uv[i][1]).setColor(r, g, b, a).setLight(light);
         }
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
         boolean isBig = type == ModParticles.NEUTRON_BARRIER_REPEL_BIG.get();
         boolean isRepel = type == ModParticles.NEUTRON_BARRIER_REPEL.get() || isBig;
         NeutronBarrierParticle p = new NeutronBarrierParticle(level, x, y, z, isRepel, isBig);
         p.pickSprite(this.sprites);
         return p;
      }
   }
}
