package nyaschenko.oki.utils;

import java.util.Map;

public class ApiRequest {
	private final String method;
	private final Map<String, String> params;
	
	public static final String METHOD_GET_CURRENT_USER = "users.getCurrentUser";
	public static final String METHOD_GET_PHOTOS = "photos.getPhotos";
	public static final String METHOD_GET_FRIENDS = "friends.get";
	public static final String METHOD_GET_STREAM = "stream.get";
	
	public static final String PARAM_USER_ID = "fid";
	public static final String PARAM_COUNT = "count";
	
	public ApiRequest(String method, Map<String, String> params) {
		this.method = method;
		this.params = params;
	}
	
	public ApiRequest(String method) {
		this.method = method;
		this.params = null;
	}
	
	@Override
	public String toString() {
		return method;
	}

	public String getMethod() {
		return method;
	}

	public Map<String, String> getParams() {
		return params;
	}
	
	
}
