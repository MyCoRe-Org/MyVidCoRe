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
package org.mycore.vidconv.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class Executable {

    private final static ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final List<String> command;

    private Process process;

    private StreamConsumer outputConsumer;

    private StreamConsumer errorConsumer;

    public Executable(final List<String> command) {
        this.command = command;
    }

    public Executable(String... command) {
        this.command = new ArrayList<>(command.length);
        Arrays.stream(command).forEach(arg -> this.command.add(arg));
    }

    public List<String> command() {
        return command;
    }

    public Process run() throws InterruptedException, ExecutionException, IOException {
        final ProcessBuilder pb = new ProcessBuilder(command);

        process = pb.start();

        outputConsumer = new StreamConsumer(process.getInputStream());
        errorConsumer = new StreamConsumer(process.getErrorStream());

        new Thread(outputConsumer).start();
        new Thread(errorConsumer).start();

        return process;
    }

    public int runAndWait() throws InterruptedException, ExecutionException {
        Future<Integer> future = EXECUTOR.submit(new Callable<Integer>() {
            public Integer call() throws Exception {
                process = run();

                return process.waitFor();
            }
        });

        return future.get();
    }

    public StreamConsumer outputConsumer() {
        return outputConsumer;
    }

    public String output() {
        return outputConsumer.getStreamOutput();
    }

    public StreamConsumer errorConsumer() {
        return errorConsumer;
    }

    public String error() {
        return errorConsumer.getStreamOutput();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return command.stream().collect(Collectors.joining(" "));
    }
}
