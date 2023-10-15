package me.mrnavastar.reincarnaria.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.network.ServerPlayerEntity;

public class ChatUtil {

    public static Component newMessage(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    public static void ERROR(ServerPlayerEntity player, String message) {
        player.sendMessage(MiniMessage.miniMessage().deserialize("<color:#ff918d><u>ERROR!</u> " + message + "</color>"));
    }
}
