package com.flowclient.mods.advancements;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import com.flowclient.mixin.ClientAdvancementsAccessor;

public final class AdvancementsUnlockController {
    private static final int TICK_INTERVAL = 20;

    private int tickCounter;

    private AdvancementsUnlockController() {
    }

    private static final AdvancementsUnlockController INSTANCE = new AdvancementsUnlockController();

    public static AdvancementsUnlockController get() {
        return INSTANCE;
    }

    public void reset() {
        this.tickCounter = 0;
        AdvancementEventContext.get().reset();
    }

    public void onBlockBroken(net.minecraft.world.level.block.state.BlockState state) {
        AdvancementEventContext.get().onBlockBroken(state);
    }

    public void onItemPickup(net.minecraft.world.item.ItemStack stack) {
        AdvancementEventContext.get().onItemPickup(stack);
    }

    public void onMobKill(net.minecraft.world.entity.Entity entity) {
        AdvancementEventContext.get().onMobKill(entity);
    }

    public void onDeath(net.minecraft.world.damagesource.DamageSource source) {
        AdvancementEventContext.get().onDeath(source);
    }

    public void onItemCrafted(net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> recipe) {
        AdvancementEventContext.get().onItemCrafted(recipe);
    }

    public void onItemUsed() {
        AdvancementEventContext.get().onItemUsed();
    }

    public void onItemConsumed(net.minecraft.world.item.ItemStack stack) {
        AdvancementEventContext.get().onItemConsumed(stack);
    }

    public void onDimensionChange(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> from, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> to) {
        AdvancementEventContext.get().onDimensionChange(from, to);
    }

    public void tick(Minecraft client) {
        if (!AdvancementsUnlockMod.isEnabled()) {
            return;
        }
        if (client.player == null || client.level == null || client.isLocalServer()) {
            return;
        }

        ClientPacketListener connection = client.getConnection();
        if (connection == null) {
            return;
        }

        this.tickCounter++;
        if (this.tickCounter < TICK_INTERVAL) {
            return;
        }
        this.tickCounter = 0;

        ClientAdvancements advancements = connection.getAdvancements();
        if (advancements == null) {
            return;
        }

        this.evaluate(advancements, client.player);
    }

    private void evaluate(ClientAdvancements advancements, LocalPlayer player) {
        ClientAdvancementsAccessor accessor = (ClientAdvancementsAccessor) advancements;
        Map<AdvancementHolder, AdvancementProgress> progressMap = accessor.flowclient$getProgress();
        ClientAdvancements.Listener listener = accessor.flowclient$getListener();
        AdvancementEventContext context = AdvancementEventContext.get();

        Map<Identifier, AdvancementProgress> packetProgress = new HashMap<>();
        AdvancementTree tree = advancements.getTree();

        for (AdvancementNode node : tree.nodes()) {
            AdvancementHolder holder = node.holder();
            AdvancementProgress progress = progressMap.computeIfAbsent(holder, ignored -> new AdvancementProgress());
            if (progress.isDone()) {
                continue;
            }

            boolean changed = false;
            for (Map.Entry<String, Criterion<?>> entry : holder.value().criteria().entrySet()) {
                String criterionName = entry.getKey();
                if (this.isCriterionGranted(progress, criterionName)) {
                    continue;
                }
                if (!ClientCriterionMatcher.matches(player, entry.getValue(), context)) {
                    continue;
                }
                progress.grantProgress(criterionName);
                changed = true;
            }

            if (!changed) {
                continue;
            }

            if (listener != null) {
                listener.onUpdateAdvancementProgress(node, progress);
            }
            packetProgress.put(holder.id(), progress);
        }

        if (!packetProgress.isEmpty()) {
            advancements.update(new ClientboundUpdateAdvancementsPacket(
                    false,
                    Collections.emptyList(),
                    Set.of(),
                    packetProgress,
                    true
            ));
        }
    }

    private boolean isCriterionGranted(AdvancementProgress progress, String criterionName) {
        CriterionProgress criterionProgress = progress.getCriterion(criterionName);
        return criterionProgress != null && criterionProgress.isDone();
    }
}
