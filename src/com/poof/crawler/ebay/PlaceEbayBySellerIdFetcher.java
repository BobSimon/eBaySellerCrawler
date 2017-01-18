package com.poof.crawler.ebay;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.poof.crawler.db.entity.ProxyHost;
import com.poof.crawler.db.entity.Schedule;
import com.poof.crawler.utils.dom.ListingParser;
import com.poof.crawler.utils.pool.SellerIDListPool;

public class PlaceEbayBySellerIdFetcher  extends PlaceEbayFetcher implements Runnable{
	private static Logger log = Logger.getLogger(PlaceEbayBySellerIdFetcher.class);

	private Schedule schedule;
	
	public PlaceEbayBySellerIdFetcher(Schedule schedule) {
		this.schedule = schedule;
	}

	@Override
	public void run() {

		log.info("starting [PlaceEbayBySellerId] thread name: [" + schedule.getName() + "], site: [" + schedule.getSite() + "], searchterm: [" + schedule.getSearchTerm() + "]");
		List<ProxyHost> proxies = getProxyHost();
		Collections.shuffle(proxies);

		try {
			SellerIDListPool.getInstance().execute(new ListingParser(schedule, String.format(PRE_URL, schedule.getSite(), URLEncoder.encode(String.format(SELLERID_LIST_URL, schedule.getSite(), schedule.getSearchTerm(), 1), "UTF-8")) + END_URL, proxies.get(0)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
