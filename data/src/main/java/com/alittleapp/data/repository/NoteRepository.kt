package com.alittleapp.data.repository

import com.alittleapp.data.db.dao.NoteDao
import com.alittleapp.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val dao: NoteDao) {

    fun getAllNotes(): Flow<List<NoteEntity>> = dao.getAllNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = dao.searchNotes(query)

    fun getRecentNotes(limit: Int = 5): Flow<List<NoteEntity>> = dao.getRecentNotes(limit)

    suspend fun getNoteById(id: Long): NoteEntity? = dao.getNoteById(id)

    suspend fun saveNote(note: NoteEntity): Long {
        return if (note.id == 0L) {
            dao.insertNote(note)
        } else {
            dao.updateNote(note.copy(updatedAt = Date()))
            note.id
        }
    }

    suspend fun deleteNote(note: NoteEntity) = dao.deleteNote(note)
}
