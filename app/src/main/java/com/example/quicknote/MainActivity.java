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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout notesContainer;
    private Flow notesFlow;
    private List<View> activeNotes = new ArrayList<>();
    private List<View> deletedNotes = new ArrayList<>();
    
    private ImageView btnBack;
    private TextView toolbarTitle;
    private LinearLayout mainIcons;
    private LinearLayout selectionIcons;
    private FloatingActionButton addNoteFab;
    
    private boolean isSelectionMode = false;
    private boolean isRecentlyDeletedView = false;
    private int noteCount = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        notesContainer = findViewById(R.id.notesContainer);
        notesFlow = findViewById(R.id.notesFlow);
        btnBack = findViewById(R.id.btn_back);
        toolbarTitle = findViewById(R.id.toolbar_title);
        mainIcons = findViewById(R.id.main_icons);
        selectionIcons = findViewById(R.id.selection_icons);
        addNoteFab = findViewById(R.id.add_note_button);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize with existing notes from XML
        int[] initialIds = {R.id.note1, R.id.note2, R.id.note3, R.id.note4, R.id.note5};
        for (int id : initialIds) {
            View note = findViewById(id);
            if (note != null) {
                activeNotes.add(note);
                setupNote(note);
            }
        }

        findViewById(R.id.btn_select).setOnClickListener(v -> enterSelectionMode());
        findViewById(R.id.btn_cancel_selection).setOnClickListener(v -> exitSelectionMode());
        findViewById(R.id.btn_delete).setOnClickListener(v -> deleteSelectedNotes());
        findViewById(R.id.btn_recently_deleted).setOnClickListener(v -> showRecentlyDeleted());
        findViewById(R.id.btn_back).setOnClickListener(v -> showMainNotes());
        findViewById(R.id.btn_add_toolbar).setOnClickListener(v -> addNewNote());
        addNoteFab.setOnClickListener(v -> addNewNote());
        
        updateFlow();
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
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
            }
            return false;
        });
    }

    private void addNewNote() {
        noteCount++;
        View newNote = LayoutInflater.from(this).inflate(R.layout.note_item, notesContainer, false);
        int newId = View.generateViewId();
        newNote.setId(newId);
        
        EditText editText = newNote.findViewById(R.id.note_edit_text);
        editText.setHint("New Note"); // Fixed hint
        setupNote(newNote);
        
        notesContainer.addView(newNote);
        activeNotes.add(newNote);
        
        if (isRecentlyDeletedView) {
            showMainNotes();
        } else {
            updateFlow();
        }
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        mainIcons.setVisibility(View.GONE);
        selectionIcons.setVisibility(View.VISIBLE);
        for (View note : activeNotes) {
            note.findViewById(R.id.note_checkbox).setVisibility(View.VISIBLE);
            note.findViewById(R.id.selection_overlay).setVisibility(View.VISIBLE);
            ((CheckBox)note.findViewById(R.id.note_checkbox)).setChecked(false);
        }
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        mainIcons.setVisibility(View.VISIBLE);
        selectionIcons.setVisibility(View.GONE);
        for (View note : activeNotes) {
            note.findViewById(R.id.note_checkbox).setVisibility(View.GONE);
            note.findViewById(R.id.selection_overlay).setVisibility(View.GONE);
        }
    }

    private void deleteSelectedNotes() {
        List<View> toDelete = new ArrayList<>();
        for (View note : activeNotes) {
            CheckBox cb = note.findViewById(R.id.note_checkbox);
            if (cb.getVisibility() == View.VISIBLE && cb.isChecked()) {
                toDelete.add(note);
            }
        }
        
        for (View note : toDelete) {
            activeNotes.remove(note);
            deletedNotes.add(note);
            note.setVisibility(View.GONE);
            note.findViewById(R.id.note_checkbox).setVisibility(View.GONE);
            note.findViewById(R.id.selection_overlay).setVisibility(View.GONE);
        }
        
        exitSelectionMode();
        updateFlow();
    }

    private void showRecentlyDeleted() {
        isRecentlyDeletedView = true;
        isSelectionMode = false;
        
        btnBack.setVisibility(View.VISIBLE);
        toolbarTitle.setText("Recently Deleted");
        mainIcons.setVisibility(View.GONE);
        selectionIcons.setVisibility(View.GONE);
        addNoteFab.setVisibility(View.GONE);
        
        for (View note : activeNotes) note.setVisibility(View.GONE);
        for (View note : deletedNotes) {
            note.setVisibility(View.VISIBLE);
            note.findViewById(R.id.deleted_note_actions).setVisibility(View.VISIBLE);
            note.findViewById(R.id.note_checkbox).setVisibility(View.GONE);
            note.findViewById(R.id.selection_overlay).setVisibility(View.GONE);
        }
        updateFlow();
    }

    private void showMainNotes() {
        isRecentlyDeletedView = false;
        
        btnBack.setVisibility(View.GONE);
        toolbarTitle.setText("QuickNote");
        mainIcons.setVisibility(View.VISIBLE);
        selectionIcons.setVisibility(View.GONE);
        addNoteFab.setVisibility(View.VISIBLE);
        
        for (View note : deletedNotes) note.setVisibility(View.GONE);
        for (View note : activeNotes) {
            note.setVisibility(View.VISIBLE);
            note.findViewById(R.id.deleted_note_actions).setVisibility(View.GONE);
            note.findViewById(R.id.note_checkbox).setVisibility(View.GONE);
            note.findViewById(R.id.selection_overlay).setVisibility(View.GONE);
        }
        updateFlow();
    }

    private void recoverNote(View note) {
        deletedNotes.remove(note);
        activeNotes.add(note);
        note.findViewById(R.id.deleted_note_actions).setVisibility(View.GONE);
        if (isRecentlyDeletedView) {
            note.setVisibility(View.GONE);
            updateFlow();
        }
    }

    private void deletePermanently(View note) {
        deletedNotes.remove(note);
        notesContainer.removeView(note);
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
