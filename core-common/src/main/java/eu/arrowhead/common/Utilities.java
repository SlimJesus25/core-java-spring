package eu.arrowhead.common;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.arrowhead.common.exception.ArrowheadException;

public class Utilities {
	
	//=================================================================================================
	// members
	
	private static final int SERVICE_CN_NAME_LENGTH = 5;
	@SuppressWarnings("unused")
	private static final int CLOUD_CN_NAME_LENGTH = 4;
	@SuppressWarnings("unused")
	private static final int AH_MASTER_CN_NAME_LENGTH = 2;
	
	private static final String AH_MASTER_SUFFIX = "eu";
	private static final String AH_MASTER_NAME = "arrowhead";
	
	private static final Logger logger = LogManager.getLogger(Utilities.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final String dateTimePattern = "yyyy-MM-dd HH:mm:ss";
	static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
	
	static {
	    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	}
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static boolean isEmpty(final String str) {
		return str == null || str.isBlank();
	}
	
	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String stripEndSlash(final String uri) {
	    if (uri != null && uri.endsWith("/")) {
	    	return uri.substring(0, uri.length() - 1);
	    }
	    
	    return uri;
	}
	
	//-------------------------------------------------------------------------------------------------
	public static String convertZonedDateTimeToUTCString(final ZonedDateTime time) {
		if (time == null) {
			return null;
		}
		
		final LocalDateTime localDateTime = LocalDateTime.ofInstant(time.toInstant(), ZoneOffset.UTC);
		return dateTimeFormatter.format(localDateTime);
	}
	
	//-------------------------------------------------------------------------------------------------
	public static ZonedDateTime parseUTCStringToLocalZonedDateTime(final String timeStr) throws DateTimeParseException {
		if (isEmpty(timeStr)) {
			return null;
		}
		
		final TemporalAccessor tempAcc = dateTimeFormatter.parse(timeStr);
		final ZonedDateTime parsedDateTime = ZonedDateTime.of(tempAcc.get(ChronoField.YEAR),
															  tempAcc.get(ChronoField.MONTH_OF_YEAR),
															  tempAcc.get(ChronoField.DAY_OF_MONTH),
															  tempAcc.get(ChronoField.HOUR_OF_DAY),
															  tempAcc.get(ChronoField.MINUTE_OF_HOUR),
															  tempAcc.get(ChronoField.SECOND_OF_MINUTE),
															  0,
															  ZoneOffset.UTC);
														
		final ZoneOffset offset = OffsetDateTime.now().getOffset();
		ZonedDateTime dateTime = ZonedDateTime.ofInstant(parsedDateTime.toInstant(), offset);
		
		return dateTime;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String toPrettyJson(final String jsonString) {
		try {
			if (jsonString != null) {
				final String jsonString_ = jsonString.trim();
				if (jsonString_.startsWith("{")) {
					Object tempObj = mapper.readValue(jsonString_, Object.class);
					return mapper.writeValueAsString(tempObj);
				} else {
					Object[] tempObj = mapper.readValue(jsonString_, Object[].class);
					return mapper.writeValueAsString(tempObj);
				}
			}
	    } catch (final IOException ex) {
	    	// it seems it is not a JSON string, so we just return untouched
	    }
		
	    return jsonString;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static <T> T fromJson(final String json, final Class<T> parsedClass) {
		if (json == null || parsedClass == null) {
			return null;
		}
		
	    try {
	    	return mapper.readValue(json, parsedClass);
	    } catch (final IOException e) {
	      throw new ArrowheadException("The specified string cannot be converted to a(n) " + parsedClass.getSimpleName() + " object.", e);
	    }
	}
	
	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static Map<String,String> text2Map(final String text) {
		if (text == null) {
			return null;
		}
		
		final Map<String,String> result = new HashMap<>();
		if (!isEmpty(text.trim())) {
			final String[] parts = text.split(",");
			for (final String part : parts) {
				final String[] pair = part.split("=");
				result.put(URLDecoder.decode(pair[0].trim(), StandardCharsets.UTF_8), pair.length == 1 ? "" : URLDecoder.decode(pair[1].trim(), StandardCharsets.UTF_8));
			}
		}
		
		return result;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String map2Text(final Map<String,String> map) {
		if (map == null) {
			return null;
		}
		
		final StringBuilder sb = new StringBuilder();
		for (final Entry<String,String> entry : map.entrySet()) {
			final String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
			final String value = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
			sb.append(key).append("=").append(value).append(", ");
		}
		
		return map.isEmpty() ? "" : sb.substring(0, sb.length() - 2);
	}
	
	//-------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @param scheme default: http
	 * @param host default: 127.0.0.1
	 * @param port default: 80
	 * @param queryParams default: null
	 * @param path default: null
	 * @param pathSegments default: null
	 */
	public static UriComponents createURI(final String scheme, final String host, final int port, final MultiValueMap<String, String> queryParams, final String path, final String... pathSegments) {
		final UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		builder.scheme(scheme == null ? CommonConstants.HTTP : scheme)
			   .host(host == null ? CommonConstants.LOCALHOST : host)
			   .port(port <= 0 ? CommonConstants.HTTP_PORT : port);
		
		if (queryParams != null) {
			builder.queryParams(queryParams);
		}
		
		if (pathSegments != null && pathSegments.length > 0) {
			builder.pathSegment(pathSegments);
		}
		
		if (!Utilities.isEmpty(path)) {
			builder.path(path);
		}
		
		return builder.build();
	}
	
	//-------------------------------------------------------------------------------------------------
	public static UriComponents createURI(final String scheme, final String host, final int port, final String path) {
		return createURI(scheme, host, port, null, path);
	}
	
	//-------------------------------------------------------------------------------------------------
	public static HttpStatus calculateHttpStatusFromArrowheadException(final ArrowheadException ex) {
		Assert.notNull(ex, "Exception is null.");
		
		HttpStatus status = HttpStatus.resolve(ex.getErrorCode());
	    if (status == null) {
	    	switch (ex.getExceptionType()) {
	    	case AUTH:
	    		status = HttpStatus.UNAUTHORIZED;
			    break;
	        case BAD_PAYLOAD:
	        case INVALID_PARAMETER:
	        	status = HttpStatus.BAD_REQUEST;
	          	break;
	        case DATA_NOT_FOUND:
	        	status = HttpStatus.NOT_FOUND;
	        	break;
	        case UNAVAILABLE:
	        	status = HttpStatus.GATEWAY_TIMEOUT;
	        	break;
	        default:
	    		status = HttpStatus.INTERNAL_SERVER_ERROR;
	    	}
	    }
	    
		return status;
	}
	
	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String getCertCNFromSubject(final String subjectName) {
		if (subjectName == null) {
			return null;
		}
		
	    try {
	    	// Subject is in LDAP format, we can use the LdapName object for parsing
	    	LdapName ldapname = new LdapName(subjectName);
	    	for (final Rdn rdn : ldapname.getRdns()) {
	    		// Find the data after the CN field
	    		if (CommonConstants.COMMON_NAME_FIELD_NAME.equalsIgnoreCase(rdn.getType())) {
	    			return (String) rdn.getValue();
	    		}
	    	}
	    } catch (final InvalidNameException ex) {
	    	logger.warn("InvalidNameException in getCertCNFromSubject: {}", ex.getMessage());
	    	logger.debug("Exception", ex);
	    }

	    return null;
	}
	
	//-------------------------------------------------------------------------------------------------
	public static X509Certificate getFirstCertFromKeyStore(final KeyStore keystore) {
		Assert.notNull(keystore, "Key store is not defined.");
		
        try {
            final Enumeration<String> enumeration = keystore.aliases();
            final String alias = enumeration.nextElement();
            return (X509Certificate) keystore.getCertificate(alias);
        } catch (final KeyStoreException | NoSuchElementException ex) {
            throw new ServiceConfigurationError("Getting the first cert from keystore failed...", ex);
        }
    }
	
	//-------------------------------------------------------------------------------------------------
	public static boolean isKeyStoreCNArrowheadValid(final String commonName) {
		if (isEmpty(commonName)) {
			return false;
		}
		
        final String[] cnFields = commonName.split("\\.", 0);
        return cnFields.length == SERVICE_CN_NAME_LENGTH && cnFields[cnFields.length - 1].equals(AH_MASTER_SUFFIX) && cnFields[cnFields.length - 2].equals(AH_MASTER_NAME);
    }
	
	//-------------------------------------------------------------------------------------------------
	public static boolean isKeyStoreCNArrowheadValid(final String clientCN, final String cloudCN) {
		if (isEmpty(clientCN) || isEmpty(cloudCN)) {
			return false;
		}
		
	    final String[] clientFields = clientCN.split("\\.", 2); // valid clientFields contains clientServiceName, cloudName.operator.arrowhead.eu
	    
	    return clientFields.length >= 2 && cloudCN.equalsIgnoreCase(clientFields[1]);
	}
	
	//-------------------------------------------------------------------------------------------------
	public static String getDatetimePattern() { return dateTimePattern; }
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private Utilities() {
		throw new UnsupportedOperationException();
	}
}