package com.timhxl.fastmine.client;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.timhxl.fastmine.network.FastMineMiningPreviewSyncPayload;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * FastMine 范围挖掘预览渲染器。
 *
 * <p>服务端根据准星目标、当前规则和世界状态返回每个实际候选方块；客户端把候选集合转换为
 * 一个体素整体，只保留形状发生转折的外壳边。相邻方块的公共边和平坦外表面上的分割线均不绘制。</p>
 */
public final class FastMinePreviewRenderer {
    private static final float LINE_HALF_WIDTH = 0.0125F;
    private static final float BOX_EXPANSION = 0.003F;
    private static final RenderPipeline THROUGH_WALLS_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("fastmine", "preview_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build()
    );
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer STAGED_BUFFER = new StagedVertexBuffer(
            () -> "FastMine Preview Buffer", RenderType.SMALL_BUFFER_SIZE
    );

    private static PreviewRenderState previewState;
    private static PreviewRequest activeRequest;
    private static int nextRequestId;
    private static boolean closed;

    private FastMinePreviewRenderer() {
    }

    /** 注册一次客户端世界渲染回调。 */
    public static void initialize() {
        LevelExtractionEvents.END_EXTRACTION.register(FastMinePreviewRenderer::extractPreview);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(FastMinePreviewRenderer::renderPreview);
    }

    /** 在提取阶段读取当前客户端状态，绘制阶段不再访问世界数据。 */
    private static void extractPreview(LevelExtractionContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || closed
                || !(minecraft.hitResult instanceof BlockHitResult blockHit)
                || blockHit.getType() != HitResult.Type.BLOCK) {
            previewState = null;
            activeRequest = null;
            return;
        }

        var settings = FastMineClientSettings.getSnapshot();
        if (settings == null || (!settings.areaEnabled() && !settings.veinEnabled())) {
            previewState = null;
            activeRequest = null;
            return;
        }

        PreviewTarget target = new PreviewTarget(blockHit.getBlockPos().immutable(), blockHit.getDirection(),
                FastMineClientSettings.getRevision(), minecraft.player.isShiftKeyDown());
        if (activeRequest == null || !activeRequest.target().equals(target)) {
            activeRequest = new PreviewRequest(target, ++nextRequestId);
            previewState = null;
            FastMineClientNetworking.requestMiningPreview(
                    target.origin(), target.hitFace(), target.crouching(), activeRequest.requestId());
        }
    }

    /** 在绘制阶段提交红色、可穿墙的空心范围外框。 */
    private static void renderPreview(LevelRenderContext context) {
        if (previewState == null || previewState.edges().isEmpty() || closed) {
            return;
        }

        VertexFormat formatBinding = THROUGH_WALLS_PIPELINE.getVertexFormatBinding(0);
        if (formatBinding == null) {
            return;
        }

        PrimitiveTopology primitive = THROUGH_WALLS_PIPELINE.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = STAGED_BUFFER.appendDraw(formatBinding, primitive,
                primitive == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null);
        VertexConsumer builder = STAGED_BUFFER.getVertexBuilder(draw);

        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4fc matrix = matrices.last().pose();
        for (GridEdge edge : previewState.edges()) {
            renderOuterEdge(matrix, builder, edge);
        }
        matrices.popPose();
        STAGED_BUFFER.upload();

        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info != null) {
            draw(info);
        }
        STAGED_BUFFER.endFrame();
    }

    /** 使用细长实体盒绘制一条单位网格边，以适配 26.2 的标准填充调试渲染管线。 */
    private static void renderOuterEdge(Matrix4fc matrix, VertexConsumer builder, GridEdge edge) {
        float x = edge.x();
        float y = edge.y();
        float z = edge.z();
        switch (edge.axis()) {
            case X -> addFilledBox(matrix, builder,
                    x - BOX_EXPANSION, y - LINE_HALF_WIDTH, z - LINE_HALF_WIDTH,
                    x + 1.0F + BOX_EXPANSION, y + LINE_HALF_WIDTH, z + LINE_HALF_WIDTH);
            case Y -> addFilledBox(matrix, builder,
                    x - LINE_HALF_WIDTH, y - BOX_EXPANSION, z - LINE_HALF_WIDTH,
                    x + LINE_HALF_WIDTH, y + 1.0F + BOX_EXPANSION, z + LINE_HALF_WIDTH);
            case Z -> addFilledBox(matrix, builder,
                    x - LINE_HALF_WIDTH, y - LINE_HALF_WIDTH, z - BOX_EXPANSION,
                    x + LINE_HALF_WIDTH, y + LINE_HALF_WIDTH, z + 1.0F + BOX_EXPANSION);
        }
    }

    /** 向四边形缓冲区写入一个不透明红色细长盒。 */
    private static void addFilledBox(Matrix4fc matrix, VertexConsumer builder, float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ) {
        addQuad(matrix, builder, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        addQuad(matrix, builder, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ);
        addQuad(matrix, builder, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
        addQuad(matrix, builder, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ);
        addQuad(matrix, builder, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ);
        addQuad(matrix, builder, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
    }

    private static void addQuad(Matrix4fc matrix, VertexConsumer builder,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                float x3, float y3, float z3, float x4, float y4, float z4) {
        builder.addVertex(matrix, x1, y1, z1).setColor(1.0F, 0.1F, 0.1F, 1.0F);
        builder.addVertex(matrix, x2, y2, z2).setColor(1.0F, 0.1F, 0.1F, 1.0F);
        builder.addVertex(matrix, x3, y3, z3).setColor(1.0F, 0.1F, 0.1F, 1.0F);
        builder.addVertex(matrix, x4, y4, z4).setColor(1.0F, 0.1F, 0.1F, 1.0F);
    }

    /** 由 GameRenderer 关闭回调释放 GPU 缓冲区。 */
    public static void close() {
        if (!closed) {
            closed = true;
            previewState = null;
            activeRequest = null;
            STAGED_BUFFER.close();
        }
    }

    private static void draw(StagedVertexBuffer.ExecuteInfo info) {
        Minecraft minecraft = Minecraft.getInstance();
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX
        );
        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = target.getColorTextureView();
        if (colorTexture == null) {
            return;
        }

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "FastMine preview render pass", colorTexture, Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(THROUGH_WALLS_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", transforms);
            renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    /** 接收网络线程已切换到客户端线程后的服务端权威预览。 */
    public static void acceptServerPreview(FastMineMiningPreviewSyncPayload payload) {
        if (activeRequest != null && activeRequest.requestId() == payload.requestId()) {
            previewState = new PreviewRenderState(buildOuterEdges(payload.positions()));
        }
    }

    /**
     * 从候选体素集合提取真实外轮廓边。
     *
     * <p>一条网格边周围最多有四个体素：一个或三个被选中时它是凸角/凹角；两个对角体素
     * 被选中时它是不连续接触处。两个相邻体素形成平面时，该边只是表面分割线，必须删除。</p>
     */
    private static List<GridEdge> buildOuterEdges(List<BlockPos> positions) {
        if (positions.isEmpty()) {
            return List.of();
        }

        Set<BlockPos> blocks = new HashSet<>(positions);
        Set<GridEdge> candidates = new HashSet<>(Math.max(16, blocks.size() * 6));
        for (BlockPos block : blocks) {
            addCandidateEdges(candidates, block);
        }

        List<GridEdge> result = new ArrayList<>();
        for (GridEdge edge : candidates) {
            if (isOuterContourEdge(blocks, edge)) {
                result.add(edge);
            }
        }
        return List.copyOf(result);
    }

    /** 将一个方块的十二条单位网格边加入去重集合。 */
    private static void addCandidateEdges(Set<GridEdge> edges, BlockPos block) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        edges.add(new GridEdge(Direction.Axis.X, x, y, z));
        edges.add(new GridEdge(Direction.Axis.X, x, y + 1, z));
        edges.add(new GridEdge(Direction.Axis.X, x, y, z + 1));
        edges.add(new GridEdge(Direction.Axis.X, x, y + 1, z + 1));

        edges.add(new GridEdge(Direction.Axis.Y, x, y, z));
        edges.add(new GridEdge(Direction.Axis.Y, x + 1, y, z));
        edges.add(new GridEdge(Direction.Axis.Y, x, y, z + 1));
        edges.add(new GridEdge(Direction.Axis.Y, x + 1, y, z + 1));

        edges.add(new GridEdge(Direction.Axis.Z, x, y, z));
        edges.add(new GridEdge(Direction.Axis.Z, x + 1, y, z));
        edges.add(new GridEdge(Direction.Axis.Z, x, y + 1, z));
        edges.add(new GridEdge(Direction.Axis.Z, x + 1, y + 1, z));
    }

    /** 判断单位网格边是不是候选集合外壳形状的一部分。 */
    private static boolean isOuterContourEdge(Set<BlockPos> blocks, GridEdge edge) {
        boolean first;
        boolean second;
        boolean third;
        boolean fourth;
        int x = edge.x();
        int y = edge.y();
        int z = edge.z();

        switch (edge.axis()) {
            case X -> {
                first = blocks.contains(new BlockPos(x, y - 1, z - 1));
                second = blocks.contains(new BlockPos(x, y - 1, z));
                third = blocks.contains(new BlockPos(x, y, z));
                fourth = blocks.contains(new BlockPos(x, y, z - 1));
            }
            case Y -> {
                first = blocks.contains(new BlockPos(x - 1, y, z - 1));
                second = blocks.contains(new BlockPos(x, y, z - 1));
                third = blocks.contains(new BlockPos(x, y, z));
                fourth = blocks.contains(new BlockPos(x - 1, y, z));
            }
            case Z -> {
                first = blocks.contains(new BlockPos(x - 1, y - 1, z));
                second = blocks.contains(new BlockPos(x, y - 1, z));
                third = blocks.contains(new BlockPos(x, y, z));
                fourth = blocks.contains(new BlockPos(x - 1, y, z));
            }
            default -> throw new IllegalStateException("Unknown FastMine preview edge axis.");
        }

        int occupied = (first ? 1 : 0) + (second ? 1 : 0) + (third ? 1 : 0) + (fourth ? 1 : 0);
        return occupied == 1 || occupied == 3
                || (occupied == 2 && ((first && third) || (second && fourth)));
    }

    /** 清除上一台服务器或上一处目标的预览状态。 */
    public static void clear() {
        previewState = null;
        activeRequest = null;
    }

    /** 当前目标、设置版本或蹲下状态发生变化时会创建新的请求。 */
    private record PreviewTarget(BlockPos origin, Direction hitFace, long settingsRevision, boolean crouching) {
    }

    private record PreviewRequest(PreviewTarget target, int requestId) {
    }

    /** 一条沿世界网格轴延伸一个方块长度的外轮廓边。 */
    private record GridEdge(Direction.Axis axis, int x, int y, int z) {
    }

    /** 绘制阶段使用的、已经删除内部线和平面分割线的外轮廓边。 */
    private record PreviewRenderState(List<GridEdge> edges) {
    }
}
