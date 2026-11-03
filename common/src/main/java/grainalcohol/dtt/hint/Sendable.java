package grainalcohol.dtt.hint;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public interface Sendable {
    @NotNull HintMessage asHintMessage();
    @NotNull ResourceLocation getIdentifier();
    void send(ServerPlayer player);
    @NotNull String getTranslationKey();
    int getVariantCount();
    boolean isImportant();
    @NotNull String[] getContext();
}
