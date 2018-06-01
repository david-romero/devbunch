package com.devbunch.feedcollector.reader;

import java.io.File;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.rometools.rome.feed.synd.SyndEntry;

@RunWith(MockitoJUnitRunner.class)
public class FeedItemReaderTest {

	@Mock
	private PropertiesPersistingMetadataStore metadataStore;

	private Resource opml = new ClassPathResource("opml/engineering_blogs_test.opml");

	private Resource _500Px = new ClassPathResource("opml/500px.xml");
	private Resource _8thlight = new ClassPathResource("opml/8thlight.xml");
	private Resource _99designs = new ClassPathResource("opml/99designs.xml");
	private Resource build_addepar = new ClassPathResource("opml/build-addepar.xml");

	private FeedItemReader feedItemReader;

	@Before
	public void setUp() throws Exception {
		final List<String> lines = Files.readLines(opml.getFile(), Charsets.UTF_8);
		final StringBuilder linesToWrite = new StringBuilder();
		for (final String line : lines) {
			if (line.contains(_500Px.getFilename())) {
				linesToWrite.append(line.replace(_500Px.getFilename(), _500Px.getURL().toString()));
			} else if (line.contains(_8thlight.getFilename())) {
				linesToWrite.append(line.replace(_8thlight.getFilename(), _8thlight.getURL().toString()));
			} else if (line.contains(_99designs.getFilename())) {
				linesToWrite.append(line.replace(_99designs.getFilename(), _99designs.getURL().toString()));
			} else if (line.contains(build_addepar.getFilename())) {
				linesToWrite.append(line.replace(build_addepar.getFilename(), build_addepar.getURL().toString()));
			} else {
				linesToWrite.append(line);
			}
		}
		File opmlTemp = File.createTempFile("feeds", ".ompl");
		Files.write(linesToWrite.toString().getBytes(Charsets.UTF_8), opmlTemp);
		feedItemReader = new FeedItemReader(metadataStore, new FileSystemResource(opmlTemp));
	}

	@Test
	public void givenAnEmptyMetadataStoreWhenAFeedIsReadThenTheFirstFeedShouldBeReturned() throws Exception {
		// given
		
		// when
		Entry<String, SyndEntry> tuple = feedItemReader.read();

		// then
		Assert.assertNotNull(tuple);
		Assert.assertEquals("500px", tuple.getKey());
		Assert.assertEquals("Javier Ruiz", tuple.getValue().getAuthor());
		Assert.assertEquals(Date.from(LocalDateTime.of(2016, Month.DECEMBER.getValue(), 16, 18, 57,55).toInstant(ZoneOffset.UTC)), tuple.getValue().getPublishedDate());

	}
	

}
