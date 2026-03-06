package com.example.quicknote;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout notesContainer;
    private Flow notesFlow;
    private final List<View> activeNotes = new ArrayList<>();
    private final List<View> deletedNotes = new ArrayList<>();
    
    private ImageView btnBack;
    private TextView toolbarTitle;
    private LinearLayout mainIcons;
    private LinearLayout selectionIcons;
    
    private boolean isSelectionMode = false;
    private boolean isRecentlyDeletedView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupInsets();
        loadInitialNotes();
        setupClickListeners();
        
        updateUI();
    }

    private void initViews() {
        notesContainer = findViewById(R.id.notesContainer);
        notesFlow = findViewById(R.id.notesFlow);
        btnBack = findViewById(R.id.btn_back);
        toolbarTitle = findViewById(R.id.toolbar_title);
        mainIcons = findViewById(R.id.main_icons);
        selectionIcons = findViewById(R.id.selection_icons);
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadInitialNotes() {
        int[] initialIds = {R.id.note1, R.id.note2, R.id.note3, R.id.note4, R.id.note5};
        for (int id : initialIds) {
            View note = findViewById(id);
            if (note != null) {
                activeNotes.add(note);
                setupNote(note);
            }
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_select).setOnClickListener(v -> enterSelectionMode());
        findViewById(R.id.btn_cancel_selection).setOnClickListener(v -> exitSelectionMode());
        findViewById(R.id.btn_delete).setOnClickListener(v -> deleteSelectedNotes());
        findViewById(R.id.btn_recently_deleted).setOnClickListener(v -> {
            isRecentlyDeletedView = true;
            isSelectionMode = false;
            updateUI();
        });
        btnBack.setOnClickListener(v -> {
            isRecentlyDeletedView = false;
            updateUI();
        });
        findViewById(R.id.btn_add_toolbar).setOnClickListener(v -> addNewNote());
    }

    private void setupNote(View noteView) {
        EditText editText = noteView.findViewById(R.id.note_edit_text);
        setupNoteScrolling(editText);
        
        noteView.findViewById(R.id.btn_recover).setOnClickListener(v -> recoverNote(noteView));
        noteView.findViewById(R.id.btn_delete_permanent).setOnClickListener(v -> deletePermanently(noteView));
        
        View overlay = noteView.findViewById(R.id.selection_overlay);
        overlay.setOnClickListener(v -> {
            CheckBox cb = noteView.findViewById(R.id.note_checkbox);
            cb.setChecked(!cb.isChecked());
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupNoteScrolling(EditText editText) {
        if (editText == null) return;
        editText.setOnTouchListener((v, event) -> {
            if (v.hasFocus()) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
            }
            return false;
        });
    }

    private void addNewNote() {
        View newNote = LayoutInflater.from(this).inflate(R.layout.note_item, notesContainer, false);
        newNote.setId(View.generateViewId());
        setupNote(newNote);
        
        notesContainer.addView(newNote);
        activeNotes.add(newNote);
        
        isRecentlyDeletedView = false;
        updateUI();
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        for (View note : activeNotes) {
            ((CheckBox)note.findViewById(R.id.note_checkbox)).setChecked(false);
        }
        updateUI();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        updateUI();
    }

    private void deleteSelectedNotes() {
        List<View> toDelete = new ArrayList<>();
        for (View note : activeNotes) {
            CheckBox cb = note.findViewById(R.id.note_checkbox);
            if (cb.isChecked()) {
                toDelete.add(note);
            }
        }
        
        activeNotes.removeAll(toDelete);
        deletedNotes.addAll(toDelete);
        
        isSelectionMode = false;
        updateUI();
    }

    private void recoverNote(View note) {
        deletedNotes.remove(note);
        activeNotes.add(note);
        updateUI();
    }

    private void deletePermanently(View note) {
        deletedNotes.remove(note);
        notesContainer.removeView(note);
        updateUI();
    }

    private void updateUI() {
        btnBack.setVisibility(isRecentlyDeletedView ? View.VISIBLE : View.GONE);
        toolbarTitle.setText(isRecentlyDeletedView ? "Recently Deleted" : "QuickNote");
        
        if (isRecentlyDeletedView) {
            mainIcons.setVisibility(View.GONE);
            selectionIcons.setVisibility(View.GONE);
        } else {
            mainIcons.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
            selectionIcons.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        }

        // Handle note visibility and inner action states
        for (View note : activeNotes) {
            note.setVisibility(isRecentlyDeletedView ? View.GONE : View.VISIBLE);
            note.findViewById(R.id.deleted_note_actions).setVisibility(View.GONE);
            
            int selectionVisibility = (!isRecentlyDeletedView && isSelectionMode) ? View.VISIBLE : View.GONE;
            note.findViewById(R.id.note_checkbox).setVisibility(selectionVisibility);
            note.findViewById(R.id.selection_overlay).setVisibility(selectionVisibility);
        }

        for (View note : deletedNotes) {
            note.setVisibility(isRecentlyDeletedView ? View.VISIBLE : View.GONE);
            note.findViewById(R.id.deleted_note_actions).setVisibility(isRecentlyDeletedView ? View.VISIBLE : View.GONE);
            note.findViewById(R.id.note_checkbox).setVisibility(View.GONE);
            note.findViewById(R.id.selection_overlay).setVisibility(View.GONE);
        }

        updateFlow();
    }

    private void updateFlow() {
        List<View> currentList = isRecentlyDeletedView ? deletedNotes : activeNotes;
        int[] ids = new int[currentList.size()];
        for (int i = 0; i < currentList.size(); i++) {
            ids[i] = currentList.get(i).getId();
        }
        notesFlow.setReferencedIds(ids);
    }
}
