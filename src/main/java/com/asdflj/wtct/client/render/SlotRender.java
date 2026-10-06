package com.asdflj.wtct.client.render;

import java.util.Collection;
import java.util.HashMap;

public class SlotRender {

    private static final HashMap<Class<? extends ISlotRender>, ISlotRender> renders = new HashMap<>();
    private static final SlotRender API = new SlotRender();

    private SlotRender() {
        registerSlotRenderHandler(RenderFluidDrop.class, new RenderFluidDrop());
        registerSlotRenderHandler(RenderFluidPacketPatternSlot.class, new RenderFluidPacketPatternSlot());
        registerSlotRenderHandler(RenderEncodedPattern.class, new RenderEncodedPattern());
        registerSlotRenderHandler(RenderPatternSlotFake.class, new RenderPatternSlotFake());
    }

    public static SlotRender instance() {
        return API;
    }

    public void registerSlotRenderHandler(Class<? extends ISlotRender> cls, ISlotRender render) {
        renders.putIfAbsent(cls, render);
    }

    public Collection<ISlotRender> getRenders() {
        return renders.values();
    }

}
