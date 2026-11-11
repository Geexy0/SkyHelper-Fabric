package com.jelly.farmhelper.fabric.feature.module;

import com.jelly.farmhelper.fabric.FarmHelperFabric;
import com.jelly.farmhelper.fabric.config.FarmHelperConfig;
import com.jelly.farmhelper.fabric.feature.FeatureRuntimeState;
import com.jelly.farmhelper.fabric.feature.GuiRetryHelper;

public class AutoSellFeatureModule extends MacroExclusiveFeatureModule {
    private enum State {
        IDLE,
        OPEN_MARKET,
        WAIT_MARKET_SCREEN,
        SELL_ITEMS,
        CLOSE_SCREEN,
        FINISH
    }

    private static final int GUI_RETRY_LIMIT = 5;

    private State state = State.IDLE;
    private long stateSinceTick = 0L;
    private long inventoryFullSinceTick = -1L;
    private long lastSellActionTick = -1L;
    private int stateRetries;

    public AutoSellFeatureModule(boolean enabled) {
        super("auto_sell", "Auto Sell", enabled);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
        resetState();
    }

    @Override
    public void onTick(FeatureRuntimeState runtime) {
        FarmHelperConfig config = FarmHelperFabric.getConfigManager().getConfig();
        if (!config.enableAutoSell) {
            resetState();
            return;
        }
        if (runtime.jacobContestActive) {
            // Pause sell cycle during Jacob; resume when contest clears (inventory threshold re-arms).
            if (isActionRunning()) {
                endTimedAction("jacob contest pause");
                resetState();
            }
            inventoryFullSinceTick = -1L;
            return;
        }
        if (runtime.activeFailsafe.isPresent()) {
            inventoryFullSinceTick = -1L;
            return;
        }

        if (!isActionRunning()) {
            tryStart(runtime, config);
            return;
        }

        if (shouldEndAction(runtime.tickCount)) {
            endTimedAction("auto-sell timeout");
            lastSellActionTick = runtime.tickCount;
            resetState();
            return;
        }

        tickState(runtime, config);
    }

    private void tryStart(FeatureRuntimeState runtime, FarmHelperConfig config) {
        if (!runtime.macroToggled) {
            inventoryFullSinceTick = -1L;
            return;
        }

        int threshold = Math.max(1, Math.min(100, config.inventoryFullRatio));
        if (runtime.inventoryFillPercent < threshold) {
            inventoryFullSinceTick = -1L;
            return;
        }

        if (inventoryFullSinceTick < 0L) {
            inventoryFullSinceTick = runtime.tickCount;
            return;
        }

        long requiredTicks = secondsToTicks(config.inventoryFullTimeSeconds);
        if (runtime.tickCount - inventoryFullSinceTick < requiredTicks) {
            return;
        }

        long cooldownTicks = secondsToTicks(config.autoSellCommandCooldownSeconds);
        if (lastSellActionTick > 0 && runtime.tickCount - lastSellActionTick < cooldownTicks) {
            return;
        }

        long actionBudget = cooldownTicks + 220L + (long) GUI_RETRY_LIMIT * 40L;
        if (!beginTimedAction(runtime.tickCount, actionBudget, "inventory threshold reached")) {
            return;
        }

        setState(State.OPEN_MARKET, runtime.tickCount);
        inventoryFullSinceTick = -1L;
    }

    private void tickState(FeatureRuntimeState runtime, FarmHelperConfig config) {
        switch (state) {
            case OPEN_MARKET -> {
                if (ticksInState(runtime.tickCount) == 0) {
                    if (config.autoSellMarketTypeNpc && !"BARN".equalsIgnoreCase(runtime.location)) {
                        queueCommand("/tptoplot barn", runtime.tickCount);
                    }
                    if (config.autoSellSacks) {
                        queueCommand("/sacks", runtime.tickCount);
                    }
                    queueCommand(config.autoSellMarketTypeNpc ? "/trades" : "/bz", runtime.tickCount);
                }
                if (ticksInState(runtime.tickCount) >= 20L) {
                    setState(State.WAIT_MARKET_SCREEN, runtime.tickCount);
                }
            }
            case WAIT_MARKET_SCREEN -> {
                if (ticksInState(runtime.tickCount) == 0) {
                    queueWaitForScreen(config.autoSellMarketTypeNpc ? "trades" : "bazaar", 160L, GUI_RETRY_LIMIT, runtime.tickCount);
                }
                if (ticksInState(runtime.tickCount) >= 50L) {
                    String title = runtime.screenTitle == null ? "" : runtime.screenTitle;
                    String lower = title.toLowerCase(java.util.Locale.ROOT);
                    boolean marketOpen = !title.isBlank()
                            && (lower.contains("trade")
                            || lower.contains("bazaar")
                            || lower.contains("sell")
                            || lower.contains("market"));
                    int retryLimit = GuiRetryHelper.clampLimit(GUI_RETRY_LIMIT, 2);
                    if (!marketOpen && GuiRetryHelper.canRetry(stateRetries, retryLimit)) {
                        stateRetries++;
                        queueCloseScreen(runtime.tickCount);
                        // Backoff is applied via OPEN_MARKET's fixed wait before WAIT_MARKET_SCREEN.
                        setState(State.OPEN_MARKET, runtime.tickCount);
                        return;
                    }
                    setState(State.SELL_ITEMS, runtime.tickCount);
                }
            }
            case SELL_ITEMS -> {
                if (ticksInState(runtime.tickCount) == 0) {
                    queueClickSlotMatching("name:sell inventory;lore:items", 80L, GUI_RETRY_LIMIT, runtime.tickCount);
                    queueClickSlotMatching("name:insta-sell;lore:sell", 80L, GUI_RETRY_LIMIT, runtime.tickCount);
                    queueClickSlotMatching("name:sell", 60L, Math.max(3, GUI_RETRY_LIMIT - 1), runtime.tickCount);
                    for (String custom : config.autoSellCustomItems.split(",")) {
                        String normalized = custom.trim();
                        if (!normalized.isEmpty()) {
                            queueClickSlotMatching("name:" + normalized, 60L, 3, runtime.tickCount);
                        }
                    }
                }
                if (ticksInState(runtime.tickCount) >= 36L) {
                    setState(State.CLOSE_SCREEN, runtime.tickCount);
                }
            }
            case CLOSE_SCREEN -> {
                if (ticksInState(runtime.tickCount) == 0) {
                    queueCloseScreen(runtime.tickCount);
                }
                if (ticksInState(runtime.tickCount) >= 10L) {
                    setState(State.FINISH, runtime.tickCount);
                }
            }
            case FINISH -> {
                endTimedAction("auto-sell complete");
                lastSellActionTick = runtime.tickCount;
                resetState();
            }
            case IDLE -> {
            }
        }
    }

    private long ticksInState(long nowTick) {
        return Math.max(0L, nowTick - stateSinceTick);
    }

    private void setState(State next, long nowTick) {
        state = next;
        stateSinceTick = nowTick;
    }

    private void resetState() {
        state = State.IDLE;
        stateSinceTick = 0L;
        inventoryFullSinceTick = -1L;
        stateRetries = 0;
    }
}
