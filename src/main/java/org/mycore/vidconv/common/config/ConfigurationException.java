/*
 * $Id$ 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 3
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */
package org.mycore.vidconv.common.config;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ConfigurationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new ConfigurationException with an error message
	 * 
	 * @param message
	 *            the error message for this exception
	 */
	public ConfigurationException(String message) {
		super(message);
	}

	/**
	 * Creates a new ConfigurationException with an error message and a
	 * reference to an exception thrown by an underlying system.
	 * 
	 * @param message
	 *            the error message for this exception
	 * @param cause
	 *            the exception that was thrown by an underlying system
	 */
	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}
