package com.platformer;

/**
 * Kelas ini adalah "pintu masuk" palsu untuk file JAR.
 * Ini diperlukan agar JavaFX bisa dimuat dengan benar saat 
 * dijalankan sebagai "fat-jar".
 */
public class Launcher {
    public static void main(String[] args) {
        // Panggil main() yang asli dari MainApp
        MainApp.main(args);
    }
}