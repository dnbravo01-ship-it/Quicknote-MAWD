package com.example.quicknote;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    private NoteDao noteDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        noteDao = AppDatabase.getInstance(this).noteDao();
        
        initViews();
        setupInsets();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotesFromDatabase();
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

    private void loadNotesFromDatabase() {
        // Clear current views from container and lists
        for (View v : activeNotes) notesContainer.removeView(v);
        for (View v : deletedNotes) notesContainer.removeView(v);
        activeNotes.clear();
        deletedNotes.clear();

        List<Note> allActive = noteDao.getActiveNotes();
        for (Note note : allActive) {
            View noteView = createNoteView(note);
            activeNotes.add(noteView);
        }

        List<Note> allDeleted = noteDao.getDeletedNotes();
        for (Note note : allDeleted) {
            View noteView = createNoteView(note);
            deletedNotes.add(noteView);
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

    private View createNoteView(Note note) {
        View noteView = LayoutInflater.from(this).inflate(R.layout.note_item, notesContainer, false);
        noteView.setId(View.generateViewId());
        noteView.setTag(note);
        
        TextView titleText = noteView.findViewById(R.id.note_title_text);
        TextView contentText = noteView.findViewById(R.id.note_text);
        TextView dateText = noteView.findViewById(R.id.note_date);
        
        titleText.setText(note.getTitle());
        contentText.setText(note.getContent());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        dateText.setText(sdf.format(new Date(note.getLastEditedAt())));
        
        setupNoteActions(noteView, note);

        notesContainer.addView(noteView);
        return noteView;
    }

    private void setupNoteActions(View noteView, Note note) {
        noteView.findViewById(R.id.btn_recover).setOnClickListener(v -> recoverNote(noteView));
        noteView.findViewById(R.id.btn_delete_permanent).setOnClickListener(v -> deletePermanently(noteView));
        
        View overlay = noteView.findViewById(R.id.selection_overlay);
        overlay.setOnClickListener(v -> {
            if (isSelectionMode) {
                CheckBox cb = noteView.findViewById(R.id.note_checkbox);
                cb.setChecked(!cb.isChecked());
            } else {
                openEditNote(note);
            }
        });

        noteView.setOnClickListener(v -> {
            if (!isSelectionMode && !isRecentlyDeletedView) {
                openEditNote(note);
            }
        });
    }

    private void openEditNote(Note note) {
        Intent intent = new Intent(this, EditNoteActivity.class);
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_ID, note.getId());
        startActivity(intent);
    }

    private void addNewNote() {
        long now = System.currentTimeMillis();
        Note newNote = new Note("", "", false, now, now);
        long id = noteDao.insert(newNote);
        newNote.setId((int) id);

        openEditNote(newNote);
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        for (View note : activeNotes) {
            CheckBox cb = note.findViewById(R.id.note_checkbox);
            if (cb != null) cb.setChecked(false);
        }
        updateUI();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        updateUI();
    }

    private void deleteSelectedNotes() {
        List<View> toDelete = new ArrayList<>();
        for (View noteView : activeNotes) {
            CheckBox cb = noteView.findViewById(R.id.note_checkbox);
            if (cb.isChecked()) {
                toDelete.add(noteView);
                Note note = (Note) noteView.getTag();
                note.setDeleted(true);
                noteDao.update(note);
            }
        }
        
        isSelectionMode = false;
        loadNotesFromDatabase();
        updateUI();
    }

    private void recoverNote(View noteView) {
        Note note = (Note) noteView.getTag();
        note.setDeleted(false);
        noteDao.update(note);
        
        loadNotesFromDatabase();
        updateUI();
    }

    private void deletePermanently(View noteView) {
        Note note = (Note) noteView.getTag();
        noteDao.delete(note);
        
        loadNotesFromDatabase();
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

        for (View note : activeNotes) {
            note.setVisibility(isRecentlyDeletedView ? View.GONE : View.VISIBLE);
            note.findViewById(R.id.deleted_note_actions).setVisibility(View.GONE);
            
            int selectionVisibility = (!isRecentlyDeletedView && isSelectionMode) ? View.VISIBLE : View.GONE;
            note.findViewById(R.id.note_checkbox).setVisibility(selectionVisibility);
            note.findViewById(R.id.selection_overlay).setVisibility(View.VISIBLE);
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
