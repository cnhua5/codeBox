import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

public class Connection {

	public static final int CONN_MODE_GET = 1;

	public static final int CONN_MODE_POST = 2;

	private int connMode = 1;

	private URL mUrl;

	protected Parameter mParameter;

	protected byte[] mPostData;

	private String mContentType;

	private JSONObject errorMsg;

	public Connection(URL url, JSONObject errorMsg) {
		this.mUrl = url;
		this.errorMsg = errorMsg;
	}

	public void setConnMode(int connMode) {
		this.connMode = connMode;
	}

	public void addParams(Parameter param) {
		this.mParameter = param;
	}

	public void setPostDataBytes(byte[] postData) {
		this.mPostData = postData;
	}

	public void setContentType(String contentType) {
		this.mContentType = contentType;
	}

	public JSONObject requestJson() {
		ByteArrayOutputStream bayos = new ByteArrayOutputStream();
		NetworkError result = request(bayos);
		if (result == NetworkError.OK) {
			try {
				String json = bayos.toString("UTF-8");
				bayos.close();
				return new JSONObject(json);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public String requestString() {
		ByteArrayOutputStream bayos = new ByteArrayOutputStream();
		NetworkError result = request(bayos);
		if (result == NetworkError.OK) {
			try {
				String str = bayos.toString("UTF-8");
				bayos.close();
				return str;
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private NetworkError request(ByteArrayOutputStream bayos) {
		String url = "";
		if (connMode == CONN_MODE_GET && mParameter != null && !mParameter.isEmpty()) {
			String query = mUrl.getQuery();
			String baseUrl = mUrl.toString();
			String s4;
			if (TextUtils.isEmpty(query))
				s4 = (new StringBuilder()).append(baseUrl).append("?").append(mParameter.toEncodedString()).toString();
			else
				s4 = (new StringBuilder()).append(baseUrl).append("&").append(mParameter.toEncodedString()).toString();
			url = s4;
			return requestInner(url, bayos);
		}
		return requestInner(mUrl.toString(), bayos);
	}

	private NetworkError requestInner(String url, ByteArrayOutputStream bayos) {

		int statusCodeInt = 0;
		try {
			URL u = new URL(url);
			HttpURLConnection httpurlconnection = (HttpURLConnection) u.openConnection();
			httpurlconnection.setConnectTimeout(10000);
			if (YummyUtil.isWifiConnected()) {
				httpurlconnection.setReadTimeout(10000);
			} else {
				httpurlconnection.setReadTimeout(30000);
			}

			if (connMode == CONN_MODE_GET) {
				httpurlconnection.setRequestMethod("GET");
				httpurlconnection.setDoOutput(false);
			} else if (connMode == CONN_MODE_POST) {
				httpurlconnection.setRequestMethod("POST");
				httpurlconnection.setDoOutput(true);
				httpurlconnection.setUseCaches(false);
				if (mPostData != null && mPostData.length > 0) {
					httpurlconnection.setRequestProperty("Content-Length", Integer.toString(mPostData.length));
				}
				if (!TextUtils.isEmpty(mContentType)) {
					httpurlconnection.setRequestProperty("Content-Type", mContentType);
				}
				if (mParameter != null && !mParameter.isEmpty()) {
					TreeMap<String, String> params = mParameter.getParams();

					for (Iterator<String> iterator = params.keySet().iterator(); iterator.hasNext();) {
						String key = (String) iterator.next();
						String val = params.get(key);
						httpurlconnection.setRequestProperty(key, val);

					}
				}

			}

			YummyUtil.printLog("Make Connect : " + url + "\n" + ((mPostData == null) ? "" : new String(mPostData)));

			httpurlconnection.connect();

			if (mPostData != null && mPostData.length > 0) {
				OutputStream outputstream = httpurlconnection.getOutputStream();
				outputstream.write(mPostData);
				outputstream.close();
			}

			NetworkError statusCode = handleResponseCode(httpurlconnection.getResponseCode());

			statusCodeInt = httpurlconnection.getResponseCode();

			YummyUtil.printLog("Make Connect : statusCode(" + httpurlconnection.getResponseCode() + ");");

			if (statusCode == NetworkError.OK) {

				if (bayos != null) {
					BufferedInputStream bufferedinputstream1 = new BufferedInputStream(httpurlconnection.getInputStream(), 8192);
					byte[] abyte1 = new byte[1024];
					int j;
					while ((j = bufferedinputstream1.read(abyte1, 0, 1024)) > 0) {
						bayos.write(abyte1, 0, j);
					}
					bayos.flush();
					bufferedinputstream1.close();
				}
				if (httpurlconnection != null) {
					httpurlconnection.disconnect();
				}
			} else {
				if (bayos != null) {
					BufferedInputStream bufferedinputstream1 = new BufferedInputStream(httpurlconnection.getInputStream(), 8192);
					byte[] abyte1 = new byte[1024];
					int j;
					while ((j = bufferedinputstream1.read(abyte1, 0, 1024)) > 0) {
						bayos.write(abyte1, 0, j);
					}
					bayos.flush();

					YummyUtil.printLog("Make Connect : " + url + ", Error Result:  " + httpurlconnection.getResponseCode() + ", Response: "
							+ bayos.toString("UTF-8"));

					bayos.close();
					bufferedinputstream1.close();
				}
				if (httpurlconnection != null) {
					httpurlconnection.disconnect();
				}
			}
			return statusCode;
		} catch (Exception e) {
			e.printStackTrace();
			try {
				errorMsg.put("errorMsg", ExceptionUtil.getErrorInfoFromException(e) + "\n statusCode(" + statusCodeInt + ")");
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
		return NetworkError.CLIENT_ERROR;
	}

	private NetworkError handleResponseCode(int i) {
		if (i == 200)
			return NetworkError.OK;
		if (i == 401) {
			return NetworkError.AUTH_ERROR;
		} else {
			return NetworkError.SERVER_ERROR;
		}
	}

	public enum NetworkError {
		OK, URL_ERROR, NETWORK_ERROR, AUTH_ERROR, CLIENT_ERROR, SERVER_ERROR, RESULT_ERROR, UNKNOWN_ERROR
	}

	private static StringBuilder appendParameter(StringBuilder stringbuilder, String s, String s1) {
		if (stringbuilder.length() > 0)
			stringbuilder.append("&");
		stringbuilder.append(s);
		stringbuilder.append("=");
		stringbuilder.append(s1);
		return stringbuilder;
	}

	private static StringBuilder appendParameter(StringBuilder stringbuilder, String s, String s1, char c) {
		if (stringbuilder.length() > 0)
			stringbuilder.append(c);
		stringbuilder.append(s);
		stringbuilder.append("=");
		stringbuilder.append(s1);
		return stringbuilder;
	}

	private static StringBuilder appendParameter(StringBuilder stringbuilder, String s, String s1, String s2) {
		if (stringbuilder.length() > 0)
			stringbuilder.append("&");
		stringbuilder.append(s);
		stringbuilder.append("=");
		try {
			stringbuilder.append(URLEncoder.encode(s1, s2));
		} catch (UnsupportedEncodingException unsupportedencodingexception) {
			return stringbuilder;
		}
		return stringbuilder;
	}

	public class Parameter {
		private boolean disallowEmptyValue;
		private TreeMap<String, String> params;

		public Parameter() {
			this(true);
		}

		public Parameter(boolean bindToConnection) {
			params = new TreeMap<String, String>();
			if (bindToConnection) {
				mParameter = this;
			}
		}

		public Parameter add(String paramString, int paramInt) {
			this.params.put(paramString, String.valueOf(paramInt));
			return this;
		}

		public Parameter add(String paramString, Object paramObject) {
			if (paramObject == null) {
				if (this.disallowEmptyValue)
					return this;
				paramObject = "";
			}
			this.params.put(paramString, String.valueOf(paramObject));
			return this;
		}

		public Parameter add(String paramString1, String paramString2) {
			if (TextUtils.isEmpty(paramString2)) {
				if (this.disallowEmptyValue)
					return this;
				paramString2 = "";
			}
			this.params.put(paramString1, paramString2);
			return this;
		}

		public Parameter add(String paramString, boolean paramBoolean) {
			this.params.put(paramString, String.valueOf(paramBoolean));
			return this;
		}

		public TreeMap<String, String> getParams() {
			return this.params;
		}

		public boolean isEmpty() {
			return this.params.isEmpty();
		}

		public void setDisallowEmptyValue(boolean paramBoolean) {
			this.disallowEmptyValue = paramBoolean;
		}

		public String toEncodedString() {
			return toEncodedString("UTF-8");
		}

		public String toEncodedString(String paramString) {
			if (this.params.isEmpty())
				return "";
			StringBuilder localStringBuilder = new StringBuilder();
			Iterator<String> localIterator = this.params.keySet().iterator();
			while (localIterator.hasNext()) {
				String str = localIterator.next();
				localStringBuilder = Connection.appendParameter(localStringBuilder, str, (String) this.params.get(str), paramString);
			}
			return localStringBuilder.toString();
		}

		public String toString() {
			return toString('&');
		}

		public String toString(char paramChar) {
			if (this.params.isEmpty())
				return "";
			StringBuilder localStringBuilder = new StringBuilder();
			Iterator<String> localIterator = this.params.keySet().iterator();
			while (localIterator.hasNext()) {
				String str = (String) localIterator.next();
				localStringBuilder = Connection.appendParameter(localStringBuilder, str, (String) this.params.get(str), paramChar);
			}
			return localStringBuilder.toString();
		}
	}
}


class ExceptionUtil {
	public static String getErrorInfoFromException(Exception e) {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			return sw.toString();
		} catch (Exception e2) {
			return "bad getErrorInfoFromException";
		}
	}
}
