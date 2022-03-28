/*
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
package org.mycore.vidconv.frontend.ws;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.websockets.ProtocolHandler;
import org.glassfish.grizzly.websockets.SimpleWebSocket;
import org.glassfish.grizzly.websockets.WebSocketListener;
import org.mycore.vidconv.backend.service.ConverterService;
import org.mycore.vidconv.common.util.JsonUtils;
import org.mycore.vidconv.frontend.widget.Widget;
import org.mycore.vidconv.frontend.widget.WidgetManager;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ConverterWebSocket extends SimpleWebSocket {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Widget converterService;

    /**
     * @param protocolHandler
     * @param listeners
     */
    public ConverterWebSocket(ProtocolHandler protocolHandler, WebSocketListener[] listeners) {
        super(protocolHandler, listeners);
        this.converterService = WidgetManager.instance().get(ConverterService.WIDGET_NAME);
    }

    /* (non-Javadoc)
     * @see org.glassfish.grizzly.websockets.SimpleWebSocket#onMessage(java.lang.String)
     */
    @Override
    public void onMessage(String command) {
        try {
            if (command.startsWith("converter")) {
                List<String> params = Arrays.asList(command.replace("converter", "").trim().split(",|;|/"));
                send(JsonUtils.toJSON(params.isEmpty() ? converterService.status() : converterService.status(params),
                    true));
            }
        } catch (JAXBException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}
