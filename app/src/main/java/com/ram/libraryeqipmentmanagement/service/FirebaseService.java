package com.ram.libraryeqipmentmanagement.service;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.ram.libraryeqipmentmanagement.LibraryEquipmentApplication;
import com.ram.libraryeqipmentmanagement.model.Department;
import com.ram.libraryeqipmentmanagement.model.Equipment;
import com.ram.libraryeqipmentmanagement.model.Library;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.ram.libraryeqipmentmanagement.model.User;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static FirebaseService instance;
    private final FirebaseAuth auth;
    private final FirebaseDatabase database;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private final Context context;

    private FirebaseService(Context context) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
        this.database = ((LibraryEquipmentApplication) context.getApplicationContext()).getDatabase();
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        
        // Enable database persistence and keep libraries in sync
        DatabaseReference librariesRef = database.getReference("libraries");
        librariesRef.keepSynced(true);
    }

    public static synchronized FirebaseService getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseService(context);
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    public void signOut() {
        auth.signOut();
    }

    public DatabaseReference getDepartmentsReference() {
        return database.getReference("departments");
    }

    public Task<DataSnapshot> getDepartmentsCount() {
        return database.getReference("departments").get();
    }

    public Task<DataSnapshot> getEquipmentStatusCount() {
        return database.getReference("equipment").get();
    }

    public Task<DataSnapshot> getEquipmentStatistics(String libraryId) {
        return database.getReference("equipment")
            .orderByChild("libraryId")
            .equalTo(libraryId)
            .get();
    }

    public Task<QuerySnapshot> getLibrariesByDepartment(String departmentId) {
        Log.d(TAG, "Fetching libraries for department: " + departmentId);
        return firestore.collection("libraries")
                .whereEqualTo("departmentId", departmentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Successfully retrieved libraries from Firestore");
                    if (!querySnapshot.isEmpty()) {
                        Log.d(TAG, "Found " + querySnapshot.size() + " libraries");
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Library library = document.toObject(Library.class);
                            library.setId(document.getId());
                            Log.d(TAG, "Library: " + library.getName() + ", ID: " + document.getId());
                        }
                    } else {
                        Log.d(TAG, "No libraries found for department: " + departmentId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching libraries: " + e.getMessage(), e);
                });
    }

    public Task<DataSnapshot> getEquipment(String libraryId) {
        return database.getReference("equipment")
            .orderByChild("libraryId")
            .equalTo(libraryId)
            .get();
    }

    public Task<Void> addEquipment(String libraryId, Equipment equipment) {
        equipment.setLibraryId(libraryId);
        return firestore.collection("libraries")
                .document(libraryId)
                .collection("equipment")
                .add(equipment)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String equipmentId = task.getResult().getId();
                        equipment.setId(equipmentId);
                        return task.getResult().update("id", equipmentId);
                    }
                    throw task.getException();
                });
    }

    public Task<Void> updateEquipment(String equipmentId, Equipment equipment) {
        return firestore.collection("libraries")
                .document(equipment.getLibraryId())
                .collection("equipment")
                .document(equipmentId)
                .set(equipment);
    }

    public Task<Void> deleteEquipment(String equipmentId, Equipment equipment) {
        if (equipment == null || equipment.getLibraryId() == null) {
            Log.e(TAG, "Invalid equipment or libraryId");
            return Tasks.forException(new IllegalArgumentException("Invalid equipment or libraryId"));
        }
        
        return firestore.collection("libraries")
                .document(equipment.getLibraryId())
                .collection("equipment")
                .document(equipmentId)
                .delete();
    }

    public Task<Void> updateDepartment(String departmentId, Department department) {
        DatabaseReference departmentRef = database.getReference("departments").child(departmentId);
        return departmentRef.setValue(department);
    }

    public void cleanup() {
        database.goOffline();
    }

    public Task<Void> addDepartment(Department department) {
        DatabaseReference departmentsRef = database.getReference("departments");
        String key = departmentsRef.push().getKey();
        department.setId(key);
        return departmentsRef.child(key).setValue(department);
    }

    public Task<Void> deleteDepartment(String departmentId) {
        DatabaseReference departmentRef = database.getReference("departments").child(departmentId);
        return departmentRef.removeValue();
    }

    public Task<Void> addLibrary(Library library) {
        return firestore.collection("libraries")
                .add(library)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentReference docRef = task.getResult();
                        if (docRef != null) {
                            library.setId(docRef.getId());
                        }
                    }
                    return null;
                });
    }

    public Task<Void> updateLibrary(Library library) {
        DatabaseReference libraryRef = database.getReference("libraries").child(library.getId());
        return libraryRef.setValue(library);
    }

    public Task<Void> deleteLibrary(String libraryId) {
        Log.d(TAG, "Deleting library with ID: " + libraryId);
        
        // First delete all cells associated with this library
        return firestore.collection("library_cells")
            .whereEqualTo("libraryId", libraryId)
            .get()
            .continueWithTask(task -> {
                if (task.isSuccessful()) {
                    WriteBatch batch = firestore.batch();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        batch.delete(document.getReference());
                    }
                    return batch.commit();
                } else {
                    throw task.getException();
                }
            })
            .continueWithTask(task -> {
                // Then delete the library itself
                return firestore.collection("libraries")
                    .document(libraryId)
                    .delete();
            });
    }

    public Task<String> getUserRole(String userId) {
        return firestore.collection("users")
            .document(userId)
            .get()
            .continueWith(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String role = task.getResult().getString("role");
                    return role != null ? role : "user";
                }
                return "user";
            });
    }

    public Task<DocumentSnapshot> getLibrary(String libraryId) {
        return firestore.collection("libraries")
                .document(libraryId)
                .get();
    }

    // Request admin access
    public Task<Void> requestAdminAccess(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isAdminRequestPending", true);
        updates.put("adminRequestStatus", "pending");
        updates.put("adminRequestTimestamp", System.currentTimeMillis());
        
        return firestore.collection("users").document(userId).update(updates);
    }

    // Get all pending admin requests
    public Task<QuerySnapshot> getPendingAdminRequests() {
        return firestore.collection("users")
                .whereEqualTo("isAdminRequestPending", true)
                .whereEqualTo("adminRequestStatus", "pending")
                .get();
    }

    // Approve admin request
    public Task<Void> approveAdminRequest(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isAdminRequestPending", false);
        updates.put("adminRequestStatus", "approved");
        updates.put("role", "admin");
        
        return firestore.collection("users").document(userId).update(updates);
    }

    // Reject admin request
    public Task<Void> rejectAdminRequest(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isAdminRequestPending", false);
        updates.put("adminRequestStatus", "rejected");
        
        return firestore.collection("users").document(userId).update(updates);
    }

    // Check if user is approved admin
    public Task<DocumentSnapshot> checkAdminStatus(String userId) {
        return firestore.collection("users").document(userId).get();
    }

    public Task<Void> createOrUpdateUser(User user) {
        return firestore.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User created/updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating/updating user", e);
                });
    }

    public Task<Void> addQuery(Map<String, Object> query) {
        return firestore.collection("queries")
                .add(query)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String queryId = task.getResult().getId();
                        return firestore.collection("queries")
                                .document(queryId)
                                .update("id", queryId);
                    }
                    throw task.getException();
                });
    }

    public Task<QuerySnapshot> getEquipmentByLibrary(String libraryId) {
        return firestore.collection("libraries")
                .document(libraryId)
                .collection("equipment")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Successfully retrieved equipment from Firestore");
                    if (!querySnapshot.isEmpty()) {
                        Log.d(TAG, "Found " + querySnapshot.size() + " equipment items");
                    } else {
                        Log.d(TAG, "No equipment found for library: " + libraryId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching equipment: " + e.getMessage(), e);
                });
    }
} 