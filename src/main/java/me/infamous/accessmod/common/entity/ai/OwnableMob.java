package me.infamous.accessmod.common.entity.ai;

import javax.annotation.Nullable;
import java.util.UUID;

public interface OwnableMob {
    @Nullable
    UUID getOwnerUUID();

    void setOwnerUUID(@Nullable UUID pUniqueId);
}
