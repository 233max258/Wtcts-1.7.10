package com.asdflj.wtct.client.gui.nethub;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;

import com.asdflj.wtct.common.nethub.IListObject;

/**
 * One row of a {@link ListCtrl}. The list only knows this interface, so the two network screens can each draw their
 * rows
 * their own way.
 */
public interface IListItem<T extends IListObject> {

    T get();

    List<String> getTooltip();

    void draw(Minecraft mc, int mouseX, int mouseY, float partialTicks);

    void setSelected(boolean selected);

    Object getId();

    void click();

    void playPressSound(SoundHandler soundHandlerIn);

    boolean isMouseOver(int mouseX, int mouseY);
}
