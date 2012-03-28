package info.dragonlady.util;

import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Set;

import org.bson.BSONCallback;
import org.bson.BSONDecoder;
import org.bson.BSONEncoder;
import org.bson.BSONObject;
import org.bson.BasicBSONCallback;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.OutputBuffer;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

/**
 * 
 * @author nobu
 *
 */
public class MongoDBAccesser {
	private Properties properties = null;
	private DB mongodb = null;
	
	/**
	 * 
	 * @param is
	 * @throws UtilException
	 */
	public MongoDBAccesser(InputStream is) throws UtilException {
		try {
			properties = new Properties();
			properties.loadFromXML(is);
			initialize();
		}
		catch(Exception e) {
			throw new UtilException(e);
		}
	}

	/**
	 * 
	 * @param prop
	 * @throws UtilException
	 */
	public MongoDBAccesser(Properties prop) throws UtilException {
		try {
			properties = new Properties();
			properties = prop;
			initialize();
		}
		catch(Exception e) {
			throw new UtilException(e);
		}
	}
	
	/**
	 * 
	 * @throws UnknownHostException
	 * @throws MongoException
	 * @throws UtilException
	 */
	private void initialize() throws UnknownHostException, MongoException, UtilException {
		if(properties != null) {
			Mongo m = null;
			
			if(properties.getProperty("host") != null && properties.getProperty("db") != null) {
				int port = -1;
				if(properties.getProperty("port") != null) {
					try {
						port = Integer.parseInt(properties.getProperty("port"));
					}
					finally {
						//NOP
					}
				}
				if(port > 0) {
					m = new Mongo(properties.getProperty("host"), port);
				}else{
					m = new Mongo(properties.getProperty("host"));
				}
				mongodb = m.getDB(properties.getProperty("db"));
				if(properties.getProperty("userid") != null && properties.getProperty("password") != null) {
					if(!mongodb.authenticate(properties.getProperty("userid"), properties.getProperty("password").toCharArray())) {
						throw new UtilException("MongoDB Authentication error");
					}
				}
			}else{
				throw new UtilException("MongoDB host or db not found.");
			}
		}else{
			throw new UtilException("MongoDB config not found.");
		}
	}
	
	/**
	 * 
	 */
	public void close() {
		if(mongodb != null) {
			mongodb.getMongo().close();
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public Mongo getMongo() {
		if(mongodb != null) {
			return mongodb.getMongo();
		}
		return null;
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 * @throws UtilException 
	 */
	public DBCollection getCollection(String name) throws UtilException {
		if(mongodb != null) {
			Set<String> collections = mongodb.getCollectionNames();
			for(String collection : collections) {
				if(collection.trim().equals(name)) {
					return mongodb.getCollection(name);
				}
			}
			throw new UtilException("Collection not found on MongoDB");
		}else{
			throw new UtilException("MongoDB not found.");
		}
	}
	
	public DBCollection createCollection(String name) {
		return mongodb.getCollection(name);
	}
	
	/**
	 * 
	 * @return
	 */
	public BasicDBObject createDBObject() {
		return new BasicDBObject();
	}
	
	/**
	 * 
	 * @return
	 */
	public BasicDBObject createDBQuery() {
		return new BasicDBObject();
	}
	
	/**
	 * 
	 * @return
	 */
	public BasicDBList createDBList() {
		return new BasicDBList();
	}
	/**
	 * 
	 * @param object
	 * @return
	 */
	public OutputBuffer encodeObject(BSONObject object) {
		BSONEncoder enc = new BasicBSONEncoder();
		OutputBuffer buf = new BasicOutputBuffer();
		enc.set(buf);
		enc.putObject(object);
		enc.done();
		return buf;
	}
	
	/**
	 * 
	 * @param outbuf
	 * @return
	 */
	public BasicDBObject decodeObject(OutputBuffer outbuf) {
		BSONDecoder dec = new BasicBSONDecoder();
		BSONCallback callback = new BasicBSONCallback();
		int size = dec.decode(outbuf.toByteArray(), callback);
		if(size > 0) {
			return new BasicDBObject(((BSONObject)callback.get()).toMap());
		}
		
		return new BasicDBObject();
	}
}
