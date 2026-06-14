# Plan 01-01 Summary: Data Layer Foundation

## What Was Done

### Task 1: Add dependencies to version catalog and build.gradle
- Added Room 2.7.1, Ktor 3.1.3, DataStore 1.1.7, KSP 2.3.20-2.0.1, and kotlinx-serialization 1.8.1 to `libs.versions.toml`
- Added all corresponding library entries (room-runtime, room-ktx, room-compiler, ktor-client-core, ktor-client-okhttp, ktor-client-content-negotiation, ktor-serialization-kotlinx-json, datastore-preferences, kotlinx-serialization-json)
- Added KSP plugin to `[plugins]` section
- Updated root `build.gradle.kts` with KSP plugin declaration (`apply false`)
- Updated app `build.gradle.kts` with KSP plugin and all library dependencies

### Task 2: Create Room database entities, DAO, and database class
- **PrayerTimeEntity.kt**: `@Entity(tableName = "prayer_times")` with fields: id, date, fajr, dhuhr, asr, maghrib, isha, latitude, longitude. Composite unique index on (date, latitude, longitude)
- **PrayerTimeDao.kt**: `@Dao` interface with Flow-based queries for single day and month, insert with REPLACE strategy, and cleanup for old entries
- **SalatDatabase.kt**: `@Database` singleton with double-checked locking via `getInstance(context)`

### Task 3: Create Ktor API client, response models, repository, and DataStore preferences
- **AladhanModels.kt**: `@Serializable` data classes matching Aladhan API v1 response (AladhanResponse, AladhanMonthResponse, AladhanData, AladhanTimings, AladhanDate, GregorianDate)
- **AladhanApiService.kt**: Ktor `HttpClient` with OkHttp engine and ContentNegotiation (JSON, ignoreUnknownKeys). Endpoints: `getTimingsByDate()` and `getTimingsForMonth()` targeting `api.aladhan.com/v1/`
- **UserPreferences.kt**: DataStore Preferences with keys for LATITUDE, LONGITUDE, IS_LOCKED, LAST_PRAYER_NAME. Flow-based getters and suspend setters
- **PrayerTimeRepository.kt**: Bridges DAO and API. Converts DD-MM-YYYY (API) → yyyy-MM-dd (DB). Strips timezone suffixes from time strings. Methods: `fetchAndCacheTodayPrayerTimes()`, `fetchAndCacheMonthlyPrayerTimes()`, `getTodayPrayerTimes()`, `cleanupOldEntries()`
- **AndroidManifest.xml**: Added `INTERNET` permission

## Verification

- [x] Room entity has `@Entity`, DAO has `@Dao`, Database has `@Database`
- [x] Ktor client targets correct Aladhan API endpoints (`api.aladhan.com`)
- [x] Repository bridges DAO and API correctly (constructor injection of both)
- [x] INTERNET permission declared in AndroidManifest.xml
- [x] DataStore preferences for location and lock state exist
- [x] All Kotlin files have correct imports and annotations

## Files Modified

| File | Action |
|------|--------|
| `app/gradle/libs.versions.toml` | Modified — added versions, libraries, plugins |
| `app/build.gradle.kts` | Modified — added KSP plugin |
| `app/app/build.gradle.kts` | Modified — added KSP plugin + dependencies |
| `app/app/src/main/java/.../data/local/PrayerTimeEntity.kt` | Created |
| `app/app/src/main/java/.../data/local/PrayerTimeDao.kt` | Created |
| `app/app/src/main/java/.../data/local/SalatDatabase.kt` | Created |
| `app/app/src/main/java/.../data/remote/AladhanModels.kt` | Created |
| `app/app/src/main/java/.../data/remote/AladhanApiService.kt` | Created |
| `app/app/src/main/java/.../data/preferences/UserPreferences.kt` | Created |
| `app/app/src/main/java/.../data/repository/PrayerTimeRepository.kt` | Created |
| `app/app/src/main/AndroidManifest.xml` | Modified — added INTERNET permission |

## Commits

1. `39c850c` — feat(data): add Room, Ktor, DataStore, KSP dependencies to version catalog and build files
2. `c0c54b6` — feat(data): add Room entity, DAO, and database for prayer times
3. `5bf5858` — feat(data): add Ktor API client, models, repository, DataStore preferences, and INTERNET permission

## Duration

~4 minutes

## Status

✅ **COMPLETE** — Data layer fully implemented and ready for scheduling and locking phases.
