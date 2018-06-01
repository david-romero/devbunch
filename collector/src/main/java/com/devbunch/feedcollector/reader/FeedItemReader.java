package com.devbunch.feedcollector.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.batch.item.adapter.ItemReaderAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.xml.sax.InputSource;

import com.google.common.collect.Maps;
import com.rometools.opml.feed.opml.Opml;
import com.rometools.opml.feed.opml.Outline;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.WireFeedInput;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FeedItemReader extends ItemReaderAdapter<Entry<String, SyndEntry>> {

	private final List<FeedEntrySource> currentEntriesToRead;

	private final List<FeedEntrySource> queue;
	
	private FeedEntrySource currentMessageSource;

	@Autowired
	public FeedItemReader(final MetadataStore metadataStore,
			@Value("classpath:opml/engineering_blogs_short.opml") final Resource opml) throws Exception {
		final WireFeedInput input = new WireFeedInput();
		Opml feed = (Opml) input.build(new InputSource(opml.getInputStream()));
		final List<Outline> outlines = feed.getOutlines();
		final List<Outline> outlinesToRead = !CollectionUtils.isEmpty(outlines) ? outlines.get(0).getChildren()
				: Collections.emptyList();

		currentEntriesToRead = new ArrayList<>();
		for (Outline outline : outlinesToRead) {
			final FeedEntrySource feedSource = new FeedEntrySource(outline, metadataStore);
			feedSource.setComponentName(outline.getText());
			feedSource.afterPropertiesSet();
			currentEntriesToRead.add(feedSource);
		}
		queue = new ArrayList<>(currentEntriesToRead);
		currentMessageSource = queue.remove(0);
	}

	@Override
	public void afterPropertiesSet() {
		super.setTargetObject(this);
		super.setTargetMethod("read");
	}

	@Override
	public Entry<String, SyndEntry> read() throws IOException {
		final Message<SyndEntry> message = receive();
		if (message != null) {
			final SyndEntry entry = message.getPayload();
			return Maps.immutableEntry(currentMessageSource.getComponentName(), entry);
		} else if (currentMessageSource != null) {
			take();
			return read();
		} else {
			backToStartState();
			return null;
		}
	}


	private Message<SyndEntry> receive() {
		if (currentMessageSource != null) {
			try {
				return currentMessageSource.receive();
			} catch (MessagingException oops) {
				log.error("Unable to read feed from " + currentMessageSource.getComponentName(), oops);
				return null;
			}
		} else {
			return null;
		}
	}

	private void take() {
		if (!queue.isEmpty()) {
			currentMessageSource = queue.remove(0);
		} else {
			currentMessageSource = null;
		}
	}

	private void backToStartState() throws IOException {
		this.queue.addAll(currentEntriesToRead);
		take();
		currentMessageSource.flush();
	}
}
