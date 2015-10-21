/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *
 */
package edu.unc.lib.dl.util;

import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * @author Gregory Jansen
 *
 */
public class DateTimeUtil {
	
	private final static Pattern semesterDatePattern = Pattern.compile("(Spring|Summer|Fall) (\\d{4})");
	private final static String SPRING_MONTH = "05";
	private final static String FALL_MONTH = "12";
	private final static String SUMMER_MONTH = "08";

	public final static DateTimeFormatter utcFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
			.withZone(DateTimeZone.UTC);
	public final static DateTimeFormatter utcYMDFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(
			DateTimeZone.UTC);

	public static Date parseUTCDateToDate(String utcDate) throws ParseException {
		return ISODateTimeFormat.dateParser().parseDateTime(utcDate).toDate();
	}

	public static Date parseUTCToDate(String utcDate) throws ParseException {
		return parseUTCToDateTime(utcDate).toDate();
	}

	public static DateTime parseUTCToDateTime(String utcDate) throws IllegalArgumentException {
		DateTime isoDT = ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(utcDate);
		return isoDT.withZone(DateTimeZone.forID("UTC"));
	}

	public static String formatDateToUTC(Date date) throws ParseException {
		DateTime dateTime = new DateTime(date);
		return utcFormatter.print(dateTime);
	}
	
	public static String parseUTCDateToSemester(String utcDate) {
		DateTime dateTime = parseUTCToDateTime(utcDate);
		
		int month = dateTime.getMonthOfYear();
		if (month >= 10 || month <= 2) {
			return "Fall " + dateTime.getYear();
		} else if (month >= 3 || month <= 6) {
			return "Spring " + dateTime.getYear();
		}
		return "Summer " + dateTime.getYear();
	}
	
	public static Date semesterToDateTime(String yearSemester) {
		Matcher matcher = semesterDatePattern.matcher(yearSemester);
		if (matcher.find()) {
			String semester = matcher.group(1);
			String year = matcher.group(2);
			String month;
			if (semester.equals("Spring")) {
				month = SPRING_MONTH;
			} else if (semester.equals("Fall")) {
				month = FALL_MONTH;
			} else {
				month = SUMMER_MONTH;
			}
			
			return parseUTCToDateTime(year + "-" + month).toDate();
		}
		
		// From timestamp format
		try {
			DateTime date = parseUTCToDateTime(yearSemester);
			
			// Shift exact dates to generic graduation months
			String monthString;
			int month = date.getMonthOfYear();
			if (month >= 10 || month <= 2) {
				monthString = FALL_MONTH;
			} else if (month >= 3 || month <= 6) {
				monthString = SPRING_MONTH;
			} else {
				monthString = SUMMER_MONTH;
			}
			
			return parseUTCToDateTime(date.getYear() + "-" + monthString).toDate();
		} catch (IllegalArgumentException e) {
			// I guess it wasn't a date, nothing can be done to save it
			return null;
		}
	}
}
