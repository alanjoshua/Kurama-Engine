package main;

import Kurama.Vulkan.VulkanRendererBase;
import Kurama.game.Game;

import static Kurama.Vulkan.VulkanUtilities.device;
import static org.lwjgl.vulkan.EXTMeshShader.VK_EXT_MESH_SHADER_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRShaderFloatControls.VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSpirv14.VK_KHR_SPIRV_1_4_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;

public class PointCloudRenderer extends VulkanRendererBase {

    public PointCloudRenderer(Game game) {
        super(game);
        DEVICE_EXTENSIONS.add(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME);
        DEVICE_EXTENSIONS.add(VK_EXT_MESH_SHADER_EXTENSION_NAME);
        DEVICE_EXTENSIONS.add(VK_KHR_SPIRV_1_4_EXTENSION_NAME);
        DEVICE_EXTENSIONS.add(VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME);
    }

    @Override
    public void initRenderer() {

    }

    @Override
    public void swapChainRecreatedEvent() {

    }

    @Override
    public void meshesMergedEvent() {

    }

    @Override
    public void cameraUpdatedEvent() {

    }

    @Override
    public void initDescriptorSets() {

    }

    @Override
    public void initBuffers() {

    }

    @Override
    public void initRenderPasses() {

    }

    @Override
    public void initPipelines() {

    }

    @Override
    public void render() {

    }

    @Override
    public void cleanUp() {
        // Wait for the device to complete all operations before release resources
        vkDeviceWaitIdle(device);

        for(int i = deletionQueue.size()-1; i >= 0; i--) {
            deletionQueue.get(i).run();
        }
    }
}
