package com.fieldnotes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY sort_order ASC, created_at ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY sort_order ASC, created_at ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM category WHERE name_key = :nameKey LIMIT 1")
    suspend fun findByNameKey(nameKey: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: CategoryEntity): Long

    @Update
    suspend fun update(entity: CategoryEntity): Int

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM category")
    suspend fun deleteAll()
}

@Dao
interface ItemDao {
    @Transaction
    @Query("SELECT * FROM item ORDER BY name ASC")
    fun observeAllWithCategory(): Flow<List<ItemWithCategory>>

    @Transaction
    @Query("SELECT * FROM item ORDER BY name ASC")
    suspend fun getAllWithCategory(): List<ItemWithCategory>

    @Transaction
    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun getByIdWithCategory(id: Long): ItemWithCategory?

    @Transaction
    @Query("SELECT * FROM item WHERE id IN (:ids)")
    suspend fun getByIdsWithCategory(ids: List<Long>): List<ItemWithCategory>

    @Query("SELECT * FROM item WHERE name_key = :nameKey LIMIT 1")
    suspend fun findByNameKey(nameKey: String): ItemEntity?

    @Insert
    suspend fun insert(entity: ItemEntity): Long

    @Update
    suspend fun update(entity: ItemEntity): Int

    @Query("DELETE FROM item WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("UPDATE item SET category_id = NULL WHERE category_id = :categoryId")
    suspend fun clearCategory(categoryId: Long)

    @Query("DELETE FROM item")
    suspend fun deleteAll()
}

@Dao
interface TemplateDao {
    @Transaction
    @Query("SELECT * FROM template ORDER BY COALESCE(last_used_at, 0) DESC, updated_at DESC")
    fun observeAllWithItems(): Flow<List<TemplateWithItems>>

    @Transaction
    @Query("SELECT * FROM template ORDER BY COALESCE(last_used_at, 0) DESC, updated_at DESC")
    suspend fun getAllWithItems(): List<TemplateWithItems>

    @Transaction
    @Query("SELECT * FROM template WHERE id = :id")
    fun observeByIdWithItems(id: Long): Flow<TemplateWithItems?>

    @Transaction
    @Query("SELECT * FROM template WHERE id = :id")
    suspend fun getByIdWithItems(id: Long): TemplateWithItems?

    @Query("SELECT * FROM template WHERE name_key = :nameKey LIMIT 1")
    suspend fun findByNameKey(nameKey: String): TemplateEntity?

    @Query("SELECT * FROM template WHERE id = :id")
    suspend fun getById(id: Long): TemplateEntity?

    @Query("SELECT * FROM template_item WHERE template_id = :templateId ORDER BY sort_order ASC, id ASC")
    suspend fun getItems(templateId: Long): List<TemplateItemEntity>

    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM template_item WHERE template_id = :templateId")
    suspend fun maxSortOrder(templateId: Long): Int

    @Insert
    suspend fun insert(entity: TemplateEntity): Long

    @Update
    suspend fun update(entity: TemplateEntity): Int

    @Query("UPDATE template SET last_used_at = :timestamp WHERE id = :id")
    suspend fun touchLastUsed(id: Long, timestamp: Long)

    @Query("DELETE FROM template WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Insert
    suspend fun insertItems(items: List<TemplateItemEntity>)

    @Query("DELETE FROM template_item WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long): Int

    @Query("DELETE FROM template")
    suspend fun deleteAll()
}

@Dao
interface PlanDao {
    @Transaction
    @Query("SELECT * FROM plan ORDER BY start_epoch_day ASC, created_at DESC")
    fun observeAllWithItems(): Flow<List<PlanWithItems>>

    @Transaction
    @Query("SELECT * FROM plan WHERE id = :id")
    fun observeByIdWithItems(id: Long): Flow<PlanWithItems?>

    @Transaction
    @Query("SELECT * FROM plan ORDER BY start_epoch_day ASC, created_at DESC")
    suspend fun getAllWithItems(): List<PlanWithItems>

    @Transaction
    @Query("SELECT * FROM plan WHERE id = :id")
    suspend fun getByIdWithItems(id: Long): PlanWithItems?

    @Query("SELECT * FROM plan WHERE id = :id")
    suspend fun getById(id: Long): PlanEntity?

    @Query("SELECT * FROM plan_item WHERE plan_id = :planId ORDER BY sort_order ASC, id ASC")
    suspend fun getItems(planId: Long): List<PlanItemEntity>

    @Query("SELECT * FROM plan_item WHERE id = :itemId")
    suspend fun getItemById(itemId: Long): PlanItemEntity?

    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM plan_item WHERE plan_id = :planId")
    suspend fun maxSortOrder(planId: Long): Int

    @Insert
    suspend fun insert(entity: PlanEntity): Long

    @Update
    suspend fun update(entity: PlanEntity): Int

    @Query("DELETE FROM plan WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Insert
    suspend fun insertItems(items: List<PlanItemEntity>)

    @Query("UPDATE plan_item SET checked = :checked WHERE id = :itemId")
    suspend fun updateChecked(itemId: Long, checked: Boolean): Int

    @Query("UPDATE plan_item SET checked = :checked WHERE plan_id = :planId")
    suspend fun updateAllChecked(planId: Long, checked: Boolean): Int

    @Query("DELETE FROM plan_item WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long): Int

    @Query("DELETE FROM plan")
    suspend fun deleteAll()
}
