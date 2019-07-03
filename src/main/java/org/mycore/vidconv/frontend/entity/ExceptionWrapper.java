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
package org.mycore.vidconv.frontend.entity;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "exception")
public class ExceptionWrapper {

	private Exception ex;

	ExceptionWrapper() {
	}

	public ExceptionWrapper(Exception ex) {
		this.ex = ex;
	}

	@XmlAttribute(name = "type")
	public String getType() {
		return ex.getClass().getName();
	}

	@XmlElement(name = "message")
	public String getMessage() {
		return ex.getMessage();
	}

	@XmlElement(name = "stackTrace")
	public String getStackTrace() {
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
}
