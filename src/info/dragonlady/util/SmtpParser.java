package info.dragonlady.util;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import info.dragonlady.util.schema.annotation.GPS;

import org.apache.commons.codec.binary.Base64;

/**
 * 
 * @author nobu
 *
 */
public class SmtpParser {
	protected static final String receivedHeader     = "Received:";
	protected static final String fromHeader         = "From:";
	protected static final String toHeader           = "To:";
	protected static final String subjetHeader       = "Subject:";
	protected static final String dateHeader         = "Date:";
	protected static final String binaryEncodeHeader = "[Cc]ontent-[Tt]ransfer-[Ee]ncoding:";
	protected static final String delimiterRegEx1    = "^[Cc]ontent-[Tt]ype:.*[Mm]ultipart/[Mm]ixed.*";
	protected static final String delimiterRegEx2    = ".+[Bb]oundary=(.+)";
	protected static final String charsetRegEx       = "^[Cc]ontent-[Tt]ype:\\s*text/plain.+[Cc]harset=(.+)";
	protected static final String mimeTypeRegEx      = "^[Cc]ontent-[Tt]ype:\\s*(.+);.*";
	protected static final String fileNameRegEx      = ".+[Nn]ame=(.+).*";
	//comment out in Google App Engin
//	protected static final Dimension QVGDimension    = new Dimension(240, 320);
	protected static final double defualtResizePercent = 0.75;  //75%
	
	protected String receivedHeaderValue      = "";
	protected String fromHeaderValue          = "";
	protected String toHeaderValue            = "";
	protected String subjectHeaderValue       = "";
	protected String binaryEncoderHeaderValue = "";
	protected String delimiterValue           = "";
	protected String charsetValue             = "";
	protected String fileNameValue            = "";
	protected String bodyValue                = "";
	protected String defaultSubject           = "";
	protected Calendar contributeDate         = null;
	protected Vector<ByteImage> imageBuffer      = new Vector<ByteImage>();
	protected boolean reduceImage             = false;
	protected boolean removeTransparency      = false;
	protected Properties properties = new Properties();
	protected MimeType mt;
	protected boolean startSubject            = false;
	protected boolean startFromAddr           = false;
	
	protected enum EXIF_TYPE {
		MOTOROLA,
		INTEL
	}
	
	/**
	 * EXIF�Ǘ��N���X 
	 * @author nobu
	 */
	public class EntryOfIFD {
		protected byte subExifTag[] = {(byte)0x87, (byte)0x69};
		protected byte GPSTag[] = {(byte)0x88, (byte)0x25};
		protected byte OriginalDateTag[] = {(byte)0x90, (byte)0x03};
		protected EXIF_TYPE exifType = null;
		
		public Byte tag[]    = new Byte[2];
		public Byte format[] = new Byte[2];
		public Byte count[]  = new Byte[4];
		public Byte data[]   = new Byte[4];
		
		/**
		 * �R���X�g���N�^
		 * @param ifddata
		 * @param type
		 */
		public EntryOfIFD(Byte[] ifddata, EXIF_TYPE type) {
			int srcPos = 0;
			exifType = type;
			System.arraycopy(ifddata, srcPos, tag, 0, tag.length);
			srcPos += tag.length;
			System.arraycopy(ifddata, srcPos, format, 0, format.length);
			srcPos += format.length;
			System.arraycopy(ifddata, srcPos, count, 0, count.length);
			srcPos += count.length;
			System.arraycopy(ifddata, srcPos, data, 0, data.length);
			srcPos += data.length;
		}
		
		/**
		 * GPS���̗L�����m�F
		 * @return
		 */
		public boolean isGPSIFD0() {
			boolean result = false;
			if(exifType == EXIF_TYPE.MOTOROLA) {
				result = tag[0] == GPSTag[0] && tag[1] == GPSTag[1];
			}else
			if(exifType == EXIF_TYPE.INTEL) {
				result = tag[1] == GPSTag[0] && tag[0] == GPSTag[1];
			}
			return result;
		}
		
		/**
		 * �B�e��̗L�����m�F
		 * @return
		 */
		public boolean isTakePictDateIFD0() {
			boolean result = false;
			if(exifType == EXIF_TYPE.MOTOROLA) {
				result = tag[0] == OriginalDateTag[0] && tag[1] == OriginalDateTag[1];
			}else
			if(exifType == EXIF_TYPE.INTEL) {
				result = tag[1] == OriginalDateTag[0] && tag[0] == OriginalDateTag[1];
			}
			return result;
		}
		
		/**
		 * �⑫���̗L�����m�F
		 * @return
		 */
		public boolean isSubIFD0() {
			boolean result = false;
			if(exifType == EXIF_TYPE.MOTOROLA) {
				result = tag[0] == subExifTag[0] && tag[1] == subExifTag[1];
			}else
			if(exifType == EXIF_TYPE.INTEL) {
				result = tag[1] == subExifTag[0] && tag[0] == subExifTag[1];
			}
			return result;
		}
	}
	
	/**
	 * �Y�t�摜�^����^�����Ǘ��N���X
	 * @author nobu
	 */
	public class ByteImage {
		private byte jpegSOI[]    = {(byte)0xff, (byte)0xd8}; //check 0xffd8(JPEG)
		private byte takePictDateTag[] = {(byte)0x90, (byte)0x03}; //check 0x9003
		private Byte SOI[]        = new Byte[2]; //check 0xffd8(JPEG)
		private Byte APP1Marker[] = new Byte[2];
		private Byte sizeOfAPP1[] = new Byte[2];
		private Byte exifHeader[] = new Byte[6]; //check Exif
		private Byte tiffHeader[] = new Byte[8]; //check MM or II
		private Byte numIFD0[]    = new Byte[2];
		private int numberOfEntryInIFD0 = 0; 
		private int offsetOfTiffHeader = SOI.length+APP1Marker.length+sizeOfAPP1.length+exifHeader.length; //IFD offset start point
		private int lengthOfIFDEntry = 12;
		private String byteAlignTypeM = "MM"; //Motrola
		private String byteAlignTypeI = "II"; //Intel
		private String exitDefinition = "Exif";
		
		protected EXIF_TYPE exifType = null;
		protected String mimeType = "unknown";
		protected String fileExtName = "jpg";
		protected Byte[] image    = new Byte[1];
		protected String ExifGPS  = null;
		protected Date takePictDate = null;
		protected boolean hasExif = false;
		
		/**
		 * �R���X�g���N�^
		 * @param mime
		 * @param data
		 * @throws SmtpException 
		 */
		public ByteImage(String mime, byte[] data) throws SmtpException {
			if(mime != null && mime.length() > 0) {
				mimeType = mime;
			}
			if(mimeType.indexOf("/") > 0) {
				fileExtName = mt.getExtendNameFromMimeType(mimeType);
			}else{
				fileExtName = mt.getExtendNameFromMimeSubType(mimeType)[0];
			}
			
			Vector<Byte> byteArray = new Vector<Byte>();
			for(int i=0;i<data.length;i++) {
				byteArray.add(data[i]);
			}
			image = byteArray.toArray(image);
			if(fileExtName.equals("jpg")) {
				checkExif();
			}
		}
		
		/**
		 * EXIF���m�F����֐�
		 * JPEG support only
		 * @throws SmtpException 
		 */
		private void checkExif() throws SmtpException {
			try {
				int srcPos = 0;
				System.arraycopy(image, srcPos, SOI, 0, SOI.length);
				srcPos += SOI.length;
				if(checkSOI()) {
					System.arraycopy(image, srcPos, APP1Marker, 0, APP1Marker.length);
					srcPos += APP1Marker.length;
					System.arraycopy(image, srcPos, sizeOfAPP1, 0, sizeOfAPP1.length);
					srcPos += sizeOfAPP1.length;
					System.arraycopy(image, srcPos, exifHeader, 0, exifHeader.length);
					srcPos += exifHeader.length;
					if(checkExifHeader()) {
						System.arraycopy(image, srcPos, tiffHeader, 0, tiffHeader.length);
						srcPos += tiffHeader.length;
						if(checkTiffHeader()) {
							System.arraycopy(image, srcPos, numIFD0, 0, numIFD0.length);
							srcPos += numIFD0.length;
							numberOfEntryInIFD0 = byte2Int(numIFD0);
							for(int i=0;i<numberOfEntryInIFD0;i++) {
								Byte ifd[] = new Byte[lengthOfIFDEntry];
								System.arraycopy(image, srcPos+lengthOfIFDEntry*i, ifd, 0, lengthOfIFDEntry);
								EntryOfIFD ifdEntry = new EntryOfIFD(ifd, exifType);
								if(ifdEntry.isGPSIFD0()) {
									ExifGPS = createGPSXML(ifdEntry.data);
								}
								if(ifdEntry.isSubIFD0()) {
									takePictDate = getPhotoDate(ifdEntry.data);
								}
							}
						}
					}
				}
			}
			catch(Exception e) {
				throw new SmtpException(e);
			}
		}
		
		/**
		 * JPEG SOI�̊m�F
		 * @return
		 */
		private boolean checkSOI() {
			return SOI[0] == jpegSOI[0] && SOI[1] == jpegSOI[1];
		}
		
		/**
		 * EXIF�w�b�_�[�̊m�F
		 * @return
		 */
		private boolean checkExifHeader() {
			byte temp[] = new byte[4];
			temp[0] = exifHeader[0];
			temp[1] = exifHeader[1];
			temp[2] = exifHeader[2];
			temp[3] = exifHeader[3];
			String exif = new String(temp, 0, 4);
			return exif.toLowerCase().equals(exitDefinition.toLowerCase());
		}
		
		/**
		 * TIFF�w�b�_�[�̊m�F
		 * @return
		 */
		private boolean checkTiffHeader() {
			boolean result = false;
			byte temp[] = new byte[2];
			temp[0] = tiffHeader[0];
			temp[1] = tiffHeader[1];
			String align = new String(temp, 0, 2);
			if(align.toLowerCase().equals(byteAlignTypeM.toLowerCase())) {
				exifType = EXIF_TYPE.MOTOROLA;
				result = true;
			}else
			if(align.toLowerCase().equals(byteAlignTypeI.toLowerCase())) {
				exifType = EXIF_TYPE.INTEL;
				result = true;
			}

			return result;
		}
		
		/**
		 * GPS����XML�ϊ��֐�
		 * @param ifdOffset
		 * @return
		 * @throws JAXBException
		 */
		private String createGPSXML(Byte ifdOffset[]) throws JAXBException {
			JAXBContext context = JAXBContext.newInstance(GPS.class);
			GPS gpsBean = new GPS();
			
			int offset = byte2Int(ifdOffset) + offsetOfTiffHeader;
			Byte GPSIFD[] = new Byte[2];
			System.arraycopy(image, offset, GPSIFD, 0, GPSIFD.length);
			int numberOfGPSIFDEntry = byte2Int(GPSIFD);
			offset+=GPSIFD.length;
			GPS.Latitude latitudeBean = new GPS.Latitude();
			GPS.Longitude longitudeBean = new GPS.Longitude();
			GPS.Datum datumBean = new GPS.Datum();
			for(int i=0;i<numberOfGPSIFDEntry;i++) {
				Byte ifd[] = new Byte[lengthOfIFDEntry];
				System.arraycopy(image, offset+lengthOfIFDEntry*i, ifd, 0, lengthOfIFDEntry);
				EntryOfIFD ifdEntry = new EntryOfIFD(ifd, exifType);
				int tag = byte2Int(ifdEntry.tag);
				switch(tag) {
				case 0x00:
					gpsBean.setVersion(String.format("%d.%d.%d.%d", (ifdEntry.data[0] & 0xff), (ifdEntry.data[1] & 0xff), (ifdEntry.data[2] & 0xff), (ifdEntry.data[3] & 0xff)));
					break;
				case 0x01:
					latitudeBean.setRef(String.format("%c", ifdEntry.data[0]));
					break;
				case 0x02:
					int latitudeOffset = byte2Int(ifdEntry.data) + offsetOfTiffHeader;
					Byte latitude[] = new Byte[8*3];
					System.arraycopy(image, latitudeOffset, latitude, 0, latitude.length);
					latitudeBean.setValue(calcCoordinate(latitude));
					break;
				case 0x03:
					longitudeBean.setRef(String.format("%c", ifdEntry.data[0]));
					break;
				case 0x04:
					int longitudeOffset = byte2Int(ifdEntry.data) + offsetOfTiffHeader;
					Byte longitude[] = new Byte[8*3];
					System.arraycopy(image, longitudeOffset, longitude, 0, longitude.length);
					longitudeBean.setValue(calcCoordinate(longitude));
					break;
				case 0x12:
					int datumLength = byte2Int(ifdEntry.count);
					int datumOffset = byte2Int(ifdEntry.data) + offsetOfTiffHeader;
					Byte datum[] = new Byte[datumLength];
					System.arraycopy(image, datumOffset, datum, 0, datumLength);
					datumBean.setValue(byte2String(datum));
					break;
				}
			}
			gpsBean.setLatitude(latitudeBean);
			gpsBean.setLongitude(longitudeBean);
			gpsBean.setDatum(datumBean);

			Marshaller marshal = context.createMarshaller();
			StringWriter sw = new StringWriter();
			marshal.marshal(gpsBean, sw);
			return sw.toString();
		}
		
		/**
		 * �B�e��̎擾
		 * @param dataOffset
		 * @return
		 * @throws ParseException
		 */
		private Date getPhotoDate(Byte dataOffset[]) throws ParseException {
			int offset = byte2Int(dataOffset) + offsetOfTiffHeader; 
			Byte subIFD[] = new Byte[2];
			System.arraycopy(image, offset, subIFD, 0, subIFD.length);
			int numberOfSubIFDEntry = byte2Int(subIFD);
			offset+=subIFD.length;
			for(int i=0;i<numberOfSubIFDEntry;i++) {
				Byte ifd[] = new Byte[lengthOfIFDEntry];
				System.arraycopy(image, offset+lengthOfIFDEntry*i, ifd, 0, lengthOfIFDEntry);
				EntryOfIFD ifdEntry = new EntryOfIFD(ifd, exifType);
				if((exifType == EXIF_TYPE.MOTOROLA && takePictDateTag[0] == ifdEntry.tag[0] && takePictDateTag[1] == ifdEntry.tag[1]) ||
					(exifType == EXIF_TYPE.INTEL && takePictDateTag[0] == ifdEntry.tag[1] && takePictDateTag[1] == ifdEntry.tag[0])) {
					int orignalPictDateLength = byte2Int(ifdEntry.count);
					int orignalPictDateOffset = byte2Int(ifdEntry.data) + offsetOfTiffHeader;
					Byte orignalPictDate[] = new Byte[orignalPictDateLength];
					System.arraycopy(image, orignalPictDateOffset, orignalPictDate, 0, orignalPictDateLength);
					String orignalPictDateStr = byte2String(orignalPictDate);
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
					return sdf.parse(orignalPictDateStr);
				}
			}
			return null;
		}
		
		/**
		 * byte���String���쐬
		 * @param args
		 * @return
		 */
		private String byte2String(Byte[] args) {
			byte bytes[] = new byte[args.length];
			int actualLength = 0;
			for(int j=0;j<args.length;j++) {
				if(args[j] != 0x00) {
					bytes[actualLength] = args[j];
					actualLength++;
				}
			}
			if(actualLength > 0) {
				return new String(bytes, 0, actualLength);
			}
			return new String();
		}
		
		/**
		 * byte���int���쐬
		 * ���g���[���`���ƃC���e���`���ŃG���f�B�A�����قȂ�B
		 * @param args
		 * @return
		 */
		private int byte2Int(Byte[] args) {
			int result = 0;
			if(exifType == EXIF_TYPE.MOTOROLA) {
				for(int i=0;i<args.length;i++) {
					result += (args[i] & 0xff) * Math.pow(256, args.length-i-1);
				}
			}
			if(exifType == EXIF_TYPE.INTEL) {
				for(int i=args.length-1;i>=0;i--) {
					result += (args[i] & 0xff) * Math.pow(256, i);
				}
			}
			return result;
		}
		
		/**
		 * �ܓx�o�x�̕�����\�����쐬
		 * @param data
		 * @return
		 */
		private String calcCoordinate(Byte data[]) {
			Byte temp[] = new Byte[4];
			
			System.arraycopy(data, 0, temp, 0, temp.length);
			int numerator   = byte2Int(temp);
			System.arraycopy(data, 4, temp, 0, temp.length);
			int denominator = byte2Int(temp);
			int degrees = numerator / denominator;

			System.arraycopy(data, 8, temp, 0, temp.length);
			numerator   = byte2Int(temp);
			System.arraycopy(data, 12, temp, 0, temp.length);
			denominator = byte2Int(temp);
			int minutes = numerator / denominator;

			System.arraycopy(data, 16, temp, 0, temp.length);
			numerator   = byte2Int(temp);
			System.arraycopy(data, 20, temp, 0, temp.length);
			denominator = byte2Int(temp);
			int seconds = numerator / denominator;

			return String.format("%d %d %d", degrees, minutes, seconds);
		}
		
		/**
		 * EXIF���̗L�����m�F
		 * @return
		 */
		public boolean hasExif() {
			return hasExif;
		}
		
		/**
		 * MIME�^�C�v���擾
		 * @return
		 */
		public String getMimeType() {
			return mimeType;
		}
		
		/**
		 * �t�@�C���g���q���擾
		 * @return
		 */
		public String getFileExtName() {
			return fileExtName;
		}
		
		/**
		 * �C���[�W���̂��擾
		 * @return
		 */
		public Byte[] getImage() {
			return image;
		}
		
		/**
		 * �C���[�W���̂��擾�i�v���~�e�B�u�j
		 * @return
		 */
		public byte[] getImagePrimitive() {
			byte result[] = new byte[image.length];
			
			for(int i=0;i<image.length;i++){
				result[i] = image[i];
			}
			
			return result;
		}
		
		/**
		 * GPS�̕���������擾
		 * @return
		 */
		public String getExifGPSString() {
			return ExifGPS;
		}
		
		/**
		 * �B�e����擾
		 * @return
		 */
		public Date getTakePictDate() {
			return takePictDate;
		}
	}
	
	/**
	 * �R���X�g���N�^�[�i����J�j
	 * @throws SmtpException 
	 */
	protected SmtpParser(Properties prop, Vector<String> mimeList) throws SmtpException {
		try {
			properties = prop;
			defaultSubject = properties.getProperty("default_subject");
			mt = MimeType.getMimeType(mimeList);
		} catch (Exception e) {
			throw new SmtpException(e);
		}
	}
	
	/**
	 * SMTP��͏���
	 * @param smtpData:SMTP������
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws SmtpException 
	 * @throws ParseException 
	 */
	static public SmtpParser parse(String smtpData, Properties prop, Vector<String> mimeList) throws UnsupportedEncodingException, IOException, SmtpException, ParseException{
		SmtpParser smtp = new SmtpParser(prop, mimeList);
		String splitSmtpData[] = smtpData.indexOf("\r") >= 0 ? smtpData.replaceAll("\\r", "").split("[\n]") : smtpData.split("[\n]");
		smtp.setReceiveAddr(splitSmtpData);
		smtp.setFromAddr(splitSmtpData);
		smtp.setToAddr(splitSmtpData);
		smtp.setSubject(splitSmtpData);
		smtp.setDate(splitSmtpData);
		smtp.setBody(smtp.divideData(splitSmtpData));
		smtp.setImage(smtp.divideData(splitSmtpData));
		return smtp;
	}

	/**
	 * SMTP��͏���
	 * @param smtpData:SMTP������
	 * @param removeTranceparency:���ߐF���폜���邩���w��iGIF,PNG�j
	 * @param reduceImage:�摜��JPEG�ɕϊ����邩���w��
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws SmtpException 
	 * @throws ParseException 
	 */
	static public SmtpParser parse(String smtpData, boolean removeTranceparency, boolean reduceImage, Properties prop, Vector<String> mimeList) throws UnsupportedEncodingException, IOException, SmtpException, ParseException{
		SmtpParser smtp = new SmtpParser(prop, mimeList);
		smtp.removeTransparency = removeTranceparency;
		smtp.reduceImage = reduceImage;
		String splitSmtpData[] = smtpData.indexOf("\r") >= 0 ? smtpData.replaceAll("\\r", "").split("[\n]") : smtpData.split("[\n]");
		smtp.setReceiveAddr(splitSmtpData);
		smtp.setFromAddr(splitSmtpData);
		smtp.setToAddr(splitSmtpData);
		smtp.setSubject(splitSmtpData);
		smtp.setDate(splitSmtpData);
		smtp.setBody(smtp.divideData(splitSmtpData));
		smtp.setImage(smtp.divideData(splitSmtpData));
		return smtp;
	}
	
	/**
	 * SMTP�{�f�B�̃T�C�Y�m�F
	 * mail_config.xml�ݒ�t�@�C���ŕύX�\
	 * �f�t�H���g5000����
	 * @param body
	 * @throws SmtpException
	 */
	protected void checkBodyLength(String body) throws SmtpException {
		int maxLength = Integer.parseInt(properties.getProperty("maxbody"));
		if(body.length() > maxLength) {
			throw new SmtpException("Message body too large.");
		}
	}

	/**
	 * ���M�����[���T�[�o�̊m�F
	 * mail_config.xml�ݒ�t�@�C���ŕύX�\
	 * @param serverAddr
	 * @throws SmtpException
	 */
	protected void checkMailServer(String serverAddr) throws SmtpException {
		String whiteServers = properties.getProperty("white-servers");
		if(whiteServers != null && whiteServers.length() > 0) {
			String serverList[] = whiteServers.split(",");
			for(String server : serverList) {
				if(serverAddr.toLowerCase().indexOf(server) >= 0) {
					int index = serverAddr.toLowerCase().indexOf(server)+server.length();
					String receivedHeaderOther = serverAddr.substring(index);
					if(receivedHeaderOther.startsWith(" ")) {
						return;
					}
				}
			}
		}
		throw new SmtpException("Illegal server access."); 
	}

	/**
	 * ��M���[���A�h���X�̐ݒ�
	 * @param smtpData
	 * @throws SmtpException 
	 */
	protected void setReceiveAddr(String[] smtpData) throws SmtpException {
		for(String data : smtpData) {
			if(data.startsWith(receivedHeader)) {
				receivedHeaderValue = data.substring(receivedHeader.length()).trim();
				checkMailServer(receivedHeaderValue);
				break;
			}
		}
	}
	
	/**
	 * ���M���[���A�h���X�̐ݒ�
	 * @param smtpData
	 */
	protected void setFromAddr(String[] smtpData) {
//		for(String data : smtpData) {
//			if(data.startsWith(fromHeader)) {
//				fromHeaderValue = data.substring(fromHeader.length()).trim();
//				if(fromHeaderValue.indexOf("<") >= 0 && fromHeaderValue.indexOf(">") > 0){
//					fromHeaderValue = fromHeaderValue.substring(fromHeaderValue.indexOf("<")+1, fromHeaderValue.indexOf(">"));
//				}
//				break;
//			}
//		}
		for(String data : smtpData) {
			if(!data.startsWith(fromHeader) && data.matches(".+:\\s*.+")) {
				if(startFromAddr) {
					startFromAddr = false;
					break;
				}
				startFromAddr = false;
			}
			if(data.startsWith(fromHeader) || startFromAddr) {
				fromHeaderValue = startFromAddr ? data.trim() : data.substring(fromHeader.length()).trim();
				startFromAddr = true;
				if(fromHeaderValue.indexOf("<") >= 0 && fromHeaderValue.indexOf(">") > 0){
					fromHeaderValue = fromHeaderValue.substring(fromHeaderValue.indexOf("<")+1, fromHeaderValue.indexOf(">"));
				}
			}
		}
	}
	
	/**
	 * ���M�惁�[���A�h���X�̐ݒ�
	 * @param smtpData
	 */
	protected void setToAddr(String[] smtpData) {
		for(String data : smtpData) {
			if(data.startsWith(toHeader)) {
				toHeaderValue = data.substring(toHeader.length()).trim();
				if(toHeaderValue.indexOf("<") >= 0 && toHeaderValue.indexOf(">") > 0){
					toHeaderValue = toHeaderValue.substring(toHeaderValue.indexOf("<")+1, toHeaderValue.indexOf(">"));
				}
				break;
			}
		}
	}

	/**
	 * �����̐ݒ�
	 * @param smtpData
	 * @throws UnsupportedEncodingException
	 */
	protected void setSubject(String[] smtpData) throws UnsupportedEncodingException{
		for(String data : smtpData) {
			if(data.startsWith(subjetHeader) || startSubject) {
				String subjectValue = startSubject ? data.trim() : data.substring(subjetHeader.length()).trim();
				startSubject  = true;
				if(subjectValue.toLowerCase().indexOf("iso-2022-jp") >= 0) {
					Matcher m = Pattern.compile("(.+)B\\?(.+)\\?(.*)").matcher(subjectValue);
					if(m.matches()) {
						String a = m.group(1) == null ? new String() : m.group(1);
						String b = m.group(3) == null ? new String() : m.group(3);
						if(a.indexOf("=?") > 0) {
							a = a.substring(0, a.indexOf("=?"));
						}else{
							a = new String();
						}
						if(b.length() > 1) {
							b = b.substring(1);
						}else{
							b = new String();
						}
						byte decode[] = Base64.decodeBase64(m.group(2).getBytes());
						//TODO Subject�������̃X�y�[�X�}��͍s�Ȃ�Ȃ��iMIME�G���R�[�h�ł͍s�Ȃ��̂��ʏ�̎d�l�j
						subjectHeaderValue =  subjectHeaderValue + a + new String(decode,"ISO-2022-JP") + b;
					}
				}else{
					if(data.startsWith(subjetHeader)){
						subjectHeaderValue = subjectHeaderValue + subjectValue;
					}else
					if(!data.matches(".+:\\s*.+")) {
						subjectHeaderValue = subjectHeaderValue + subjectValue;
					}
				}
			}
			if(!data.startsWith(subjetHeader) && data.matches(".+:\\s*.+")) {
				if(startSubject) {
					startSubject = false;
					break;
				}
				startSubject = false;
			}
		}
	}
	
	/**
	 * ��M��̐ݒ�
	 * @param smtpData
	 * @throws ParseException
	 */
	protected void setDate(String[] smtpData) throws ParseException {
		for(String data : smtpData) {
			if(data.startsWith(dateHeader)) {
				String dateStr = data.substring(dateHeader.length()).trim();
				//Sample format >> Mon, 20 Aug 2007 19:46:01 +0900
				SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
				contributeDate = Calendar.getInstance();
				contributeDate.setTime(sdf.parse(dateStr, new ParsePosition(0)));
			}
		}
	}
	
	/**
	 * SMTP�{�f�B�̉��
	 * @param smtpData
	 * @return
	 */
	protected Vector<String> divideData(String[] smtpData) {
		Vector<String> result = new Vector<String>();
		StringBuffer tempData = new StringBuffer();
		boolean startFlg = false;
		
		for(int i=0;i<smtpData.length;i++) {
			String data = smtpData[i];

			if(startFlg) {
				tempData.append(data+"\n");
			}
			if(data.matches(delimiterRegEx1)) {
				return divideMultipartData(smtpData, i);
			}
			if(data.length() < 1) {
				startFlg = true;
			}
		}
		
		result.add(tempData.toString());
		return result;
	}
	
	/**
	 * SMTP�{�f�B�̉�́i�}���`�p�[�g�j
	 * @param smtpData
	 * @param index
	 * @return
	 */
	protected Vector<String> divideMultipartData(String[] smtpData, int index) {
		Vector<String> result = new Vector<String>();
		StringBuffer tempData = new StringBuffer();
		boolean startFlg = false;
		int offset = 1;

		String delimiter = smtpData[index];
		for(int i=index;i<smtpData.length;i++) {
			Matcher m = Pattern.compile(delimiterRegEx2).matcher(smtpData[i]);
			if(m.matches()) {
				delimiter = m.group(1);
				if(delimiter.startsWith("\"")){
					delimiter = delimiter.substring(1);
				}
				if(delimiter.endsWith("\"")){
					delimiter = delimiter.substring(0, delimiter.length()-1);
				}
				offset+=i;
				break;
			}
		}
		for(int i=offset;i<smtpData.length;i++) {
			if(startFlg && !smtpData[i].startsWith("--"+delimiter)) {
				tempData.append(smtpData[i]+"\n");
			}
			if(smtpData[i].startsWith("--"+delimiter)) {
				if(startFlg) {
					result.add(tempData.toString());
					tempData = new StringBuffer();
				}else{
					startFlg = true;
				}
			}
		}
		return result;
	}
	
	/**
	 * ���b�Z�[�W�{�f�B�̐ݒ�
	 * @param partData
	 * @throws SmtpException 
	 */
	protected void setBody(Vector<String> partData) throws SmtpException {
		boolean startFlg = false;
		StringBuffer tempData = new StringBuffer();
		String mimeType = "nomime";
		
		for(String parts : partData) {
			String splitData[] = parts.split("\n");
			for(int i=0;i<splitData.length;i++) {
				String data = splitData[i];
				if(startFlg) {
					tempData.append(data+"\n");
				}
				if(data.length() < 1) {
					startFlg = true;
				}
				if(data.matches(mimeTypeRegEx)) {
					Matcher m = Pattern.compile(mimeTypeRegEx).matcher(data);
					if(m.matches()) {
						mimeType = m.group(1).trim();
						mimeType = mimeType.substring(mimeType.lastIndexOf("/")+1);
					}
				}
			}
			// text/plain or no mime-type content only (text/html not support)
			if(mimeType.toLowerCase().indexOf("plain") >= 0) {
				bodyValue += tempData.toString();
				checkBodyLength(bodyValue);
			}else
			if(mimeType.toLowerCase().indexOf("nomime") >= 0) {
				bodyValue += parts;
				checkBodyLength(bodyValue);
			}
			startFlg = false;
			tempData = new StringBuffer();
		}
	}
	
	/**
	 * �Y�t�t�@�C���̐ݒ�
	 * @param partData
	 * @throws IOException
	 * @throws SmtpException 
	 */
	protected void setImage(Vector<String> partData) throws IOException, SmtpException{
		boolean startFlg = false;
		StringBuffer tempData = new StringBuffer();
		String mimeType = "plain";

		for(String parts : partData) {
			String splitData[] = parts.split("\n");
			for(int i=0;i<splitData.length;i++) {
				String data = splitData[i];
				if(startFlg) {
					tempData.append(data+"\n");
				}
				if(data.length() < 1) {
					startFlg = true;
				}
				if(data.matches(mimeTypeRegEx)) {
					Matcher m = Pattern.compile(mimeTypeRegEx).matcher(data);
					if(m.matches()) {
						mimeType = m.group(1).trim();
//						mimeType = mimeType.substring(mimeType.lastIndexOf("/")+1);
					}
				}
				if(data.matches(fileNameRegEx)) {
					Matcher m = Pattern.compile(fileNameRegEx).matcher(data);
					if(m.matches()) {
						fileNameValue = m.group(1).trim();
						if(fileNameValue.startsWith("\"")){
							fileNameValue = fileNameValue.substring(1);
						}
						if(fileNameValue.endsWith("\"")){
							fileNameValue = fileNameValue.substring(0, fileNameValue.length()-1);
						}
					}
				}
			}
			if(mimeType.toLowerCase().indexOf("plain") < 0 && mimeType.toLowerCase().indexOf("html") < 0) {
				if(mt.isBinalyData(mimeType)) {
					byte[] imageValue = Base64.decodeBase64(tempData.toString().getBytes());
					ByteImage byteImage = new ByteImage(mimeType, imageValue);
					imageBuffer.add(byteImage);
//					byte[] imageValue = Base64.decodeBase64(tempData.toString().getBytes());
//					BufferedImage bim = ImageIO.read(new ByteArrayInputStream(imageValue));
//					if(bim != null) {
//						if(mimeType.toLowerCase().indexOf("jpeg") < 0 && removeTransparency) {
//							bim = checkTransparency(bim);
//						}
//						if(reduceImage) {
//							bim = checkDimension(bim);
//						}
//						ByteArrayOutputStream bos = new ByteArrayOutputStream();
//						if(removeTransparency || mimeType.toLowerCase().indexOf("jpg") >=0 || mimeType.toLowerCase().indexOf("jpeg") >= 0) {
//							ImageIO.write(bim, "jpeg", bos);
//							imageValue = bos.toByteArray();
//						}else
//						if(mimeType.toLowerCase().indexOf("gif") >=0) {
//							if(!ImageIO.write(bim, "gif", bos)) {
//								bim = checkTransparency(bim);
//								ImageIO.write(bim, "jpeg", bos);
//								fileNameValue = "jpg";
//								mimeType = "jpg";
//								imageValue = bos.toByteArray();
//							}
//						}else
//						if(mimeType.toLowerCase().indexOf("png") >=0) {
//							ImageIO.write(bim, "png", bos);
//							imageValue = bos.toByteArray();
//						}else
//						if(mimeType.toLowerCase().indexOf("bmp") >=0 || mimeType.toLowerCase().indexOf("bitmap") >=0) {
//							ImageIO.write(bim, "bmp", bos);
//							imageValue = bos.toByteArray();
//						}
//						ByteImage byteImage = new ByteImage(mimeType, imageValue);
//						imageBuffer.add(byteImage);
//					}
//				}else{
//					byte[] imageValue = Base64.decodeBase64(tempData.toString().getBytes());
//					ByteImage byteImage = new ByteImage(mimeType, imageValue);
//					imageBuffer.add(byteImage);
				}
			}
			startFlg = false;
			tempData = new StringBuffer();
		}
	}
	
	/**
	 * �摜�T�C�Y�̕␳
	 * @param bim
	 * @return
	 */
	//comment out in Google App Engin
	/*
	public BufferedImage checkDimension(BufferedImage bim) {
		double width          = bim.getWidth();
		double height         = bim.getHeight();
		double safeWidth      = QVGDimension.width * defualtResizePercent;
		double safeHeight     = QVGDimension.height * defualtResizePercent;

		//to QVGA
		if(width > safeWidth || height > safeHeight) {
			double withRatio   = safeWidth / width;
			double heightRatio = safeHeight / height;
			if(withRatio > heightRatio && withRatio < 0) {
				width = width * withRatio;
				height = height * withRatio;
			}else 
			if(heightRatio > withRatio && heightRatio < 0) {
				width = width * heightRatio;
				height = height * heightRatio;
			}else 
			if(heightRatio == height) {
				width = width * withRatio;
				height = height * heightRatio;
			}else 
			if(withRatio < 1) {
				width = width * withRatio;
				height = height * withRatio;
			}else 
			if(heightRatio < 1) {
				width = width * heightRatio;
				height = height * heightRatio;
			}else{
				width = width * withRatio;
				height = height * heightRatio;
			}
			
			BufferedImage newImage = null;
			if(bim.getColorModel() instanceof IndexColorModel) {
				newImage = new BufferedImage((int)width, (int)height, bim.getType(), (IndexColorModel)bim.getColorModel());
			}else{
				newImage = new BufferedImage((int)width, (int)height, BufferedImage.TYPE_INT_RGB);
			}
			Image newScaleImg = bim.getScaledInstance((int)width, (int)height, Image.SCALE_SMOOTH);
			newImage.getGraphics().drawImage(newScaleImg, 0, 0, null);
			return newImage;
		}

		return bim;
	}
	*/
	
	/**
	 * ���ߐF�̕␳
	 * @param bim
	 * @return
	 */
	//comment out in Google App Engin
	/*
	public BufferedImage checkTransparency(BufferedImage bim) {
		ColorModel cm = bim.getColorModel();
		int transparency  = cm.getTransparency();
		int componentsNum = cm.getNumComponents();
		if(transparency == Transparency.BITMASK && componentsNum == 4 && cm instanceof IndexColorModel) {
			IndexColorModel icm = (IndexColorModel)cm;
			int pixLen    = icm.getMapSize();
			int pixSize   = icm.getPixelSize();
			byte reds[]   = new byte[pixLen];
			byte greens[] = new byte[pixLen];
			byte blues[]  = new byte[pixLen];

			icm.getBlues(blues);
			icm.getGreens(greens);
			icm.getReds(reds);

			int transPixel     = icm.getTransparentPixel();
			blues[transPixel]  = (byte)255;
			greens[transPixel] = (byte)255;
			reds[transPixel]   = (byte)255;
			int width          = bim.getWidth();
			int height         = bim.getHeight();
			int imageType      = bim.getType();
			
			//Transparency.OPAQUE
			IndexColorModel icmOpaq = new IndexColorModel(pixSize, pixLen, reds, greens, blues);
			BufferedImage OpaqBim = new BufferedImage(width, height, imageType, icmOpaq);
			if(transPixel > 0) {
				int transRGB = icm.getRGB(transPixel);
				for(int y=0;y<height;y++) {
					for(int x=0;x<width;x++) {
						if(bim.getRGB(x, y) == transRGB) {
							OpaqBim.setRGB(x, y, 0xffffffff);
						}else{
							OpaqBim.setRGB(x, y, bim.getRGB(x, y));
						}
					}
				}
			}
			return OpaqBim;
		}
		
		return bim;
	}
	*/

	/**
	 * ��M���[���A�h���X�̎擾
	 * @return
	 */
	public String getReceivedAddr() {
		return receivedHeaderValue;
	}
	
	/**
	 * ���M�惁�[���A�h���X�̎擾
	 * @return
	 */
	public String getToAddr() {
		return toHeaderValue;
	}
	
	/**
	 * ���M���[���A�h���X�̎擾
	 * @return
	 */
	public String getFromAddr() {
		return fromHeaderValue;
	}
	
	/**
	 * �����̎擾
	 * @return
	 */
	public String getSubject() {
		if(subjectHeaderValue == null || subjectHeaderValue.length() < 1) {
			subjectHeaderValue = defaultSubject;
		}
		return subjectHeaderValue;
	}
	
	/**
	 * ���g�p
	 * @return
	 */
	public HashMap<String, String> getSubjectParam() {
		HashMap<String, String> result = new HashMap<String, String>();
		if(subjectHeaderValue.indexOf("_") >= 0 && subjectHeaderValue.indexOf("-") >= 0) {
			for(String param : subjectHeaderValue.split("_")) {
				String key = param.substring(0, param.indexOf("-"));
				String value = param.substring(param.indexOf("-")+1);
				result.put(key, value);
			}
		}else
		if(subjectHeaderValue.indexOf("-") >= 0) {
			String key = subjectHeaderValue.substring(0, subjectHeaderValue.indexOf("-"));
			String value = subjectHeaderValue.substring(subjectHeaderValue.indexOf("-")+1);
			result.put(key, value);
		}
		
		return result;
	}
	
	/**
	 * ���[���{�f�B�̎擾
	 * @return
	 */
	public String getBody() {
		if(bodyValue != null && bodyValue.length() > 0 && bodyValue.charAt(bodyValue.length()-1) == '\n'){
			bodyValue = bodyValue.substring(0, bodyValue.length()-1);
		}
		return bodyValue;
	}
	
	/**
	 * ���[���{�f�B�̐ݒ�
	 * @param body
	 */
	public void setBody(String body) {
		bodyValue = body;
	}
	
	/**
	 * �Y�t�t�@�C���̎擾
	 * @param index
	 * @return
	 */
	public ByteImage getImage(int index) {
		return imageBuffer.get(index);
	}

	/**
	 * �S�Ă̓Y�t�t�@�C�����擾
	 * @return
	 */
	public Vector<ByteImage>getImages() {
		return imageBuffer;
	}
	
	/**
	 * 
	 * @return
	 */
	public Calendar getContributeDate() {
		return contributeDate;
	}
}
