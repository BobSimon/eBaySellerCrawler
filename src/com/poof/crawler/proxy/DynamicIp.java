package com.poof.crawler.proxy;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.poof.crawler.db.DBUtil;
import com.poof.crawler.db.entity.ProxyHost;

public class DynamicIp implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(DynamicIp.class);
	private final static String order = "";
	private static volatile long gettimes = 0;
	private static List<HttpProxy> lastIP = new ArrayList<HttpProxy>();

	public static void main(String[] args) throws InterruptedException {
		for (int i = 0; i < 100; i++) {
			new Runnable() {
				public void run() {
					try {
						TimeUnit.MILLISECONDS.sleep(new Random().nextInt(1000));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// List<HttpProxy> proxys = DynamicIp.getIPList();
					// System.err.println(proxys.get(0).toString());
				}
			}.run();
			;
		}
	}

	@Override
	public void run() {
		while (true) {

			try {
				java.net.URL url = new java.net.URL("http://api.ip.data5u.com/dynamic/get.html?order=" + order + "&ttl");
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(3000);
				connection.setReadTimeout(3000);
				connection = (HttpURLConnection) url.openConnection();

				InputStream raw = connection.getInputStream();
				InputStream in = new BufferedInputStream(raw);
				byte[] data = new byte[in.available()];
				int bytesRead = 0;
				int offset = 0;
				while (offset < data.length) {
					bytesRead = in.read(data, offset, data.length - offset);
					if (bytesRead == -1) {
						break;
					}
					offset += bytesRead;
				}
				gettimes++;
				in.close();
				raw.close();
				String[] res = new String(data, "UTF-8").split("\n");
				List<HttpProxy> newip = new ArrayList<HttpProxy>();
				for (String ip : res) {
					String[] parts = ip.split(",");
					parts = parts[0].split(":");
					if (parts.length == 2) {
						HttpProxy proxy = new HttpProxy(Proxy.Type.HTTP, parts[0], Integer.valueOf(parts[1]));
						System.err.println("new :" + proxy);
						newip.add(proxy);
					}
				}

				if (null != newip && !newip.isEmpty()) {
					lastIP.clear();
					lastIP.addAll(newip);
					System.out.println(">>>>>>>>>>>>>>第" + gettimes + "次获取动态IP " + lastIP.size() + " 个");
					// log.info("api.ip.data5u.com/dynamic/get.html>>>>>>>>>>>>>>第"
					// + gettimes + "次获取动态IP " + lastIP.size() + " 个");
				} else {
					// log.error("api.ip.data5u.com/dynamic/get.html>>>>>>>>>>>>>>获取IP为空");
				}
			} catch (Exception e) {
				e.printStackTrace();
				// log.error("api.ip.data5u.com/dynamic/get.html>>>>>>>>>>>>>>获取IP出错",
				// e);
			} finally {
				try {
					TimeUnit.SECONDS.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized static HttpProxy getAProxy() {
		if (lastIP.size() > 0) {
			HttpProxy ip = lastIP.get((int) (Math.random() * lastIP.size()));
			return ip;
		}
		return null;
	}

	public synchronized static HttpProxy getAProxy(Integer proxyIndex) {
		if (lastIP.size() > 0) {
			HttpProxy ip = lastIP.size() < proxyIndex ? lastIP.get(proxyIndex) : lastIP.get((int) (Math.random() * lastIP.size()));
			return ip;
		}
		return null;
	}

	private static volatile ProxyPool proxyPool = null;

	public static ProxyPool getProxyHost() {
		if (proxyPool == null) {
			try {
				proxyPool = new ProxyPool();
				List<ProxyHost> list = DBUtil.queryBeanList(DBUtil.openConnection(), "select * from t_proxy_host", ProxyHost.class);
				for (ProxyHost proxyHost : list) {
					proxyPool.add(proxyHost.getIp(), proxyHost.getPort(), proxyHost.getUsername(), proxyHost.getPwd());
				}
			} catch (Exception e) {
				e.printStackTrace();
				log.error(log.getName() + " : program error: " + e);
			} finally {
			}
		}

		return proxyPool;
	}
}
