package grainalcohol.dtt.mixin.modification;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import grainalcohol.dtt.client.DTTServerConfigCache;
import net.depression.listener.client.ClientTickEventListener;
import net.depression.mental.MentalStatus;
import net.depression.network.MentalTraitPacket;
import net.minecraft.client.KeyboardHandler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientTickEventListener.class, remap = false)
public class ClientTickEventListenerMixin {
    @WrapOperation(
            method = "onClientLevelTick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/depression/client/ClientMentalStatus;isMentalTraitSelected:Z",
                    opcode = Opcodes.GETSTATIC
            )
    )
    private static boolean disableMentalTraitSelectedScreen(Operation<Boolean> original) {
        if (DTTServerConfigCache.disableMentalTraitSelectScreen) {
            MentalTraitPacket.sendToServer(MentalStatus.DEFAULT_MENTAL_TRAIT == null ? "normal" : MentalStatus.DEFAULT_MENTAL_TRAIT);
            return true;
        }

        return original.call();
    }

    @WrapOperation(
            method = "onClientLevelTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyboardHandler;keyPress(JIIII)V",
                    remap = true
            )
    )
    private static void saferCombatForCatatonicStupor(KeyboardHandler keyboard, long window, int key, int scancode, int action, int modifiers, Operation<Void> original) {
        if (!DTTServerConfigCache.saferCatatonicStupor) {
            original.call(keyboard, window, key, scancode, action, modifiers);
        }
    }
}
