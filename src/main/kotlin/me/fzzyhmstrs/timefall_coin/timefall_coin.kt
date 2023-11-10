@file:Suppress("PropertyName")

package me.fzzyhmstrs.timefall_coin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry
import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.loot.v2.LootTableEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.item.ItemStack
import net.minecraft.loot.LootPool
import net.minecraft.loot.condition.RandomChanceLootCondition
import net.minecraft.loot.entry.ItemEntry
import net.minecraft.loot.entry.TagEntry
import net.minecraft.loot.provider.number.ConstantLootNumberProvider
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.world.GameRules
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.random.Random


object TC: ModInitializer {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    const val MOD_ID = "timefall_coin"

    var CHEST_CHANCE: Float? = null

    val TIMEFALL_COIN: Item = Registry.register(Registries.ITEM, Identifier(MOD_ID,"coin"), Item(FabricItemSettings()))
    val TIMEFALL_COIN_PILE: Item = Registry.register(Registries.ITEM, Identifier(MOD_ID,"coin_pile"), Item(FabricItemSettings()))

    fun getChestChance(): Float {
        if (CHEST_CHANCE == null){
            CHEST_CHANCE = readOrCreate( { Config() }, Config::class.java).chestLootChance
        }
        return (CHEST_CHANCE ?: 0.02f)
    }

    override fun onInitialize() {

        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register{ _, _ ->
            CHEST_CHANCE = null
        }

        LootTableEvents.MODIFY.register{_,_,id,tableBuilder,_->
            if (id.path.startsWith("chests")) {
                val poolBuilder = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1.0F))
                    .conditionally(RandomChanceLootCondition.builder(getChestChance()))
                    .with(ItemEntry.builder(TIMEFALL_COIN))
                tableBuilder.pool(poolBuilder)
                val poolBuilder2 = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1.0F))
                    .conditionally(RandomChanceLootCondition.builder(getChestChance()/5f))
                    .with(ItemEntry.builder(Registries.ITEM.get(Identifier("paraglider:stamina_vessel"))))
                tableBuilder.pool(poolBuilder2)
            }
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
            .register(ItemGroupEvents.ModifyEntries { entries: FabricItemGroupEntries ->
                entries.add(ItemStack(TIMEFALL_COIN))
                entries.add(ItemStack(TIMEFALL_COIN_PILE))
            })
    }

    private fun <T> readOrCreate(configClass: () -> T, classType: Class<T>): T {
        val dir = FabricLoader.getInstance().configDir.toFile()
        if (!dir.exists() && !dir.mkdirs()) {
            println("Could not create directory, using default configs.")
            return configClass()
        }
        val f = File(dir, "timefall_coin_config.json")
        try {
            if (f.exists()) {
                return gson.fromJson(f.readLines().joinToString(""), classType)
            } else if (!f.createNewFile()) {
                println("Failed to create default config file (timefall_coin_config.json), using default config.")
            } else {
                f.writeText(gson.toJson(configClass()))
            }
            return configClass()
        } catch (e: Exception) {
            println("Failed to read config file! Using default values: " + e.message)
            return configClass()
        }
    }
}

class Config{
    var chestLootChance: Float = 0.02f
}

/*
@Environment(value = EnvType.CLIENT)
object TemplateKotlinClient: ClientModInitializer{

    override fun onInitializeClient() {
    }

    fun random(): Random{
        return Random(System.currentTimeMillis())
    }
}*/
