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
package edu.unc.lib.dl.data.ingest.solr.filter.collection;

import static edu.unc.lib.dl.xml.NamespaceConstants.FOXML_URI;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.joda.time.DateTime;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author bbpennel
 * @date Oct 19, 2015
 */
public class DateGraduatedFilter extends CollectionSupplementalInformationFilter {

	private final XPathExpression<Element> notePath;
	private final XPathExpression<Element> dateIssuedPath;
	private final XPathExpression<Element> dateCreatedPath;
	
	private final Pattern semesterDatePattern;
	private final String SPRING_MONTH = "05";
	private final String FALL_MONTH = "12";
	private final String SUMMER_MONTH = "08";
	
	
	public static final String DATE_GRAD_FIELD = "date_graduated_d";
	
	public DateGraduatedFilter() {
		XPathFactory xFactory = XPathFactory.instance();
		Namespace foxmlNS = Namespace.getNamespace("foxml", FOXML_URI);
		List<Namespace> namespaces = Arrays.asList(JDOMNamespaceUtil.MODS_V3_NS, foxmlNS);
		notePath = xFactory.compile("mods:note[@displayLabel = 'Graduated' and @type='thesis']",
				Filters.element(), null, namespaces);
		dateIssuedPath = xFactory.compile("mods:originInfo/mods:dateIssued",
				Filters.element(), null, namespaces);
		dateCreatedPath = xFactory.compile("mods:originInfo/mods:dateCreated",
				Filters.element(), null, namespaces);
		
		semesterDatePattern = Pattern.compile("(Spring|Summer|Fall) (\\d{4})");
	}
	
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		IndexDocumentBean idb = dip.getDocument();
		Element mods = dip.getMods();
		
		if (mods == null) {
			return;
		}
		
		String dateGraduated = getDateGraduated(mods, notePath);
		if (dateGraduated == null) {
			dateGraduated = getDateGraduated(mods, dateIssuedPath);
		}
		if (dateGraduated == null) {
			dateGraduated = getDateGraduated(mods, dateCreatedPath);
		}

		if (dateGraduated != null) {
			Date graduated = getFormattedDateGraduated(dateGraduated);
			if (graduated != null) {
				idb.addDynamicField(DATE_GRAD_FIELD, graduated);
			}
		}
	}
	
	private String getDateGraduated(Element mods, XPathExpression<Element> xpath) {
		Element el = xpath.evaluateFirst(mods);
		if (el != null) {
			return el.getTextNormalize();
		}
		return null;
	}

	private Date getFormattedDateGraduated(String graduatedValue) {
		// From human readable format
		Matcher matcher = semesterDatePattern.matcher(graduatedValue);
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
			
			return DateTimeUtil.parseUTCToDateTime(year + "-" + month).toDate();
		}
		
		// From timestamp format
		try {
			DateTime date = DateTimeUtil.parseUTCToDateTime(graduatedValue);
			
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
			
			return DateTimeUtil.parseUTCToDateTime(date.getYear() + "-" + monthString).toDate();
		} catch (IllegalArgumentException e) {
			// I guess it wasn't a date, nothing can be done to save it
			return null;
		}
	}
}
