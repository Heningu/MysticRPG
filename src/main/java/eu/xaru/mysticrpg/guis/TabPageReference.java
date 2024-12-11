package eu.xaru.mysticrpg.guis;

import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

public record TabPageReference(
        PagedGui<Item> pagedGui,
        ControlItem<PagedGui<?>> backControl,
        ControlItem<PagedGui<?>> forwardControl
) {}
