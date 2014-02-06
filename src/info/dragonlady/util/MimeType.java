package info.dragonlady.util;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

public final class MimeType {
	
	public enum MEDIA_TYPE {
		VIDEO,
		AUDIO,
		IMAGE,
		TEXT
	}

	private static final HashMap<String, String> mimeMap = new HashMap<String, String>();
	
	private MimeType(Vector<String> mimeList) throws UtilException {
		try {
			if(mimeMap.size() < 1){
				for(String mime : mimeList) {
					if(mime != null && mime.length() > 0 && mime.indexOf(",") > 0) {
						String mimeData[] = mime.split(",");
						mimeMap.put(mimeData[0], mimeData[1]);
					}
				}
			}
		}
		catch(Exception e) {
			throw new UtilException(e);
		}
	}
	
	public static final MimeType getMimeType(Vector<String> mimeList) throws UtilException {
		return new MimeType(mimeList);
	}

	public String getExtendNameFromMimeType(String mimeType) {
		if(mimeMap.containsKey(mimeType)) {
			return mimeMap.get(mimeType);
		}
		return null;
	}
	
	public String[] getExtendNameFromMimeSubType(String subType) {
		Vector<String> result = new Vector<String>();
		Set<String>keys = mimeMap.keySet();
		for(String key : keys) {
			String keySubType = key.substring(key.indexOf("/")+1);
			if(keySubType != null && keySubType.length() > 0 && subType != null && keySubType.toLowerCase().equals(subType.toLowerCase())) {
				result.add(mimeMap.get(key));
			}
		}
		String dummy[] = {new String()};
		return result.toArray(dummy);
	}
	
	public String[] getMimeTypeFromExtendName(String name) {
		Vector<String> result = new Vector<String>();
		if(mimeMap.containsValue(name)) {
			Set<String>keys = mimeMap.keySet();
			for(String key : keys) {
				if(mimeMap.get(key).equals(name)) {
					result.add(key);
				}
			}
		}
		String dummy[] = {new String()};
		return result.toArray(dummy);
	}
	
	public boolean isBinalyData(String mimeType) {
		if(getExtendNameFromMimeType(mimeType) != null) {
			return true;
		}
		if(getExtendNameFromMimeSubType(mimeType).length > 0) {
			return true;
		}
		return false;
	}
	
	public MEDIA_TYPE getMediaTypeFromMimeType(String mimeType) {
		if(getExtendNameFromMimeType(mimeType) != null) {
			String type = mimeType.substring(0, mimeType.indexOf("/"));
			if(MEDIA_TYPE.VIDEO.toString().toLowerCase().equals(type)) {
				return MEDIA_TYPE.VIDEO;
			}
			if(MEDIA_TYPE.AUDIO.toString().toLowerCase().equals(type)) {
				return MEDIA_TYPE.AUDIO;
			}
			if(MEDIA_TYPE.IMAGE.toString().toLowerCase().equals(type)) {
				return MEDIA_TYPE.IMAGE;
			}
			if(type.toLowerCase().equals("application")) {
				return MEDIA_TYPE.AUDIO;
			}
		}
		
		return MEDIA_TYPE.TEXT;
	}
}
