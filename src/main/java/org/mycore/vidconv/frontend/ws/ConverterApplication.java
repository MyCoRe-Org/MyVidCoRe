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

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.websockets.ProtocolHandler;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.grizzly.websockets.WebSocketListener;
import org.mycore.vidconv.backend.service.ConverterService.ConverterJob;
import org.mycore.vidconv.common.event.Event;
import org.mycore.vidconv.common.event.EventManager;
import org.mycore.vidconv.common.event.Listener;
import org.mycore.vidconv.common.event.annotation.AutoExecutable;
import org.mycore.vidconv.common.event.annotation.Shutdown;
import org.mycore.vidconv.common.event.annotation.Startup;
import org.mycore.vidconv.common.util.JsonUtils;
import org.mycore.vidconv.frontend.entity.ConverterWrapper;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@AutoExecutable(name = "Converter Web Socket")
public class ConverterApplication extends WebSocketApplication implements Listener {

    private static final Logger LOGGER = LogManager.getLogger();

    private static ConverterApplication app;

    @Startup
    protected static void register() {
        app = new ConverterApplication();
        WebSocketEngine.getEngine().register("", "/converter", app);
    }

    @Shutdown
    protected static void unregister() {
        WebSocketEngine.getEngine().unregister(app);
    }

    public ConverterApplication() {
        EventManager.instance().addListener(this);
    }

    /* (non-Javadoc)
     * @see org.glassfish.grizzly.websockets.WebSocketApplication#createSocket(org.glassfish.grizzly.websockets.ProtocolHandler, org.glassfish.grizzly.http.HttpRequestPacket, org.glassfish.grizzly.websockets.WebSocketListener[])
     */
    @Override
    public WebSocket createSocket(ProtocolHandler handler, HttpRequestPacket requestPacket,
        WebSocketListener... listeners) {
        return new ConverterWebSocket(handler, listeners);
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.common.event.Listener#handleEvent(org.mycore.vidconv.common.event.Event)
     */
    @Override
    public void handleEvent(Event<?> event) throws Exception {
        if (!event.isInternal()) {
            this.getWebSockets().parallelStream().filter(WebSocket::isConnected)
                .forEach(ws -> {
                    try {
                        Event<?> ev = event;
                        if (event.getObject().getClass().equals(ConverterJob.class)) {
                            ConverterJob job = (ConverterJob) event.getObject();
                            ev = new Event<ConverterWrapper>(event.getType(),
                                new ConverterWrapper(job.id(), job).basicCopy(), event.getSource()).setInternal(false);
                        }

                        ws.send(JsonUtils.toJSON(ev, true));
                    } catch (JAXBException | IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                });
        }
    }

}
