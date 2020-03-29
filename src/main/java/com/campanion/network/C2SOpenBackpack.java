package com.campanion.network;

import com.campanion.Campanion;
import com.campanion.item.BackpackItem;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public class C2SOpenBackpack {
    public static final Identifier ID = new Identifier(Campanion.MOD_ID, "open_backpack");

    public static Packet<?> createPacket() {
        return ClientSidePacketRegistry.INSTANCE.toPacket(ID, new PacketByteBuf(Unpooled.buffer()));
    }

    @Environment(EnvType.CLIENT)
    public static void onPacket(PacketContext context, PacketByteBuf byteBuf) {
        PlayerEntity player = context.getPlayer();
        ItemStack stack = player.getEquippedStack(EquipmentSlot.CHEST);
        if(!(stack.getItem() instanceof BackpackItem)) {
            boolean set = false;
            for (Hand value : Hand.values()) {
                ItemStack held = player.getStackInHand(value);
                if(held.getItem() instanceof BackpackItem) {
                    stack = held;
                    set = true;
                }
            }
            if(!set) {
                return;
            }
        }
        BackpackItem.Type type = ((BackpackItem) stack.getItem()).type;
        player.openContainer(type.createFactory(stack));
    }
}
