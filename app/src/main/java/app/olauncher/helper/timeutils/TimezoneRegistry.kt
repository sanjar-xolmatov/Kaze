package app.olauncher.helper.timeutils

data class CityZone(val city: String, val zoneId: String)

object TimezoneRegistry {

    private val cities = listOf(
        CityZone("Tokyo", "Asia/Tokyo"),
        CityZone("London", "Europe/London"),
        CityZone("New York", "America/New_York"),
        CityZone("Los Angeles", "America/Los_Angeles"),
        CityZone("Chicago", "America/Chicago"),
        CityZone("Sydney", "Australia/Sydney"),
        CityZone("Dubai", "Asia/Dubai"),
        CityZone("Mumbai", "Asia/Kolkata"),
        CityZone("Delhi", "Asia/Kolkata"),
        CityZone("New Delhi", "Asia/Kolkata"),
        CityZone("Singapore", "Asia/Singapore"),
        CityZone("Hong Kong", "Asia/Hong_Kong"),
        CityZone("Shanghai", "Asia/Shanghai"),
        CityZone("Beijing", "Asia/Shanghai"),
        CityZone("Seoul", "Asia/Seoul"),
        CityZone("Paris", "Europe/Paris"),
        CityZone("Berlin", "Europe/Berlin"),
        CityZone("Moscow", "Europe/Moscow"),
        CityZone("Istanbul", "Europe/Istanbul"),
        CityZone("Honolulu", "Pacific/Honolulu"),
        CityZone("Anchorage", "America/Anchorage"),
        CityZone("Denver", "America/Denver"),
        CityZone("Phoenix", "America/Phoenix"),
        CityZone("Toronto", "America/Toronto"),
        CityZone("Mexico City", "America/Mexico_City"),
        CityZone("Sao Paulo", "America/Sao_Paulo"),
        CityZone("Buenos Aires", "America/Argentina/Buenos_Aires"),
        CityZone("Cairo", "Africa/Cairo"),
        CityZone("Lagos", "Africa/Lagos"),
        CityZone("Johannesburg", "Africa/Johannesburg"),
        CityZone("Nairobi", "Africa/Nairobi"),
        CityZone("Jakarta", "Asia/Jakarta"),
        CityZone("Bangkok", "Asia/Bangkok"),
    )

    fun resolve(location: String): CityZone? =
        cities.find { it.city.equals(location.trim(), ignoreCase = true) }
}
