package com.example.musicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageCacheManager {
    private static final String TAG = "ImageCacheManager";
    private static final String CACHE_DIR = "album_art_cache";
    private final Context context;
    private final File cacheDir;

    public ImageCacheManager(Context context) {
        this.context = context.getApplicationContext();

        // Create cache directory in app's external files directory
        File externalDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (externalDir != null) {
            cacheDir = new File(externalDir, CACHE_DIR);
        } else {
            // Fallback to internal storage if external is not available
            cacheDir = new File(context.getFilesDir(), CACHE_DIR);
        }

        if (!cacheDir.exists()) {
            boolean created = cacheDir.mkdirs();
            if (!created) {
                Log.e(TAG, "Failed to create cache directory");
            }
        }
    }

    /**
     * Download and save an image from a URL to local storage
     * @param imageUrl The URL of the image to download
     * @param callback Callback to be notified when the image is saved
     */
    public void cacheImageFromUrl(String imageUrl, final ImageCacheCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            if (callback != null) {
                callback.onError("Invalid image URL");
            }
            return;
        }

        // Generate a unique filename based on the URL
        final String filename = generateFilename(imageUrl);
        final File imageFile = new File(cacheDir, filename);

        // Check if the image is already cached
        if (imageFile.exists()) {
            if (callback != null) {
                callback.onSuccess(imageFile.getAbsolutePath());
            }
            return;
        }

        // Download the image using Glide
        Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // Save the bitmap to a file
                        boolean saved = saveBitmapToFile(resource, imageFile);
                        if (saved && callback != null) {
                            callback.onSuccess(imageFile.getAbsolutePath());
                        } else if (callback != null) {
                            callback.onError("Failed to save image");
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                        if (callback != null) {
                            callback.onError("Failed to download image");
                        }
                    }
                });
    }

    /**
     * Get the local path for a cached image
     * @param imageUrl The original URL of the image
     * @return The local file path if cached, null otherwise
     */
    public String getCachedImagePath(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        // Check if the URL is already a local file path
        if (imageUrl.startsWith("file://") || imageUrl.startsWith("/")) {
            File file = new File(imageUrl.startsWith("file://") ?
                    imageUrl.substring(7) : imageUrl);
            if (file.exists()) {
                return imageUrl;
            }
        }

        // Generate filename and check if it exists in cache
        String filename = generateFilename(imageUrl);
        File imageFile = new File(cacheDir, filename);

        if (imageFile.exists()) {
            return imageFile.getAbsolutePath();
        }

        return null;
    }

    /**
     * Load a bitmap from a local file path or URL
     * @param path The path or URL of the image
     * @return The loaded bitmap or null if it couldn't be loaded
     */
    public Bitmap loadBitmap(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // If it's a URL, try to get the cached version first
        if (path.startsWith("http")) {
            String cachedPath = getCachedImagePath(path);
            if (cachedPath != null) {
                path = cachedPath;
            } else {
                return null; // Not cached yet
            }
        }

        // Load the bitmap from the file
        try {
            return BitmapFactory.decodeFile(path);
        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap: " + e.getMessage());
            return null;
        }
    }

    /**
     * Clear all cached images
     * @return true if successful, false otherwise
     */
    public boolean clearCache() {
        if (cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        Log.e(TAG, "Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Generate a unique filename for an image URL
     * @param url The URL of the image
     * @return A unique filename based on the URL
     */
    private String generateFilename(String url) {
        try {
            // Create MD5 Hash of the URL
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(url.getBytes());
            byte[] messageDigest = digest.digest();

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String h = Integer.toHexString(0xFF & b);
                while (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }
            return hexString.toString() + ".jpg";
        } catch (NoSuchAlgorithmException e) {
            // Fallback if MD5 is not available
            return "img_" + url.hashCode() + ".jpg";
        }
    }

    /**
     * Save a bitmap to a file
     * @param bitmap The bitmap to save
     * @param file The file to save to
     * @return true if successful, false otherwise
     */
    private boolean saveBitmapToFile(Bitmap bitmap, File file) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap: " + e.getMessage());
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing file output stream: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Callback interface for image caching operations
     */
    public interface ImageCacheCallback {
        void onSuccess(String localPath);
        void onError(String message);
    }
}