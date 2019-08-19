/*
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
package org.mycore.vidconv.frontend.filter;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.io.output.ProxyOutputStream;
import org.apache.logging.log4j.LogManager;

/**
 * Ignores IOException when writing to client OutputStream
 * @see <a href="https://stackoverflow.com/a/39006980">Stack Overflow</a>
 * 
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Provider
@Priority(Priorities.ENTITY_CODER)
public class IgnoreClientAbortInterceptor implements WriterInterceptor {

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.ws.rs.ext.WriterInterceptor#aroundWriteTo(javax.ws.rs.ext.
	 * WriterInterceptorContext)
	 */
	@Override
	public void aroundWriteTo(WriterInterceptorContext writerInterceptorContext)
			throws IOException, WebApplicationException {
		writerInterceptorContext
				.setOutputStream(new ClientAbortExceptionOutputStream(writerInterceptorContext.getOutputStream()));
		try {
			writerInterceptorContext.proceed();
		} catch (Exception e) {
			for (Throwable cause = e; cause != null; cause = cause.getCause()) {
				if (cause instanceof ClientAbortException) {
					LogManager.getLogger().info("Client closed response too early.");
					return;
				}
			}
			throw e;
		}
	}

	private static class ClientAbortExceptionOutputStream extends ProxyOutputStream {
		public ClientAbortExceptionOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		protected void handleIOException(IOException e) throws IOException {
			throw new ClientAbortException(e);
		}
	}

	@SuppressWarnings("serial")
	private static class ClientAbortException extends IOException {
		public ClientAbortException(IOException e) {
			super(e);
		}
	}

}
