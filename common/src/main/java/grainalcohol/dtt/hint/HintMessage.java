package grainalcohol.dtt.hint;

import grainalcohol.dtt.hint.timer.TimeUnit;
import grainalcohol.dtt.hint.timer.Timer;
import grainalcohol.dtt.registry.DTTRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class HintMessage {
    public static final Timer DEFAULT_TIMER = Timer.of(3, TimeUnit.GAME_HOUR);
    private final ResourceLocation identifier;
    private final boolean isImportant;
    private final int variantCount;

    public HintMessage(ResourceLocation identifier, boolean isImportant, int variantCount) {
        this.identifier = identifier;
        this.isImportant = isImportant;
        this.variantCount = variantCount;
        // 自动注册到全局注册表中，方便同时管理
        Registry.register(DTTRegistries.GLOBAL_HINT_MESSAGE_REGISTRY, this.getIdentifier(), this);
    }

    @NotNull
    public ResourceLocation getIdentifier() {
        return identifier;
    }

    public int getVariantCount() {
        return variantCount;
    }

    public boolean isImportant() {
        return isImportant;
    }

    public abstract boolean autoSend();

    @Nullable
    public abstract Predicate<ServerPlayer> getGlobalCondition();

    @NotNull
    public abstract String getTranslationKey();

    /**
     * 提示附带的效果，服务端实际发出这条提示后执行一次（见 {@link HintMessageSender} 的“afterSend 的执行时机”）。
     * 没有附带效果时返回 null。
     */
    @Nullable
    public abstract Consumer<ServerPlayer> getAfterSend();

    @NotNull
    public abstract Timer getCooldownTimer();

    @Nullable
    public abstract Timer getAccumulateTimer();

    @NotNull
    public HintMessageInstance createInstance() {
        return new HintMessageInstance(this);
    }
}
