package com.github.zhitron.universal;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Moment类的JUnit 4单元测试
 */
public class MomentTest {
    @Before
    public void setUp() {
        Moment moment = Moment.now();
    }

    @Test
    public void testBasicCreation() {
        // 测试now()方法
        Moment now = Moment.now();
        assertNotNull(now);
        assertTrue(now.getTimestamp() > 0);
        // 测试ofDate方法
        Moment dateMoment = Moment.ofDate(20250416);
        assertEquals(2025, dateMoment.getYearAsNum());
        assertEquals(4, dateMoment.getMonthAsNum());
        assertEquals(16, dateMoment.getDayAsNum());
        // 测试ofDateTime方法
        Moment dateTimeMoment = Moment.ofDateTime(20250416132647L);
        assertEquals(2025, dateTimeMoment.getYearAsNum());
        assertEquals(4, dateTimeMoment.getMonthAsNum());
        assertEquals(16, dateTimeMoment.getDayAsNum());
        assertEquals(13, dateTimeMoment.getHourAsNum());
        assertEquals(26, dateTimeMoment.getMinuteAsNum());
        assertEquals(47, dateTimeMoment.getSecondAsNum());
    }

    @Test
    public void testLeapYear() {
        // 测试闰年判断
        assertEquals(1, Moment.leap(2024));  // 闰年
        assertEquals(0, Moment.leap(2025));  // 平年
        assertEquals(1, Moment.leap(2000));  // 世纪闰年
        assertEquals(0, Moment.leap(1900));  // 世纪平年
    }

    @Test
    public void testDaysInMonth() {
        // 测试每月天数
        assertEquals(31, Moment.days(2025, 1));   // 一月
        assertEquals(28, Moment.days(2025, 2));   // 平年二月
        assertEquals(29, Moment.days(2024, 2));   // 闰年二月
        assertEquals(31, Moment.days(2025, 3));   // 三月
        assertEquals(30, Moment.days(2025, 4));   // 四月
    }

    @Test
    public void testDaysInYear() {
        // 测试每年天数
        assertEquals(365, Moment.days(2025));  // 平年
        assertEquals(366, Moment.days(2024));  // 闰年
    }

    @Test
    public void testBasicTimeOperations() {
        // 测试基本时间设置
        Moment m = new Moment();
        m.setYear(2025).setMonth(4).setDay(16).setHour(13).setMinute(26).setSecond(47);
        assertEquals(2025, m.getYearAsNum());
        assertEquals(4, m.getMonthAsNum());
        assertEquals(16, m.getDayAsNum());
        assertEquals(13, m.getHourAsNum());
        assertEquals(26, m.getMinuteAsNum());
        assertEquals(47, m.getSecondAsNum());
        // 测试时间格式化
        assertEquals("20250416", m.getDateAsStr());
        assertEquals("132647", m.getTimeAsStr());
        assertEquals(20250416132647L, m.getDateTimeAsNum());
    }

    @Test
    public void testAddOperations() {
        // 测试加法操作
        Moment m = Moment.ofDate(20250416);
        // 加一天
        m.addDay(1);
        assertEquals(20250417, m.getDateAsNum());
        // 加一个月
        m.addMonth(1);
        assertEquals(20250517, m.getDateAsNum());
        // 加一年
        m.addYear(1);
        assertEquals(20260517, m.getDateAsNum());
        // 加一小时
        m.addHour(1);
        assertEquals(20260517010000L, m.getDateTimeAsNum());
        // 加一分钟
        m.addMinute(1);
        assertEquals(20260517010100L, m.getDateTimeAsNum());
        // 加一秒
        m.addSecond(1);
        assertEquals(20260517010101L, m.getDateTimeAsNum());
    }

    @Test
    public void testEdgeCases() {
        // 测试月末日期
        Moment m = Moment.ofDate(20250131);  // 1月31日
        m.addMonth(1);  // 加一个月应该是2月28日(2025年不是闰年)
        assertEquals(20250228, m.getDateAsNum());
        // 测试年末
        m = Moment.ofDate(20251231);  // 12月31日
        m.addDay(1);  // 加一天应该是2026年1月1日
        assertEquals(20260101, m.getDateAsNum());
        // 测试月末加月份
        m = Moment.ofDate(20240131);  // 2024年1月31日(闰年)
        m.addMonth(1);  // 加一个月应该是2月29日
        assertEquals(20240229, m.getDateAsNum());
    }

    @Test
    public void testExtremeValues() {
        // 测试极限值 Integer.MAX_VALUE
        Moment m = Moment.ofDate(20250416);
        // 加 Integer.MAX_VALUE 天
        m.addDay(Integer.MAX_VALUE);
        assertTrue(m.getTimestamp() > 0);
        // 测试加 Integer.MAX_VALUE 个月
        Moment m2 = Moment.ofDate(20250416);
        m2.addMonth(Integer.MAX_VALUE);
        assertTrue(m2.getYearAsNum() > 2025);
    }

    @Test
    public void testTimestampConversion() {
        // 测试时间戳转换
        long currentMillis = System.currentTimeMillis();
        Moment m = Moment.of(currentMillis);
        assertEquals(currentMillis, m.getTimestamp());
        // 测试与Java Time API的互操作性
        LocalDateTime localDateTime = m.toLocalDateTime();
        Moment m2 = Moment.of(localDateTime);
        assertEquals(m.getYearAsNum(), m2.getYearAsNum());
        assertEquals(m.getMonthAsNum(), m2.getMonthAsNum());
        assertEquals(m.getDayAsNum(), m2.getDayAsNum());
        assertEquals(m.getHourAsNum(), m2.getHourAsNum());
        assertEquals(m.getMinuteAsNum(), m2.getMinuteAsNum());
        assertEquals(m.getSecondAsNum(), m2.getSecondAsNum());
        // 测试与Date的互操作性
        Date date = m.toDate();
        Moment m3 = Moment.of(date);
        assertEquals(m.getTimestamp(), m3.getTimestamp());
    }

    @Test
    public void testStringFormat() {
        // 测试字符串格式化
        Moment m = Moment.ofDate(20250416);
        m.setHour(13).setMinute(26).setSecond(47).setMillis(123);
        assertEquals("2025-04-16T13:26:47.123Z", m.toString());
        // 测试自定义格式
        assertEquals("2025/04/16", m.toString("yyyy/MM/dd"));
        assertEquals("2025-04-16 13:26:47", m.toString("yyyy-MM-dd HH:mm:ss"));
        assertEquals("20250416132647123", m.getDateTimeAsStr() + m.getMillisAsStr());
    }

    @Test
    public void testQuarterOperations() {
        // 测试季度操作
        Moment m = new Moment();
        // 设置第一季度初
        m.setQuarterStart(1);
        assertEquals(1, m.getMonthAsNum());
        assertEquals(1, m.getDayAsNum());
        // 设置第二季度末
        m.setQuarterEnd(2);
        assertEquals(6, m.getMonthAsNum());
        assertEquals(30, m.getDayAsNum());
        // 设置第三季度初
        m.setQuarterStart(3);
        assertEquals(7, m.getMonthAsNum());
        assertEquals(1, m.getDayAsNum());
        // 设置第四季度末
        m.setQuarterEnd(4);
        assertEquals(12, m.getMonthAsNum());
        assertEquals(31, m.getDayAsNum());
    }

    @Test
    public void testComparison() {
        // 测试比较操作
        Moment m1 = Moment.ofDate(20250416);
        Moment m2 = Moment.ofDate(20250417);
        assertTrue(m1.compareTo(m2) < 0);
        assertTrue(m2.compareTo(m1) > 0);
    }

    @Test
    public void testMicrosAndNanos() {
        // 测试微秒和纳秒操作
        Moment m = new Moment(System.currentTimeMillis(), 500, 500);
        assertEquals(500, m.getMicrosAsNum());
        assertEquals(500, m.getNanosAsNum());
        m.setMicros(999);
        m.setNanos(999);
        assertEquals(999, m.getMicrosAsNum());
        assertEquals(999, m.getNanosAsNum());
        // 测试微秒和纳秒的极限值
        m.setMicros(999);
        assertEquals(999, m.getMicrosAsNum()); // 应该被限制在999
        m.setNanos(999);
        assertEquals(999, m.getNanosAsNum()); // 应该被限制在999
    }

    @Test
    public void testBoundaryConditions() {
        // 测试边界条件
        // 测试最小日期
        Moment minMoment = new Moment(Long.MIN_VALUE, 0, 0);
        minMoment.getTimestamp();
        // 测试最大日期
        Moment maxMoment = new Moment(Long.MAX_VALUE, 999, 999);
        maxMoment.getTimestamp();
        // 测试跨世纪日期
        Moment m = Moment.ofDate(19991231);
        m.addDay(1);
        assertEquals(20000101, m.getDateAsNum());
        // 测试闰年世纪日期
        m = Moment.ofDate(20000228);
        m.addDay(1);
        assertEquals(20000229, m.getDateAsNum()); // 2000年是闰年
        m.addDay(1);
        assertEquals(20000301, m.getDateAsNum());
    }

    @Test
    public void testExtremeAddOperations() {
        // 测试极端加法操作
        Moment m = Moment.ofDate(20250416);
        // 测试加很大的年数
        m.addYear(1000000L);
        assertEquals(2025 + 1000000, m.getYearAsNum());
        // 测试减很大的年数
        m.addYear(-1000000L);
        assertEquals(2025, m.getYearAsNum());
        // 测试加很大的月数
        m.addMonth(1000000L);
        assertTrue(m.getYearAsNum() > 2025);
        // 测试加很大的天数
        Moment m2 = Moment.ofDate(20250416);
        m2.addDay(1000000L);
        assertTrue(m2.getYearAsNum() > 2025);
    }

    @Test
    public void testSetOperations() {
        // 测试设置操作
        Moment m = new Moment();
        // 测试设置年份
        m.setYear(2025);
        assertEquals(2025, m.getYearAsNum());
        // 测试设置月份
        m.setMonth(4);
        assertEquals(4, m.getMonthAsNum());
        // 测试设置日期
        m.setDay(16);
        assertEquals(16, m.getDayAsNum());
        // 测试设置小时
        m.setHour(13);
        assertEquals(13, m.getHourAsNum());
        // 测试设置分钟
        m.setMinute(26);
        assertEquals(26, m.getMinuteAsNum());
        // 测试设置秒
        m.setSecond(47);
        assertEquals(47, m.getSecondAsNum());
        // 测试设置毫秒
        m.setMillis(123);
        assertEquals(123, m.getMillisAsNum());
    }

    @Test
    public void testYearStartEnd() {
        // 测试年初和年末设置
        Moment m = new Moment();
        // 设置年初
        m.setYearStart(2025);
        assertEquals(2025, m.getYearAsNum());
        assertEquals(1, m.getMonthAsNum());
        assertEquals(1, m.getDayAsNum());
        // 设置年末
        m.setYearEnd(2025);
        assertEquals(2025, m.getYearAsNum());
        assertEquals(12, m.getMonthAsNum());
        assertEquals(31, m.getDayAsNum());
    }

    @Test
    public void testMonthStartEnd() {
        // 测试月初和月末设置
        Moment m = new Moment();
        m.setYear(2025);
        // 设置月初
        m.setMonthStart(2); // 2月
        assertEquals(2, m.getMonthAsNum());
        assertEquals(1, m.getDayAsNum());
        // 设置月末
        m.setMonthEnd(2); // 2月
        assertEquals(2, m.getMonthAsNum());
        assertEquals(28, m.getDayAsNum()); // 2025年不是闰年
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIntegerOverflowInYear() {
        // 测试年份整数溢出
        Moment m = Moment.ofDate(20250416);
        m.addYear((long) Integer.MAX_VALUE + 1);
    }

    @Test
    public void testGetNumberOfDays() {
        // 测试获取年和月的天数
        Moment m = new Moment();
        m.setYear(2025);
        assertEquals(365, m.getNumberOfDaysYear());
        m.setMonth(2); // 2月
        assertEquals(28, m.getNumberOfDaysMonth()); // 2025年不是闰年
        m.setYear(2024); // 闰年
        assertEquals(366, m.getNumberOfDaysYear());
        assertEquals(29, m.getNumberOfDaysMonth());
    }

    @Test
    public void testYearMonthDayArithmetic() {
        // 测试年份加减
        Moment m1 = Moment.ofDate(20250416);
        m1.addYear(5);
        assertEquals(2030, m1.getYearAsNum());
        assertEquals(20300416, m1.getDateAsNum());
        m1.addYear(-3);
        assertEquals(2027, m1.getYearAsNum());
        assertEquals(20270416, m1.getDateAsNum());
        // 测试月份加减
        Moment m2 = Moment.ofDate(20250416);
        m2.addMonth(8);
        assertEquals(2025, m2.getYearAsNum());
        assertEquals(12, m2.getMonthAsNum());
        assertEquals(16, m2.getDayAsNum());
        m2.addMonth(5);
        assertEquals(2026, m2.getYearAsNum());
        assertEquals(5, m2.getMonthAsNum());
        assertEquals(16, m2.getDayAsNum());
        // 测试日期加减
        Moment m3 = Moment.ofDate(20250416);
        m3.addDay(15);
        assertEquals(2025, m3.getYearAsNum());
        assertEquals(5, m3.getMonthAsNum());
        assertEquals(1, m3.getDayAsNum());
        m3.addDay(-5);
        assertEquals(2025, m3.getYearAsNum());
        assertEquals(4, m3.getMonthAsNum());
        assertEquals(26, m3.getDayAsNum());
    }

    @Test
    public void testHourMinuteSecondArithmetic() {
        // 测试小时加减
        Moment m1 = Moment.ofDateTime(20250416132647L);
        m1.addHour(5);
        assertEquals(18, m1.getHourAsNum());
        assertEquals(20250416182647L, m1.getDateTimeAsNum());
        m1.addHour(8);
        assertEquals(2025, m1.getYearAsNum());
        assertEquals(4, m1.getMonthAsNum());
        assertEquals(17, m1.getDayAsNum());
        assertEquals(2, m1.getHourAsNum());
        assertEquals(20250417022647L, m1.getDateTimeAsNum());
        // 测试分钟加减
        Moment m2 = Moment.ofDateTime(20250416132647L);
        m2.addMinute(40);
        assertEquals(14, m2.getHourAsNum());
        assertEquals(6, m2.getMinuteAsNum());
        assertEquals(20250416140647L, m2.getDateTimeAsNum());
        // 测试秒加减
        Moment m3 = Moment.ofDateTime(20250416132647L);
        m3.addSecond(20);
        assertEquals(2025, m3.getYearAsNum());
        assertEquals(4, m3.getMonthAsNum());
        assertEquals(16, m3.getDayAsNum());
        assertEquals(13, m3.getHourAsNum());
        assertEquals(27, m3.getMinuteAsNum());
        assertEquals(7, m3.getSecondAsNum());
        assertEquals(20250416132707L, m3.getDateTimeAsNum());
    }

    @Test
    public void testCrossBoundaryOperations() {
        // 测试跨年操作
        Moment m1 = Moment.ofDate(20251231);
        m1.addDay(1);
        assertEquals(20260101, m1.getDateAsNum());
        // 测试跨月操作(月末)
        Moment m2 = Moment.ofDate(20250131);
        m2.addMonth(1);
        assertEquals(20250228, m2.getDateAsNum()); // 2025年2月只有28天
        // 测试跨季度操作
        Moment m3 = Moment.ofDate(20250331);
        m3.addMonth(1);
        assertEquals(20250430, m3.getDateAsNum()); // 3月31日到4月30日
        // 测试跨天操作(时分秒)
        Moment m4 = Moment.ofDateTime(20250416235959L);
        m4.addSecond(1);
        assertEquals(20250417000000L, m4.getDateTimeAsNum());
    }

    @Test
    public void testNegativeArithmeticOperations() {
        // 测试负数年份操作
        Moment m1 = Moment.ofDate(20250416);
        m1.addYear(-5);
        assertEquals(2020, m1.getYearAsNum());
        assertEquals(20200416, m1.getDateAsNum());
        // 测试负数月份操作
        Moment m2 = Moment.ofDate(20250416);
        m2.addMonth(-3);
        assertEquals(2025, m2.getYearAsNum());
        assertEquals(1, m2.getMonthAsNum());
        assertEquals(16, m2.getDayAsNum());
        // 测试负数日期操作
        Moment m3 = Moment.ofDate(20250416);
        m3.addDay(-15);
        assertEquals(2025, m3.getYearAsNum());
        assertEquals(4, m3.getMonthAsNum());
        assertEquals(1, m3.getDayAsNum());
        // 测试负数小时操作
        Moment m4 = Moment.ofDateTime(20250416132647L);
        m4.addHour(-15);
        assertEquals(2025, m4.getYearAsNum());
        assertEquals(4, m4.getMonthAsNum());
        assertEquals(15, m4.getDayAsNum());
        assertEquals(22, m4.getHourAsNum());
        // 测试负数分钟操作
        Moment m5 = Moment.ofDateTime(20250416132647L);
        m5.addMinute(-30);
        assertEquals(12, m5.getHourAsNum());
        assertEquals(56, m5.getMinuteAsNum());
        // 测试负数秒操作
        Moment m6 = Moment.ofDateTime(20250416132647L);
        m6.addSecond(-50);
        assertEquals(25, m6.getMinuteAsNum());
        assertEquals(57, m6.getSecondAsNum());
    }

    @Test
    public void testLargeValueArithmetic() {
        // 测试大数值年份操作
        Moment m1 = Moment.ofDate(20250416);
        m1.addYear(100);
        assertEquals(2125, m1.getYearAsNum());
        // 测试大数值月份操作
        Moment m2 = Moment.ofDate(20250416);
        m2.addMonth(24);
        assertEquals(2027, m2.getYearAsNum());
        assertEquals(4, m2.getMonthAsNum());
        // 测试大数值日期操作
        Moment m3 = Moment.ofDate(20250416);
        m3.addDay(365);
        assertEquals(2026, m3.getYearAsNum());
        assertEquals(4, m3.getMonthAsNum());
        assertEquals(16, m3.getDayAsNum());
        // 测试大数值小时操作
        Moment m4 = Moment.ofDateTime(20250416132647L);
        m4.addHour(24);
        assertEquals(2025, m4.getYearAsNum());
        assertEquals(4, m4.getMonthAsNum());
        assertEquals(17, m4.getDayAsNum());
        assertEquals(13, m4.getHourAsNum());
    }
}
