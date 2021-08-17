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

package se.uu.ub.cora.alvin.indexmessenger.parser;

import java.util.Map;

import se.uu.ub.cora.indexmessenger.parser.MessageParser;

public class MessageParserSpy implements MessageParser {

	public Map<String, String> headers;
	public String message;
	public boolean getParsedIdWasCalled = false;
	public boolean getParsedTypeWasCalled = false;
	public boolean createWorkOrder = true;

	@Override
	public void parseHeadersAndMessage(Map<String, String> headers, String message) {
		this.headers = headers;
		this.message = message;

	}

	@Override
	public boolean shouldWorkOrderBeCreatedForMessage() {
		return createWorkOrder;
	}

	@Override
	public String getRecordId() {
		getParsedIdWasCalled = true;
		return "someParsedIdFromMessageParserSpy";
	}

	@Override
	public String getRecordType() {
		getParsedTypeWasCalled = true;
		return "someParsedTypeFromMessageParserSpy";
	}

	@Override
	public String getModificationType() {
		// TODO Auto-generated method stub
		return null;
	}

}
