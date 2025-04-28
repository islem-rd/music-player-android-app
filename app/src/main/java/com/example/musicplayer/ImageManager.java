package com.example.musicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageManager {
    private static final String TAG = "ImageManager";
    private static final String CACHE_DIR = "album_art_cache";
    private static ImageManager instance;
    private final Context context;
    private final File cacheDir;
    private final ExecutorService executor;

    private ImageManager(Context context) {
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

        executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized ImageManager getInstance(Context context) {
        if (instance == null) {
            instance = new ImageManager(context);
        }
        return instance;
    }

    /**
     * Load an image into an ImageView, handling caching automatically
     * @param imageView The ImageView to load the image into
     * @param imagePath The path or URL of the image
     * @param defaultResId The default resource ID to use if the image can't be loaded
     */
    public void loadImage(ImageView imageView, String imagePath, int defaultResId) {
        if (imagePath == null || imagePath.isEmpty()) {
            imageView.setImageResource(defaultResId);
            return;
        }

        // Check if it's a URL or a local path
        if (imagePath.startsWith("http")) {
            // It's a URL, check if we have it cached
            String cachedPath = getCachedImagePath(imagePath);

            if (cachedPath != null) {
                // Use the cached version
                loadImageFromPath(imageView, cachedPath, defaultResId);
            } else {
                // Load from URL and cache it
                loadImageFromUrlAndCache(imageView, imagePath, defaultResId);
            }
        } else {
            // It's a local file path or content URI
            loadImageFromPath(imageView, imagePath, defaultResId);
        }
    }

    /**
     * Load an image from a local path into an ImageView
     * @param imageView The ImageView to load the image into
     * @param imagePath The local path of the image
     * @param defaultResId The default resource ID to use if the image can't be loaded
     */
    private void loadImageFromPath(ImageView imageView, String imagePath, int defaultResId) {
        try {
            Glide.with(context)
                    .load(imagePath)
                    .apply(new RequestOptions()
                            .placeholder(defaultResId)
                            .error(defaultResId)
                            .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Failed to load image from path: " + imagePath, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(imageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image from path: " + imagePath, e);
            imageView.setImageResource(defaultResId);
        }
    }

    /**
     * Load an image from a URL into an ImageView and cache it
     * @param imageView The ImageView to load the image into
     * @param imageUrl The URL of the image
     * @param defaultResId The default resource ID to use if the image can't be loaded
     */
    private void loadImageFromUrlAndCache(ImageView imageView, String imageUrl, int defaultResId) {
        try {
            // First load the image into the ImageView to show it immediately
            Glide.with(context)
                    .load(imageUrl)
                    .apply(new RequestOptions()
                            .placeholder(defaultResId)
                            .error(defaultResId)
                            .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(imageView);

            // Also download the image to cache it
            Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            // Cache the image in the background
                            executor.execute(() -> {
                                try {
                                    // Save to cache
                                    String filename = generateFilename(imageUrl);
                                    File imageFile = new File(cacheDir, filename);
                                    saveBitmapToFile(resource, imageFile);

                                    Log.d(TAG, "Image cached successfully: " + imageFile.getAbsolutePath());
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to cache image: " + e.getMessage(), e);
                                }
                            });
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Do nothing
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error loading image from URL: " + imageUrl, e);
            imageView.setImageResource(defaultResId);
        }
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
            Log.e(TAG, "Error saving bitmap: " + e.getMessage(), e);
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing file output stream: " + e.getMessage(), e);
                }
            }
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
}