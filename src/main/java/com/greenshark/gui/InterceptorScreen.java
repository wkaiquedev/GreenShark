package com.greenshark.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.greenshark.action.PacketActions;
import com.greenshark.inspect.PacketInspector;
import com.greenshark.model.CapturedPacket;
import com.greenshark.model.Direction;
import com.greenshark.model.PacketLog;
import com.greenshark.net.HeldPacket;
import com.greenshark.net.InterceptQueue;
import com.greenshark.net.PacketInterceptor;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Tela principal estilo Burp: lista de pacotes, inspeção, replay e edição. */
public class InterceptorScreen extends Screen {

    // Substrings de nomes de classe considerados "ruído" (alto volume).
    private static final Set<String> NOISE = Set.of(
            "PlayerMoveC2SPacket", "EntityS2CPacket", "EntityPositionS2CPacket",
            "EntityVelocityUpdateS2CPacket", "EntitySetHeadYawS2CPacket",
            "KeepAlive", "ChunkData", "LightUpdate", "ChunkDeltaUpdate",
            "EntityTrackerUpdateS2CPacket", "ParticleS2CPacket", "CommonPingS2CPacket",
            "UpdateTime", "EntityAttributesS2CPacket");

    private TextFieldWidget filterField;
    private String filter = "";
    private boolean hideNoise = true;
    private double scroll = 0;
    private long selectedId = -1;
    private List<CapturedPacket> view = new ArrayList<>();
    private boolean renderErrorLogged = false;

    public InterceptorScreen() {
        super(Text.literal("GreenShark — Interceptador"));
    }

    private int listX() {
        return 8;
    }

    private int listW() {
        return this.width / 2 - 16;
    }

    private int listTop() {
        return 62;
    }

    private int listBottom() {
        return this.height - 44;
    }

    private int rowH() {
        return 12;
    }

    @Override
    protected void init() {
        int y = 30;
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Intercept S→C: " + onoff(PacketInterceptor.interceptInbound)),
                        b -> {
                            PacketInterceptor.interceptInbound = !PacketInterceptor.interceptInbound;
                            if (!PacketInterceptor.interceptInbound) {
                                InterceptQueue.INSTANCE.forwardAll();
                            }
                            clearAndInit();
                        })
                .dimensions(8, y, 138, 20).build());
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Intercept C→S: " + onoff(PacketInterceptor.interceptOutbound)),
                        b -> {
                            PacketInterceptor.interceptOutbound = !PacketInterceptor.interceptOutbound;
                            if (!PacketInterceptor.interceptOutbound) {
                                InterceptQueue.INSTANCE.forwardAll();
                            }
                            clearAndInit();
                        })
                .dimensions(150, y, 138, 20).build());
        addDrawableChild(ButtonWidget.builder(
                        Text.literal(PacketLog.INSTANCE.isCapturing() ? "Captura: §aON" : "Captura: §7OFF"),
                        b -> {
                            PacketLog.INSTANCE.setCapturing(!PacketLog.INSTANCE.isCapturing());
                            clearAndInit();
                        })
                .dimensions(292, y, 96, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Limpar"),
                        b -> {
                            PacketLog.INSTANCE.clear();
                            selectedId = -1;
                        })
                .dimensions(392, y, 58, 20).build());
        addDrawableChild(ButtonWidget.builder(
                        Text.literal(hideNoise ? "Ruído: §7oculto" : "Ruído: §avisível"),
                        b -> {
                            hideNoise = !hideNoise;
                            clearAndInit();
                        })
                .dimensions(454, y, 110, 20).build());

        filterField = new TextFieldWidget(this.textRenderer, listX(), listTop() - 18, listW(), 14,
                Text.literal("filtro"));
        filterField.setText(filter);
        filterField.setPlaceholder(Text.literal("filtrar por nome…"));
        filterField.setChangedListener(s -> filter = s);
        addDrawableChild(filterField);

        // Ações do pacote selecionado (lado direito, base).
        int rx = this.width / 2 + 8;
        int rw = this.width - rx - 8;
        int half = (rw - 6) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Repetir (replay)"), b -> doReplay())
                .dimensions(rx, this.height - 34, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Editar & enviar"), b -> doEdit())
                .dimensions(rx + half + 6, this.height - 34, half, 20).build());

        // Controles de pacote segurado (intercept).
        HeldPacket held = InterceptQueue.INSTANCE.peek();
        if (held != null) {
            addDrawableChild(ButtonWidget.builder(Text.literal("§aEncaminhar [F]"),
                            b -> {
                                HeldPacket h = InterceptQueue.INSTANCE.poll();
                                if (h != null) {
                                    h.forward();
                                }
                                clearAndInit();
                            })
                    .dimensions(8, this.height - 34, 120, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("§cDescartar [X]"),
                            b -> {
                                HeldPacket h = InterceptQueue.INSTANCE.poll();
                                if (h != null) {
                                    h.drop();
                                }
                                clearAndInit();
                            })
                    .dimensions(132, this.height - 34, 120, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Editar & encaminhar"),
                            b -> editHeld())
                    .dimensions(256, this.height - 34, 150, 20).build());
        }

        com.greenshark.GreenSharkClient.LOGGER.info(
                "[GreenShark][diag] GUI aberta: PacketLog.size={} capturing={} view(após filtro)={}",
                PacketLog.INSTANCE.size(), PacketLog.INSTANCE.isCapturing(), computeView().size());
    }

    private String onoff(boolean b) {
        return b ? "§aON" : "§7OFF";
    }

    private List<CapturedPacket> computeView() {
        List<CapturedPacket> all = PacketLog.INSTANCE.snapshot();
        String f = filter.toLowerCase(Locale.ROOT);
        List<CapturedPacket> out = new ArrayList<>();
        // Percorre do mais novo para o mais antigo: o pacote recém-capturado
        // aparece no TOPO da lista (como um console de rede).
        for (int i = all.size() - 1; i >= 0; i--) {
            CapturedPacket p = all.get(i);
            if (hideNoise && isNoise(p.className)) {
                continue;
            }
            if (!f.isEmpty() && !p.name.toLowerCase(Locale.ROOT).contains(f)) {
                continue;
            }
            out.add(p);
        }
        return out;
    }

    private boolean isNoise(String className) {
        for (String n : NOISE) {
            if (className.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private CapturedPacket selected() {
        for (CapturedPacket p : view) {
            if (p.id == selectedId) {
                return p;
            }
        }
        return null;
    }

    private void doReplay() {
        CapturedPacket p = selected();
        if (p == null) {
            toast("§7Selecione um pacote primeiro.");
            return;
        }
        boolean ok = PacketActions.replay(p);
        toast(ok ? "Replay enviado: " + p.name : "§cFalha no replay (sem conexão).");
    }

    private void doEdit() {
        CapturedPacket p = selected();
        if (p == null) {
            toast("§7Selecione um pacote primeiro.");
            return;
        }
        this.client.setScreen(new FieldEditScreen(this, p.packet, p.direction, "Salvar & enviar", edited -> {
            boolean ok = p.direction == Direction.SERVERBOUND
                    ? PacketActions.replayServerbound(edited)
                    : PacketActions.replayClientbound(edited);
            toast(ok ? "Pacote editado enviado." : "§cFalha ao enviar.");
        }));
    }

    private void editHeld() {
        HeldPacket held = InterceptQueue.INSTANCE.peek();
        if (held == null) {
            return;
        }
        this.client.setScreen(new FieldEditScreen(this, held.packet, held.direction, "Salvar & encaminhar", edited -> {
            HeldPacket h = InterceptQueue.INSTANCE.poll();
            if (h != null) {
                h.packet = edited;
                h.forward();
            }
        }));
    }

    private void toast(String s) {
        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(Text.literal("§b[GreenShark] §f" + s), false);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (mouseX >= listX() && mouseX <= listX() + listW()
                && mouseY >= listTop() && mouseY <= listBottom()) {
            int idx = (int) ((mouseY - listTop() + scroll) / rowH());
            if (idx >= 0 && idx < view.size()) {
                selectedId = view.get(idx).id;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= listX() && mouseX <= listX() + listW()
                && mouseY >= listTop() && mouseY <= listBottom()) {
            scroll -= vertical * rowH() * 2;
            double maxScroll = Math.max(0, view.size() * rowH() - (listBottom() - listTop()));
            scroll = Math.max(0, Math.min(scroll, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        HeldPacket held = InterceptQueue.INSTANCE.peek();
        if (held != null) {
            if (keyCode == GLFW.GLFW_KEY_F) {
                HeldPacket h = InterceptQueue.INSTANCE.poll();
                if (h != null) {
                    h.forward();
                }
                clearAndInit();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_X) {
                HeldPacket h = InterceptQueue.INSTANCE.poll();
                if (h != null) {
                    h.drop();
                }
                clearAndInit();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.view = computeView();
        super.render(ctx, mouseX, mouseY, delta);

        try {
        ctx.drawTextWithShadow(this.textRenderer, this.title, 8, 10, 0xFF55FF55);
        int heldCount = InterceptQueue.INSTANCE.size();
        String status = "Total: " + PacketLog.INSTANCE.size() + "   Exibindo: " + view.size()
                + (heldCount > 0 ? "   §cSEGURADOS: " + heldCount : "");
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7" + status), 8, 20, 0xFFFFFFFF);

        // Painel da lista.
        ctx.fill(listX() - 2, listTop() - 2, listX() + listW() + 2, listBottom() + 2, 0x88000000);
        ctx.enableScissor(listX(), listTop(), listX() + listW(), listBottom());
        int y = listTop() - (int) scroll;
        for (CapturedPacket p : view) {
            if (y + rowH() >= listTop() && y <= listBottom()) {
                if (p.id == selectedId) {
                    ctx.fill(listX(), y, listX() + listW(), y + rowH(), 0x804CAF50);
                }
                int color = p.direction == Direction.CLIENTBOUND ? 0xFF7FB0FF : 0xFFFFC77F;
                String line = "#" + p.id + " " + p.direction.arrow + " " + p.name;
                ctx.drawText(this.textRenderer, this.textRenderer.trimToWidth(line, listW() - 6),
                        listX() + 3, y + 2, color, false);
            }
            y += rowH();
        }
        ctx.disableScissor();

        // Painel de detalhe.
        int dx = this.width / 2 + 8;
        int dy = listTop();
        int dw = this.width - dx - 8;
        ctx.fill(dx - 2, dy - 2, dx + dw + 2, listBottom() + 2, 0x88000000);
        CapturedPacket sel = selected();
        if (sel != null) {
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("§e" + sel.name + " §7(" + sel.direction.arrow + ")"), dx, dy, 0xFFFFFFFF);
            int fy = dy + 14;
            for (PacketInspector.Field f : PacketInspector.describe(sel.packet)) {
                if (fy > listBottom() - 4) {
                    break;
                }
                String head = (f.editable() ? "§a" : "§7") + f.name() + " §8: §7" + f.type();
                ctx.drawText(this.textRenderer, this.textRenderer.trimToWidth(head, dw - 6),
                        dx + 2, fy, 0xFFFFFFFF, false);
                fy += 10;
                if (fy > listBottom() - 4) {
                    break;
                }
                ctx.drawText(this.textRenderer, this.textRenderer.trimToWidth("  = " + f.value(), dw - 6),
                        dx + 2, fy, 0xFFC0C0C0, false);
                fy += 12;
            }
        } else {
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("§7Selecione um pacote à esquerda."), dx, dy, 0xFFFFFFFF);
        }

        // Aviso de pacote segurado.
        HeldPacket held = InterceptQueue.INSTANCE.peek();
        if (held != null) {
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("§cSEGURADO §f" + held.direction.arrow + " "
                            + held.packet.getClass().getSimpleName()), 8, listBottom() + 6, 0xFFFFFFFF);
        }
        } catch (Throwable t) {
            if (!renderErrorLogged) {
                renderErrorLogged = true;
                com.greenshark.GreenSharkClient.LOGGER.error("[GreenShark][diag] EXCEÇÃO ao desenhar a tela", t);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
