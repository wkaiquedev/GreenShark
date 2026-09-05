package com.greenshark;

import com.greenshark.gui.InterceptorScreen;
import com.greenshark.model.PacketLog;
import com.greenshark.net.PacketInterceptor;
import com.greenshark.net.PipelineInjector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Ponto de entrada client-side do GreenShark. */
public class GreenSharkClient implements ClientModInitializer {

    public static final String MOD_ID = "greenshark";
    public static final Logger LOGGER = LoggerFactory.getLogger("GreenShark");

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.greenshark.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                KeyBinding.Category.MISC));

        // Injeta o interceptor assim que entra num mundo/servidor.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PipelineInjector.inject(handler.getConnection());
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(
                            "§a[GreenShark] §finterceptor ativo — aperte §eI§f para abrir."), false);
                }
            });
        });

        // Abre a tela ao apertar a tecla.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                client.setScreen(new InterceptorScreen());
            }
        });

        // HUD discreto no canto superior esquerdo.
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) {
                return;
            }
            StringBuilder sb = new StringBuilder("§a● GreenShark §7")
                    .append(PacketLog.INSTANCE.size()).append(" pkts");
            if (PacketInterceptor.interceptInbound || PacketInterceptor.interceptOutbound) {
                sb.append(" §c[INTERCEPT]");
            }
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal(sb.toString()), 4, 4, 0xFFFFFFFF);
        });

        LOGGER.info("[GreenShark] Inicializado. Tecla padrão: I");
    }
}
