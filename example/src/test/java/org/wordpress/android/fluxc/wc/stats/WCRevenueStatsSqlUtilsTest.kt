package org.wordpress.android.fluxc.wc.stats

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.persistence.WCRevenueStatsSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCRevenueStatsSqlUtilsTest {
    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCRevenueStatsModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testSimpleInsertionAndRetrieval() {
        // insert a first stats entry and verify that it is stored correctly
        val revenueStatsModel = WCStatsTestUtils.generateSampleRevenueStatsModel()
        WCRevenueStatsSqlUtils.insertOrUpdateStats(revenueStatsModel)

        with(WellSql.select(WCRevenueStatsModel::class.java).asModel) {
            assertEquals(1, size)
            assertEquals("day", first().interval)
            assertEquals(revenueStatsModel.startDate, first().startDate)
            assertEquals(revenueStatsModel.endDate, first().endDate)
        }

        // Create a second stats entry for this site with same start & end date but with different interval
        val revenueStatsModel2 =
                WCStatsTestUtils.generateSampleRevenueStatsModel(interval = "month", data = "fake-data")
        WCRevenueStatsSqlUtils.insertOrUpdateStats(revenueStatsModel2)

        with(WellSql.select(WCRevenueStatsModel::class.java).asModel) {
            assertEquals(2, size)
            assertEquals("day", first().interval)
            assertEquals(revenueStatsModel.startDate, first().startDate)
            assertEquals(revenueStatsModel.endDate, first().endDate)
            assertEquals("month", get(1).interval)
            assertEquals(revenueStatsModel2.startDate, get(1).startDate)
            assertEquals(revenueStatsModel2.endDate, get(1).endDate)
        }

        // Create a third stats entry for this site with same interval but different start & end date
        val revenueStatsModel3 =
                WCStatsTestUtils.generateSampleRevenueStatsModel(
                        interval = "day", data = "fake-data2",
                        startDate = "2019-07-07 00:00:00", endDate = "2019-07-13 23:59:59"
                )
        WCRevenueStatsSqlUtils.insertOrUpdateStats(revenueStatsModel3)

        with(WellSql.select(WCRevenueStatsModel::class.java).asModel) {
            assertEquals(3, size)
            assertEquals("day", first().interval)
            assertEquals(revenueStatsModel.startDate, first().startDate)
            assertEquals(revenueStatsModel.endDate, first().endDate)
            assertEquals("month", get(1).interval)
            assertEquals(revenueStatsModel2.startDate, get(1).startDate)
            assertEquals(revenueStatsModel2.endDate, get(1).endDate)
            assertEquals("day", get(2).interval)
            assertEquals(revenueStatsModel3.startDate, get(2).startDate)
            assertEquals(revenueStatsModel3.endDate, get(2).endDate)
        }

        // Overwrite an existing entry and verify that update is happening correctly
        val revenueStatsModel4 = WCStatsTestUtils.generateSampleRevenueStatsModel()
        WCRevenueStatsSqlUtils.insertOrUpdateStats(revenueStatsModel4)

        with(WellSql.select(WCRevenueStatsModel::class.java).asModel) {
            assertEquals(3, size)
            assertEquals("day", first().interval)
            assertEquals(revenueStatsModel.startDate, first().startDate)
            assertEquals(revenueStatsModel.endDate, first().endDate)
            assertEquals("month", get(1).interval)
            assertEquals(revenueStatsModel2.startDate, get(1).startDate)
            assertEquals(revenueStatsModel2.endDate, get(1).endDate)
            assertEquals("day", get(2).interval)
            assertEquals(revenueStatsModel3.startDate, get(2).startDate)
            assertEquals(revenueStatsModel3.endDate, get(2).endDate)
        }

        // Add another "day" entry, but for another site
        val revenueStatsModel5 = WCStatsTestUtils.generateSampleRevenueStatsModel(localSiteId = 8)
        WCRevenueStatsSqlUtils.insertOrUpdateStats(revenueStatsModel5)

        with(WellSql.select(WCRevenueStatsModel::class.java).asModel) {
            assertEquals(4, size)
            assertEquals("day", first().interval)
            assertEquals(revenueStatsModel.startDate, first().startDate)
            assertEquals(revenueStatsModel.endDate, first().endDate)
            assertEquals("month", get(1).interval)
            assertEquals(revenueStatsModel2.startDate, get(1).startDate)
            assertEquals(revenueStatsModel2.endDate, get(1).endDate)
            assertEquals("day", get(2).interval)
            assertEquals(revenueStatsModel3.startDate, get(2).startDate)
            assertEquals(revenueStatsModel3.endDate, get(2).endDate)
            assertEquals("day", get(3).interval)
            assertEquals(revenueStatsModel5.localSiteId, get(3).localSiteId)
            assertEquals(revenueStatsModel5.startDate, get(3).startDate)
            assertEquals(revenueStatsModel5.endDate, get(3).endDate)
        }
    }

    @Test
    fun testGetRawStatsForSiteAndUnit() {
        // revenue stats model for current day
        val currentDayStatsModel = WCStatsTestUtils.generateSampleRevenueStatsModel(interval = "hour")
        val site = SiteModel().apply { id = currentDayStatsModel.localSiteId }
        WCRevenueStatsSqlUtils.insertOrUpdateStats(currentDayStatsModel)

        // revenue stats model for this week
        val currentWeekStatsModel =
                WCStatsTestUtils.generateSampleRevenueStatsModel(
                        interval = "day", data = "fake-data", startDate = "2019-07-07", endDate = "2019-07-09"
                )
        WCRevenueStatsSqlUtils.insertOrUpdateStats(currentWeekStatsModel)

        // revenue stats model for this month
        val currentMonthStatsModel =
                WCStatsTestUtils.generateSampleRevenueStatsModel(
                        interval = "day", data = "fake-data", startDate = "2019-07-01", endDate = "2019-07-09"
                )
        WCRevenueStatsSqlUtils.insertOrUpdateStats(currentMonthStatsModel)

        // current day stats for alternate site
        val site2 = SiteModel().apply { id = 8 }
        val altSiteOrderStatsModel = WCStatsTestUtils.generateSampleRevenueStatsModel(
                localSiteId = site2.id, interval = "hour"
        )
        WCRevenueStatsSqlUtils.insertOrUpdateStats(altSiteOrderStatsModel)

        val currentDayStats = WCRevenueStatsSqlUtils.getRawStatsForSiteIntervalAndDate(
                site, OrderStatsApiUnit.HOUR, currentDayStatsModel.startDate, currentDayStatsModel.endDate
        )
        assertNotNull(currentDayStats)
        with(currentDayStats) {
            assertEquals("hour", interval)
            assertEquals(currentDayStatsModel.startDate, startDate)
            assertEquals(currentDayStatsModel.endDate, endDate)
            assertEquals(currentDayStatsModel.localSiteId, localSiteId)
        }

        val currentWeekStats = WCRevenueStatsSqlUtils.getRawStatsForSiteIntervalAndDate(
                site, OrderStatsApiUnit.DAY, currentWeekStatsModel.startDate, currentWeekStatsModel.endDate
        )
        assertNotNull(currentWeekStats)
        with(currentWeekStats) {
            assertEquals("day", interval)
            assertEquals(currentWeekStatsModel.startDate, startDate)
            assertEquals(currentWeekStatsModel.endDate, endDate)
            assertEquals(currentWeekStatsModel.localSiteId, localSiteId)
        }

        val currentMonthStats = WCRevenueStatsSqlUtils.getRawStatsForSiteIntervalAndDate(
                site, OrderStatsApiUnit.DAY, currentMonthStatsModel.startDate, currentMonthStatsModel.endDate
        )
        assertNotNull(currentMonthStats)
        with(currentMonthStats) {
            assertEquals("day", interval)
            assertEquals(currentMonthStatsModel.startDate, startDate)
            assertEquals(currentMonthStatsModel.endDate, endDate)
            assertEquals(currentMonthStatsModel.localSiteId, localSiteId)
        }

        val altCurrentDayStats = WCRevenueStatsSqlUtils.getRawStatsForSiteIntervalAndDate(
                site2, OrderStatsApiUnit.HOUR, altSiteOrderStatsModel.startDate, altSiteOrderStatsModel.endDate
        )
        assertNotNull(altCurrentDayStats)
        with(altCurrentDayStats) {
            assertEquals("hour", interval)
            assertEquals(altSiteOrderStatsModel.startDate, startDate)
            assertEquals(altSiteOrderStatsModel.endDate, endDate)
            assertEquals(altSiteOrderStatsModel.localSiteId, localSiteId)
        }

        val nonExistentSite = WCRevenueStatsSqlUtils.getRawStatsForSiteIntervalAndDate(
                SiteModel().apply { id = 88 },
                OrderStatsApiUnit.DAY, currentDayStatsModel.startDate, currentDayStatsModel.endDate
        )
        assertNull(nonExistentSite)

        val missingData = WCRevenueStatsSqlUtils.getRawStatsForSiteIntervalAndDate(
                site, OrderStatsApiUnit.YEAR, currentDayStatsModel.startDate, currentDayStatsModel.endDate)
        assertNull(missingData)
    }
}
