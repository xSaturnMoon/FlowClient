package com.flowclient.mods.jei;

import mezz.jei.api.IModPlugin;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class JeiRuntimeController {
    private static final Logger LOGGER = LoggerFactory.getLogger("flowclient-jei");
    private static final String INTERNAL = "mezz.jei.common.Internal";
    private static final String FABRIC_GUI_PLUGIN = "mezz.jei.fabric.plugins.fabric.FabricGuiPlugin";

    private static boolean savedOverlayEnabled = true;
    private static boolean savedBookmarkEnabled = true;
    private static boolean savedCheatEnabled = false;
    private static boolean savedEditModeEnabled = false;
    private static boolean jeiCurrentlyForcedOff;
    private static Boolean lastLoggedEnabled;

    private JeiRuntimeController() {
    }

    public static boolean isJeiModLoaded() {
        return FabricLoader.getInstance().isModLoaded("jei");
    }

    public static boolean isJeiInternalPresent() {
        try {
            Class.forName(INTERNAL, false, JeiRuntimeController.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    public static void logStartupDiagnostics() {
        LOGGER.info("[flowclient-jei] startup diagnostics: jeiModLoaded={}, jeiInternalPresent={}, flowEnabled={}",
                isJeiModLoaded(),
                isJeiInternalPresent(),
                JeiMod.isEnabled());
    }

    public static void syncFromFlowConfig() {
        applyEnabled(JeiMod.isEnabled());
    }

    public static void enforce() {
        if (JeiMod.isEnabled()) {
            return;
        }
        applyEnabled(false);
    }

    public static void applyEnabled(boolean enabled) {
        if (!isJeiInternalPresent()) {
            LOGGER.warn("[flowclient-jei] applyEnabled({}) skipped: {} not on classpath (jeiModLoaded={})",
                    enabled, INTERNAL, isJeiModLoaded());
            return;
        }

        try {
            Object toggleState = invokeStatic(INTERNAL, "getClientToggleState");
            if (!enabled) {
                if (!jeiCurrentlyForcedOff) {
                    captureToggleState(toggleState);
                    jeiCurrentlyForcedOff = true;
                }
                disableJei(toggleState);
                logAppliedState("disabled", enabled, toggleState);
            } else {
                jeiCurrentlyForcedOff = false;
                restoreAndEnable(toggleState);
                logAppliedState("enabled", enabled, toggleState);
            }
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("[flowclient-jei] Could not apply JEI enabled={}: {}", enabled, ex.toString());
        }
    }

    private static void logAppliedState(String label, boolean requestedEnabled, Object toggleState)
            throws ReflectiveOperationException {
        if (lastLoggedEnabled != null && lastLoggedEnabled == requestedEnabled) {
            return;
        }
        lastLoggedEnabled = requestedEnabled;
        LOGGER.info("[flowclient-jei] applied JEI {} state (overlay={}, bookmark={}, cheat={}, edit={})",
                label,
                readToggleField(toggleState, "overlayEnabled"),
                invoke(toggleState, "isBookmarkOverlayEnabled"),
                invoke(toggleState, "isCheatItemsEnabled"),
                readToggleField(toggleState, "editModeEnabled"));
    }

    private static void captureToggleState(Object toggleState) throws ReflectiveOperationException {
        savedOverlayEnabled = (boolean) invoke(toggleState, "isOverlayEnabled");
        savedBookmarkEnabled = (boolean) invoke(toggleState, "isBookmarkOverlayEnabled");
        savedCheatEnabled = (boolean) invoke(toggleState, "isCheatItemsEnabled");
        savedEditModeEnabled = (boolean) invoke(toggleState, "isEditModeEnabled");
        LOGGER.info("[flowclient-jei] captured JEI toggle state before disable: overlay={}, bookmark={}, cheat={}, edit={}",
                savedOverlayEnabled, savedBookmarkEnabled, savedCheatEnabled, savedEditModeEnabled);
    }

    private static void disableJei(Object toggleState) throws ReflectiveOperationException {
        setToggleField(toggleState, "overlayEnabled", false);
        invoke(toggleState, "setBookmarkEnabled", false);
        invoke(toggleState, "setCheatItemsEnabled", false);
        setToggleField(toggleState, "editModeEnabled", false);
        closeRecipesGui();
    }

    private static void restoreAndEnable(Object toggleState) throws ReflectiveOperationException {
        setToggleField(toggleState, "overlayEnabled", savedOverlayEnabled);
        invoke(toggleState, "setBookmarkEnabled", savedBookmarkEnabled);
        invoke(toggleState, "setCheatItemsEnabled", savedCheatEnabled);
        setToggleField(toggleState, "editModeEnabled", savedEditModeEnabled);
        refreshGuiState();
    }

    private static void refreshGuiState() throws ReflectiveOperationException {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.gui.screen();
        if (screen == null) {
            LOGGER.debug("[flowclient-jei] refreshGuiState skipped: no current screen");
            return;
        }

        Optional<?> runtime = (Optional<?>) invokeStatic(INTERNAL, "getOptionalJeiRuntime");
        if (runtime.isEmpty()) {
            LOGGER.debug("[flowclient-jei] refreshGuiState skipped: JEI runtime not available yet");
            return;
        }

        Object jeiRuntime = runtime.get();
        refreshOverlayScreenProperties(invoke(jeiRuntime, "getIngredientListOverlay"), screen);
        refreshOverlayScreenProperties(invoke(jeiRuntime, "getBookmarkOverlay"), screen);

        Object guiEventHandler = findGuiEventHandler();
        if (guiEventHandler != null) {
            invoke(guiEventHandler, "onGuiInit", screen);
            invoke(guiEventHandler, "onGuiOpen", screen);
        }

        Object clientInputHandler = findClientInputHandler();
        if (clientInputHandler != null) {
            invoke(clientInputHandler, "onInitGui");
        }
    }

    private static void refreshOverlayScreenProperties(Object overlay, Screen screen) throws ReflectiveOperationException {
        if (overlay == null) {
            return;
        }

        Object updater = invoke(overlay, "getScreenPropertiesUpdater");
        invoke(updater, "updateScreen", screen);
        invoke(updater, "forceUpdate");
    }

    private static Object findGuiEventHandler() {
        Object eventRegistration = findEventRegistration();
        if (eventRegistration == null) {
            return null;
        }

        try {
            Field field = eventRegistration.getClass().getDeclaredField("guiEventHandler");
            field.setAccessible(true);
            return field.get(eventRegistration);
        } catch (ReflectiveOperationException ex) {
            LOGGER.debug("[flowclient-jei] Could not access JEI GuiEventHandler: {}", ex.toString());
            return null;
        }
    }

    private static Object findClientInputHandler() {
        Object eventRegistration = findEventRegistration();
        if (eventRegistration == null) {
            return null;
        }

        try {
            Field field = eventRegistration.getClass().getDeclaredField("clientInputHandler");
            field.setAccessible(true);
            return field.get(eventRegistration);
        } catch (ReflectiveOperationException ex) {
            LOGGER.debug("[flowclient-jei] Could not access JEI ClientInputHandler: {}", ex.toString());
            return null;
        }
    }

    private static Object findEventRegistration() {
        for (IModPlugin plugin : FabricLoader.getInstance().getEntrypoints("jei", IModPlugin.class)) {
            if (!FABRIC_GUI_PLUGIN.equals(plugin.getClass().getName())) {
                continue;
            }

            try {
                Field field = plugin.getClass().getDeclaredField("eventRegistration");
                field.setAccessible(true);
                return field.get(plugin);
            } catch (ReflectiveOperationException ex) {
                LOGGER.debug("[flowclient-jei] Could not access JEI EventRegistration: {}", ex.toString());
                return null;
            }
        }
        return null;
    }

    private static void closeRecipesGui() throws ReflectiveOperationException {
        Optional<?> runtime = (Optional<?>) invokeStatic(INTERNAL, "getOptionalJeiRuntime");
        if (runtime.isEmpty()) {
            return;
        }

        Object recipesGui = invoke(runtime.get(), "getRecipesGui");
        if (!(boolean) invoke(recipesGui, "isOpen")) {
            return;
        }

        invoke(recipesGui, "onClose");
    }

    private static boolean readToggleField(Object toggleState, String fieldName) throws ReflectiveOperationException {
        Field field = toggleState.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(toggleState);
    }

    private static void setToggleField(Object toggleState, String fieldName, boolean value)
            throws ReflectiveOperationException {
        Field field = toggleState.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(toggleState, value);
    }

    private static Object invokeStatic(String className, String methodName, Object... args)
            throws ReflectiveOperationException {
        return invoke(null, Class.forName(className), methodName, args);
    }

    private static Object invoke(Object target, String methodName, Object... args)
            throws ReflectiveOperationException {
        Class<?> clazz = target != null ? target.getClass() : null;
        if (clazz == null) {
            throw new ReflectiveOperationException("Missing target for " + methodName);
        }
        return invoke(target, clazz, methodName, args);
    }

    private static Object invoke(Object target, Class<?> clazz, String methodName, Object... args)
            throws ReflectiveOperationException {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Boolean) {
                parameterTypes[i] = boolean.class;
            } else if (arg instanceof Screen) {
                parameterTypes[i] = Screen.class;
            } else if (arg instanceof Minecraft) {
                parameterTypes[i] = Minecraft.class;
            } else {
                parameterTypes[i] = arg.getClass();
            }
        }

        Method method = clazz.getMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
