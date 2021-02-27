import com.beust.klaxon.Json

data class Weather(@Json(name = "Temp") val temperature: Array<String>)


