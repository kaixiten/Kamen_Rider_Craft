package com.kelco.kamenridercraft.client.models;


import com.kelco.kamenridercraft.KamenRiderCraftCore;
import com.kelco.kamenridercraft.entities.bikes.baseBike2Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Bike2Model extends GeoModel<baseBike2Entity> {
    private long lastRenderedInstance = -1L;

    @Override
    public ResourceLocation getModelResource(baseBike2Entity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderCraftCore.MOD_ID, "geo/"+animatable.NAME_MODEL+".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(baseBike2Entity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderCraftCore.MOD_ID, "textures/entities/"+animatable.NAME+".png");
    }

    @Override
    public ResourceLocation getAnimationResource(baseBike2Entity animatable) {
        return ResourceLocation.fromNamespaceAndPath(KamenRiderCraftCore.MOD_ID, "animations/"+animatable.NAME_ANIMATIONS+".animation.json");
    }


    @Override
    public void setCustomAnimations(baseBike2Entity an, long instanceId, AnimationState<baseBike2Entity> state) {


        GeoBone front_fork = this.getAnimationProcessor().getBone("front_fork");
        GeoBone front_fork2 = this.getAnimationProcessor().getBone("front_fork2");
        GeoBone b_wheel= this.getAnimationProcessor().getBone("b_wheel");
        GeoBone f_wheel = this.getAnimationProcessor().getBone("f_wheel");
        EntityModelData entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
        baseBike2Entity entityData2 = (baseBike2Entity) state.getData(DataTickets.ENTITY);

        baseBike2Entity  animatable= state.getAnimatable();
        if (front_fork != null) {
            front_fork.setRotY(entityData.headPitch());
        }
        if (front_fork2 != null) {
            front_fork2.setRotY(entityData.headPitch());
        }
        if (b_wheel != null & f_wheel != null) {
               // f_wheel.setRotX(entityData2.yHeadRot);
               // b_wheel.setRotX(entityData2.yHeadRot);
            }
    }
}