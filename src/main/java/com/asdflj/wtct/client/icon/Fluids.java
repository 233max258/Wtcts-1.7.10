package com.asdflj.wtct.client.icon;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;

import com.asdflj.wtct.api.WtctAPI;
import com.asdflj.wtct.util.NameConst;

public enum Fluids {

    Mana("fluids/mana", WtctAPI.instance()
        .getMana());

    private final String name;
    private final Fluid fluid;
    public net.minecraft.util.IIcon IIcon;

    Fluids(String name, Fluid fluid) {
        this.name = name;
        this.fluid = fluid;
    }

    public String getName() {
        return this.name;
    }

    public IIcon getIcon() {
        return this.IIcon;
    }

    public final Fluid getFluid() {
        return this.fluid;
    }

    public void registerIcon(final TextureMap map) {
        this.IIcon = map.registerIcon(NameConst.RES_KEY + this.name);
        this.fluid.setStillIcon(this.IIcon);
    }
}
