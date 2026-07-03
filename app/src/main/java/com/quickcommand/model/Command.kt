package com.quickcommand.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 手势类型枚举
 */
enum class GestureType(val displayName: String, val isPredefined: Boolean) {
    CIRCLE("画圆圈", true),
    TRIANGLE("画三角形", true),
    SQUARE("画方形", true),
    V_SHAPE("画 V 形", true),
    CHECKMARK("画对勾 ✓", true),
    SWIPE_UP("上滑", true),
    SWIPE_DOWN("下滑", true),
    SWIPE_LEFT("左滑", true),
    SWIPE_RIGHT("右滑", true),
    DOUBLE_TAP("双击", true),
    CUSTOM("自定义手势", false);

    companion object {
        fun fromName(name: String): GestureType =
            entries.find { it.name == name } ?: CUSTOM

        val predefinedEntries = entries.filter { it.isPredefined }
    }
}

/**
 * 命令动作类型枚举
 */
enum class ActionType(val displayName: String) {
    OPEN_APP("打开应用"),
    SET_REMINDER("设置提醒"),
    TOGGLE_WIFI("开关 WiFi"),
    TOGGLE_BLUETOOTH("开关蓝牙"),
    TOGGLE_FLASHLIGHT("开关手电筒"),
    OPEN_WEBSITE("打开网页"),
    TAKE_SCREENSHOT("截屏");
}

/**
 * 命令实体
 */
@Entity(tableName = "commands")
@TypeConverters(CommandConverters::class)
data class Command(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // 命令名称
    val gestureType: GestureType,        // 触发手势类型
    val customGesturePoints: String? = null, // 自定义手势点序列 (JSON)
    val actionType: ActionType,          // 执行动作类型
    val actionParam: String? = null,     // 动作参数（包名/URL/提醒时间等）
    val isEnabled: Boolean = true,       // 是否启用
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 手势点数据类（用于自定义手势）
 */
data class GesturePoint(val x: Float, val y: Float, val time: Long)

/**
 * Room 类型转换器
 */
class CommandConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromGestureType(value: GestureType): String = value.name

    @TypeConverter
    fun toGestureType(value: String): GestureType = GestureType.fromName(value)

    @TypeConverter
    fun fromActionType(value: ActionType): String = value.name

    @TypeConverter
    fun toActionType(value: String): ActionType =
        ActionType.entries.find { it.name == value } ?: ActionType.OPEN_APP

    companion object {
        private val gson = Gson()

        fun gesturePointsToJson(points: List<GesturePoint>): String = gson.toJson(points)

        fun gesturePointsFromJson(json: String?): List<GesturePoint> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val type = object : TypeToken<List<GesturePoint>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
