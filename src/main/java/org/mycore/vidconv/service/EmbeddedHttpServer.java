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
package org.mycore.vidconv.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.mycore.vidconv.widget.Widget;

import com.sun.net.httpserver.HttpServer;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@SuppressWarnings("restriction")
public class EmbeddedHttpServer extends Widget {

    public static final String WIDGET_NAME = "embeddedHttpServer";

    public static final String EVENT_START = "startHttpServer";

    public static final String EVENT_STOP = "stopHttpServer";

    private static final Logger LOGGER = LogManager.getLogger(EmbeddedHttpServer.class);

    private HttpServer httpServer;

    private String host;

    private Integer port = 8085;

    public EmbeddedHttpServer(final String host, final Integer port) {
        super(WIDGET_NAME);

        this.host = host;
        if (port != null && port.intValue() > 0)
            this.port = port;
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Embedded HTTP Server...");
        if (httpServer == null) {
            httpServer = createHttpServer();
            LOGGER.info(
                    String.format(
                            "Jersey Application Server started with WADL available at " + "%sapplication.wadl",
                            getURI()));
        }
        httpServer.start();
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping Embedded HTTP Server...");
        httpServer.stop(0);
    }

    private HttpServer createHttpServer()
            throws IOException, IllegalArgumentException, UriBuilderException, URISyntaxException {
        ResourceConfig resourceConfig = new ResourceConfig().packages("org.mycore.vidconv.resource")
                .register(MoxyJsonFeature.class);
        return JdkHttpServerFactory.createHttpServer(getURI(), resourceConfig, false);
    }

    private URI getURI() throws IllegalArgumentException, UriBuilderException, URISyntaxException {
        return UriBuilder.fromUri(new URI("http://" + getHostName() + "/")).port(this.port.intValue()).build();
    }

    private String getHostName() {
        if (this.host != null && !this.host.isEmpty())
            return this.host;

        String hostName = "localhost";
        try {
            hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            LOGGER.error(e);
        }

        return hostName;
    }
}
