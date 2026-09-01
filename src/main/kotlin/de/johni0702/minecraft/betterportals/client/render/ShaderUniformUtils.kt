package de.johni0702.minecraft.betterportals.client.render

import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.shader.ShaderManager
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Matrix4f

/**
 * Supplies the state which cannot be represented by the native ESSL fixed-function
 * interface.  The normal 1.12 compatibility path does not expose these uniforms,
 * so all lookups are conditional and cost nothing there.
 */
internal object ShaderUniformUtils {
    private val matrixBuffer = GLAllocation.createDirectFloatBuffer(16)

    private fun currentMatrix(pname: Int): Matrix4f {
        matrixBuffer.clear()
        GL11.glGetFloat(pname, matrixBuffer)
        matrixBuffer.flip().limit(16)
        return Matrix4f().apply { load(matrixBuffer) }
    }

    fun setPortalState(shader: ShaderManager) {
        // These uniforms exist only in the direct ESSL branch.  The compatibility
        // branch is fed by SFPEW/MobileGlues and returns null here.
        if (shader.getShaderUniform("bpModelViewMatrix") == null) return

        shader.getShaderUniformOrDefault("bpModelViewMatrix")
                .set(currentMatrix(GL11.GL_MODELVIEW_MATRIX))
        shader.getShaderUniformOrDefault("bpProjectionMatrix")
                .set(currentMatrix(GL11.GL_PROJECTION_MATRIX))

        val fog = GlStateManager.fogState
        val range = fog.end - fog.start
        val scale = if (fog.mode == GlStateManager.FogMode.LINEAR.capabilityId && range > 0f) {
            1f / range
        } else {
            0f
        }
        shader.getShaderUniformOrDefault("bpFogStart").set(fog.start)
        shader.getShaderUniformOrDefault("bpFogScale").set(scale)
        with(GlStateManager.clearState.color) {
            shader.getShaderUniformOrDefault("bpWorldFogColor").set(red, green, blue)
        }
    }

    fun setInverseModelViewProjection(shader: ShaderManager) {
        if (shader.getShaderUniform("bpInverseModelViewProjectionMatrix") == null) return

        val modelView = currentMatrix(GL11.GL_MODELVIEW_MATRIX)
        val projection = currentMatrix(GL11.GL_PROJECTION_MATRIX)
        val modelViewProjection = Matrix4f()
        Matrix4f.mul(projection, modelView, modelViewProjection)
        val inverse = Matrix4f.invert(modelViewProjection, null) ?: Matrix4f()
        shader.getShaderUniformOrDefault("bpInverseModelViewProjectionMatrix").set(inverse)
    }
}
