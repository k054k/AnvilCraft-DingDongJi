package com.dingdongji.mod.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;

/**
 * Records the live animation phase of animated atlas sprites (frame and
 * subFrame) whenever vanilla ticks them, so the armor glow can reuse the
 * exact same clock as the in-game outline blocks instead of guessing a
 * shared phase from gameTime.
 */
public final class GlowPhaseTracker {
   private static final Map<SpriteTicker, SpriteContents> TICKER_OWNER = new WeakHashMap<>();
   private static final Map<SpriteContents, Long> PHASES = new WeakHashMap<>();

   private GlowPhaseTracker() {
   }

   public static void bind(SpriteTicker ticker, SpriteContents contents) {
      TICKER_OWNER.put(ticker, contents);
   }

   public static void record(SpriteTicker ticker, int frame, int subFrame) {
      SpriteContents contents = TICKER_OWNER.get(ticker);
      if (contents != null) {
         PHASES.put(contents, (long)frame << 32 | (long)subFrame & 0xFFFFFFFFL);
      }
   }

   /**
    * Returns the global animation phase in ticks (frame * 10 + subFrame),
    * or -1 when the sprite has never been ticked.
    */
   public static int phase(SpriteContents contents) {
      Long bits = PHASES.get(contents);
      if (bits == null) {
         return -1;
      }

      return (int)(bits >> 32) * 10 + (int)(bits & 0xFFFFFFFFL);
   }
}
