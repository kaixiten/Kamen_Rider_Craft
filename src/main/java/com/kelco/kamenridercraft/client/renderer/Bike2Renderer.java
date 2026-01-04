package com.kelco.kamenridercraft.client.renderer;

import com.kelco.kamenridercraft.KamenRiderCraftCore;
import com.kelco.kamenridercraft.client.models.Bike2Model;
import com.kelco.kamenridercraft.client.models.BikeModel;
import com.kelco.kamenridercraft.entities.bikes.baseBike2Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Bike2Renderer extends GeoEntityRenderer<baseBike2Entity> {



	public Bike2Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Bike2Model());
    	this.scaleWidth = 1.1f;
		this.scaleHeight = 1.1f;

    }

	@Override
	public ResourceLocation getTextureLocation(baseBike2Entity animatable) {
		 return ResourceLocation.fromNamespaceAndPath(KamenRiderCraftCore.MOD_ID, "textures/entities/"+animatable.NAME+".png");
	}


	
    @Override
    public void render(baseBike2Entity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
    	poseStack.translate(0, -0.15, 0);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}