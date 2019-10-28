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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import se.uu.ub.cora.indexmessenger.IndexMessageException;
import se.uu.ub.cora.indexmessenger.parser.MessageParser;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;

public class AlvinMessageParser implements MessageParser {
	private Logger logger = LoggerProvider.getLoggerForClass(AlvinMessageParser.class);
	private String parsedRecordId;
	private String parsedRecordType;
	private boolean workOrderShouldBeCreated = true;

	@Override
	public void parseHeadersAndMessage(Map<String, String> headers, String message) {
		try {
			tryToParseMessage(headers, message);
		} catch (IndexMessageException e) {
			handleError(e);
		}
	}

	private void tryToParseMessage(Map<String, String> headers, String message) {
		if (messageIsFromClassic(headers)) {
			extractRecordIdFromHeaders(headers);
			extractRecordTypeIdFromMessage(message);
		} else {
			preventWorkOrderFromBeeingCreated();
		}
	}

	private boolean messageIsFromClassic(Map<String, String> headers) {
		boolean fromClassic = true;
		String origin = headers.get("messageSentFrom");
		if (origin != null && "Cora".equals(origin))
			fromClassic = false;
		return fromClassic;
	}

	private void extractRecordIdFromHeaders(Map<String, String> headers) {
		if (!headers.containsKey("PID")) {
			throw IndexMessageException.withMessage("No pid found in header");
		}
		parsedRecordId = headers.get("PID");
	}

	private void extractRecordTypeIdFromMessage(String message) {
		Matcher matcher = createMatcherForRecordType(message);
		boolean matchFound = matcher.find();
		throwErrorIfRecordTypeNotFound(matchFound);
		parsedRecordType = matcher.group(1);
	}

	private Matcher createMatcherForRecordType(String message) {
		String patternString = "\"alvin\\.updates\\.(\\S*?)\"";
		Pattern pattern = Pattern.compile(patternString);
		return pattern.matcher(message);
	}

	private void throwErrorIfRecordTypeNotFound(boolean matchFound) {
		if (!matchFound) {
			throw IndexMessageException.withMessage("No recordType found");
		}
	}

	private void handleError(IndexMessageException e) {
		logger.logErrorUsingMessage(e.getMessage());
		preventWorkOrderFromBeeingCreated();
	}

	private void preventWorkOrderFromBeeingCreated() {
		workOrderShouldBeCreated = false;
	}

	@Override
	public String getParsedId() {
		return parsedRecordId;
	}

	@Override
	public String getParsedType() {
		return parsedRecordType;
	}

	@Override
	public boolean shouldWorkOrderBeCreatedForMessage() {
		return workOrderShouldBeCreated;
	}
}
