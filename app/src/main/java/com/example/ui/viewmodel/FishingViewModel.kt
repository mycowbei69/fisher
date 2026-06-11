package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.CatchEntity
import com.example.data.model.FishingSpot
import com.example.data.model.WeatherStation
import com.example.data.network.GeminiClient
import com.example.data.repository.CatchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

class FishingViewModel(application: Application) : AndroidViewModel(application) {

    private val catchDao = AppDatabase.getDatabase(application).catchDao()
    private val repository = CatchRepository(catchDao)

    // --- Weather & Station State ---
    val weatherStations = listOf(
        WeatherStation("基隆嶼海域", 25.1905, 121.7801, "漲潮中", 1.8, 1.2, "東北", 18.2, "東北風", 24.5, 26.0, "多雲時晴", "安全", 85),
        WeatherStation("澎湖漁翁島", 23.6015, 119.5085, "退潮中", 0.6, 0.4, "北", 12.0, "西北風", 26.2, 28.5, "晴朗無雲", "安全", 92),
        WeatherStation("鼻頭角外海", 25.1285, 121.9125, "退潮三分", -0.2, 2.5, "東北", 32.5, "東北強風", 22.8, 24.0, "局部陣雨", "注意 (浪高風大)", 45),
        WeatherStation("萊萊鶯歌石礁岩", 25.0118, 122.0003, "滿潮", 2.2, 3.4, "東", 38.0, "偏東風", 23.1, 23.5, "陰天微雨", "危險 (瘋狗浪警報)", 15),
        WeatherStation("墾丁佳樂水", 22.0005, 120.8525, "乾潮", -0.8, 0.8, "南", 14.5, "西南風", 28.0, 31.0, "晴朗", "安全", 78)
    )

    val selectedStation = MutableStateFlow(weatherStations[0])
    val isWeatherRefreshing = MutableStateFlow(false)

    // Gemini advisory report state
    val aiWeatherReport = MutableStateFlow("")
    val isGeneratingReport = MutableStateFlow(false)

    // --- Spot Recommendations & Bite Times State ---
    val fishingSpots = listOf(
        FishingSpot("基隆嶼大王礁", "北部頂級鱸魚與紅魽天堂，流急魚大。", "基隆嶼海域", "金目鱸、紅魽、嘉鱲", "中度", "04:30 - 06:30, 18:00 - 20:00", 25.1915, 121.7820, 4.8f, "潮汐交匯，清晨或黃昏大咬概率高"),
        FishingSpot("澎湖外垵斷崖", "深場磯釣聖地，硬尾、白鯛咬況極佳。", "澎湖漁翁島", "硬尾、白鯛、白帶魚", "安全", "06:00 - 08:30, 17:00 - 19:00", 23.6020, 119.5090, 4.9f, "水質清澈，配合退潮二分極佳"),
        FishingSpot("萊萊磯釣場", "極富挑戰的地形，大黑鯛最愛，但浪大極具風險。", "萊萊鶯歌石礁岩", "黑鯛、黑毛、石鯛", "危險", "05:00 - 07:00, 16:30 - 18:30", 25.0125, 122.0010, 4.5f, "大浪過後常有巨物入港，須著救生衣及防滑鞋"),
        FishingSpot("深澳酋長岩防波堤", "適合親子與新手的一斑釣點，舒適安全。", "基隆嶼海域", "白帶魚、軟絲、小卷", "安全", "19:00 - 23:00, 01:00 - 03:00", 25.1320, 121.8025, 4.2f, "夜間探照燈點亮，白帶魚成群咬餌"),
        FishingSpot("鼻頭角黃金坪", "斷崖下方深水區，高難度巨型磯物。", "鼻頭角外海", "石斑、紅槽、牛港鰺", "中度", "08:00 - 10:00, 15:00 - 17:00", 25.1300, 121.9150, 4.6f, "底物豐沛，適合重磯釣法")
    )

    // --- Safety Navigation & Simulated GPS Geofencing ---
    // User current fake location
    val userLatitude = MutableStateFlow(25.1850)
    val userLongitude = MutableStateFlow(121.7750)
    val userSpeed = MutableStateFlow(4.5) // knots
    val userBearing = MutableStateFlow(45.0) // degrees

    // Safety status and alerts
    val isGeofenceBreached = MutableStateFlow(false)
    val breachMessage = MutableStateFlow("")
    val breachZoneName = MutableStateFlow("")

    // Geofencing Zones
    val geofenceZones = listOf(
        GeofenceZone("萊萊風浪破碎礁岩區", 25.0125, 122.0010, 300.0, "海面突發瘋狗浪、暗流頻繁，非着全套防護具禁止入內。"),
        GeofenceZone("鼻頭角東側暗流特危區", 25.1300, 121.9150, 250.0, "裂流、礁石破碎、離岸風大，曾發生釣客墜海，請遠離！"),
        GeofenceZone("基隆嶼軍事與科研管制區", 25.1915, 121.7820, 200.0, "非特許船隻及釣客禁止進入特定管制港口與軍用營區邊界。")
    )

    // --- Catch Records Local State (Observe DB Flow!) ---
    val catchLogs: StateFlow<List<CatchEntity>> = repository.allCatches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unsyncedCount = MutableStateFlow(0)
    val cloudAiAnalysisResponse = MutableStateFlow("")
    val isSyncing = MutableStateFlow(false)

    init {
        // Evaluate initial GPS position
        checkGeofence(userLatitude.value, userLongitude.value)
        updateUnsyncedCount()
    }

    // Refresh weather simulate open data API call
    fun refreshWeather() {
        viewModelScope.launch {
            isWeatherRefreshing.value = true
            kotlinx.coroutines.delay(1200) // Simulated delay
            
            // Randomly oscillate the values slightly to simulate dynamic updates
            val current = selectedStation.value
            val changeFactor = (Math.random() - 0.5) * 0.4
            val newWaveHeight = max(0.1, round((current.waveHeight + changeFactor) * 10) / 10.0)
            val newWindSpeed = max(5.0, round((current.windSpeed + changeFactor * 10) * 10) / 10.0)
            val newTemp = round((current.airTemperature + (Math.random() - 0.5) * 1.0) * 10) / 10.0

            selectedStation.value = current.copy(
                waveHeight = newWaveHeight,
                windSpeed = newWindSpeed,
                airTemperature = newTemp,
                tideHeight = round((current.tideHeight + (Math.random() - 0.5) * 0.3) * 10) / 10.0
            )
            isWeatherRefreshing.value = false
            
            // Auto refresh report if a report was already open or requested
            if (aiWeatherReport.value.isNotEmpty()) {
                generateAiWeatherReport()
            }
        }
    }

    fun selectStation(station: WeatherStation) {
        selectedStation.value = station
        // Move user location to near this station automatically to demo GPS Navigation
        userLatitude.value = station.latitude - 0.005
        userLongitude.value = station.longitude - 0.005
        checkGeofence(userLatitude.value, userLongitude.value)
        aiWeatherReport.value = "" // clear previous advisory to let user recreate
    }

    // Call Gemini API for weather assessment
    fun generateAiWeatherReport() {
        viewModelScope.launch {
            isGeneratingReport.value = true
            aiWeatherReport.value = "AI 海釣氣象分析中心正透過雲端串接分析 government open data 與衛星實時潮位..."
            
            val station = selectedStation.value
            val prompt = """
                您是「國家智慧海釣觀測中心」的高級海洋顧問AI。
                請針對以下觀測點的實時海洋氣候數據進行專業語氣分析：
                觀測地點: ${station.name}
                目前潮汐狀態: ${station.tideState} (潮位高度: ${station.tideHeight}公尺)
                當前浪高: ${station.waveHeight}公尺 (浪向: ${station.waveDirection})
                平均風速: ${station.windSpeed} km/h (風向: ${station.windDirection})
                水溫: ${station.waterTemperature}°C / 氣溫: ${station.airTemperature}°C
                安全及警告評估: ${station.safetyStatus}
                
                請就以下三點給出具體且熱情的專業報告：
                1. 航行與磯釣安全係數（是否需要全套膠鞋、救生衣配戴建議及地理危險警示）
                2. 魚類咬餌活性影響（水溫與潮汐如何影響該海域之大型真鯛、黑鯛或鱸魚的索餌偏好）
                3. 今日推薦釣法與推薦時機點（如漂浮、重磯或路亞等）
                
                文字請精煉，使用繁體中文，並請帶有『智慧海釣、永續海洋』之推廣語句。
            """.trimIndent()

            val response = withContext(Dispatchers.IO) {
                GeminiClient.getGeminiReport(prompt, "你是一位經驗非常豐富的海釣專家與海洋學家，熱愛推廣永續海洋政策。")
            }
            aiWeatherReport.value = response
            isGeneratingReport.value = false
        }
    }

    // --- Navigation Actions ---
    fun moveUserLocation(latDelta: Double, lngDelta: Double) {
        userLatitude.value = round((userLatitude.value + latDelta) * 10000) / 10000.0
        userLongitude.value = round((userLongitude.value + lngDelta) * 10000) / 10000.0
        userBearing.value = (userBearing.value + (Math.random() - 0.5) * 30 + 360) % 360
        userSpeed.value = max(1.0, userSpeed.value + (Math.random() - 0.5) * 2)
        checkGeofence(userLatitude.value, userLongitude.value)
    }

    fun jumpToLocation(lat: Double, lng: Double) {
        userLatitude.value = lat
        userLongitude.value = lng
        checkGeofence(lat, lng)
    }

    private fun checkGeofence(lat: Double, lng: Double) {
        var breached = false
        var msg = ""
        var zoneName = ""
        
        for (zone in geofenceZones) {
            val dist = calculateDistance(lat, lng, zone.latitude, zone.longitude)
            if (dist <= zone.radiusMeters) {
                breached = true
                zoneName = zone.name
                msg = "※【越界警告】您已進入「${zone.name}」地理圍欄內！\n" +
                      "當前距離中心僅 ${round(dist)} 公尺 (安全限制半徑: ${zone.radiusMeters}公尺)\n" +
                      "危險因素: ${zone.description}\n" +
                      "⚠️ 警告: 海象險惡，請立刻回航至安全水域！"
                break
            } else if (dist <= zone.radiusMeters + 200.0) {
                // Warning near
                breached = false
                zoneName = "接近中 - " + zone.name
                msg = "🔔【接近警示】您正靠近「${zone.name}」安全屏障！\n" +
                      "距離風浪危險帶僅 ${round(dist)} 公尺。請主動修正航偏，確保安全。"
            }
        }
        
        isGeofenceBreached.value = breached
        breachMessage.value = msg
        breachZoneName.value = zoneName
    }

    // Calculate distance on sphere
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // --- Catch Logging Database Action ---
    fun addCatchLog(
        fishName: String,
        weight: Double,
        length: Double,
        locationName: String,
        tide: String,
        windSpeed: Double,
        waveHeight: Double,
        temp: Double,
        rating: Int,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newCatch = CatchEntity(
                fishName = fishName,
                weight = weight,
                length = length,
                locationName = locationName,
                tide = tide,
                windSpeed = windSpeed,
                waveHeight = waveHeight,
                temperature = temp,
                rating = rating,
                notes = notes,
                isSynced = false // Initial offline
            )
            repository.insertCatch(newCatch)
            updateUnsyncedCount()
        }
    }

    fun deleteCatch(catch: CatchEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCatch(catch)
            updateUnsyncedCount()
        }
    }

    fun updateUnsyncedCount() {
        viewModelScope.launch {
            val unsyncedList = withContext(Dispatchers.IO) {
                repository.getUnsyncedCatches()
            }
            unsyncedCount.value = unsyncedList.size
        }
    }

    // Sync to AWS Cloud AI Simulator (Meets catch log and cloud uploading in flow diagram)
    fun syncCatchesToCloud() {
        viewModelScope.launch {
            val unsyncedList = withContext(Dispatchers.IO) {
                repository.getUnsyncedCatches()
            }
            if (unsyncedList.isEmpty()) {
                cloudAiAnalysisResponse.value = "目前沒有任何需要同步的離線漁獲日誌。"
                return@launch
            }

            isSyncing.value = true
            cloudAiAnalysisResponse.value = "正在打包 ${unsyncedList.size} 筆離線海釣點日誌、GPS風速與歷史潮汐，上傳至 AWS S3 / API Gateway ..."
            
            kotlinx.coroutines.delay(2000) // Simulated AWS transmission delay

            // Prepare Gemini metadata prompt to do AWS AI analysis personalization
            val listSummary = unsyncedList.joinToString("\n") {
                "- 在 [${it.locationName}] 捕獲 [${it.fishName}] 重 ${it.weight}kg, 長 ${it.length}cm, 當時潮汐 [${it.tide}], 浪高 ${it.waveHeight}m, 風速 ${it.windSpeed}km/h"
            }

            val prompt = """
                您是「AWS海洋AI分析引擎」大模型。用戶剛剛將當地的海釣數據從離線終端上傳同步至雲端S3與Lambda伺服器。
                以下為用戶上傳的最新海釣漁獲日誌：
                $listSummary
                
                請綜合以上數據進行機器學習個人化優化：
                1. 診斷大咬時效規律（指出用戶在哪種潮汐或溫度下獲得最好的漁獲效率）
                2. 生成專屬個人化釣點模型優化建議（對於用戶所釣的魚種，提出最適合的泳層水溫和路亞假餌動作）
                3. 給出一個熱情的『雲端智慧，永續生態』的分析結語。
                
                請使用有條理的繁體中文，格式清晰美觀。
            """.trimIndent()

            val aiResponse = withContext(Dispatchers.IO) {
                GeminiClient.getGeminiReport(prompt, "您是部署在AWS雲端的智慧海洋大數據分析助手，專門做大咬時機預測和釣法優化指導。")
            }

            // Mark database records as synced
            withContext(Dispatchers.IO) {
                for (item in unsyncedList) {
                    repository.updateCatch(item.copy(isSynced = true))
                }
            }

            cloudAiAnalysisResponse.value = aiResponse
            isSyncing.value = false
            updateUnsyncedCount()
        }
    }

    fun clearCloudAiResponse() {
        cloudAiAnalysisResponse.value = ""
    }
}

data class GeofenceZone(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val description: String
)
