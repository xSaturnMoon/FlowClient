package com.flowclient.mods.voicechat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Enables or disables Simple Voice Chat at runtime without restarting Minecraft.
 * When disabled, tears down microphone capture, UDP voice sockets, and background audio threads.
 */
public final class VoiceChatRuntimeController {
    private static final Logger LOGGER = LoggerFactory.getLogger("flowclient-voicechat");

    private static final String VOICECHAT_MOD_ID = "voicechat";
    private static final String CLIENT_MANAGER = "de.maxhenkel.voicechat.voice.client.ClientManager";
    private static final String CLIENT_VOICECHAT = "de.maxhenkel.voicechat.voice.client.ClientVoicechat";
    private static final String VOICECHAT = "de.maxhenkel.voicechat.Voicechat";
    private static final String REQUEST_SECRET_PACKET = "de.maxhenkel.voicechat.net.RequestSecretPacket";
    private static final String PACKET = "de.maxhenkel.voicechat.net.Packet";
    private static final String CLIENT_SERVER_NET_MANAGER = "de.maxhenkel.voicechat.net.ClientServerNetManager";
    private static final String VOICECHAT_GUI_PREFIX = "de.maxhenkel.voicechat.gui";

    private static Boolean lastLoggedEnabled;
    private static int enforceLogCounter;
    private static boolean voiceChatCurrentlyTornDown;

    private VoiceChatRuntimeController() {
    }

    public static boolean isVoiceChatLoaded() {
        return FabricLoader.getInstance().isModLoaded(VOICECHAT_MOD_ID);
    }

    public static boolean isVoiceChatInternalPresent() {
        try {
            Class.forName(CLIENT_MANAGER, false, VoiceChatRuntimeController.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    public static void logStartupDiagnostics() {
        LOGGER.info("[flowclient-voicechat] startup diagnostics: voiceChatModLoaded={}, voiceChatInternalPresent={}, flowEnabled={}",
                isVoiceChatLoaded(),
                isVoiceChatInternalPresent(),
                VoiceChatMod.isEnabled());
        logNetManagerMethodDiagnostics();
    }

    private static void logNetManagerMethodDiagnostics() {
        if (!isVoiceChatInternalPresent()) {
            return;
        }

        try {
            Class<?> netManagerClass = Class.forName(CLIENT_SERVER_NET_MANAGER);
            LOGGER.info("[flowclient-voicechat] {} declared methods:", CLIENT_SERVER_NET_MANAGER);
            for (Method method : netManagerClass.getDeclaredMethods()) {
                LOGGER.info("[flowclient-voicechat]   {}", formatMethod(method));
            }

            Class<?> packetClass = Class.forName(REQUEST_SECRET_PACKET);
            LOGGER.info("[flowclient-voicechat] {} exists; constructors:", REQUEST_SECRET_PACKET);
            for (Constructor<?> constructor : packetClass.getConstructors()) {
                LOGGER.info("[flowclient-voicechat]   {}", formatConstructor(constructor));
            }
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("[flowclient-voicechat] could not introspect SVC net classes: {}", ex.toString());
        }
    }

    private static String formatMethod(Method method) {
        StringBuilder builder = new StringBuilder();
        int modifiers = method.getModifiers();
        if (Modifier.isPublic(modifiers)) {
            builder.append("public ");
        } else if (Modifier.isProtected(modifiers)) {
            builder.append("protected ");
        } else if (!Modifier.isPrivate(modifiers)) {
            builder.append("package-private ");
        }
        if (Modifier.isStatic(modifiers)) {
            builder.append("static ");
        }
        builder.append(method.getReturnType().getName())
                .append(' ')
                .append(method.getName())
                .append('(');
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(parameterTypes[i].getName());
        }
        builder.append(')');
        return builder.toString();
    }

    private static String formatConstructor(Constructor<?> constructor) {
        StringBuilder builder = new StringBuilder("public ")
                .append(constructor.getDeclaringClass().getName())
                .append('(');
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(parameterTypes[i].getName());
        }
        builder.append(')');
        return builder.toString();
    }

    public static void syncFromFlowConfig() {
        applyEnabled(VoiceChatMod.isEnabled());
    }

    public static void enforce() {
        if (VoiceChatMod.isEnabled()) {
            return;
        }

        closeVoiceChatScreens();

        if (!hasActiveVoiceResources()) {
            int count = ++enforceLogCounter;
            if (count <= 3 || count % 200 == 0) {
                logRuntimeState("enforce-idle(#" + count + ")");
            }
            return;
        }

        LOGGER.warn("[flowclient-voicechat] enforce: voice resources still active while disabled, tearing down again");
        try {
            tearDownVoiceChat();
            voiceChatCurrentlyTornDown = true;
        } catch (ReflectiveOperationException ex) {
            LOGGER.error("[flowclient-voicechat] enforce teardown failed: {}", ex.toString());
        }
    }

    public static void applyEnabled(boolean enabled) {
        if (!isVoiceChatInternalPresent()) {
            if (enabled) {
                LOGGER.warn("[flowclient-voicechat] applyEnabled(true) skipped: {} not on classpath (modLoaded={})",
                        CLIENT_MANAGER, isVoiceChatLoaded());
            }
            return;
        }

        try {
            if (!enabled) {
                if (voiceChatCurrentlyTornDown && !hasActiveVoiceResources()) {
                    logAppliedState("disabled", enabled);
                    return;
                }
                logRuntimeState("before-disable");
                tearDownVoiceChat();
                persistSvcMutedOnDisable();
                voiceChatCurrentlyTornDown = true;
                logRuntimeState("after-disable");
                logAppliedState("disabled", enabled);
            } else {
                voiceChatCurrentlyTornDown = false;
                logRuntimeState("before-enable");
                reconnectVoiceChat();
                logRuntimeState("after-enable");
                logAppliedState("enabled", enabled);
            }
        } catch (ReflectiveOperationException ex) {
            LOGGER.error("[flowclient-voicechat] Could not apply voice chat enabled={}: {}", enabled, ex.toString(), ex);
            throw new IllegalStateException(ex);
        }
    }

    public static void shutdownOnClientStop() {
        if (!isVoiceChatInternalPresent()) {
            return;
        }

        try {
            tearDownVoiceChat();
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("[flowclient-voicechat] shutdown teardown failed: {}", ex.toString());
        }
    }

    private static void tearDownVoiceChat() throws ReflectiveOperationException {
        Object client = invokeStatic(CLIENT_MANAGER, "getClient");
        if (client == null) {
            LOGGER.info("[flowclient-voicechat] tearDown: no active ClientVoicechat instance");
            return;
        }

        Object connection = invoke(client, "getConnection");
        if (connection != null) {
            boolean connected = (boolean) invoke(connection, "isConnected");
            LOGGER.info("[flowclient-voicechat] tearDown: voice UDP connection active before close (connected={})",
                    connected);
            try {
                invoke(connection, "close");
                LOGGER.info("[flowclient-voicechat] voice UDP connection closed");
            } catch (ReflectiveOperationException ex) {
                LOGGER.error("[flowclient-voicechat] failed to close voice UDP connection: {}", ex.toString());
            }
        } else {
            LOGGER.info("[flowclient-voicechat] tearDown: no voice UDP connection reference");
        }

        Object micThread = invoke(client, "getMicThread");
        if (micThread != null) {
            logMicrophoneState("before-closeMicThread", micThread);
        } else {
            LOGGER.info("[flowclient-voicechat] tearDown: no MicThread reference before close");
        }

        try {
            invoke(client, "closeMicThread");
            LOGGER.info("[flowclient-voicechat] closeMicThread() invoked");
        } catch (ReflectiveOperationException ex) {
            LOGGER.error("[flowclient-voicechat] closeMicThread() failed: {}", ex.toString());
        }

        micThread = invoke(client, "getMicThread");
        if (micThread != null) {
            logMicrophoneState("after-closeMicThread", micThread);
        }

        try {
            invoke(client, "close");
            LOGGER.info("[flowclient-voicechat] ClientVoicechat.close() invoked");
        } catch (ReflectiveOperationException ex) {
            LOGGER.error("[flowclient-voicechat] ClientVoicechat.close() failed: {}", ex.toString());
        }

        verifyMicrophoneClosed(client);
        verifyConnectionClosed(client);

        Object manager = invokeStatic(CLIENT_MANAGER, "instance");
        setField(manager, "client", null);
        LOGGER.info("[flowclient-voicechat] ClientManager.client cleared");
    }

    private static void reconnectVoiceChat() throws ReflectiveOperationException {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            LOGGER.info("[flowclient-voicechat] reconnect skipped: not in a world");
            return;
        }

        Object manager = invokeStatic(CLIENT_MANAGER, "instance");
        Object existingClient = invokeStatic(CLIENT_MANAGER, "getClient");
        if (existingClient != null) {
            LOGGER.info("[flowclient-voicechat] reconnect: closing existing client before re-requesting secret");
            invoke(existingClient, "close");
            setField(manager, "client", null);
        }

        int compatibilityVersion = getStaticIntField(VOICECHAT, "COMPATIBILITY_VERSION");
        Constructor<?> packetConstructor = Class.forName(REQUEST_SECRET_PACKET).getConstructor(int.class);
        Object packet = packetConstructor.newInstance(compatibilityVersion);
        sendPacketToServer(packet);
        LOGGER.info("[flowclient-voicechat] RequestSecretPacket sent via sendToServer(Packet) (compat={})",
                compatibilityVersion);

        Object newClient = Class.forName(CLIENT_VOICECHAT).getConstructor().newInstance();
        setField(manager, "client", newClient);
        LOGGER.info("[flowclient-voicechat] new ClientVoicechat created, awaiting server secret");

        Object stateManager = invokeStatic(CLIENT_MANAGER, "getPlayerStateManager");
        boolean muted = (boolean) invoke(stateManager, "isMuted");
        LOGGER.info("[flowclient-voicechat] SVC enabled: mic reconnected, muted state preserved (muted={})", muted);
    }

    private static void persistSvcMutedOnDisable() throws ReflectiveOperationException {
        Object stateManager = invokeStatic(CLIENT_MANAGER, "getPlayerStateManager");
        invoke(stateManager, "setMuted", true);
        LOGGER.info("[flowclient-voicechat] SVC disabled: mic muted flag set to true (persisted)");
    }

    private static void verifyMicrophoneClosed(Object client) throws ReflectiveOperationException {
        Object micThread = invoke(client, "getMicThread");
        if (micThread == null) {
            LOGGER.info("[flowclient-voicechat] microphone stream closed (MicThread absent)");
            return;
        }

        boolean threadClosed = (boolean) invoke(micThread, "isClosed");
        Object mic = getField(micThread, "mic");
        if (mic == null) {
            LOGGER.info("[flowclient-voicechat] microphone stream closed (mic reference null, threadClosed={})",
                    threadClosed);
            return;
        }

        boolean micOpen = (boolean) invoke(mic, "isOpen");
        if (micOpen) {
            LOGGER.error("[flowclient-voicechat] microphone still open after teardown (threadClosed={}), forcing close",
                    threadClosed);
            try {
                invoke(mic, "close");
                boolean stillOpen = (boolean) invoke(mic, "isOpen");
                if (stillOpen) {
                    LOGGER.error("[flowclient-voicechat] forced microphone close failed: isOpen still true");
                } else {
                    LOGGER.info("[flowclient-voicechat] microphone stream closed (forced)");
                }
            } catch (ReflectiveOperationException ex) {
                LOGGER.error("[flowclient-voicechat] forced microphone close failed: {}", ex.toString());
            }
        } else {
            LOGGER.info("[flowclient-voicechat] microphone stream closed (threadClosed={})", threadClosed);
        }
    }

    private static void verifyConnectionClosed(Object client) throws ReflectiveOperationException {
        Object connection = invoke(client, "getConnection");
        if (connection == null) {
            LOGGER.info("[flowclient-voicechat] voice UDP socket closed (connection null)");
            return;
        }

        boolean connected = (boolean) invoke(connection, "isConnected");
        if (connected) {
            LOGGER.error("[flowclient-voicechat] voice UDP connection still active after teardown, forcing close");
            try {
                invoke(connection, "close");
                boolean stillConnected = (boolean) invoke(connection, "isConnected");
                if (stillConnected) {
                    LOGGER.error("[flowclient-voicechat] forced voice UDP close failed: still connected");
                } else {
                    LOGGER.info("[flowclient-voicechat] voice UDP connection closed (forced)");
                }
            } catch (ReflectiveOperationException ex) {
                LOGGER.error("[flowclient-voicechat] forced voice UDP close failed: {}", ex.toString());
            }
        } else {
            LOGGER.info("[flowclient-voicechat] voice UDP connection closed");
        }
    }

    private static void logMicrophoneState(String phase, Object micThread) throws ReflectiveOperationException {
        boolean threadClosed = (boolean) invoke(micThread, "isClosed");
        Object mic = getField(micThread, "mic");
        boolean micOpen = mic != null && (boolean) invoke(mic, "isOpen");
        LOGGER.info("[flowclient-voicechat] {}: micThreadClosed={}, micOpen={}", phase, threadClosed, micOpen);
    }

    private static boolean hasActiveVoiceResources() {
        if (!isVoiceChatInternalPresent()) {
            return false;
        }

        try {
            Object client = invokeStatic(CLIENT_MANAGER, "getClient");
            if (client == null) {
                return false;
            }

            Object connection = invoke(client, "getConnection");
            if (connection != null && (boolean) invoke(connection, "isConnected")) {
                return true;
            }

            Object micThread = invoke(client, "getMicThread");
            if (micThread == null) {
                return false;
            }

            if (!(boolean) invoke(micThread, "isClosed")) {
                return true;
            }

            Object mic = getField(micThread, "mic");
            return mic != null && (boolean) invoke(mic, "isOpen");
        } catch (ReflectiveOperationException ex) {
            LOGGER.debug("[flowclient-voicechat] could not probe active voice resources: {}", ex.toString());
            return false;
        }
    }

    private static void logRuntimeState(String phase) {
        if (!isVoiceChatInternalPresent()) {
            return;
        }

        try {
            Object client = invokeStatic(CLIENT_MANAGER, "getClient");
            if (client == null) {
                LOGGER.info("[flowclient-voicechat] {}: client=null", phase);
                return;
            }

            Object connection = invoke(client, "getConnection");
            boolean connected = connection != null && (boolean) invoke(connection, "isConnected");
            Object micThread = invoke(client, "getMicThread");
            boolean threadClosed = micThread == null || (boolean) invoke(micThread, "isClosed");
            Object mic = micThread != null ? getField(micThread, "mic") : null;
            boolean micOpen = mic != null && (boolean) invoke(mic, "isOpen");

            LOGGER.info("[flowclient-voicechat] {}: udpConnected={}, micThreadClosed={}, micOpen={}",
                    phase, connected, threadClosed, micOpen);
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("[flowclient-voicechat] {}: could not read runtime state: {}", phase, ex.toString());
        }
    }

    private static void logAppliedState(String label, boolean requestedEnabled) {
        if (lastLoggedEnabled != null && lastLoggedEnabled == requestedEnabled) {
            return;
        }
        lastLoggedEnabled = requestedEnabled;
        LOGGER.info("[flowclient-voicechat] applied voice chat {} state", label);
    }

    private static void closeVoiceChatScreens() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.gui.screen();
        if (screen == null) {
            return;
        }

        if (screen.getClass().getName().startsWith(VOICECHAT_GUI_PREFIX)) {
            minecraft.gui.setScreen(null);
            LOGGER.debug("[flowclient-voicechat] closed open SVC screen: {}", screen.getClass().getName());
        }
    }

    private static int getStaticIntField(String className, String fieldName) throws ReflectiveOperationException {
        Field field = Class.forName(className).getField(fieldName);
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static Object getField(Object target, String fieldName) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static void sendPacketToServer(Object packet) throws ReflectiveOperationException {
        Class<?> netManagerClass = Class.forName(CLIENT_SERVER_NET_MANAGER);
        Class<?> packetBaseClass = Class.forName(PACKET);
        Method method = netManagerClass.getMethod("sendToServer", packetBaseClass);
        method.setAccessible(true);
        method.invoke(null, packet);
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
            } else if (arg instanceof Integer) {
                parameterTypes[i] = int.class;
            } else {
                parameterTypes[i] = arg.getClass();
            }
        }

        Method method = clazz.getMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
