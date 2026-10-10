package com.asdflj.wtct.loader;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.world.World;

import com.asdflj.wtct.Wtct;
import com.asdflj.wtct.network.wrapper.WtctNetworkWrapper;

import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.relauncher.Side;

public class ChannelLoader implements Runnable {

    public static final ChannelLoader INSTANCE = new ChannelLoader();

    public static Set<Class<?>> getClasses(String packageName) throws IOException {
        ClassLoader classLoader = Thread.currentThread()
            .getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        Set<Class<?>> classes = new LinkedHashSet<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("jar".equals(resource.getProtocol())) {
                processJarFile(classes, resource, packageName);
            } else if ("file".equals(resource.getProtocol())) {
                // Dev environment: the classes are plain .class files under build/classes/java/main,
                // not a jar. Without this branch nothing at all was registered while running from the
                // IDE or `./gradlew runClient`, which silently killed every packet this mod sends.
                processDirectory(classes, resource, packageName);
            }
        }
        return classes;
    }

    /** The directory counterpart of {@link #processJarFile}, for a dev-environment classpath. */
    private static void processDirectory(Set<Class<?>> classes, URL directoryUrl, String packageName) {
        java.io.File dir;
        try {
            dir = new java.io.File(directoryUrl.toURI());
        } catch (final Exception e) {
            return;
        }
        collectFromDirectory(classes, dir, packageName);
    }

    private static void collectFromDirectory(final Set<Class<?>> classes, final java.io.File dir,
        final String packageName) {
        final java.io.File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (final java.io.File child : children) {
            if (child.isDirectory()) {
                collectFromDirectory(classes, child, packageName + "." + child.getName());
                continue;
            }
            final String fileName = child.getName();
            if (!fileName.endsWith(".class")) {
                continue;
            }
            final String className = packageName + '.' + fileName.substring(0, fileName.length() - ".class".length());
            try {
                classes.add(Class.forName(className));
            } catch (ClassNotFoundException | NoClassDefFoundError ignored) {

            }
        }
    }

    private static void processJarFile(Set<Class<?>> classes, URL jarFileUrl, String packageName) {
        JarFile jarFile = null;
        try {
            JarURLConnection jarURLConnection = (JarURLConnection) jarFileUrl.openConnection();
            if (jarURLConnection != null) {
                jarFile = jarURLConnection.getJarFile();
                if (jarFile != null) {
                    Enumeration<JarEntry> jarEntries = jarFile.entries();
                    while (jarEntries.hasMoreElements()) {
                        JarEntry jarEntry = jarEntries.nextElement();
                        String jarEntryName = jarEntry.getName();
                        if (jarEntryName.startsWith(packageName.replace('.', '/') + '/')
                            && jarEntryName.endsWith(".class")) {
                            String className = jarEntryName.substring(0, jarEntryName.lastIndexOf("."))
                                .replaceAll("/", ".");
                            try {
                                classes.add(Class.forName(className));
                            } catch (ClassNotFoundException ignored) {

                            }
                        }
                    }
                }
            }
        } catch (IOException ignored) {} finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException ignored) {}
            }
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void run() {
        int id = 0;
        int registered = 0;
        WtctNetworkWrapper netHandler = Wtct.proxy.netHandler;
        Set<Class<?>> result;
        try {
            result = getClasses("com.asdflj.wtct.network");
        } catch (Exception e) {
            System.err.println("[wtct] could not scan the packet package; no packet will be registered");
            e.printStackTrace();
            return;
        }
        // Register each packet on its own: one unloadable class must not take the whole channel with
        // it, and the discriminator has to stay tied to the packet rather than to the loop position,
        // so a failure in the middle cannot shift every later packet onto the wrong id.
        for (Class<?> aClass : result) {
            if (!aClass.getName()
                .endsWith("Handler")) {
                continue;
            }
            final String packetName = aClass.getName()
                .replace("$Handler", "");
            try {
                final Class c = Class.forName(packetName);
                final IMessageHandler cls = (IMessageHandler) aClass.getConstructor()
                    .newInstance();
                final int discriminator = discriminator(packetName);
                netHandler.registerMessage(cls, c, discriminator, sideOf(c));
                registered++;
            } catch (Throwable t) {
                System.err.println("[wtct] failed to register packet " + packetName);
                t.printStackTrace();
            }
        }
        System.out.println("[wtct] registered " + registered + " packet(s) on " + Wtct.MODID);
    }

    /**
     * A packet's discriminator, derived from its own name.
     *
     * <p>
     * This used to be a running counter over a set whose iteration order came from the classpath, so
     * a packet could land on a different id on the client than on the server - and then every message
     * of that type was decoded as whatever the other side had put there. The mapping is now a pure
     * function of the name, which is the same on both sides because it is the same jar.
     */
    private static final java.util.List<String> PACKET_ORDER = java.util.Arrays.asList(
        "CPacketCraftRequest",
        "CPacketFillCraftingSlot",
        "CPacketFindCellItem",
        "CPacketFluidCellMark",
        "CPacketFluidUpdate",
        "CPacketInventoryAction",
        "CPacketInventoryActionExtend",
        "CPacketNEIRecipe",
        "CPacketNetworkCraftingItems",
        "CPacketOpenTerminal",
        "CPacketPatternNameSet",
        "CPacketPatternValueSet",
        "CPacketRenamer",
        "CPacketSwitchGuis",
        "CPacketTerminalBtns",
        "CPacketTransferRecipe",
        "CPacketTypeFilter",
        "CPacketTypeFilterRequest",
        "SPacketAutoFillPending",
        "SPacketCraftableKeys",
        "SPacketCraftingDebugCardUpdate",
        "SPacketCraftingStateUpdate",
        "SPacketFindCellItem",
        "SPacketMEFluidInvUpdate",
        "SPacketMEItemInvUpdate",
        "SPacketSetItemAmount",
        "SPacketSetItemName",
        "SPacketStringUpdate",
        "SPacketSwitchBack",
        "SPacketTypeFilter",
        // Appended rather than inserted alphabetically: the discriminator is a packet's index in this
        // table, so a new entry at the end leaves every existing packet on the id it already had.
        "CPacketInventoryRequest");

    private static int discriminator(final String packetName) {
        final String simple = packetName.substring(packetName.lastIndexOf('.') + 1);
        final int index = PACKET_ORDER.indexOf(simple);
        if (index >= 0) {
            return index;
        }
        // A packet that is not in the table still gets a stable id of its own, so adding one cannot
        // silently move the others.
        return 100 + Math.abs(simple.hashCode() % 100);
    }

    /** Which side handles a packet: this mod's client-to-server packets are the "CPacket" ones. */
    private static Side sideOf(final Class<?> packet) {
        return packet.getSimpleName()
            .startsWith("C") ? Side.SERVER : Side.CLIENT;
    }

    public static void sendPacketToAllPlayers(Packet packet, World world) {
        for (Object player : world.playerEntities) {
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).playerNetServerHandler.sendPacket(packet);
            }
        }
    }
}
