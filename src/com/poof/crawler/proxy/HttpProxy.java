package com.poof.crawler.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.poof.crawler.utils.HttpStatus;
import com.poof.crawler.utils.IpUtils;

/**
 * @author <a href="mailto:admin@wilkey.vip">wilkey</a>
 * @version create at 2016年3月27日 10:47
 */
public class HttpProxy {

	private final static Logger logger = LoggerFactory.getLogger(HttpProxy.class);

	public final static int DEFAULT_REUSE_TIME_INTERVAL = 0;// ms，从一次请求结束到再次可以请求的默认时间间隔
	public final static int FAIL_REVIVE_TIME_INTERVAL = 2 * 60 * 60 * 1000; // ms,请求失败，重试的时间间隔

	private Proxy proxy;
	private InetAddress localAddr;
	private String username;
	private String password;

	private int reuseTimeInterval = 0;
	private Long canReuseTime = 0L; // 当前Proxy可重用的时间,纳秒

	private int failedNum = 0;
	private int borrowNum = 0;

	private Map<HttpStatus, Integer> countErrorStatus = new HashMap<HttpStatus, Integer>();

	public HttpProxy(String address, int port, String username, String password) {
		this(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(address, port)), username, password, 0, 0, DEFAULT_REUSE_TIME_INTERVAL);
	}

	public HttpProxy(String address, int port) {
		this(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(address, port)), 0, 0, DEFAULT_REUSE_TIME_INTERVAL);
	}
	
	public HttpProxy(Proxy.Type type, String address, int port) {
		this(new Proxy(type, new InetSocketAddress(address, port)), 0, 0, DEFAULT_REUSE_TIME_INTERVAL);
	}

	public HttpProxy(Proxy proxy) {
		this(proxy, 0, 0, DEFAULT_REUSE_TIME_INTERVAL);
	}

	public HttpProxy(Proxy proxy, String username, String password) {
		this(proxy, username, password, 0, 0, DEFAULT_REUSE_TIME_INTERVAL);
	}

	public HttpProxy(Proxy proxy, int borrowNum, int failedNum, int reuseTimeInterval) {
		this.localAddr = IpUtils.getLocalAddr(); // 获取当前机器的ip地址
		if (localAddr == null) {
			logger.error("cannot get local IP!");
			System.exit(0);
		}
		this.proxy = proxy;
		this.canReuseTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(reuseTimeInterval, TimeUnit.MILLISECONDS);
	}

	public HttpProxy(Proxy proxy, String username, String password, int borrowNum, int failedNum, int reuseTimeInterval) {
		this.localAddr = IpUtils.getLocalAddr(); // 获取当前机器的ip地址
		if (localAddr == null) {
			logger.error("cannot get local IP!");
			System.exit(0);
		}
		this.proxy = proxy;
		this.username = username;
		this.password = password;
		this.canReuseTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(reuseTimeInterval, TimeUnit.MILLISECONDS);
	}

	/**
	 * 检查本地机器和Proxy之间的连通性
	 *
	 * @return
	 */
	public boolean check() {
		boolean isReachable = false;
		Socket socket = null;
		try {
			socket = new Socket();
			socket.bind(new InetSocketAddress(localAddr, 0));
			socket.connect(proxy.address(), 1000 * 15);
			isReachable = true;
		} catch (Exception e) {
			logger.error("bad proxy >>>" + this.proxy.toString());
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return isReachable;
	}

	public void success() {
		this.failedNum = 0; // 将 failNum 清零
		countErrorStatus.clear();
	}

	/**
	 * 代理失败，记录相应的错误状态码
	 *
	 * @param httpStatus
	 */
	public void fail(HttpStatus httpStatus) {
		if (countErrorStatus.containsKey(httpStatus)) {
			countErrorStatus.put(httpStatus, countErrorStatus.get(httpStatus) + 1);
		} else {
			countErrorStatus.put(httpStatus, 1);
		}
		this.failedNum++;
	}

	/**
	 * 输出错误请求的日志
	 *
	 * @return
	 */
	public String countErrorStatus() {
		StringBuilder stringBuilder = new StringBuilder();
		for (Map.Entry<HttpStatus, Integer> entry : countErrorStatus.entrySet()) {
			stringBuilder.append(entry.getKey().name()).append("_").append(entry.getKey().getCode()).append("->").append(entry.getValue());
		}
		return stringBuilder.toString();
	}

	/**
	 * 对请求进行计数
	 */
	public void borrow() {
		this.borrowNum++;
	}

	public int getFailedNum() {
		return failedNum;
	}

	public int getBorrowNum() {
		return borrowNum;
	}

	public void setReuseTimeInterval(int reuseTimeInterval) {
		this.reuseTimeInterval = reuseTimeInterval;
		this.canReuseTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(reuseTimeInterval, TimeUnit.MILLISECONDS);
	}

	public int getReuseTimeInterval() {
		return this.reuseTimeInterval;
	}

	public Proxy getProxy() {
		return proxy;
	}

	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	public int compareTo(Delayed o) {
		HttpProxy that = (HttpProxy) o;
		return canReuseTime > that.canReuseTime ? 1 : (canReuseTime < that.canReuseTime ? -1 : 0);
	}

	public long getDelay(TimeUnit unit) {
		return unit.convert(canReuseTime - System.nanoTime(), TimeUnit.NANOSECONDS);
	}

	@Override
	public String toString() {
		return this.proxy.toString() + " 使用:" + borrowNum + "次 ";
	}

	public String getKey() {
		InetSocketAddress address = (InetSocketAddress) proxy.address();
		return address.getAddress().getHostAddress() + ":" + address.getPort();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
