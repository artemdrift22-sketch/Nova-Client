package dev.nova.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * 3D-рендер в мире. Вызывать из WorldRenderEvents (Module.onRender3D).
 * Все линии идут через RenderLayer.getLines() — у него включён тест глубины, поэтому
 * контуры естественно прячутся за стенами (честный режим, без wallhack).
 */
public final class Render3DUtils {
    private Render3DUtils() {}

    private static MatrixStack stack(WorldRenderContext ctx) {
        MatrixStack ms = ctx.matrixStack();
        if (ms == null) {
            Camera c = ctx.camera();
            ms = new MatrixStack();
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(c.getPitch()));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(c.getYaw() + 180f));
        }
        return ms;
    }

    private static VertexConsumerProvider.Immediate buffers() {
        return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
    }

    private static void line(VertexConsumer vc, MatrixStack.Entry e, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        dx /= len; dy /= len; dz /= len;
        vc.vertex(e, x1, y1, z1).color(color).normal(e, dx, dy, dz);
        vc.vertex(e, x2, y2, z2).color(color).normal(e, dx, dy, dz);
    }

    /**
     * Контур коробки (12 рёбер).
     * throughWalls игнорируется намеренно: Nova — визуальный мод для серверов, сквозь стены не рисуем.
     */
    public static void drawBoxESP(WorldRenderContext ctx, Box box, int color, boolean throughWalls) {
        MatrixStack ms = stack(ctx);
        Vec3d cam = ctx.camera().getPos();
        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumerProvider.Immediate imm = buffers();
        VertexConsumer vc = imm.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry e = ms.peek();
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
        // низ
        line(vc, e, x1, y1, z1, x2, y1, z1, color);
        line(vc, e, x2, y1, z1, x2, y1, z2, color);
        line(vc, e, x2, y1, z2, x1, y1, z2, color);
        line(vc, e, x1, y1, z2, x1, y1, z1, color);
        // верх
        line(vc, e, x1, y2, z1, x2, y2, z1, color);
        line(vc, e, x2, y2, z1, x2, y2, z2, color);
        line(vc, e, x2, y2, z2, x1, y2, z2, color);
        line(vc, e, x1, y2, z2, x1, y2, z1, color);
        // стойки
        line(vc, e, x1, y1, z1, x1, y2, z1, color);
        line(vc, e, x2, y1, z1, x2, y2, z1, color);
        line(vc, e, x2, y1, z2, x2, y2, z2, color);
        line(vc, e, x1, y1, z2, x1, y2, z2, color);
        ms.pop();
        imm.draw(RenderLayer.getLines());
    }

    /** Хитбокс сущности с интерполяцией позиции (без дёргания на высоком FPS). */
    public static void drawEntityBox(WorldRenderContext ctx, Entity entity, int color) {
        float delta = ctx.tickCounter().getTickDelta(true);
        Vec3d lerped = entity.getLerpedPos(delta);
        Box box = entity.getBoundingBox().offset(lerped.subtract(entity.getPos()));
        drawBoxESP(ctx, box, color, false);
    }

    /** Отрезок в мире. */
    public static void drawLine3D(WorldRenderContext ctx, Vec3d start, Vec3d end, int color) {
        drawPolyline(ctx, List.of(start, end), color);
    }

    /** Ломаная (траектория стрелы и т.п.). */
    public static void drawPolyline(WorldRenderContext ctx, List<Vec3d> points, int color) {
        if (points.size() < 2) return;
        MatrixStack ms = stack(ctx);
        Vec3d cam = ctx.camera().getPos();
        ms.push();
        VertexConsumerProvider.Immediate imm = buffers();
        VertexConsumer vc = imm.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry e = ms.peek();
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3d a = points.get(i).subtract(cam), b = points.get(i + 1).subtract(cam);
            line(vc, e, (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z, color);
        }
        ms.pop();
        imm.draw(RenderLayer.getLines());
    }

    /** Квадрат с текстурой, всегда повёрнутый к камере (иконки, питомцы-спрайты). */
    public static void drawBillboard(WorldRenderContext ctx, Vec3d pos, Identifier texture, float scale) {
        MatrixStack ms = stack(ctx);
        Vec3d cam = ctx.camera().getPos();
        ms.push();
        ms.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        ms.multiply(ctx.camera().getRotation());
        ms.scale(scale, scale, scale);
        RenderLayer layer = RenderLayer.getEntityTranslucent(texture);
        VertexConsumerProvider.Immediate imm = buffers();
        VertexConsumer vc = imm.getBuffer(layer);
        MatrixStack.Entry e = ms.peek();
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        quadVertex(vc, e, -0.5f, -0.5f, 0f, 1f, light);
        quadVertex(vc, e, 0.5f, -0.5f, 1f, 1f, light);
        quadVertex(vc, e, 0.5f, 0.5f, 1f, 0f, light);
        quadVertex(vc, e, -0.5f, 0.5f, 0f, 0f, light);
        ms.pop();
        imm.draw(layer);
    }

    private static void quadVertex(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float u, float v, int light) {
        vc.vertex(e, x, y, 0f).color(0xFFFFFFFF).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, 0f, 0f, 1f);
    }
}
