package com.github.zhitron.universal;

import java.time.*;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间对象
 *
 * @author zhitron
 */
@SuppressWarnings("UnusedReturnValue")
public class Moment implements Comparable<Moment> {
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final int[][] MD = {{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31}, {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31}};
    private static final int[] YD = {365, 366};
    private static final int SECOND_NEXT = 60, MINUTE_NEXT = 60, HOUR_NEXT = 24, MONTH_NEXT = 12, YEAR_START = 1970;
    private static final long SECOND_MS = 1000, MINUTE_MS = SECOND_MS * SECOND_NEXT, HOUR_MS = MINUTE_MS * MINUTE_NEXT, DAY_MS = HOUR_MS * HOUR_NEXT;
    private static final Pattern P_EXCLUDE = Pattern.compile("\\D+"),
            P_DATE_NUM = Pattern.compile("(?<date>[1-9]\\d{3}((0[13578]|10|12)31|(0[0-13-9]|1[012])30|(0\\d|1[012])[012]\\d))"),
            P_YEAR = Pattern.compile("(?<y>[\\[(\"']?((\\d\\s*?){2}){1,2}[])\"']?[ 年\\\\/-]?\\s*)"),
            P_MONTH = Pattern.compile("(?<m>(\\d\\s*?){1,2}[ 月\\\\/-]\\s*)"),
            P_DAY = Pattern.compile("(?<d>(\\d\\s*?){1,2}('T'|[ 日\\\\/T-])\\s*?)"),
            P_QUARTER = Pattern.compile("(?<q>((第\\s*?[1234一二三四]|[上下本])?\\s*?季度[初末]?)|(Q\\s*?[1-4]\\s*?(季度)?[初末]?))");
    private static final Pattern
            P_DATE_STR = Pattern.compile(P_YEAR.pattern() + P_MONTH.pattern() + P_DAY.pattern()),
            P_DATE_QUARTER = Pattern.compile(P_YEAR.pattern() + "?" + P_QUARTER.pattern()),
            P_DATE_FOR_CHINA = Pattern.compile("(((\\d\\s*?){2}){1,2}年|(本|[上去]{1,2}|[下明]{1,2}))?(年?[年期月]|期期|月月)[初末]?");

    private long timestamp;
    protected int year, month, day, hour, minute, second, millis, micros, nanos;
    protected boolean update = false;

    protected Moment() {
    }

    protected Moment(long timestamp, int micros, int nanos) {
        setTimestamp(timestamp);
        this.micros = micros;
        this.nanos = nanos;
    }

    protected Moment(Moment moment) {
        moment.update();
        this.timestamp = moment.timestamp;
        this.year = moment.year;
        this.month = moment.month;
        this.day = moment.day;
        this.hour = moment.hour;
        this.minute = moment.minute;
        this.second = moment.second;
        this.millis = moment.millis;
        this.micros = moment.micros;
        this.nanos = moment.nanos;
        this.update = moment.update;
    }

    /**
     * 判断是否时闰年
     *
     * @param year 年份
     * @return 如果是返回1，否则返回0
     */
    public static int leap(int year) {
        return year % 400 == 0 || (year % 4 == 0 && year % 100 != 0) ? 1 : 0;
    }

    /**
     * 获取指定年份月份的天数
     *
     * @param year  年份
     * @param month 月份1-12
     * @return 返回指定年份的天数
     */
    public static int days(int year, int month) {
        return MD[year % 400 == 0 || (year % 4 == 0 && year % 100 != 0) ? 1 : 0][month - 1];
    }

    /**
     * 获取指定年份的天数
     *
     * @param year 年份
     * @return 返回指定年份的天数
     */
    public static int days(int year) {
        return YD[year % 400 == 0 || (year % 4 == 0 && year % 100 != 0) ? 1 : 0];
    }

    /**
     * 解析日期时间格式字符串，将其拆分为格式化标记和普通文本的列表。
     *
     * @param format 日期时间格式字符串，如 "yyyy-MM-dd HH:mm:ss"
     * @return 包含格式化标记和普通文本的列表
     */
    private static List<String> parseFormat(String format) {
        int len = format.length(), s = 0;
        List<String> list = new LinkedList<>();
        boolean symbol = false;
        for (int i = 0; i < len; i++) {
            char c = format.charAt(i);
            if ("yMdHmsS".indexOf(c) >= 0) {
                if (!symbol) {
                    if (s != i) list.add(format.substring(s, i));
                    s = i;
                }
                symbol = true;
            } else {
                if (symbol) {
                    if (s != i) list.add(format.substring(s, i));
                    s = i;
                }
                symbol = false;
            }
        }
        list.add(format.substring(s));
        return list;
    }

    /**
     * 解析中文日期
     *
     * @param input 输入中文文字
     * @return 返回解析的日期
     */
    public String[] parseToDate(String input) {
        List<Object> main = Collections.singletonList(input);
        //处理“年月日”格式
        main = parseToDate(main, P_DATE_STR, (matcher, date) -> {
            String temp, tmp;
            temp = matcher.group("y");
            temp = P_EXCLUDE.matcher(temp).replaceAll("");
            if (temp.length() == 2) {
                temp = (tmp = getYearAsStr()).substring(0, tmp.length() - 2) + temp;
            }
            date.setYear(Integer.parseInt(temp));

            temp = matcher.group("m");
            temp = P_EXCLUDE.matcher(temp).replaceAll("");
            date.setMonthIfCorrect(Integer.parseInt(temp));

            temp = matcher.group("d");
            temp = P_EXCLUDE.matcher(temp).replaceAll("");
            date.setDayIfCorrect(Integer.parseInt(temp));
        });
        //处理“yyyyMMdd”格式
        main = parseToDate(main, P_DATE_NUM, (matcher, date) -> {
            String temp = matcher.group("date");
            temp = P_EXCLUDE.matcher(temp).replaceAll("");
            date.setDate(Integer.parseInt(temp));
        });
        //处理“季度”格式
        main = parseToDate(main, P_DATE_QUARTER, (matcher, date) -> {
            String temp, tmp;
            temp = matcher.group("y");
            if (temp != null) {
                temp = P_EXCLUDE.matcher(temp).replaceAll("");
                if (temp.length() == 2) {
                    temp = (tmp = getYearAsStr()).substring(0, tmp.length() - 2) + temp;
                }
                date.setYear(Integer.parseInt(temp));
            }
            temp = matcher.group("q");
            char[] cs = temp.toCharArray();
            if (cs[0] == '第' || cs[0] == 'Q') {
                date.setDay(1);
                switch (cs[1]) {
                    case '1':
                    case '一':
                        date.setMonth(1);
                        break;
                    case '2':
                    case '二':
                        date.setMonth(4);
                        break;
                    case '3':
                    case '三':
                        date.setMonth(7);
                        break;
                    default:
                        date.setMonth(10);
                        break;
                }
                if (cs.length < 3 || cs[cs.length - 1] != '初') {
                    date.addMonth(3);
                    date.addDay(-1);
                }
            } else {
                int q = getQuarter();
                date.setMonth((q - 1) * 3 + 1);
                date.setDay(1);
                if (cs.length < 2 || cs[cs.length - 1] != '初') {
                    date.addMonth(3);
                    date.addDay(-1);
                }
                date.addMonth(cs[0] == '下' ? 3 : (cs[0] == '上' ? -3 : 0));
            }
        });
        //处理“中国日期”格式
        main = parseToDate(main, P_DATE_FOR_CHINA, (matcher, date) -> {
            String group = matcher.group();
            char[] cs = group.toCharArray();
            int len = cs.length;
            Boolean flag = null;
            int last = "初末".indexOf(cs[len - 1]) >= 0 ? 2 : 1;
            if (cs[len - last] == '月') {
                flag = false;
                if (last == 1 || cs[len - 1] != '初') {
                    date.setMonthEnd(0);
                } else {
                    date.setMonthStart(0);
                }
            } else if (cs[len - last] == '年') {
                flag = true;
                if (last == 1 || cs[len - 1] != '初') {
                    date.setYearEnd();
                } else {
                    date.setYearStart();
                }
            } else {
                if (last == 1 || cs[len - 1] != '初') {
                    date.setQuarterEnd(0);
                } else {
                    date.setQuarterStart(0);
                }
            }
            if ((len -= last) > 0) {
                int i = 0, c = 0;
                if (Character.isDigit(cs[i])) {
                    int year = Character.digit(cs[i], 10);
                    for (i++; i < len && Character.isDigit(cs[i]); i++) {
                        System.out.println(year);
                        year = 10 * year + Character.digit(cs[i], 10);
                        System.out.println(year);
                    }
                    if (i == 2) {
                        year = date.getYearAsNum() / 100 * 100 + year;
                    }
                    date.setYear(year);
                    return;
                }
                if ("上去".indexOf(cs[i]) >= 0) {
                    c--;
                    if (++i < len && "上去".indexOf(cs[i]) >= 0) {
                        c--;
                        i++;
                    }
                } else if ("下明".indexOf(cs[i]) >= 0) {
                    c++;
                    if (++i < len && "下明".indexOf(cs[i]) >= 0) {
                        c++;
                        i++;
                    }
                } else {
                    i++;
                }
                if (c != 0) {
                    if (i == len) {
                        if (flag == null || flag) {
                            date.addYear(i * c);
                        } else {
                            date.addMonth(i * c);
                        }
                    } else {
                        switch (cs[i]) {
                            case '期':
                            case '年':
                                date.addYear(i * c);
                                break;
                            case '月':
                                date.addMonth(i * c);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        });
        //处理“只有年”格式
        main = parseToDate(main, P_YEAR, (matcher, date) -> {
            String temp, tmp;
            temp = matcher.group("y");
            temp = P_EXCLUDE.matcher(temp).replaceAll("");
            if (temp.length() == 2) {
                temp = (tmp = getYearAsStr()).substring(0, tmp.length() - 2) + temp;
            }
            date.setYear(Integer.parseInt(temp));
        });

        return main.stream().filter(o -> o instanceof Moment).map(o -> ((Moment) o).getDateAsStr()).toArray(String[]::new);
    }

    /**
     * 将字符串解析为日期对象列表
     *
     * @param main    包含待解析字符串和已解析日期对象的列表
     * @param pattern 用于匹配日期格式的正则表达式模式
     * @param handle  处理匹配结果的函数，接受匹配器和日期对象作为参数
     * @return 包含原始字符串片段和解析后的日期对象的列表
     */
    private List<Object> parseToDate(List<Object> main, Pattern pattern, BiConsumer<Matcher, Moment> handle) {
        List<Object> list = new LinkedList<>();
        String string;
        // 遍历主列表中的每个元素
        for (Object object : main) {
            // 如果元素已经是日期对象，则直接添加到结果列表中
            if (object instanceof Moment) {
                list.add(object);
                continue;
            }
            // 将元素转换为字符串进行处理
            string = (String) object;
            int last = 0;
            // 使用给定的正则表达式模式匹配字符串
            for (Matcher matcher = pattern.matcher(string); matcher.find(); ) {
                int start = matcher.start(), end = matcher.end();
                // 如果匹配项之前有未处理的字符串，则将其添加到结果列表中
                if (start != last) {
                    list.add(string.substring(last, start));
                }
                last = end;
                // 从当前对象复制一个新日期对象
                Moment date = new Moment(this);
                // 使用提供的处理函数处理匹配结果
                handle.accept(matcher, date);
                // 将处理后的日期对象添加到结果列表中
                list.add(date);
            }
            // 添加最后一个匹配项之后的字符串片段
            list.add(string.substring(last));
        }
        return list;
    }

    /**
     * 创建当前时间{@link Moment}
     *
     * @return {@link Moment}
     */
    public static Moment now() {
        return new Moment(System.currentTimeMillis(), 0, 0);
    }

    /**
     * 创建{@link Moment}
     *
     * @param input 输入参数
     * @return {@link Moment}
     */
    public static Moment ofDate(int input) {
        return new Moment().setDate(input);
    }

    /**
     * 创建{@link Moment}
     *
     * @param input 输入参数
     * @return {@link Moment}
     */
    public static Moment ofDateTime(long input) {
        return new Moment().setDateTime(input);
    }

    /**
     * 创建{@link Moment}
     *
     * @param input 输入参数
     * @return {@link Moment}
     */
    public static Moment of(long input) {
        return new Moment(input, 0, 0);
    }

    /**
     * 创建{@link Moment}
     *
     * @param input 输入参数
     * @return {@link Moment}
     */
    public static Moment of(LocalDateTime input) {
        return new Moment().setTimestamp(input);
    }

    /**
     * 创建{@link Moment}
     *
     * @param input  输入参数
     * @param zoneId 时区
     * @return {@link Moment}
     */
    public static Moment of(LocalDateTime input, ZoneId zoneId) {
        return new Moment().setTimestamp(input, zoneId);
    }

    /**
     * 创建{@link Moment}
     *
     * @param input 输入参数
     * @return {@link Moment}
     */
    public static Moment of(ZonedDateTime input) {
        return new Moment().setTimestamp(input);
    }

    /**
     * 创建{@link Moment}
     *
     * @param input 输入参数
     * @return {@link Moment}
     */
    public static Moment of(OffsetDateTime input) {
        return new Moment().setTimestamp(input);
    }

    /**
     * 创建{@link Moment}
     *
     * @param input 输入参数
     * @return {@link Moment}
     */
    public static Moment of(Date input) {
        return new Moment().setTimestamp(input);
    }

    /**
     * 创建{@link Moment}
     *
     * @param input 输入参数
     * @return {@link Moment}
     */
    public static Moment of(Instant input) {
        return new Moment().setTimestamp(input);
    }

    /**
     * 创建{@link Moment}
     *
     * @param input  输入参数
     * @param format 时间格式
     * @return {@link Moment}
     */
    public static Moment of(String input, String format) {
        return new Moment().setTimestamp(input, format);
    }

    /**
     * 创建{@link Moment}
     *
     * @param input  输入参数
     * @param format 时间格式
     * @param zoneId 时区
     * @return {@link Moment}
     */
    public static Moment of(String input, String format, ZoneId zoneId) {
        return new Moment().setTimestamp(input, format, zoneId);
    }

    /**
     * 更新矫正时间时间戳
     *
     * @return 返回this
     */
    public final Moment update() {
        if (this.update) {
            long value = 0;
            if (year >= YEAR_START) {
                value += millis;
                value += second * SECOND_MS;
                value += minute * MINUTE_MS;
                value += hour * HOUR_MS;
                value += day * DAY_MS;
                for (int v = 0; v < month; v++) value += MD[leap(year)][v] * DAY_MS;
                for (int v = YEAR_START; v < year; v++) value += YD[leap(v)] * DAY_MS;
            } else {
                value -= (1000 - millis);
                value -= (SECOND_NEXT - 1 - second) * SECOND_MS;
                value -= (MINUTE_NEXT - 1 - minute) * MINUTE_MS;
                value -= (HOUR_NEXT - 1 - hour) * HOUR_MS;
                value -= (MD[leap(year)][month] - 1 - day) * DAY_MS;
                for (int v = MONTH_NEXT - 1; v > month; v--) value -= MD[leap(year)][v] * DAY_MS;
                for (int v = YEAR_START - 1; v > year; v--) value -= YD[leap(v)] * DAY_MS;
            }
            this.timestamp = value;
            this.update = false;
        }
        return this;
    }

    /**
     * 获取{@code yyyyMMdd}时间数字如20250416
     *
     * @return 获取值
     */
    public final int getDateAsNum() {
        return getYearAsNum() * 10000 + getMonthAsNum() * 100 + getDayAsNum();
    }

    /**
     * 获取{@code yyyyMMdd}时间数字如20250416
     *
     * @return 获取值
     */
    public final String getDateAsStr() {
        return String.valueOf(getDateAsNum());
    }

    /**
     * 获取{@code HHmmss}时间数字如132647
     *
     * @return 获取值
     */
    public final int getTimeAsNum() {
        return getHourAsNum() * 10000 + getMinuteAsNum() * 100 + getSecondAsNum();
    }

    /**
     * 获取{@code HHmmss}时间数字如132647
     *
     * @return 获取值
     */
    public final String getTimeAsStr() {
        return String.valueOf(getTimeAsNum());
    }

    /**
     * 获取{@code yyyyMMddHHmmss}时间数字如20250416132647
     *
     * @return 获取值
     */
    public final long getDateTimeAsNum() {
        return getDateAsNum() * 1000000L + getTimeAsNum();
    }

    /**
     * 获取{@code yyyyMMddHHmmss}时间数字如20250416132647
     *
     * @return 获取值
     */
    public final String getDateTimeAsStr() {
        return String.valueOf(getDateTimeAsNum());
    }

    /**
     * 获取时间戳
     *
     * @return 返回时间戳
     */
    public final long getTimestamp() {
        return update().timestamp;
    }

    /**
     * 获取{@code int}类型的年属性域
     *
     * @return 返回{@code int}类型的年属性域
     */
    public final int getYearAsNum() {
        return this.year;
    }

    /**
     * 获取{@code String}类型的年属性域
     *
     * @return 返回{@code String}类型的年属性域
     */
    public final String getYearAsStr() {
        return String.valueOf(getYearAsNum());
    }

    /**
     * 获取{@code int}类型的月属性域
     *
     * @return 返回{@code int}类型的月属性域
     */
    public final int getMonthAsNum() {
        return this.month + 1;
    }

    /**
     * 获取{@code String}类型的月属性域
     *
     * @return 返回{@code String}类型的月属性域
     */
    public final String getMonthAsStr() {
        return String.valueOf(getMonthAsNum() + 100).substring(1);
    }

    /**
     * 获取{@code int}类型的日属性域
     *
     * @return 返回{@code int}类型的日属性域
     */
    public final int getDayAsNum() {
        return this.day + 1;
    }

    /**
     * 获取{@code String}类型的日属性域
     *
     * @return 返回{@code String}类型的日属性域
     */
    public final String getDayAsStr() {
        return String.valueOf(getDayAsNum() + 100).substring(1);
    }

    /**
     * 获取{@code int}类型的时属性域
     *
     * @return 返回{@code int}类型的时属性域
     */
    public final int getHourAsNum() {
        return this.hour;
    }

    /**
     * 获取{@code String}类型的时属性域
     *
     * @return 返回{@code String}类型的时属性域
     */
    public final String getHourAsStr() {
        return String.valueOf(getHourAsNum() + 100).substring(1);
    }

    /**
     * 获取{@code int}类型的分属性域
     *
     * @return 返回{@code int}类型的分属性域
     */
    public final int getMinuteAsNum() {
        return this.minute;
    }

    /**
     * 获取{@code String}类型的分属性域
     *
     * @return 返回{@code String}类型的分属性域
     */
    public final String getMinuteAsStr() {
        return String.valueOf(getMinuteAsNum() + 100).substring(1);
    }

    /**
     * 获取{@code int}类型的秒属性域
     *
     * @return 返回{@code int}类型的秒属性域
     */
    public final int getSecondAsNum() {
        return this.second;
    }

    /**
     * 获取{@code String}类型的秒属性域
     *
     * @return 返回{@code String}类型的秒属性域
     */
    public final String getSecondAsStr() {
        return String.valueOf(getSecondAsNum() + 100).substring(1);
    }

    /**
     * 获取{@code int}类型的毫秒属性域
     *
     * @return 返回{@code int}类型的毫秒属性域
     */
    public final int getMillisAsNum() {
        return this.millis;
    }

    /**
     * 获取{@code String}类型的毫秒属性域
     *
     * @return 返回{@code String}类型的毫秒属性域
     */
    public final String getMillisAsStr() {
        return String.valueOf(getMillisAsNum() + 1000).substring(1);
    }

    /**
     * 获取{@code int}类型的微秒属性域
     *
     * @return 返回{@code int}类型的微秒属性域
     */
    public final int getMicrosAsNum() {
        return this.micros;
    }

    /**
     * 获取{@code String}类型的微秒属性域
     *
     * @return 返回{@code String}类型的微秒属性域
     */
    public final String getMicrosAsStr() {
        return String.valueOf(getMicrosAsNum() + 1000).substring(1);
    }

    /**
     * 获取{@code int}类型的纳秒属性域
     *
     * @return 返回{@code int}类型的纳秒属性域
     */
    public final int getNanosAsNum() {
        return this.nanos;
    }

    /**
     * 获取{@code String}类型的纳秒属性域
     *
     * @return 返回{@code String}类型的纳秒属性域
     */
    public final String getNanosAsStr() {
        return String.valueOf(getNanosAsNum() + 1000).substring(1);
    }

    /**
     * 获取季度1-4
     *
     * @return 返回季度1-4
     */
    public final int getQuarter() {
        return (getMonthAsNum() + 2) / 3;
    }

    /**
     * 设置季度初
     *
     * @return this
     */
    public final Moment setQuarterStart() {
        return setQuarterStart(0);
    }

    /**
     * 设置季度初
     *
     * @param value 设置第几季度
     * @return this
     */
    public final Moment setQuarterStart(int value) {
        return setMonth(3 * (1 <= value && value <= 4 ? value : getQuarter()) - 2).setDay(1);
    }

    /**
     * 设置季度末
     *
     * @return this
     */
    public final Moment setQuarterEnd() {
        return setQuarterEnd(0);
    }

    /**
     * 设置季度末
     *
     * @param value 设置第几季度
     * @return this
     */
    public final Moment setQuarterEnd(int value) {
        return setMonth(3 * (1 <= value && value <= 4 ? value : getQuarter())).setMonthEnd(0);
    }

    /**
     * 获取年的天数
     *
     * @return 返回年的天数
     */
    public int getNumberOfDaysYear() {
        return Moment.days(getYearAsNum());
    }

    /**
     * 获取月的天数
     *
     * @return 返回月的天数
     */
    public int getNumberOfDaysMonth() {
        return Moment.days(getYearAsNum(), getMonthAsNum());
    }

    /**
     * 比较{@link Moment}大小
     *
     * @return 负整数、零或正整数，因为此对象小于、等于或大于指定对象。
     */
    @Override
    public int compareTo(Moment o) {
        return Long.compare(getTimestamp(), o.getTimestamp());
    }

    /**
     * 将时间转成{@link LocalDateTime}
     *
     * @return 返回格式化后的 {@link LocalDateTime}
     */
    public final LocalDateTime toLocalDateTime() {
        return toLocalDateTime(UTC);
    }

    /**
     * 将时间转成{@link LocalDateTime}
     *
     * @param zoneId 时区
     * @return 返回格式化后的 {@link LocalDateTime}
     */
    public final LocalDateTime toLocalDateTime(ZoneId zoneId) {
        return LocalDateTime.ofInstant(toInstant(), zoneId);
    }

    /**
     * 将时间转成{@link ZonedDateTime}
     *
     * @return 返回格式化后的 {@link ZonedDateTime}
     */
    public final ZonedDateTime toZonedDateTime() {
        return toZonedDateTime(UTC);
    }

    /**
     * 将时间转成{@link ZonedDateTime}
     *
     * @param zoneId 时区
     * @return 返回格式化后的 {@link ZonedDateTime}
     */
    public final ZonedDateTime toZonedDateTime(ZoneId zoneId) {
        return ZonedDateTime.ofInstant(toInstant(), zoneId);
    }

    /**
     * 将时间转成{@link OffsetDateTime}
     *
     * @return 返回格式化后的 {@link OffsetDateTime}
     */
    public final OffsetDateTime toOffsetDateTime() {
        return toOffsetDateTime(UTC);
    }

    /**
     * 将时间转成{@link OffsetDateTime}
     *
     * @param zoneId 时区
     * @return 返回格式化后的 {@link OffsetDateTime}
     */
    public final OffsetDateTime toOffsetDateTime(ZoneId zoneId) {
        return OffsetDateTime.ofInstant(toInstant(), zoneId);
    }

    /**
     * 将时间转成{@link Date}
     *
     * @return 返回格式化后的 {@link Date}
     */
    public final Date toDate() {
        return new Date(getTimestamp());
    }

    /**
     * 将时间转成{@link Instant}
     *
     * @return 返回格式化后的 {@link Instant}
     */
    public final Instant toInstant() {
        long timestamp = getTimestamp(), interval = 1000;
        long nanoOfSecond = (timestamp % interval) * interval * interval + getMicrosAsNum() * interval + getNanosAsNum();
        return Instant.ofEpochSecond(timestamp / interval, nanoOfSecond);
    }

    /**
     * 将时间转成{@link String}
     *
     * @param format 时间格式
     * @return 返回格式化后的 {@link String}
     */
    public final String toString(String format) {
        return toString(format, UTC);
    }

    /**
     * 将时间转成{@link String}
     *
     * @param format 时间格式
     * @param zoneId 时区
     * @return 返回格式化后的 {@link String}
     */
    public final String toString(String format, ZoneId zoneId) {
        List<String> formats = Moment.parseFormat(format);
        LocalDateTime localDateTime = toLocalDateTime(zoneId);
        StringBuilder result = new StringBuilder();
        for (String token : formats) {
            switch (token) {
                case "yyyy":
                    result.append(localDateTime.getYear());
                    break;
                case "MM":
                    result.append(String.valueOf(localDateTime.getMonthValue() + 100).substring(1));
                    break;
                case "dd":
                    result.append(String.valueOf(localDateTime.getDayOfMonth() + 100).substring(1));
                    break;
                case "HH":
                    result.append(String.valueOf(localDateTime.getHour() + 100).substring(1));
                    break;
                case "mm":
                    result.append(String.valueOf(localDateTime.getMinute() + 100).substring(1));
                    break;
                case "ss":
                    result.append(String.valueOf(localDateTime.getSecond() + 100).substring(1));
                    break;
                case "SSS":
                    result.append(String.valueOf(localDateTime.getNano() / 1000 / 1000 + 1000).substring(1));
                    break;
                default:
                    result.append(token);
                    break;
            }
        }
        return result.toString();
    }

    @Override
    public final String toString() {
        return getYearAsStr() + '-' + getMonthAsStr() + '-' + getDayAsStr() + 'T' + getHourAsStr() + ':' + getMinuteAsStr() + ':' + getSecondAsStr() + '.' + getMillisAsStr() + "Z";
    }

    /**
     * 设置{@code yyyyMMdd}时间数字如20250416
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setDate(int input) {
        int factor;
        if (input >= 0) factor = 1;
        else {
            input = -input;
            factor = -1;
        }
        if (input < 1_00) {
            setDay(input);
        } else if (input < 1_00_00) {
            setMonth(input / 100 % 100);
            setDay(input % 100);
        } else {
            setYear(factor * input / 10000);
            setMonth(input / 100 % 100);
            setDay(input % 100);
        }
        return this;
    }

    /**
     * 设置{@code HHmmss}时间数字如132647
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setTime(int input) {
        int factor;
        if (input >= 0) factor = 1;
        else {
            input = -input;
            factor = -1;
        }
        if (input < 100) {
            setSecond(input);
        } else if (input < 10000) {
            setMinute(input / 100 % 100);
            setSecond(input % 100);
        } else {
            setHour(factor * input / 10000);
            setMinute(input / 100 % 100);
            setSecond(input % 100);
        }
        return this;
    }

    /**
     * 设置{@code yyyyMMddHHmmss}时间数字如20250416132647
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setDateTime(long input) {
        setTime((int) (input % 1000000));
        setDate((int) (input / 1000000));
        return this;
    }

    /**
     * 设置时间戳
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setTimestamp(String input, String format) {
        return setTimestamp(input, format, UTC);
    }

    /**
     * 设置时间戳
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setTimestamp(String input, String format, ZoneId zoneId) {
        int year = this.year, month = this.month, day = this.day,
                hour = 0, minute = 0, second = 0, nanoOfSecond = 0, t = 0;
        List<String> list = Moment.parseFormat(format);
        String field = "";
        // 遍历每个标记进行解析
        for (String token : list) {
            switch (token) {
                case "yyyy":
                    try {
                        field = input.substring(t, t += 4);
                        year = Integer.parseInt(field);
                    } catch (Throwable e) {
                        throw new IllegalArgumentException("There was an error in parsing the 'yyyy' pattern of the substring '" + field + "' returned (" + (t - 4) + "," + t + ") in the string '" + input + "' index.");
                    }
                    break;
                case "MM":
                    try {
                        field = input.substring(t, t += 2);
                        month = Integer.parseInt(field);
                    } catch (Throwable e) {
                        throw new IllegalArgumentException("There was an error in parsing the 'MM' pattern of the substring '" + field + "' returned (" + (t - 2) + "," + t + ") in the string '" + input + "' index.");
                    }
                    break;
                case "dd":
                    try {
                        field = input.substring(t, t += 2);
                        day = Integer.parseInt(field);
                    } catch (Throwable e) {
                        throw new IllegalArgumentException("There was an error in parsing the 'dd' pattern of the substring '" + field + "' returned (" + (t - 2) + "," + t + ") in the string '" + input + "' index.");
                    }
                    break;
                case "HH":
                    try {
                        field = input.substring(t, t += 2);
                        hour = Integer.parseInt(field);
                    } catch (Throwable e) {
                        throw new IllegalArgumentException("There was an error in parsing the 'HH' pattern of the substring '" + field + "' returned (" + (t - 2) + "," + t + ") in the string '" + input + "' index.");
                    }
                    break;
                case "mm":
                    try {
                        field = input.substring(t, t += 2);
                        minute = Integer.parseInt(field);
                    } catch (Throwable e) {
                        throw new IllegalArgumentException("There was an error in parsing the 'mm' pattern of the substring '" + field + "' returned (" + (t - 2) + "," + t + ") in the string '" + input + "' index.");
                    }
                    break;
                case "ss":
                    try {
                        field = input.substring(t, t += 2);
                        second = Integer.parseInt(field);
                    } catch (Throwable e) {
                        throw new IllegalArgumentException("There was an error in parsing the 'ss' pattern of the substring '" + field + "' returned (" + (t - 2) + "," + t + ") in the string '" + input + "' index.");
                    }
                    break;
                case "SSS":
                    try {
                        field = input.substring(t, t += 3);
                        nanoOfSecond = Integer.parseInt(field);
                    } catch (Throwable e) {
                        throw new IllegalArgumentException("There was an error in parsing the 'SSS' pattern of the substring '" + field + "' returned (" + (t - 3) + "," + t + ") in the string '" + input + "' index.");
                    }
                    break;
                default:
                    t += token.length();
                    break;
            }
        }
        if (month < 1 || month > MONTH_NEXT) {
            throw new IllegalArgumentException("The month exceeds the range of [1,12],The actual value resolved is '" + month + "'.");
        }
        if (day < 1 || day > (t = MD[leap(year)][month])) {
            throw new IllegalArgumentException("The dat exceeds the range of [1," + t + "],The actual value resolved is '" + day + "'.");
        }
        if (hour < 0 || hour >= HOUR_NEXT) {
            throw new IllegalArgumentException("The hour exceeds the range of [0,24),The actual value resolved is '" + hour + "'.");
        }
        if (minute < 0 || minute >= MINUTE_NEXT) {
            throw new IllegalArgumentException("The minute exceeds the range of [0,60),The actual value resolved is '" + minute + "'.");
        }
        if (second < 0 || second >= SECOND_NEXT) {
            throw new IllegalArgumentException("The second exceeds the range of [0,60),The actual value resolved is '" + second + "'.");
        }
        if (nanoOfSecond < 0 || nanoOfSecond >= 1000) {
            throw new IllegalArgumentException("The millis exceeds the range of [0,1000),The actual value resolved is '" + nanoOfSecond + "'.");
        }
        nanoOfSecond *= 1000000;
        return setTimestamp(LocalDateTime.of(year, month, day, hour, minute, second, nanoOfSecond), zoneId);
    }

    /**
     * 设置时间戳
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setTimestamp(LocalDateTime input) {
        return setTimestamp(input, UTC);
    }

    /**
     * 设置时间戳
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setTimestamp(LocalDateTime input, ZoneId zoneId) {
        return setTimestamp(input.toInstant(zoneId instanceof ZoneOffset ?
                (ZoneOffset) zoneId : zoneId.getRules().getOffset(Instant.now())));
    }

    /**
     * 设置时间戳
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setTimestamp(ZonedDateTime input) {
        return setTimestamp(input.toInstant());
    }

    /**
     * 设置时间戳
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setTimestamp(OffsetDateTime input) {
        return setTimestamp(input.toInstant());
    }

    /**
     * 设置时间戳
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setTimestamp(Date input) {
        return setTimestamp(input.getTime());
    }

    /**
     * 设置时间戳
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setTimestamp(Instant input) {
        int next = input.getNano();
        this.nanos = next % 1000;
        this.micros = next / 1000 % 1000;
        return setTimestamp(input.toEpochMilli());
    }

    /**
     * 设置时间戳
     *
     * @param input 输入参数
     * @return 返回this
     */
    public final Moment setTimestamp(long input) {
        int v, t;
        this.timestamp = input;
        if (input >= 0) {
            this.millis = (int) (input % 1000);
            input /= 1000;
            this.second = (int) (input % SECOND_NEXT);
            input /= SECOND_NEXT;
            this.minute = (int) (input % MINUTE_NEXT);
            input /= MINUTE_NEXT;
            this.hour = (int) (input % HOUR_NEXT);
            input /= HOUR_NEXT;
            for (v = YEAR_START; input >= (t = YD[leap(v)]); input -= t) v++;
            this.year = v;
            for (v = 0; input >= (t = MD[leap(year)][v]); input -= t) v++;
            this.month = v;
            this.day = (int) input;
        } else {
            input = Math.abs(input);
            this.millis = (int) (1000 - input % 1000);
            input /= 1000;
            this.second = (int) (SECOND_NEXT - 1 - input % SECOND_NEXT);
            input /= SECOND_NEXT;
            this.minute = (int) (MINUTE_NEXT - 1 - input % MINUTE_NEXT);
            input /= MINUTE_NEXT;
            this.hour = (int) (HOUR_NEXT - 1 - input % HOUR_NEXT);
            input /= HOUR_NEXT;
            for (v = YEAR_START - 1; input >= (t = YD[leap(v)]); input -= t) v--;
            this.year = v;
            for (v = MONTH_NEXT - 1; input >= (t = MD[leap(year)][v]); input -= t) v--;
            this.month = v;
            this.day = (int) (MD[leap(year)][month] - 1 - input);
        }
        this.update = false;
        return this;
    }

    /**
     * 设置年，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setYear(int value) {
        this.year = value;
        this.update = true;
        return this;
    }

    /**
     * 设置年初
     *
     * @return this
     */
    public final Moment setYearStart() {
        return setMonth(1).setDay(1);
    }

    /**
     * 设置年初
     *
     * @param value 设置第几年
     * @return this
     */
    public final Moment setYearStart(int value) {
        return setYear(value).setMonth(1).setDay(1);
    }

    /**
     * 设置年末
     *
     * @return this
     */
    public final Moment setYearEnd() {
        return setMonth(12).setDay(31);
    }

    /**
     * 设置年末
     *
     * @param value 设置第几年
     * @return this
     */
    public final Moment setYearEnd(int value) {
        return setYear(value).setMonth(12).setDay(31);
    }

    /**
     * 设置月，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setMonth(int value) {
        value -= 1;
        this.update = true;
        if (0 <= value && value < MONTH_NEXT) {
            this.month = value;
        } else if (value < 0) {
            this.month = 0;
            this.addMonth(value + 1);
        } else {
            this.month = MONTH_NEXT - 1;
            this.addMonth(value - MONTH_NEXT + 1);
        }
        return this;
    }

    /**
     * 设置月，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setMonthIfCorrect(int value) {
        value -= 1;
        if (0 <= value && value < MONTH_NEXT) {
            this.month = value;
            this.update = true;
        }
        return this;
    }

    /**
     * 设置月初，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setMonthStart(int value) {
        return setMonthIfCorrect(value).setDay(1);
    }

    /**
     * 设置月末，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setMonthEnd(int value) {
        return setMonthIfCorrect(value).setDay(1).addMonth(1).addDay(-1);
    }

    /**
     * 设置天，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setDay(int value) {
        value -= 1;
        this.update = true;
        int dayNext = MD[leap(this.year)][this.month];
        if (0 <= value && value < dayNext) {
            this.day = value;
        } else if (value < 0) {
            this.day = 0;
            this.addDay(value);
        } else {
            this.day = dayNext - 1;
            this.addDay(value - dayNext + 1);
        }
        return this;
    }

    /**
     * 设置天，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setDayIfCorrect(int value) {
        value -= 1;
        int dayNext = MD[leap(this.year)][this.month];
        if (0 <= value && value < dayNext) {
            this.day = value;
            this.update = true;
        }
        return this;
    }

    /**
     * 设置小时，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setHour(int value) {
        this.update = true;
        if (0 <= value && value < HOUR_NEXT) {
            this.hour = value;
        } else if (value < 0) {
            this.hour = 0;
            this.addHour(value);
        } else {
            this.hour = HOUR_NEXT - 1;
            this.addHour(value - HOUR_NEXT + 1);
        }
        return this;
    }

    /**
     * 设置小时，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setHourIfCorrect(int value) {
        if (0 <= value && value < HOUR_NEXT) {
            this.hour = value;
            this.update = true;
        }
        return this;
    }

    /**
     * 设置分钟，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setMinute(int value) {
        this.update = true;
        if (0 <= value && value < MINUTE_NEXT) {
            this.minute = value;
        } else if (value < 0) {
            this.minute = 0;
            this.addMinute(value);
        } else {
            this.minute = MINUTE_NEXT - 1;
            this.addMinute(value - MINUTE_NEXT + 1);
        }
        return this;
    }

    /**
     * 设置分钟，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setMinuteIfCorrect(int value) {
        if (0 <= value && value < MINUTE_NEXT) {
            this.minute = value;
            this.update = true;
        }
        return this;
    }

    /**
     * 设置秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setSecond(int value) {
        this.update = true;
        if (0 <= value && value < SECOND_NEXT) {
            this.second = value;
        } else if (value < 0) {
            this.second = 0;
            this.addSecond(value);
        } else {
            this.second = SECOND_NEXT - 1;
            this.addSecond(value - SECOND_NEXT + 1);
        }
        return this;
    }

    /**
     * 设置秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setSecondIfCorrect(int value) {
        if (0 <= value && value < SECOND_NEXT) {
            this.second = value;
            this.update = true;
        }
        return this;
    }

    /**
     * 设置毫秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setMillis(int value) {
        this.update = true;
        if (0 <= value && value < 1000) {
            this.millis = value;
        } else if (value < 0) {
            this.millis = 0;
            this.addMillis(value);
        } else {
            this.millis = 1000 - 1;
            this.addMillis(value - 1000 + 1);
        }
        return this;
    }

    /**
     * 设置毫秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setMillisIfCorrect(int value) {
        if (0 <= value && value < 1000) {
            this.millis = value;
            this.update = true;
        }
        return this;
    }

    /**
     * 设置微秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setMicros(int value) {
        this.update = true;
        if (0 <= value && value < 1000) {
            this.micros = value;
        } else if (value < 0) {
            this.micros = 0;
            this.addMicros(value);
        } else {
            this.micros = 1000 - 1;
            this.addMicros(value - 1000 + 1);
        }
        return this;
    }

    /**
     * 设置微秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setMicrosIfCorrect(int value) {
        if (0 <= value && value < 1000) {
            this.micros = value;
            this.update = true;
        }
        return this;
    }

    /**
     * 设置纳秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setNanos(int value) {
        this.update = true;
        if (0 <= value && value < 1000) {
            this.nanos = value;
        } else if (value < 0) {
            this.nanos = 0;
            this.addNanos(value);
        } else {
            this.nanos = 1000 - 1;
            this.addNanos(value - 1000 + 1);
        }
        return this;
    }

    /**
     * 设置纳秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment setNanosIfCorrect(int value) {
        if (0 <= value && value < 1000) {
            this.nanos = value;
            this.update = true;
        }
        return this;
    }

    /**
     * 增加年，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment addYear(long value) {
        value = Math.addExact(year, value);
        int year = (int) value;
        if (year != value) {
            throw new IllegalArgumentException("Integer overflow: The year value is outside the allowable range");
        }
        this.year = year;
        this.update = true;
        return this;
    }

    /**
     * 增加月，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment addMonth(long value) {
        if (value != 0) {
            long time = Math.addExact(this.month, value);
            this.addYear(time / 12);
            if ((time %= MONTH_NEXT) < 0) {
                time += MONTH_NEXT;
                this.addYear(-1);
            }
            this.month = Math.toIntExact(time);
            this.update = true;
            int max = getNumberOfDaysMonth();
            int day = getDayAsNum();
            if (day > max) {
                setDay(max);
            }
        }
        return this;
    }

    /**
     * 增加天，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment addDay(long value) {
        if (value != 0) {
            long time = Math.addExact(this.day, value);
            if (time >= 0) {
                for (; time >= (value = MD[leap(this.year)][this.month]); time -= value) addMonth(1);
            } else {
                for (; time < 0; time += MD[leap(this.year)][this.month]) addMonth(-1);
            }
            this.day = (int) time;
            this.update = true;
        }
        return this;
    }

    /**
     * 增加小时，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment addHour(long value) {
        if (value != 0) {
            long time = Math.addExact(this.hour, value);
            this.addDay(time / HOUR_NEXT);
            if ((time %= HOUR_NEXT) < 0) {
                time += HOUR_NEXT;
                this.addDay(-1);
            }
            this.hour = Math.toIntExact(time);
            this.update = true;
        }
        return this;
    }

    /**
     * 增加分钟，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment addMinute(long value) {
        if (value != 0) {
            long time = Math.addExact(this.minute, value);
            this.addHour(time / MINUTE_NEXT);
            if ((time %= MINUTE_NEXT) < 0) {
                time += MINUTE_NEXT;
                this.addHour(-1);
            }
            this.minute = Math.toIntExact(time);
            this.update = true;
        }
        return this;
    }

    /**
     * 增加秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment addSecond(long value) {
        if (value != 0) {
            long time = Math.addExact(this.second, value);
            this.addMinute(time / SECOND_NEXT);
            if ((time %= SECOND_NEXT) < 0) {
                time += SECOND_NEXT;
                this.addMinute(-1);
            }
            this.second = Math.toIntExact(time);
            this.update = true;
        }
        return this;
    }

    /**
     * 增加毫秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment addMillis(long value) {
        if (value != 0) {
            long time = Math.addExact(this.millis, value);
            this.addSecond(time / 1000);
            if ((time %= 1000) < 0) {
                time += 1000;
                this.addSecond(-1);
            }
            this.millis = Math.toIntExact(time);
            this.update = true;
        }
        return this;
    }

    /**
     * 增加微秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment addMicros(long value) {
        if (value != 0) {
            long time = Math.addExact(this.micros, value);
            this.addMillis(time / 1000);
            if ((time %= 1000) < 0) {
                time += 1000;
                this.addMillis(-1);
            }
            this.micros = Math.toIntExact(time);
            this.update = true;
        }
        return this;
    }

    /**
     * 增加纳秒，会根据时间规则计算相应的时间值
     *
     * @param value 增加的时间数值，自然数
     * @return 返回this
     */
    public final Moment addNanos(long value) {
        if (value != 0) {
            long time = Math.addExact(this.nanos, value);
            this.addMicros(time / 1000);
            if ((time %= 1000) < 0) {
                time += 1000;
                this.addMicros(-1);
            }
            this.nanos = Math.toIntExact(time);
            this.update = true;
        }
        return this;
    }
}
