package com.example.quicknote;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY lastEditedAt DESC")
    List<Note> getActiveNotes();

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY lastEditedAt DESC")
    List<Note> getDeletedNotes();

    @Query("SELECT * FROM notes WHERE id = :id")
    Note getNoteById(int id);

    @Insert
    long insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);
}
