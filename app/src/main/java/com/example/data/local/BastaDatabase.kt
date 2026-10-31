package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CashboxConfig
import com.example.data.model.Customer
import com.example.data.model.DebtPayment
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Category

@Database(
    entities = [
        Product::class,
        Sale::class,
        SaleItem::class,
        Customer::class,
        DebtPayment::class,
        Expense::class,
        CashboxConfig::class,
        Category::class
    ],
    version = 4,
    exportSchema = false
)
abstract class BastaDatabase : RoomDatabase() {
    abstract fun bastaDao(): BastaDao

    // Domain-specific DAO accessors for sales, inventory, debts, and expenses
    fun productDao(): BastaDao = bastaDao()
    fun saleDao(): BastaDao = bastaDao()
    fun customerDao(): BastaDao = bastaDao()
    fun debtDao(): BastaDao = bastaDao()
    fun expenseDao(): BastaDao = bastaDao()

    companion object {
        @Volatile
        private var INSTANCE: BastaDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN currentStock INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("UPDATE products SET currentStock = quantity")
                } catch (ignored: Exception) {
                    // Column already present
                }
            }
        }

        fun getDatabase(context: Context): BastaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BastaDatabase::class.java,
                    "basta_manager.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
