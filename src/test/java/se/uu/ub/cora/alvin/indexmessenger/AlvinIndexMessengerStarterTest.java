/*
 * Copyright 2019 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.uu.ub.cora.alvin.indexmessenger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.indexmessenger.CoraCredentials;
import se.uu.ub.cora.indexmessenger.IndexMessengerListener;
import se.uu.ub.cora.indexmessenger.log.LoggerFactorySpy;
import se.uu.ub.cora.indexmessenger.log.LoggerSpy;
import se.uu.ub.cora.javaclient.cora.CoraClientFactoryImp;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.messaging.AmqpMessageRoutingInfo;
import se.uu.ub.cora.messaging.MessagingProvider;

public class AlvinIndexMessengerStarterTest {

	private LoggerFactorySpy loggerFactorySpy = new LoggerFactorySpy();
	private MessagingFactorySpy messagingFactorySpy;
	private String testedClassName = "AlvinIndexMessengerStarter";

	@BeforeMethod
	public void setUp() {
		loggerFactorySpy.resetLogs(testedClassName);
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		messagingFactorySpy = new MessagingFactorySpy();
		MessagingProvider.setMessagingFactory(messagingFactorySpy);
	}

	@Test
	public void testConstructorIsPrivate() throws Exception {
		Constructor<AlvinIndexMessengerStarter> constructor = AlvinIndexMessengerStarter.class
				.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	public void testMainMethod() {
		String args[] = new String[] { "alvinIndexer.properties" };
		AlvinIndexMessengerStarter.main(args);
		assertInfoMessagesForStartup();
		assertNoFatalErrorMessages();
	}

	private void assertInfoMessagesForStartup() {
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 0),
				"AlvinIndexMessengerStarter starting...");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 1),
				"Sending indexOrders to: someBaseUrl using appToken from: someAppTokenVerifierUrl");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 2),
				"Listening for index messages from: messaging.alvin-portal.org using port: 5672");
		assertEquals(loggerFactorySpy.getInfoLogMessageUsingClassNameAndNo(testedClassName, 3),
				"AlvinIndexMessengerStarter started");
		assertEquals(loggerFactorySpy.getNoOfInfoLogMessagesUsingClassname(testedClassName), 4);
	}

	private void assertNoFatalErrorMessages() {
		LoggerSpy loggerSpy = loggerFactorySpy.createdLoggers.get(testedClassName);
		assertNotNull(loggerSpy);
	}

	@Test
	public void testMainMethodWithoutPropertiesFileNameShouldUseDefaultFilename() {
		String args[] = new String[] {};
		AlvinIndexMessengerStarter.main(args);
		assertNoFatalErrorMessages();
	}

	@Test
	public void testMainMethodCoraClientFactorySetUpCorrectly() throws Exception {
		String args[] = new String[] { "alvinIndexer.properties" };
		AlvinIndexMessengerStarter.main(args);

		IndexMessengerListener messageListener = AlvinIndexMessengerStarter.indexMessengerListener;
		CoraClientFactoryImp coraClientFactory = (CoraClientFactoryImp) messageListener
				.getCoraClientFactory();

		// assert same as in alvinindexer.properties
		assertEquals(coraClientFactory.getAppTokenVerifierUrl(), "someAppTokenVerifierUrl");
		assertEquals(coraClientFactory.getBaseUrl(), "someBaseUrl");
	}

	@Test
	public void testMainMethodMessageParserFactorySetUpCorrectly() throws Exception {
		String args[] = new String[] { "alvinIndexer.properties" };
		AlvinIndexMessengerStarter.main(args);

		IndexMessengerListener messageListener = AlvinIndexMessengerStarter.indexMessengerListener;
		assertTrue(messageListener.getMessageParserFactory() instanceof AlvinMessageParserFactory);
	}

	@Test
	public void testMainMethodMessagingRoutingInfoSetUpCorrectly()
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, InstantiationException {

		String args[] = new String[] { "alvinIndexer.properties" };
		AlvinIndexMessengerStarter.main(args);

		IndexMessengerListener messageListener = AlvinIndexMessengerStarter.indexMessengerListener;
		AmqpMessageRoutingInfo messagingRoutingInfo = (AmqpMessageRoutingInfo) messageListener
				.getMessageRoutingInfo();
		// assert same as in alvinindexer.properties
		assertNotNull(messagingRoutingInfo);
		assertEquals(messagingRoutingInfo.hostname, "messaging.alvin-portal.org");
		assertEquals(messagingRoutingInfo.port, "5672");
		assertEquals(messagingRoutingInfo.virtualHost, "alvin");
		assertEquals(messagingRoutingInfo.exchange, "index");
		assertEquals(messagingRoutingInfo.routingKey, "#");

	}

	@Test
	public void testMainMethodCoraCredentialsSetUpCorrectly()
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, InstantiationException {

		String args[] = new String[] { "alvinIndexer.properties" };
		AlvinIndexMessengerStarter.main(args);

		IndexMessengerListener messageListener = AlvinIndexMessengerStarter.indexMessengerListener;
		CoraCredentials credentials = messageListener.getCredentials();

		// assert same as in alvinindexer.properties
		assertEquals(credentials.userId, "userIdForCora");
		assertEquals(credentials.appToken, "appTokenForCora");
	}

	@Test
	public void testErrorHandling() throws Exception {
		String args[] = new String[] { "someNoneExistingFile" };

		AlvinIndexMessengerStarter.main(args);

		assertEquals(loggerFactorySpy.getNoOfFatalLogMessagesUsingClassName(testedClassName), 1);
		Exception exception = loggerFactorySpy.getFatalLogErrorUsingClassNameAndNo(testedClassName,
				0);
		assertTrue(exception instanceof RuntimeException);
		assertEquals(exception.getMessage(), "inStream parameter is null");
		assertEquals(loggerFactorySpy.getFatalLogMessageUsingClassNameAndNo(testedClassName, 0),
				"Unable to start AlvinIndexMessengerStarter ");
	}

	@Test
	public void testErrorHandlingNoAppTokenVerifierUrl() throws Exception {

		String fileName = "propertiesForTestingMissingParameterApptokenUrl.properties";
		String propertyName = "appTokenVerifierUrl";
		testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	}

	private void testPropertiesErrorWhenPropertyIsMissing(String fileName, String propertyName) {
		String args[] = new String[] { fileName };

		AlvinIndexMessengerStarter.main(args);
		assertCorrectErrorForMissingProperty(propertyName);
	}

	@Test
	public void testErrorHandlingNoBaseUrl() throws Exception {
		String fileName = "propertiesForTestingMissingParameterBaseUrl.properties";
		String propertyName = "baseUrl";
		testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	}

	@Test
	public void testPropertiesErrorWhenHostnameIsMissing() {
		String propertyName = "messaging.hostname";
		String fileName = "propertiesForTestingMissingParameterHostname.properties";
		testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	}

	@Test
	public void testPropertiesErrorWhenPortIsMissing() {
		String fileName = "propertiesForTestingMissingParameterPort.properties";
		String propertyName = "messaging.port";
		testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	}

	@Test
	public void testPropertiesErrorWhenVirtualHostIsMissing() {
		String propertyName = "messaging.virtualHost";
		String fileName = "propertiesForTestingMissingParameterVirtualHost.properties";
		testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	}

	@Test
	public void testPropertiesErrorWhenExchangeIsMissing() {
		String propertyName = "messaging.exchange";
		String fileName = "propertiesForTestingMissingParameterExchange.properties";
		testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	}

	@Test
	public void testPropertiesErrorWhenRoutingKeyIsMissing() {
		String propertyName = "messaging.routingKey";
		String fileName = "propertiesForTestingMissingParameterRoutingKey.properties";
		testPropertiesErrorWhenPropertyIsMissing(fileName, propertyName);
	}

	private void assertCorrectErrorForMissingProperty(String propertyName) {
		assertEquals(loggerFactorySpy.getNoOfFatalLogMessagesUsingClassName(testedClassName), 1);
		Exception exception = loggerFactorySpy.getFatalLogErrorUsingClassNameAndNo(testedClassName,
				0);
		assertTrue(exception instanceof RuntimeException);
		assertEquals(exception.getMessage(),
				"Property with name " + propertyName + " not found in properties");
		assertEquals(loggerFactorySpy.getFatalLogMessageUsingClassNameAndNo(testedClassName, 0),
				"Unable to start AlvinIndexMessengerStarter ");
	}

}
