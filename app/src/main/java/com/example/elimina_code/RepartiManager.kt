import android.content.Context
import org.json.JSONObject

object RepartiManager {
    val counterMap = mutableMapOf<String, Int>()
    val colorMap = mutableMapOf<String, String>()

    fun saveReparti(context: Context) {
        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Salva i dati come JSON
        val counterJson = JSONObject(counterMap as Map<*, *>).toString()
        val colorJson = JSONObject(colorMap as Map<*, *>).toString()

        editor.putString("counterMap", counterJson)
        editor.putString("colorMap", colorJson)
        editor.apply()
    }

    fun loadReparti(context: Context) {
        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val counterJson = sharedPreferences.getString("counterMap", "{}")
        val colorJson = sharedPreferences.getString("colorMap", "{}")

        // Carica i dati dai JSON salvati
        val loadedCounterMap = JSONObject(counterJson!!)
        val loadedColorMap = JSONObject(colorJson!!)

        counterMap.clear()
        colorMap.clear()

        for (key in loadedCounterMap.keys()) {
            counterMap[key] = loadedCounterMap.getInt(key)
        }
        for (key in loadedColorMap.keys()) {
            colorMap[key] = loadedColorMap.getString(key)
        }
    }
}
