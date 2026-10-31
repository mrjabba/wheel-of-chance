package com.cedarparkgames.wheelofchance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import java.util.Random;

public class DesktopLauncher {
    public static void main(String[] args) throws Exception {
        if (relaunchedOnFirstThread()) return;
        Wheel wheel = new Wheel(new Random(), Wheel.parse(System.getenv("PREDETERMINED_VALUES")));
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Wheel of Chance");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        new Lwjgl3Application(new WheelGame(wheel), config);
    }

    /** macOS needs GLFW on the first thread; relaunch with -XstartOnFirstThread (same trick as cartoon-combatant). */
    private static boolean relaunchedOnFirstThread() throws Exception {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) return false;
        if ("1".equals(System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + ProcessHandle.current().pid()))) return false;
        String java = ProcessHandle.current().info().command().orElse("java");
        new ProcessBuilder(java, "-XstartOnFirstThread", "-cp", System.getProperty("java.class.path"),
                DesktopLauncher.class.getName())
                .inheritIO().start().waitFor();
        return true;
    }
}
