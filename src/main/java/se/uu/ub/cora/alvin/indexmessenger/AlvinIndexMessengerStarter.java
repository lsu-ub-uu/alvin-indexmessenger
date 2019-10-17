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

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import se.uu.ub.cora.indexmessenger.CoraCredentials;
import se.uu.ub.cora.indexmessenger.IndexMessengerListener;
import se.uu.ub.cora.indexmessenger.parser.MessageParserFactory;
import se.uu.ub.cora.javaclient.cora.CoraClientFactory;
import se.uu.ub.cora.javaclient.cora.CoraClientFactoryImp;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.messaging.AmqpMessageRoutingInfo;

public class AlvinIndexMessengerStarter {

	private static Logger logger = LoggerProvider
			.getLoggerForClass(AlvinIndexMessengerStarter.class);

	protected static IndexMessengerListener indexMessengerListener;

	private AlvinIndexMessengerStarter() {
	}

	public static void main(String[] args) {
		String propertiesFileName = getFilenameFromArgsOrDefault(args);
		logger.logInfoUsingMessage("AlvinIndexMessengerStarter starting...");

		try (InputStream input = AlvinIndexMessengerStarter.class.getClassLoader()
				.getResourceAsStream(propertiesFileName)) {

			Properties properties = loadProperites(input);
			createIndexMessengerListener(properties);
			logger.logInfoUsingMessage("AlvinIndexMessengerStarter started");

		} catch (Exception ex) {
			logger.logFatalUsingMessageAndException("Unable to start AlvinIndexMessengerStarter ",
					ex);
		}
	}

	private static String getFilenameFromArgsOrDefault(String[] args) {
		if (args.length > 0) {
			return args[0];
		}
		return "alvinIndexer.properties";
	}

	private static Properties loadProperites(InputStream input) throws IOException {
		Properties properties = new Properties();
		properties.load(input);
		return properties;
	}

	private static void createIndexMessengerListener(Properties properties) {
		CoraClientFactory coraClientFactory = createCoraClientFactoryFromProperties(properties);
		MessageParserFactory messageParserFactory = new AlvinMessageParserFactory();
		AmqpMessageRoutingInfo routingInfo = createMessageRoutingInfoFromProperties(properties);
		CoraCredentials credentials = createCoraCredentialsFromProperties(properties);

		indexMessengerListener = new IndexMessengerListener(coraClientFactory, messageParserFactory,
				routingInfo, credentials);

		String logM = "Listening for index messages from: {0} using port: {1}";
		String formattedLogMessage = MessageFormat.format(logM, routingInfo.hostname,
				routingInfo.port);
		logger.logInfoUsingMessage(formattedLogMessage);

	}

	private static CoraClientFactory createCoraClientFactoryFromProperties(Properties properties) {
		String baseUrl = extractPropertyThrowErrorIfNotFound(properties, "baseUrl");
		String appTokenVerifierUrl = extractPropertyThrowErrorIfNotFound(properties,
				"appTokenVerifierUrl");
		String logM2 = "Sending indexOrders to: {0} using appToken from: {1}";
		String formattedLogMessage2 = MessageFormat.format(logM2, baseUrl, appTokenVerifierUrl);
		logger.logInfoUsingMessage(formattedLogMessage2);
		return CoraClientFactoryImp.usingAppTokenVerifierUrlAndBaseUrl(appTokenVerifierUrl,
				baseUrl);
	}

	private static String extractPropertyThrowErrorIfNotFound(Properties properties,
			String propertyName) {
		throwErrorIfPropertyNameIsMissing(properties, propertyName);
		return properties.getProperty(propertyName);
	}

	private static void throwErrorIfPropertyNameIsMissing(Properties properties,
			String propertyName) {
		if (!properties.containsKey(propertyName)) {
			throw new RuntimeException(
					"Property with name " + propertyName + " not found in properties");
		}
	}

	private static AmqpMessageRoutingInfo createMessageRoutingInfoFromProperties(
			Properties properties) {
		String hostname = extractPropertyThrowErrorIfNotFound(properties, "messaging.hostname");
		String port = extractPropertyThrowErrorIfNotFound(properties, "messaging.port");
		String virtualHost = extractPropertyThrowErrorIfNotFound(properties,
				"messaging.virtualHost");
		String exchange = extractPropertyThrowErrorIfNotFound(properties, "messaging.exchange");
		String routingKey = extractPropertyThrowErrorIfNotFound(properties, "messaging.routingKey");
		return new AmqpMessageRoutingInfo(hostname, port, virtualHost, exchange, routingKey);
	}

	private static CoraCredentials createCoraCredentialsFromProperties(Properties properties) {
		String userId = properties.getProperty("cora.userId");
		String apptoken = properties.getProperty("cora.appToken");
		return new CoraCredentials(userId, apptoken);
	}
}
