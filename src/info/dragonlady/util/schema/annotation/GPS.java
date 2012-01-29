package info.dragonlady.util.schema.annotation;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/*
<gps version=''>
<latitude ref=''></latitude>
<longitude ref=''></longitude>
<datum></datum>
</gps>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "gps", propOrder = {
	    "latitude",
	    "longitude",
	    "datum"
})
@XmlRootElement
public class GPS {
	protected Latitude latitude;
	protected Longitude longitude;
	protected Datum datum;
	@XmlAttribute(name="version")
	protected String version;

	public Latitude getLatitude() {
		return latitude;
	}

	public void setLatitude(Latitude latitude) {
		this.latitude = latitude;
	}

	public Longitude getLongitude() {
		return longitude;
	}

	public void setLongitude(Longitude longitude) {
		this.longitude = longitude;
	}

	public Datum getDatum() {
		return datum;
	}

	public void setDatum(Datum datum) {
		this.datum = datum;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
	public static class Latitude {
		@XmlValue
		protected String value;
		@XmlAttribute(name="ref")
		protected String latitudeRef;

		public String getRef() {
			return latitudeRef;
		}
		public void setRef(String latitudeRef) {
			this.latitudeRef = latitudeRef;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}
	
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
	public static class Longitude {
		@XmlValue
		protected String value;
		@XmlAttribute(name="ref")
		protected String longitudeRef;

		public String getRef() {
			return longitudeRef;
		}
		public void setRef(String longitudeRef) {
			this.longitudeRef = longitudeRef;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
	public static class Datum {
		@XmlValue
		protected String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}
