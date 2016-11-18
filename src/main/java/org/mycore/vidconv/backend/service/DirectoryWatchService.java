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
package org.mycore.vidconv.backend.service;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.common.event.Event;
import org.mycore.vidconv.common.event.EventManager;
import org.mycore.vidconv.frontend.widget.Widget;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class DirectoryWatchService extends Widget {

    public static final String WIDGET_NAME = "directoryWatcher";

    public static final String EVENT_ENTRY_CREATE = ENTRY_CREATE.name();

    public static final String EVENT_ENTRY_DELETE = ENTRY_DELETE.name();

    public static final String EVENT_ENTRY_MODIFIY = ENTRY_MODIFY.name();

    private static final Logger LOGGER = LogManager.getLogger(DirectoryWatchService.class);

    private static final long EVENT_DELAY = 30000;

    private static final EventManager EVENT_MANGER = EventManager.instance();

    private final Map<WatchKey, Path> keys = new ConcurrentHashMap<>();

    private final Map<Path, InitialEvent> events = new ConcurrentHashMap<>();

    private ExecutorService service;

    private WatchService ws;

    public DirectoryWatchService() throws IOException {
        this(FileSystems.getDefault());
    }

    private DirectoryWatchService(final FileSystem fs) throws IOException {
        super(WIDGET_NAME);

        final DirectoryWatchService that = this;
        service = Executors.newCachedThreadPool();
        ws = fs.newWatchService();

        service.submit(new Runnable() {
            @SuppressWarnings("serial")
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    WatchKey key;
                    try {
                        key = ws.poll(10, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | ClosedWatchServiceException e) {
                        break;
                    }
                    if (key != null) {
                        Path path = keys.get(key);
                        key.pollEvents().forEach(we -> {
                            WatchEvent<Path> event = cast(we);
                            WatchEvent.Kind<Path> kind = event.kind();
                            Path name = event.context();
                            Path child = path.resolve(name);

                            LOGGER.debug(String.format(Locale.ROOT, "%s: %s %s", kind.name(), path, child));

                            if (!events.containsKey(child)) {
                                events.put(child, new InitialEvent(kind, Instant.now()));
                            } else {
                                events.get(child).lastModified = Instant.now();
                            }

                            if (kind == ENTRY_CREATE) {
                                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                                    try {
                                        walk(child, keys, ws);
                                    } catch (IOException e) {
                                        LOGGER.error(e.getMessage(), e);
                                    }
                                }
                            }
                        });

                        if (!key.reset()) {
                            LOGGER.debug(String.format(Locale.ROOT, "%s is invalid", key));
                            keys.remove(key);
                            if (keys.isEmpty()) {
                                break;
                            }
                        }
                    }

                    // fire event after a given time without changes
                    events.forEach((p, ie) -> {
                        if (Duration.between(ie.lastModified, Instant.now()).toMillis() > EVENT_DELAY) {
                            EVENT_MANGER.fireEvent(new Event(ie.kind.name(), new HashMap<String, Object>() {
                                {
                                    put("path", p);
                                }
                            }, that.getClass()));
                            events.remove(p);
                        }
                    });
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.frontend.widget.Widget#status()
     */
    @Override
    public Status status() {
        final Status status = new Status();
        status.setActive(!service.isTerminated() && !service.isShutdown());
        status.setPaths(keys.values().stream().map(p -> p.toUri().getPath()).collect(Collectors.toList()));
        return status;
    }

    public void registerDirectory(Path dir) throws IOException {
        reg(dir, keys, ws);
    }

    private static void walk(Path root, final Map<WatchKey, Path> keys, final WatchService ws) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) throws IOException {
                reg(dir, keys, ws);
                return super.preVisitDirectory(dir, attrs);
            }
        });
    }

    private static void reg(Path dir, Map<WatchKey, Path> keys, WatchService ws) throws IOException {
        WatchKey key = dir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    @XmlRootElement
    static class Status {
        private boolean active;

        private List<String> paths;

        /**
         * @return the active
         */
        public boolean isActive() {
            return active;
        }

        /**
         * @param active the active to set
         */
        public void setActive(boolean active) {
            this.active = active;
        }

        /**
         * @return the paths
         */
        @XmlElementWrapper
        @XmlElement(name = "path")
        public List<String> getPaths() {
            return paths;
        }

        /**
         * @param paths the paths to set
         */
        public void setPaths(List<String> paths) {
            this.paths = paths;
        }
    }

    static class InitialEvent {
        protected final Kind<Path> kind;

        protected Instant lastModified;

        protected InitialEvent(final Kind<Path> kind, final Instant lastModified) {
            this.kind = kind;
            this.lastModified = lastModified;
        }
    }
}
