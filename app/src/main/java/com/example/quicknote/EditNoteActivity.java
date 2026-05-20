package com.example.quicknote;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditNoteActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "extra_note_id";

    private EditText titleEdit;
    private EditText contentEdit;
    private TextView lastEditedText;
    private NoteDao noteDao;
    private Note note;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        noteDao = AppDatabase.getInstance(this).noteDao();

        titleEdit = findViewById(R.id.edit_note_title);
        contentEdit = findViewById(R.id.edit_note_content);
        lastEditedText = findViewById(R.id.last_edited_text);

        int noteId = getIntent().getIntExtra(EXTRA_NOTE_ID, -1);
        if (noteId != -1) {
            new Thread(() -> {
                note = noteDao.getNoteById(noteId);
                runOnUiThread(this::populateViews);
            }).start();
        }

        findViewById(R.id.btn_back_edit).setOnClickListener(v -> finish());

        setupAutoSave();
    }

    private void populateViews() {
        if (note != null) {
            titleEdit.setText(note.getTitle());
            contentEdit.setText(note.getContent());
            updateLastEditedDisplay();
        }
    }

    private void updateLastEditedDisplay() {
        if (note != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(note.getLastEditedAt()));
            lastEditedText.setText("Last edited: " + dateStr);
        }
    }

    private void setupAutoSave() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (note != null) {
                    note.setTitle(titleEdit.getText().toString());
                    note.setContent(contentEdit.getText().toString());
                    note.setLastEditedAt(System.currentTimeMillis());
                    new Thread(() -> noteDao.update(note)).start();
                    updateLastEditedDisplay();
                }
            }
        };
        titleEdit.addTextChangedListener(watcher);
        contentEdit.addTextChangedListener(watcher);
    }
}
