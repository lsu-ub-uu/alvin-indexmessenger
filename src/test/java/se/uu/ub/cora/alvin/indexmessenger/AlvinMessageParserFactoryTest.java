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

import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.alvin.indexmessenger.AlvinMessageParser;
import se.uu.ub.cora.alvin.indexmessenger.AlvinMessageParserFactory;
import se.uu.ub.cora.indexmessenger.log.LoggerFactorySpy;
import se.uu.ub.cora.indexmessenger.parser.MessageParser;
import se.uu.ub.cora.indexmessenger.parser.MessageParserFactory;
import se.uu.ub.cora.logger.LoggerProvider;

public class AlvinMessageParserFactoryTest {

	private LoggerFactorySpy loggerFactory;

	@BeforeMethod
	public void setUp() {
		loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);
	}

	@Test
	public void testFactor() {
		MessageParserFactory factory = new AlvinMessageParserFactory();
		MessageParser messageParser = factory.factor();
		assertTrue(messageParser instanceof AlvinMessageParser);
	}

}
