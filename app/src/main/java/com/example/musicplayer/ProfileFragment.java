package com.example.musicplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CircleImageView profileImage;
    private TextView textUsername, textEmail;
    private LinearLayout layoutChangeUsername, layoutChangePassword;
    private View btnChangePhoto;
    private OnBackPressedListener onBackPressedListener;
    private Uri imageUri;

    public interface OnBackPressedListener {
        void onBackPressed();
    }

    public void setOnBackPressedListener(OnBackPressedListener listener) {
        this.onBackPressedListener = listener;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (onBackPressedListener != null) {
            onBackPressedListener.onBackPressed();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image);
        textUsername = view.findViewById(R.id.text_username);
        textEmail = view.findViewById(R.id.text_email);
        btnChangePhoto = view.findViewById(R.id.btn_change_photo);
        layoutChangeUsername = view.findViewById(R.id.layout_change_username);
        layoutChangePassword = view.findViewById(R.id.layout_change_password);

        // Set up back button
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Set up logout button
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> logoutUser());

        // Set up change photo button
        btnChangePhoto.setOnClickListener(v -> openImageChooser());

        // Set up change username button
        layoutChangeUsername.setOnClickListener(v -> showChangeUsernameDialog());

        // Set up change password button
        layoutChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Load user data
        loadUserData();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                profileImage.setImageBitmap(bitmap);
                uploadProfileImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Afficher les informations de base
            textUsername.setText(user.getDisplayName() != null ? user.getDisplayName() : "Anonymous");
            textEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");

            // Charger l'image depuis Firestore
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // Vérifier si une image est stockée en Base64
                                String imageBase64 = document.getString("profileImage");

                                if (imageBase64 != null && !imageBase64.isEmpty()) {
                                    // Convertir Base64 en bitmap et afficher
                                    byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    profileImage.setImageBitmap(decodedByte);
                                } else {
                                    // Aucune image dans Firestore, utiliser l'image par défaut
                                    profileImage.setImageResource(R.drawable.default_profile);
                                }
                            } else {
                                // Document n'existe pas, utiliser l'image par défaut
                                profileImage.setImageResource(R.drawable.default_profile);
                            }
                        } else {
                            // Erreur de lecture, utiliser l'image par défaut
                            profileImage.setImageResource(R.drawable.default_profile);
                            Toast.makeText(getContext(), "Erreur de chargement du profil", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Utilisateur non connecté
            Toast.makeText(getContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }

    private void logoutUser() {
        mAuth.signOut();

        // Reset profile image to default in the current view
        profileImage.setImageResource(R.drawable.default_profile);

        // Notify the parent activity to reset the profile image
        if (getActivity() instanceof DecouvrirActivity) {
            ((DecouvrirActivity) getActivity()).resetProfileImage();
        }

        Toast.makeText(getContext(), "Déconnecté avec succès", Toast.LENGTH_SHORT).show();
        // Juste avant ou après onBackPressed
        if (getActivity() instanceof DecouvrirActivity) {
            ((DecouvrirActivity) getActivity()).updateHistoryVisibility();
        }

        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    // Method to open image chooser
    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Sélectionner une image"), PICK_IMAGE_REQUEST);
    }

    // Method to upload profile image
    private void uploadProfileImage(Bitmap bitmap) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Show loading toast
        Toast.makeText(getContext(), "Mise à jour de la photo de profil...", Toast.LENGTH_SHORT).show();

        // Resize and compress bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        // Update Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImage", imageBase64);

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Photo de profil mise à jour", Toast.LENGTH_SHORT).show();

                    // Update parent activity if needed
                    if (getActivity() instanceof DecouvrirActivity) {
                        ((DecouvrirActivity) getActivity()).updateProfileImage(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Method to show change username dialog
    private void showChangeUsernameDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Modifier le nom d'utilisateur");

        // Set up the input
        final EditText input = new EditText(getContext());
        input.setHint("Nouveau nom d'utilisateur");
        input.setText(user.getDisplayName());
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            String newUsername = input.getText().toString().trim();
            if (!TextUtils.isEmpty(newUsername)) {
                updateUsername(newUsername);
            } else {
                Toast.makeText(getContext(), "Le nom d'utilisateur ne peut pas être vide", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Method to update username
    private void updateUsername(String newUsername) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Show loading toast
        Toast.makeText(getContext(), "Mise à jour du nom d'utilisateur...", Toast.LENGTH_SHORT).show();

        // Update Firebase Auth
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("username", newUsername);

                        db.collection("users").document(user.getUid())
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    textUsername.setText(newUsername);
                                    Toast.makeText(getContext(), "Nom d'utilisateur mis à jour", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Erreur Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(getContext(), "Erreur: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to show change password dialog
    private void showChangePasswordDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Modifier le mot de passe");

        // Create layout for dialog
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 10, 20, 10);

        // Current password
        TextInputLayout currentPasswordLayout = new TextInputLayout(getContext());
        currentPasswordLayout.setHint("Mot de passe actuel");
        TextInputEditText currentPassword = new TextInputEditText(getContext());
        currentPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentPasswordLayout.addView(currentPassword);
        layout.addView(currentPasswordLayout);

        // New password
        TextInputLayout newPasswordLayout = new TextInputLayout(getContext());
        newPasswordLayout.setHint("Nouveau mot de passe");
        TextInputEditText newPassword = new TextInputEditText(getContext());
        newPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordLayout.addView(newPassword);
        layout.addView(newPasswordLayout);

        // Confirm new password
        TextInputLayout confirmPasswordLayout = new TextInputLayout(getContext());
        confirmPasswordLayout.setHint("Confirmer le nouveau mot de passe");
        TextInputEditText confirmPassword = new TextInputEditText(getContext());
        confirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordLayout.addView(confirmPassword);
        layout.addView(confirmPasswordLayout);

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            String currentPwd = currentPassword.getText().toString().trim();
            String newPwd = newPassword.getText().toString().trim();
            String confirmPwd = confirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(currentPwd) || TextUtils.isEmpty(newPwd) || TextUtils.isEmpty(confirmPwd)) {
                Toast.makeText(getContext(), "Tous les champs sont requis", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPwd.length() < 6) {
                Toast.makeText(getContext(), "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPwd.equals(confirmPwd)) {
                Toast.makeText(getContext(), "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePassword(currentPwd, newPwd);
        });
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Method to update password
    private void updatePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        // Show loading toast
        Toast.makeText(getContext(), "Mise à jour du mot de passe...", Toast.LENGTH_SHORT).show();

        // Re-authenticate user
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User re-authenticated, update password
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(getContext(), "Mot de passe mis à jour", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), "Erreur: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "Mot de passe actuel incorrect", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
