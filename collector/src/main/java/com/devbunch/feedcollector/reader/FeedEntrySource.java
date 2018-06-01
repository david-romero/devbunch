package com.devbunch.feedcollector.reader;

import java.io.Flushable;
import java.io.IOException;
import java.net.URL;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.feed.inbound.FeedEntryMessageSource;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.util.Assert;

import com.rometools.opml.feed.opml.Outline;


final class FeedEntrySource  extends FeedEntryMessageSource implements InitializingBean, Flushable  {

	FeedEntrySource(final Outline outline, MetadataStore store) throws Exception {
		super(new URL(outline.getXmlUrl()), outline.getText());
		Assert.isInstanceOf(InitializingBean.class, store);
		Assert.isInstanceOf(Flushable.class, store);
		this.store = store;
		setMetadataStore(store);
		((InitializingBean)store).afterPropertiesSet();
	}

	volatile MetadataStore store;

	@Override
	public void flush() throws IOException {
		((Flushable)store).flush();
	}
	
	
}
