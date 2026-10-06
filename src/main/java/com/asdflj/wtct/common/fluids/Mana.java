package com.asdflj.wtct.common.fluids;

import net.minecraftforge.fluids.Fluid;

import com.asdflj.wtct.util.NameConst;

public class Mana extends Fluid {

    public Mana() {
        super(NameConst.MANA);
        this.setDensity(10000);
        this.setViscosity(10000);
        this.setTemperature(300);
    }

}
