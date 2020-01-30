package com.solutions.grutne.flovind

import com.solutions.grutne.flovind.utils.FloVindDateUtils
import org.junit.Test
import org.threeten.bp.format.DateTimeParseException

class FloVindDateUtilsTest {

    /**
     * getRawDateTimeInMillis ()
     * */
    @Test
    fun getRawDateTimeInMillis_assert_Jan_01_1970_at_01_00_is_0() {
        val dateString = "1970-01-01T01:00:00+01:00"
        val dateStringInMillis = 0
        assert(FloVindDateUtils.getRawDateTimeInMillis(rawDate = dateString).compareTo(dateStringInMillis) == 0)
    }

    @Test
    fun getRawDateTimeInMillis_assert_Jan_21_2020_at_03_04_is_1579745040000() {
        val dateString = "2020-01-23T03:04:00+01:00"
        val dateStringInMillis = 1579745040000
        assert(FloVindDateUtils.getRawDateTimeInMillis(rawDate = dateString).compareTo(dateStringInMillis) == 0)
    }

    @Test
    fun getRawDateTimeInMillis_assert_Oct_28_2043_at_19_31_is_2329669860000() {
        val dateString = "2043-10-28T19:31:00+01:00"
        val dateStringInMillis = 2329669860000
        assert(FloVindDateUtils.getRawDateTimeInMillis(rawDate = dateString).compareTo(dateStringInMillis) == 0)
    }

    @Test(expected = DateTimeParseException::class)
    fun getRawDateTimeException() {
        val unsupportedFormat = "1988-10-12 19:34"
        FloVindDateUtils.getRawDateTimeInMillis(unsupportedFormat)
    }

    /**
     * getPersistentDatePlusOne ()
     */

    @Test
    fun getPersistentDatePlusOne_assert_last_day_of_june_returns_first_day_of_july() {
        val originalDate = "1999-06-30"
        val originalPlusOne = "1999-07-01"
        assert(FloVindDateUtils.getPersistentDatePlusOne(originalDate) == originalPlusOne)
    }

    @Test
    fun getPersistentDatePlusOne_assert_last_day_of_year_returns_first_day_of_following_year() {
        val originalDate = "2019-12-31"
        val originalPlusOne = "2020-01-01"
        assert(FloVindDateUtils.getPersistentDatePlusOne(originalDate) == originalPlusOne)
    }

    @Test
    fun getPersistentDatePlusOne_assert_feb_29_in_leap_year_2020() {
        val originalDate = "2020-02-28"
        val originalPlusOne = "2020-02-29"
        assert(FloVindDateUtils.getPersistentDatePlusOne(originalDate) == originalPlusOne)
    }

    /**
     * getPersistentDateMinusOne ()
     */
    @Test
    fun getPersistentDateMinusOne_assert_first_day_july_returns_last_day_june() {
        val originalDate = "1999-07-01"
        val originalMinusOne = "1999-06-30"
        assert(FloVindDateUtils.getPersistentDateMinusOne(originalDate) == originalMinusOne)
    }

    @Test
    fun getPersistentDateMinusOne_assert_first_day_of_year_returns_last_day__of_lat_year() {
        val originalDate = "2020-01-01"
        val originalMinusOne = "2019-12-31"
        assert(FloVindDateUtils.getPersistentDateMinusOne(originalDate) == originalMinusOne)
    }

    @Test
    fun getPersistentDatePlusOne_assert_feb28_from_feb29_in_leap_year_2020() {
        val originalMinusOne = "2020-02-28"
        val originalDate = "2020-02-29"
        assert(FloVindDateUtils.getPersistentDateMinusOne(originalDate) == originalMinusOne)
    }

    /**
     * getPersistentDateInMillis()
     * */
    @Test
    fun getPersistentDateInMillis_assert_start_of_19700102_is_82800000() {
        val persistentDate = "1970-01-02"
        val dateInMillis = 82800000

        assert(FloVindDateUtils.getPersistentDateInMillis(persistentDate = persistentDate).compareTo(dateInMillis) == 0)
    }

    @Test
    fun getPersistentDateInMillis_assert_start_of_20200229_1582930800000() {
        val persistentDate = "2020-02-29"
        val dateInMillis = 1582930800000

        assert(FloVindDateUtils.getPersistentDateInMillis(persistentDate = persistentDate).compareTo(dateInMillis) == 0)
    }


    /**
     * isTomorrowLast()
     * */
    @Test
    fun isDateLastInBatch_assert_feb2_NOT_last_when_now_is_feb4(){
        val date = "2020-02-02"
        val now = 1580770800000

        assert(!FloVindDateUtils.isDateLastInBatch(date, now))
    }
    @Test
    fun isDateLastInBatch_assert_feb14_IS_last_when_now_is_feb4(){
        val date = "2020-02-14"
        val now = 1580770800000

        assert(FloVindDateUtils.isDateLastInBatch(date, now))
    }
}