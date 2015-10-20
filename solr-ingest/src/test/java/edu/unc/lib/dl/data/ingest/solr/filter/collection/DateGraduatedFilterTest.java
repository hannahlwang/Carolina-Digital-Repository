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

import static edu.unc.lib.dl.data.ingest.solr.filter.collection.DateGraduatedFilter.DATE_GRAD_FIELD;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author bbpennel
 * @date Oct 19, 2015
 */
public class DateGraduatedFilterTest {

	@Mock
	private DocumentIndexingPackageDataLoader loader;
	private DocumentIndexingPackageFactory factory;
	
	private DateGraduatedFilter filter;
	
	@Before
	public void setup() throws Exception {
		initMocks(this);
		
		filter = new DateGraduatedFilter();
		
		factory = new DocumentIndexingPackageFactory();
		factory.setDataLoader(loader);
	}
	
	@Test
	public void graduateNoteTest() throws Exception {
		SAXBuilder builder = new SAXBuilder();
		Document mods = builder.build(new FileInputStream(new File(
				"src/test/resources/datastream/etdMODS.xml")));
		when(loader.loadMDDescriptive(any(DocumentIndexingPackage.class))).thenReturn(mods.getRootElement());
		
		DocumentIndexingPackage dip = factory.createDip("uuid:test");
		filter.filter(dip);
		
		IndexDocumentBean idb = dip.getDocument();
		
		assertEquals("Spring 2015", idb.getDynamicFields().get(DATE_GRAD_FIELD));
	}
	
	@Test
	public void graduatedIssuedTest() throws Exception {
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		Element mods = FOXMLJDOMUtil.getDatastreamContent(Datastream.MD_DESCRIPTIVE, foxml);
		when(loader.loadMDDescriptive(any(DocumentIndexingPackage.class))).thenReturn(mods);
		
		DocumentIndexingPackage dip = factory.createDip("uuid:test");
		filter.filter(dip);
		
		IndexDocumentBean idb = dip.getDocument();
		
		assertEquals("Spring 2008", idb.getDynamicFields().get(DATE_GRAD_FIELD));
		
		mods.getChild("originInfo", JDOMNamespaceUtil.MODS_V3_NS).getChild("dateIssued", JDOMNamespaceUtil.MODS_V3_NS)
			.setText("2011-12");
		filter.filter(dip);
		assertEquals("Fall 2011", idb.getDynamicFields().get(DATE_GRAD_FIELD));
		
		mods.getChild("originInfo", JDOMNamespaceUtil.MODS_V3_NS).getChild("dateIssued", JDOMNamespaceUtil.MODS_V3_NS)
			.setText("Summer 2012");
		filter.filter(dip);
		assertEquals("Summer 2012", idb.getDynamicFields().get(DATE_GRAD_FIELD));
	}
}
