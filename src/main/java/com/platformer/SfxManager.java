package com.platformer;

import javafx.scene.media.AudioClip;
import java.net.URL;

/**
 * Utility class untuk memuat Sound Effects (SFX).
 * Menggunakan AudioClip yang lebih ringan untuk suara pendek.
 */
public class SfxManager {

    /**
     * Memuat file AudioClip dari folder resources.
     * @param fileName Nama file (misal: "jump.mp3")
     * @return AudioClip yang siap dimainkan, atau null jika gagal
     */
    public static AudioClip load(String fileName) {
        try {
            // Path utama (standar jika 'assets' ada di root resources)
            String path = "/sfx/" + fileName;
            URL sfxUrl = SfxManager.class.getResource(path);
            
            if (sfxUrl != null) {
                AudioClip clip = new AudioClip(sfxUrl.toExternalForm());
                return clip;
            } else {
                System.err.println("SFX file not found: " + path);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error loading SFX: " + fileName);
            e.printStackTrace();
            return null;
        }
    }
}