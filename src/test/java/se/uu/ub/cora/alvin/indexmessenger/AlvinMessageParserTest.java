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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.alvin.indexmessenger.AlvinMessageParser;
import se.uu.ub.cora.indexmessenger.log.LoggerFactorySpy;
import se.uu.ub.cora.indexmessenger.parser.MessageParser;
import se.uu.ub.cora.logger.LoggerProvider;

public class AlvinMessageParserTest {
	private String message;
	private Map<String, String> headers;
	private LoggerFactorySpy loggerFactory;
	private String testedClassname = "AlvinMessageParser";
	private MessageParser messageParser;

	@BeforeMethod
	public void setUp() {
		loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);

		headers = new HashMap<>();
		headers.put("__TypeId__", "epc.messaging.amqp.EPCFedoraMessage");
		headers.put("ACTION", "UPDATE");
		headers.put("PID", "alvin-place:1");

		message = "{\"pid\":\"alvin-place:1\",\"routingKey\":\"alvin.updates.place\","
				+ "\"action\":\"UPDATE\",\"dsId\":null,"
				+ "\"headers\":{\"ACTION\":\"UPDATE\",\"PID\":\"alvin-place:1\"}}";
		messageParser = new AlvinMessageParser();
	}

	@Test
	public void testInit() throws Exception {
		messageParser = new AlvinMessageParser();
	}

	@Test
	public void testMessageParserReturnsCorrectId() throws Exception {
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(messageParser.getParsedId(), headers.get("PID"));
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMessageParserReturnsCorrectOtherId() throws Exception {
		headers.put("PID", "alvin-person:2");

		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(messageParser.getParsedId(), headers.get("PID"));
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMessageParserReturnsCorrectType() throws Exception {
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(messageParser.getParsedType(), "place");
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMessageParserReturnsCorrectOtherType() throws Exception {
		message = "{\"pid\":\"alvin-person:22\",\"routingKey\":\"alvin.updates.person\","
				+ "\"action\":\"UPDATE\",\"dsId\":null,"
				+ "\"headers\":{\"ACTION\":\"UPDATE\",\"PID\":\"alvin-person:22\"}}";
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(messageParser.getParsedType(), "person");
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMessageParserNoPidWorkOrderShouldNotBeCreated() throws Exception {
		headers.remove("PID");

		messageParser.parseHeadersAndMessage(headers, message);
		assertFalse(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMessageParserLogsWhenNoPidWorkOrderShouldNotBeCreated() throws Exception {
		headers.remove("PID");

		assertEquals(loggerFactory.getNoOfErrorLogMessagesUsingClassName(testedClassname), 0);
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(loggerFactory.getNoOfErrorLogMessagesUsingClassName(testedClassname), 1);
		assertEquals(loggerFactory.getErrorLogMessageUsingClassNameAndNo(testedClassname, 0),
				"No pid found in header");
	}

	@Test
	public void testMessageParserNoCorrectRoutingKeyInMessage() throws Exception {
		message = "{\"pid\":\"alvin-person:22\",\"routingKey\":\"alvin.NOTupdates.person\","
				+ "\"action\":\"UPDATE\",\"dsId\":null,"
				+ "\"headers\":{\"ACTION\":\"UPDATE\",\"PID\":\"alvin-person:22\"}}";
		messageParser.parseHeadersAndMessage(headers, message);
		assertFalse(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testMessageParserLogsWhenNoCorrectRoutingKeyInMessage() throws Exception {
		message = "{\"pid\":\"alvin-person:22\",\"routingKey\":\"alvin.NOTupdates.person\","
				+ "\"action\":\"UPDATE\",\"dsId\":null,"
				+ "\"headers\":{\"ACTION\":\"UPDATE\",\"PID\":\"alvin-person:22\"}}";
		assertEquals(loggerFactory.getNoOfErrorLogMessagesUsingClassName(testedClassname), 0);
		messageParser.parseHeadersAndMessage(headers, message);
		assertEquals(loggerFactory.getNoOfErrorLogMessagesUsingClassName(testedClassname), 1);
		assertEquals(loggerFactory.getErrorLogMessageUsingClassNameAndNo(testedClassname, 0),
				"No recordType found");
	}

	@Test
	public void testDoNotIndexIfMessageIsFromCora() throws Exception {
		headers.put("messageSentFrom", "Cora");
		messageParser.parseHeadersAndMessage(headers, message);
		assertFalse(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

	@Test
	public void testIndexIfMessageSentFromIsInHeaderButNotFromCora() throws Exception {
		headers.put("messageSentFrom", "NOTFromCora");
		messageParser.parseHeadersAndMessage(headers, message);
		assertTrue(messageParser.shouldWorkOrderBeCreatedForMessage());
	}

}
