package info.dragonlady.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.Vector;

import javax.sql.rowset.serial.SerialBlob;

public class DBAccesser {

	protected static enum DB_STATE_TYPE {
		INT,
		STRING,
		BOOL,
//		BYTE,
		LONG,
//		BIG_DECIMAL,
		FLOAT,
		DOUBLE,
//		SHORT,
		DATE,
//		TIME,
		TIMESTAMP,
		BLOB,
		OBJECT,
	}

	public class ByteImage {
		protected Byte[] image = null;
		
		public ByteImage(byte[] data) {
			if(data != null && data.length > 0) {
				image = new Byte[data.length];
				for(int i=0;i<data.length;i++) {
					image[i] = data[i];
				}
			}
		}
		
		public Byte[] getByteImage() {
			return image;
		}
		
		public byte[] getPrimitiveByteImage() {
			byte result[] = new byte[image.length];
			for(int i=0;i<image.length;i++){
				result[i] = image[i];
			}
			return result;
		}
	}
	
	protected static final String dateFormat = "yyyy/MM/dd HH:mm:ss.SSS";
	
	public class DBStatementParam {
		protected DB_STATE_TYPE stateType;
		protected String value;
		protected ByteImage image;
		protected Object object;
		
		public void setStateType(DB_STATE_TYPE type) {
			stateType = type;
		}
		public void setvalue(String value) {
			this.value = value;
		}
		public void setvalue(ByteImage image) {
			this.image = image;
		}
		public void setvalue(Object object) {
			this.object = object;
		}
		public DB_STATE_TYPE getStateType() {
			return stateType;
		}
		public String getvalue() {
			return value;
		}
		public ByteImage getImage() {
			return image;
		}
		public Object getObject() {
			return object;
		}
	}

	private Properties properties = new Properties();

	/**
	 * 
	 * @param is
	 * @throws InvalidPropertiesFormatException
	 * @throws IOException
	 */
	public DBAccesser(InputStream is) throws InvalidPropertiesFormatException, IOException {
		properties.loadFromXML(is);
	}

	/**
	 * 
	 * @param prop
	 */
	public DBAccesser(Properties prop) {
		properties = prop;
	}

	/**
	 * 
	 * @return
	 * @throws UtilException
	 */
	public Connection getConnection() throws UtilException {
		Connection con = null;
		try {
			String jdbcDriver = properties.getProperty("jdbcDriver");
			String dbUrl = properties.getProperty("dbUrl");
			String dbUserId = properties.getProperty("dbUserId");
			String dbPassword = properties.getProperty("dbPassword");
			Class.forName(jdbcDriver);
			con = DriverManager.getConnection(dbUrl, dbUserId, dbPassword);
			con.setAutoCommit(false);
		}
		catch(Exception e) {
			throw new UtilException(e);
		}
		return con;
	}
	
	/**
	 * 
	 * @param value
	 * @return
	 */
	public DBStatementParam createIntDBParam(int value) {
		DBStatementParam result = new DBStatementParam();
		result.setStateType(DB_STATE_TYPE.INT);
		result.setvalue(String.valueOf(value));
		return result;
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	public DBStatementParam createStringDBParam(String value) {
		DBStatementParam result = new DBStatementParam();
		result.setStateType(DB_STATE_TYPE.STRING);
		result.setvalue(value);
		return result;
	}
	
	/**
	 * 
	 * @param value
	 * @return
	 */
	public DBStatementParam createBooleanDBParam(boolean value) {
		DBStatementParam result = new DBStatementParam();
		result.setStateType(DB_STATE_TYPE.BOOL);
		result.setvalue(String.valueOf(value));
		return result;
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	public DBStatementParam createLongDBParam(long value) {
		DBStatementParam result = new DBStatementParam();
		result.setStateType(DB_STATE_TYPE.LONG);
		result.setvalue(String.valueOf(value));
		return result;
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	public DBStatementParam createFloatDBParam(float value) {
		DBStatementParam result = new DBStatementParam();
		result.setStateType(DB_STATE_TYPE.FLOAT);
		result.setvalue(String.valueOf(value));
		return result;
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	public DBStatementParam createDoubleDBParam(double value) {
		DBStatementParam result = new DBStatementParam();
		result.setStateType(DB_STATE_TYPE.DOUBLE);
		result.setvalue(String.valueOf(value));
		return result;
	}
	
	/**
	 * 
	 * @param value
	 * @return
	 */
	public DBStatementParam createDateDBParam(String value) {
		DBStatementParam result = new DBStatementParam();
		result.setStateType(DB_STATE_TYPE.DATE);
		result.setvalue(value);
		return result;
	}

	/**
	 * 
	 * @param value
	 * @return
	 */
	public DBStatementParam createTimestampDBParam(String value) {
		DBStatementParam result = new DBStatementParam();
		result.setStateType(DB_STATE_TYPE.TIMESTAMP);
		result.setvalue(value);
		return result;
	}
	
	/**
	 * 
	 * @param value
	 * @return
	 */
	public DBStatementParam createBlodDBParam(byte[] value) {
		DBStatementParam result = new DBStatementParam();
		result.setStateType(DB_STATE_TYPE.BLOB);
		ByteImage byteImage = new ByteImage(value);
		result.setvalue(byteImage);
		return result;
	}
	
	/**
	 * 
	 * @param value
	 * @return
	 */
	public DBStatementParam createObjectDBParam(Object value) {
		DBStatementParam result = new DBStatementParam();
		result.setStateType(DB_STATE_TYPE.OBJECT);
		result.setvalue(value);
		return result;
	}
	
	/**
	 * 
	 * @param con
	 * @param statement
	 * @param params
	 * @return
	 * @throws SQLException
	 * @throws ParseException
	 */
	protected PreparedStatement preparedStatement(Connection con, String statement, DBStatementParam... params) throws SQLException, ParseException {
		PreparedStatement st = con.prepareStatement(statement);
		if(params != null && params.length > 0 && params[0] != null) {
			int i=1;
			for(DBStatementParam param:params) {
				if(param.getStateType() == DB_STATE_TYPE.STRING) {
					st.setString(i, param.getvalue());
				}else
				if(param.getStateType() == DB_STATE_TYPE.INT) {
					st.setInt(i, Integer.parseInt(param.getvalue()));
				}else
				if(param.getStateType() == DB_STATE_TYPE.BOOL) {
					st.setBoolean(i, Boolean.parseBoolean(param.getvalue()));
				}else
				if(param.getStateType() == DB_STATE_TYPE.LONG) {
					st.setLong(i, Long.parseLong(param.getvalue()));
				}else
				if(param.getStateType() == DB_STATE_TYPE.DATE) {
					SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
					st.setDate(i, new java.sql.Date(sdf.parse(param.getvalue()).getTime()));
				}else
				if(param.getStateType() == DB_STATE_TYPE.FLOAT) {
					st.setFloat(i, Float.parseFloat(param.getvalue()));
				}else
				if(param.getStateType() == DB_STATE_TYPE.DOUBLE) {
					st.setDouble(i, Double.parseDouble(param.getvalue()));
				}else
				if(param.getStateType() == DB_STATE_TYPE.TIMESTAMP) {
					SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
					st.setTimestamp(i, new Timestamp(sdf.parse(param.getvalue()).getTime()));
				}else
				if(param.getStateType() == DB_STATE_TYPE.BLOB) {
					SerialBlob blob = new SerialBlob(param.getImage().getPrimitiveByteImage());
					st.setBlob(i, blob);
				}else
				if(param.getStateType() == DB_STATE_TYPE.OBJECT) {
					st.setObject(i, param.getObject());
				}
				i++;
			}
		}
		return st;
	}
	
	/**
	 * 
	 * @param sqlKey
	 * @return
	 */
	public String getQuery(String sqlKey) {
		return properties.getProperty(sqlKey);
	}

	/**
	 * 
	 * @param sqlKey
	 * @param con
	 * @param params
	 * @return
	 * @throws UtilException
	 */
	public Vector<HashMap<String, Object>> selectQuery(String sqlKey, Connection con, DBStatementParam[] params) throws UtilException {
		Vector<HashMap<String, Object>> result = new Vector<HashMap<String, Object>>();
		PreparedStatement st = null;
		try {
			String sql = properties.getProperty(sqlKey);
			st = preparedStatement(con, sql, params);
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				ResultSetMetaData meta = rs.getMetaData();
				HashMap<String, Object> record = new HashMap<String, Object>(); 
				for(int i=1;i<=meta.getColumnCount();i++){
					String columnName = meta.getColumnName(i);
					int type = meta.getColumnType(i);
					if(type == Types.VARCHAR) {
						record.put(columnName, rs.getString(columnName));
					}
					if(type == Types.LONGVARCHAR) {
						record.put(columnName, rs.getString(columnName));
					}
					if(type == Types.CLOB) {
						record.put(columnName, rs.getString(columnName));
					}
					if(type == Types.CHAR) {
						record.put(columnName, rs.getString(columnName));
					}
					if(type == Types.NVARCHAR) {
						record.put(columnName, rs.getNCharacterStream(columnName));
					}
					if(type == Types.LONGNVARCHAR) {
						record.put(columnName, rs.getNCharacterStream(columnName));
					}
					if(type == Types.NCLOB) {
						record.put(columnName, rs.getNClob(columnName));
					}
					if(type == Types.NCHAR) {
						record.put(columnName, rs.getNCharacterStream(columnName));
					}
					if(type == Types.INTEGER || type == Types.TINYINT || type == Types.SMALLINT || type == Types.NUMERIC || type == Types.DECIMAL) {
						record.put(columnName, rs.getInt(columnName));
					}
					if(type == Types.BIGINT) {
						try {
							record.put(columnName, rs.getLong(columnName));
						}
						catch(Exception e) {
							record.put(columnName, (java.math.BigInteger)rs.getObject(columnName));
						}
					}
					if(type == Types.DOUBLE) {
						record.put(columnName, rs.getDouble(columnName));
					}
					if(type == Types.FLOAT) {
						record.put(columnName, rs.getFloat(columnName));
					}
					if(type == Types.BOOLEAN) {
						record.put(columnName, rs.getBoolean(columnName));
					}
					if(type == Types.BIT) {
						record.put(columnName, rs.getByte(columnName));
					}
					if(type == Types.DATE || type == Types.TIME) {
						record.put(columnName, rs.getDate(columnName));
					}
					if(type == Types.TIMESTAMP) {
						record.put(columnName, rs.getTimestamp(columnName));
					}
					if(type == Types.BLOB) {
						Blob blob = rs.getBlob(columnName);
						ByteImage image = new ByteImage(blob.getBytes(1, (int)blob.length()));
						record.put(columnName, image);
					}
					if(type == Types.JAVA_OBJECT) {
						record.put(columnName, rs.getObject(columnName));
					}
					if(type == Types.ARRAY) {
						record.put(columnName, rs.getArray(columnName));
					}
					if(type == Types.BLOB) {
						record.put(columnName, rs.getBytes(columnName));
					}
					if(type == Types.SQLXML) {
						record.put(columnName, rs.getSQLXML(columnName));
					}
				}
				result.add(record);
			}
		}
		catch(Exception e) {
			throw new UtilException(e);
		}
		finally {
			if(st != null) {
				try {
					st.close();
				}
				catch(Exception e) {
					//NOP
				}
			}
		}
		return result;
	}

	/**
	 * 
	 * @param sqlKey
	 * @param con
	 * @param params
	 * @return
	 * @throws UtilException
	 */
	public int updateQuery(String sqlKey, Connection con, DBStatementParam[] params) throws UtilException {
		PreparedStatement st = null;
		int result = -1;
		try {
			String sql = properties.getProperty(sqlKey);
			st = preparedStatement(con, sql, params);
			result = st.executeUpdate();
		}
		catch(Exception e) {
			throw new UtilException(e);
		}
		finally {
			if(st != null) {
				try {
					st.close();
				}
				catch(Exception e) {
					//NOP
				}
			}
		}
		return result;
	}

	/**
	 * 
	 * @param sql
	 * @param con
	 * @param params
	 * @return
	 * @throws UtilException
	 */
	public Vector<HashMap<String, Object>> selectQueryWithSQL(String sql, Connection con, DBStatementParam[] params) throws UtilException {
		Vector<HashMap<String, Object>> result = new Vector<HashMap<String, Object>>();
		PreparedStatement st = null;
		try {
			st = preparedStatement(con, sql, params);
			ResultSet rs = st.executeQuery();
			while(rs.next()) {
				ResultSetMetaData meta = rs.getMetaData();
				HashMap<String, Object> record = new HashMap<String, Object>(); 
				for(int i=1;i<=meta.getColumnCount();i++){
					String columnName = meta.getColumnName(i);
					int type = meta.getColumnType(i);
					if(type == Types.VARCHAR) {
						record.put(columnName, rs.getString(columnName));
					}
					if(type == Types.LONGVARCHAR) {
						record.put(columnName, rs.getString(columnName));
					}
					if(type == Types.CLOB) {
						record.put(columnName, rs.getString(columnName));
					}
					if(type == Types.CHAR) {
						record.put(columnName, rs.getString(columnName));
					}
					if(type == Types.NVARCHAR) {
						record.put(columnName, rs.getNCharacterStream(columnName));
					}
					if(type == Types.LONGNVARCHAR) {
						record.put(columnName, rs.getNCharacterStream(columnName));
					}
					if(type == Types.NCLOB) {
						record.put(columnName, rs.getNClob(columnName));
					}
					if(type == Types.NCHAR) {
						record.put(columnName, rs.getNCharacterStream(columnName));
					}
					if(type == Types.INTEGER || type == Types.TINYINT || type == Types.SMALLINT || type == Types.NUMERIC || type == Types.DECIMAL) {
						record.put(columnName, rs.getInt(columnName));
					}
					if(type == Types.BIGINT) {
						try {
							record.put(columnName, rs.getLong(columnName));
						}
						catch(Exception e) {
							record.put(columnName, (java.math.BigInteger)rs.getObject(columnName));
						}
					}
					if(type == Types.DOUBLE) {
						record.put(columnName, rs.getDouble(columnName));
					}
					if(type == Types.FLOAT) {
						record.put(columnName, rs.getFloat(columnName));
					}
					if(type == Types.BOOLEAN) {
						record.put(columnName, rs.getBoolean(columnName));
					}
					if(type == Types.BIT) {
						record.put(columnName, rs.getByte(columnName));
					}
					if(type == Types.DATE || type == Types.TIME) {
						record.put(columnName, rs.getDate(columnName));
					}
					if(type == Types.TIMESTAMP) {
						record.put(columnName, rs.getTimestamp(columnName));
					}
					if(type == Types.BLOB) {
						Blob blob = rs.getBlob(columnName);
						ByteImage image = new ByteImage(blob.getBytes(1, (int)blob.length()));
						record.put(columnName, image);
					}
					if(type == Types.JAVA_OBJECT) {
						record.put(columnName, rs.getObject(columnName));
					}
					if(type == Types.ARRAY) {
						record.put(columnName, rs.getArray(columnName));
					}
					if(type == Types.BLOB) {
						record.put(columnName, rs.getBytes(columnName));
					}
					if(type == Types.SQLXML) {
						record.put(columnName, rs.getSQLXML(columnName));
					}
				}
				result.add(record);
			}
		}
		catch(Exception e) {
			throw new UtilException(e);
		}
		finally {
			if(st != null) {
				try {
					st.close();
				}
				catch(Exception e) {
					//NOP
				}
			}
		}
		return result;
	}

	/**
	 * 
	 * @param sql
	 * @param con
	 * @param params
	 * @return
	 * @throws UtilException
	 */
	public int updateQueryWithSQL(String sql, Connection con, DBStatementParam[] params) throws UtilException {
		PreparedStatement st = null;
		int result = -1;
		try {
			st = preparedStatement(con, sql, params);
			result = st.executeUpdate();
		}
		catch(Exception e) {
			throw new UtilException(e);
		}
		finally {
			if(st != null) {
				try {
					st.close();
				}
				catch(Exception e) {
					//NOP
				}
			}
		}
		return result;
	}
}
