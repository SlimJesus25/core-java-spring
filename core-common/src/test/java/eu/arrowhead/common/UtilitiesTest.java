package eu.arrowhead.common;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ServiceConfigurationError;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import eu.arrowhead.common.exception.ArrowheadException;

@RunWith(SpringRunner.class)
public class UtilitiesTest {
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testCalculateHttpStatusFromArrowheadExceptionBradbury() {
		final ArrowheadException ex = new ArrowheadException("Fahrenheit", 451);
		final HttpStatus result = Utilities.calculateHttpStatusFromArrowheadException(ex);
		Assert.assertEquals(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS, result);
	}
	
	@Test
	public void testCalculateHttpStatusFromArrowheadExceptionInternalServerError() {
		final ArrowheadException ex = new ArrowheadException("Does not matter.");
		final HttpStatus result = Utilities.calculateHttpStatusFromArrowheadException(ex);
		Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result);
	}
	
	@Test
	public void testGetCertCNFromSubjectOk() {
		String result = Utilities.getCertCNFromSubject("cn=abc.def.gh");
		Assert.assertEquals("abc.def.gh", result);
	}
	
	@Test
	public void testGetCertCNFromSubjectNotOk() {
		String result = Utilities.getCertCNFromSubject("abc.def.gh");
		Assert.assertNull(result);
	}
	
	@Test
	public void testGetCertCNFromSubjectNullParameter() {
		String result = Utilities.getCertCNFromSubject(null);
		Assert.assertNull(result);
	}
	
	@Test
	public void testGetFirstCertFromKeyStoreNotInitializedKeyStore() throws KeyStoreException {
		expectedException.expect(ServiceConfigurationError.class);
		
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		Utilities.getFirstCertFromKeyStore(keystore);
	}
	
	@Test
	public void testGetFirstCertFromKeyStoreEmptyKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		expectedException.expect(ServiceConfigurationError.class);
		
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		keystore.load(null, null);
		Utilities.getFirstCertFromKeyStore(keystore);
	}
	
	@Test
	public void testIsKeyStoreCNArrowheadValidGood() {
		final boolean result = Utilities.isKeyStoreCNArrowheadValid("service.cloud.operator.arrowhead.eu");
		Assert.assertTrue(result);
	}
	
	@Test
	public void testIsKeyStoreCNArrowheadValidWrongFormat() {
		final boolean result = Utilities.isKeyStoreCNArrowheadValid("service;cloud;operator;arrowhead;eu");
		Assert.assertFalse(result);
	}
	
	@Test
	public void testIsKeyStoreCNArrowheadValidTooShort() {
		final boolean result = Utilities.isKeyStoreCNArrowheadValid("cloud.operator.arrowhead.eu");
		Assert.assertFalse(result);
	}

	@Test
	public void testIsKeyStoreCNArrowheadValidWrongSuffix() {
		final boolean result = Utilities.isKeyStoreCNArrowheadValid("service.cloud.operator.arrowhead.hu");
		Assert.assertFalse(result);
	}

	@Test
	public void testIsKeyStoreCNArrowheadValidWrongMasterName() {
		final boolean result = Utilities.isKeyStoreCNArrowheadValid("service.cloud.operator.arowhead.eu");
		Assert.assertFalse(result);
	}
	
	@Test
	public void testIsKeyStoreCNArrowheadValidCompareWithCloudCNGood() {
		final boolean result = Utilities.isKeyStoreCNArrowheadValid("service.cloud.operator.arrowhead.eu", "cloud.OPERATOR.arrowhead.eu");
		Assert.assertTrue(result);
	}
	
	@Test
	public void testIsKeyStoreCNArrowheadValidCompareWithCloudCNWrongFormat() {
		final boolean result = Utilities.isKeyStoreCNArrowheadValid("service;cloud;operator;arrowhead;eu", "cloud.operator.arrowhead.eu");
		Assert.assertFalse(result);
	}

	@Test
	public void testIsKeyStoreCNArrowheadValidCompareWithCloudCNDifferentCloud() {
		final boolean result = Utilities.isKeyStoreCNArrowheadValid("service.cloud2.operator.arrowhead.eu", "cloud.OPERATOR.arrowhead.eu");
		Assert.assertFalse(result);
	}
}