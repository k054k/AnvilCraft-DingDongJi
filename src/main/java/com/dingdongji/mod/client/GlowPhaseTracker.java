package com.dingdongji.mod.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the emissive armor glow from the actual animation definitions of the
 * metal block outline textures that AnvilCraft ships in the game.
 *
 * Nothing is read from blocks placed in the world, and no private rendering
 * classes are touched: when the block atlas is stitched, the three outline
 * sprites already exist as resources, so we read their .png.mcmeta animation
 * data (frame order, per-frame time, interpolate flag) straight from the
 * resource packs and replay it ourselves.
 *
 * The pulse comes from TextureAtlas.cycleAnimationFrames, the exact call the
 * game uses to advance those sprites, so both start at the same stitch origin
 * and tick in lockstep. A client-tick fallback keeps the clock alive if a
 * third-party renderer replaces that path.
 */
@EventBusSubscriber({Dist.CLIENT})
public final class GlowPhaseTracker {
   private static final Logger LOGGER = LoggerFactory.getLogger("DingDongJi/GlowPhase");

   public enum Outline {
      EMBER("anvilcraft", "block/ember_metal_block_outline"),
      FROST("anvilcraft", "block/frost_metal_block_outline"),
      TRANS("anvilcraft", "block/transcendium_block_outline");

      final String namespace;
      final String path;

      Outline(String namespace, String path) {
         this.namespace = namespace;
         this.path = path;
      }
   }

   private record FrameEntry(int index, int time) {
   }

   private static final class Timeline {
      private final List<FrameEntry> frames;
      private final boolean interpolate;
      private final int period;
      private final int maxIndex;

      private Timeline(List<FrameEntry> frames, boolean interpolate) {
         this.frames = frames;
         this.interpolate = interpolate;
         int total = 0;
         int max = 0;

         for (FrameEntry entry : frames) {
            total += entry.time();
            max = Math.max(max, entry.index());
         }

         this.period = total;
         this.maxIndex = Math.max(max, 1);
      }

      // Returns darkness in [0,1] (0 = bright sheet frame, 1 = darkest sheet
      // frame) at the given animation pulse, following the mcmeta semantics.
      private float darknessAt(long pulse) {
         if (this.period <= 0) {
            return 0.0F;
         }

         long t = pulse % (long)this.period;
         int elapsed = 0;

         for (int i = 0; i < this.frames.size(); i++) {
            FrameEntry entry = this.frames.get(i);
            if (t < (long)(elapsed + entry.time()) || i == this.frames.size() - 1) {
               long sub = t - (long)elapsed;
               float base = (float)entry.index() / (float)this.maxIndex;
               if (!this.interpolate || entry.time() <= 0) {
                  return base;
               }

               FrameEntry next = this.frames.get((i + 1) % this.frames.size());
               float toward = (float)next.index() / (float)this.maxIndex;
               return Mth.clamp(base + (toward - base) * ((float)sub / (float)entry.time()), 0.0F, 1.0F);
            }

            elapsed += entry.time();
         }

         return 0.0F;
      }
   }

   private static final Map<Outline, Timeline> TIMELINES = new EnumMap<>(Outline.class);
   private static TextureAtlas blockAtlas;
   private static long pulse;
   private static boolean pulsedThisClientTick;

   private GlowPhaseTracker() {
   }

   @SubscribeEvent
   public static void onAtlasStitched(TextureAtlasStitchedEvent event) {
      // Never let resource parsing abort the resource reload / main menu.
      try {
         if (!event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            return;
         }

         Map<Outline, Timeline> parsed = new EnumMap<>(Outline.class);

         for (Outline outline : Outline.values()) {
            Timeline timeline = loadTimeline(outline);
            if (timeline != null) {
               parsed.put(outline, timeline);
            }
         }

         if (!parsed.isEmpty()) {
            TIMELINES.clear();
            TIMELINES.putAll(parsed);
            blockAtlas = event.getAtlas();
            pulse = 0L;
            pulsedThisClientTick = false;
         }
      } catch (Throwable t) {
         LOGGER.warn("Failed to parse glow outline animation metadata, using fallback breathing", t);
      }
   }

   private static Timeline loadTimeline(Outline outline) {
      ResourceLocation mcmeta = ResourceLocation.fromNamespaceAndPath(
         outline.namespace, "textures/" + outline.path + ".png.mcmeta"
      );
      Minecraft mc = Minecraft.getInstance();
      if (mc.getResourceManager() == null) {
         return null;
      } else {
         var resOpt = mc.getResourceManager().getResource(mcmeta);
         if (resOpt.isEmpty()) {
            return null;
         } else {
            try (Reader reader = new InputStreamReader(resOpt.get().open(), StandardCharsets.UTF_8)) {
               JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
               if (!root.has("animation")) {
                  return null;
               } else {
                  JsonObject anim = root.getAsJsonObject("animation");
                  int frameTime = anim.has("frametime") ? Math.max(anim.get("frametime").getAsInt(), 1) : 1;
                  boolean interpolate = anim.has("interpolate") && anim.get("interpolate").getAsBoolean();
                  List<FrameEntry> entries = new ArrayList<>();
                  if (anim.has("frames") && anim.get("frames").isJsonArray()) {
                     JsonArray frames = anim.getAsJsonArray("frames");

                     for (JsonElement frameEl : frames) {
                        if (frameEl.isJsonPrimitive()) {
                           entries.add(new FrameEntry(frameEl.getAsInt(), frameTime));
                        } else if (frameEl.isJsonObject()) {
                           JsonObject frameObj = frameEl.getAsJsonObject();
                           int index = frameObj.has("index") ? frameObj.get("index").getAsInt() : 0;
                           int time = frameObj.has("time") ? Math.max(frameObj.get("time").getAsInt(), 1) : frameTime;
                           entries.add(new FrameEntry(index, time));
                        }
                     }
                  }

                  return entries.isEmpty() ? null : new Timeline(entries, interpolate);
               }
            } catch (Exception e) {
               LOGGER.warn("Failed to read {}", mcmeta, e);
               return null;
            }
         }
      }
   }

   public static void onAtlasPulse(TextureAtlas atlas) {
      if (atlas == blockAtlas && !pulsedThisClientTick) {
         pulse++;
         pulsedThisClientTick = true;
      }
   }

   @SubscribeEvent
   public static void onClientTickPost(ClientTickEvent.Post event) {
      // Fallback for renderers that replace cycleAnimationFrames: keep the
      // canonical clock advancing once per client tick.
      if (blockAtlas != null && !pulsedThisClientTick) {
         pulse++;
      }

      pulsedThisClientTick = false;
   }

   /**
    * Returns darkness in [0,1] for the given outline at the current game
    * animation pulse, or -1 if the sprite definition has not been loaded yet.
    */
   public static float darkness(Outline outline) {
      Timeline timeline = TIMELINES.get(outline);
      return timeline == null ? -1.0F : timeline.darknessAt(pulse);
   }
}
