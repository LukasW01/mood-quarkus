package dev.wigger.mood.entry

import io.quarkus.hibernate.orm.panache.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
class EntryRepository : PanacheRepository<Entry> {
    fun persistOne(entry: Entry) = persistAndFlush(entry)

    fun delete(id: UUID) = delete("id = ?1", id)

    fun findByUserId(userId: Long): List<Entry>? = find("user.id = ?1", userId).list()

    fun findByUserIdForLastSevenDays(userId: Long): List<Entry>? = find("user.id = ?1 and date >= ?2 ", userId, LocalDate.now().minusDays(7)).list()

    fun findByUserIdAndDate(userId: Long, date: List<LocalDate>): MutableList<Entry>? = list("user.id = ?1 and date in ?2", userId, date)
    
    fun findByIdAndUserId(id: UUID, userId: Long): Entry? = find("id = ?1 and user.id = ?2", id, userId).firstResult()

    fun findByUuid(id: UUID): Entry? = find("id = ?1", id).firstResult()

    fun updateOne(id: UUID, entry: Entry) {
        findByUuid(id)?.apply {
            mood = entry.mood
            journal = entry.journal
            date = entry.date
            color = entry.color
            persistAndFlush(this)
        }
    }
}
