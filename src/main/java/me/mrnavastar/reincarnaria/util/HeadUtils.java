package me.mrnavastar.reincarnaria.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;

public class HeadUtils {

    public static NbtCompound nbtFromProfile(GameProfile profile) {
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), profile));

        NbtCompound displayTag = new NbtCompound();
        displayTag.putString("Name", getHeadName(profile.getName()));
        nbtCompound.put("display", displayTag);

        return nbtCompound;
    }

    public static NbtCompound addLore(NbtCompound nbt, PlayerEntity attacker) {
        NbtCompound display = nbt.getCompound("display");

        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(getItalicJsonText("Killed by " + attacker.getName())));

        display.put("Lore", loreList);
        nbt.put("display", display);

        return nbt;
    }

    private static String getHeadName(String name) {
        return name + "'s Severed Head";
    }

    private static String getItalicJsonText(String string) {
        return Text.Serializer.toJson(Text.literal(string).styled(style -> style.withItalic(true)));
    }
}