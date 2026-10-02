package com.dingdongji.mod.client;

import com.dingdongji.mod.input.ModKeyBindings;

/**
 * 按键显示名的安全访问代理。
 * 公共事件类（服务端也会加载）不允许在常量池中直接出现 KeyMapping 等客户端类型，
 * 因此所有按键名读取都经本类转发：本类仅在客户端 tooltip 事件中被实际调用，
 * 方法字节码内的客户端符号引用只会在执行时解析，专用服务端永不触发。
 */
public final class ClientKeyNames {
   private ClientKeyNames() {
   }

   public static String abilityKey() {
      return ModKeyBindings.ABILITY_KEY.getTranslatedKeyMessage().getString();
   }

   public static String glowingVisionKey() {
      return ModKeyBindings.GLOWING_VISION_KEY.getTranslatedKeyMessage().getString();
   }

   public static String neutronBarrierKey() {
      return ModKeyBindings.NEUTRON_BARRIER_KEY.getTranslatedKeyMessage().getString();
   }

   public static String phaseShiftKey() {
      return ModKeyBindings.PHASE_SHIFT_KEY.getTranslatedKeyMessage().getString();
   }

   /** 铁砧本体"切换工具模式"键（默认左Alt），盔甲功能轮盘的触发键。 */
   public static String switchToolModeKey() {
      return dev.dubhe.anvilcraft.client.init.ModKeyMappings.SWITCH_TOOL_MODE.get().getTranslatedKeyMessage().getString();
   }
}
