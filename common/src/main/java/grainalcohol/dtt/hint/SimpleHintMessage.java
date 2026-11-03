package grainalcohol.dtt.hint;

import net.minecraft.resources.ResourceLocation;

public abstract class SimpleHintMessage extends HintMessage {
    public SimpleHintMessage(ResourceLocation identifier, boolean isImportant, int variantCount) {
        super(identifier, isImportant, variantCount);
    }

    @Override
    public boolean autoSend() {
        return true;
    }
}
