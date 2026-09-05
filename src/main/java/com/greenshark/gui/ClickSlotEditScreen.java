package com.greenshark.gui;

import java.util.function.Consumer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

/**
 * Editor especializado do {@link ClickSlotC2SPacket} — o pacote-chave pra testar
 * GUIs de plugin (leilão, mercado, loja). Rotula os campos corretamente mesmo em
 * produção (usa os acessores tipados, não os nomes ofuscados) e mantém os hashes
 * de item originais (o MC moderno não deixa forjar item por aqui).
 */
public class ClickSlotEditScreen extends Screen {

    private final Screen parent;
    private final ClickSlotC2SPacket orig;
    private final String actionLabel;
    private final Consumer<Packet<?>> onApply;

    private TextFieldWidget syncIdF;
    private TextFieldWidget revisionF;
    private TextFieldWidget slotF;
    private TextFieldWidget buttonF;
    private SlotActionType action;

    public ClickSlotEditScreen(Screen parent, ClickSlotC2SPacket orig,
                               String actionLabel, Consumer<Packet<?>> onApply) {
        super(Text.literal("Editar ClickSlot (C→S)"));
        this.parent = parent;
        this.orig = orig;
        this.actionLabel = actionLabel;
        this.onApply = onApply;
        this.action = orig.actionType();
    }

    private TextFieldWidget field(int x, int y, int w, String val) {
        TextFieldWidget t = new TextFieldWidget(this.textRenderer, x, y, w, 18, Text.literal(""));
        t.setMaxLength(32);
        t.setText(val);
        addDrawableChild(t);
        return t;
    }

    @Override
    protected void init() {
        int x = 230;
        int w = 130;
        int y = 50;
        syncIdF = field(x, y, w, Integer.toString(orig.syncId()));
        y += 26;
        revisionF = field(x, y, w, Integer.toString(orig.revision()));
        y += 26;
        slotF = field(x, y, w, Short.toString(orig.slot()));
        y += 26;
        buttonF = field(x, y, w, Byte.toString(orig.button()));
        y += 26;

        addDrawableChild(ButtonWidget.builder(Text.literal("actionType: §e" + action.name()), b -> {
            SlotActionType[] v = SlotActionType.values();
            action = v[(action.ordinal() + 1) % v.length];
            clearAndInit();
        }).dimensions(x, y, w + 70, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(actionLabel), b -> apply())
                .dimensions(this.width / 2 - 154, this.height - 30, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancelar"), b -> close())
                .dimensions(this.width / 2 + 4, this.height - 30, 150, 20).build());
    }

    private void apply() {
        try {
            int syncId = Integer.parseInt(syncIdF.getText().trim());
            int revision = Integer.parseInt(revisionF.getText().trim());
            short slot = Short.parseShort(slotF.getText().trim());
            byte button = Byte.parseByte(buttonF.getText().trim());
            ClickSlotC2SPacket edited = new ClickSlotC2SPacket(
                    syncId, revision, slot, button, action, orig.modifiedStacks(), orig.cursor());
            onApply.accept(edited);
            close();
        } catch (Throwable t) {
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(
                        Text.literal("§c[GreenShark] Erro: " + t.getMessage()), false);
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(this.textRenderer, this.title, 20, 16, 0xFF55FF55);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("§7Forjar clique de container. Os hashes de item são mantidos."),
                20, 30, 0xFFFFFFFF);

        int y = 54;
        String[] labels = {"syncId (int)", "revision (int)", "slot (short)", "button (byte)"};
        for (String l : labels) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§f" + l), 20, y, 0xFFFFFFFF);
            y += 26;
        }

        int mods = 0;
        try {
            mods = orig.modifiedStacks().size();
        } catch (Throwable ignored) {
        }
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("§8dica: slot fora do menu / valores estranhos testam a validação do plugin"),
                20, this.height - 52, 0xFFAAAAAA);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("§8modifiedStacks=" + mods + "  (hashes originais mantidos)"),
                20, this.height - 42, 0xFFAAAAAA);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
