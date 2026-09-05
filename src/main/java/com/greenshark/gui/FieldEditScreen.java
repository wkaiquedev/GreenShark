package com.greenshark.gui;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.greenshark.inspect.PacketEditor;
import com.greenshark.inspect.PacketInspector;
import com.greenshark.model.Direction;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;

/** Tela de edição dos campos simples de um pacote antes de reenviá-lo. */
public class FieldEditScreen extends Screen {

    private final Screen parent;
    private final Packet<?> packet;
    private final Direction direction;
    private final String actionLabel;
    private final Consumer<Packet<?>> onApply;
    private final List<PacketInspector.Field> fields;
    private final Map<String, TextFieldWidget> editors = new LinkedHashMap<>();

    public FieldEditScreen(Screen parent, Packet<?> packet, Direction direction,
                           String actionLabel, Consumer<Packet<?>> onApply) {
        super(Text.literal("Editar: " + packet.getClass().getSimpleName()));
        this.parent = parent;
        this.packet = packet;
        this.direction = direction;
        this.actionLabel = actionLabel;
        this.onApply = onApply;
        this.fields = PacketInspector.describe(packet);
    }

    @Override
    protected void init() {
        editors.clear();
        int y = 40;
        int labelW = 170;
        int fieldX = 20 + labelW;
        int fieldW = Math.min(320, this.width - fieldX - 20);

        for (PacketInspector.Field f : fields) {
            if (!f.editable()) {
                continue;
            }
            if (y > this.height - 60) {
                break;
            }
            TextFieldWidget tf = new TextFieldWidget(this.textRenderer, fieldX, y, fieldW, 16, Text.literal(f.name()));
            tf.setMaxLength(32767);
            tf.setText(f.value());
            editors.put(f.name(), tf);
            addDrawableChild(tf);
            y += 20;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal(actionLabel), b -> apply())
                .dimensions(this.width / 2 - 154, this.height - 30, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancelar"), b -> close())
                .dimensions(this.width / 2 + 4, this.height - 30, 150, 20).build());
    }

    private void apply() {
        Map<String, String> edits = new HashMap<>();
        for (var e : editors.entrySet()) {
            edits.put(e.getKey(), e.getValue().getText());
        }
        try {
            Packet<?> edited = PacketEditor.applyEdits(packet, edits);
            onApply.accept(edited);
            close();
        } catch (Throwable t) {
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(
                        Text.literal("§c[GreenShark] Erro ao editar: " + t.getMessage()), false);
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(this.textRenderer, this.title, 20, 16, 0xFF55FF55);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("§7" + direction.arrow + "  —  edite os campos simples e confirme"),
                20, 26, 0xFFFFFFFF);

        int y = 40;
        for (PacketInspector.Field f : fields) {
            if (!f.editable()) {
                continue;
            }
            if (y > this.height - 60) {
                break;
            }
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("§f" + f.name() + " §8(" + f.type() + ")"), 20, y + 4, 0xFFFFFFFF);
            y += 20;
        }

        long ro = fields.stream().filter(f -> !f.editable()).count();
        if (ro > 0) {
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("§8" + ro + " campo(s) complexos mantidos como estão."),
                    20, this.height - 46, 0xFFAAAAAA);
        }
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
