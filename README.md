# MyVidCoRe - My Video Converter [![Build Status](https://travis-ci.org/MyCoRe-Org/MyVidCoRe.svg?branch=master)](https://travis-ci.org/MyCoRe-Org/MyVidCoRe)

## Getting Started

This Project requires at least Java 8 and Maven 3 to build.

Build Project with this command:

    $ mvn clean install

You also need to install FFMpeg and FFProbe, please visit the [FFMpeg download page](https://ffmpeg.org/download.html).

** Running Server

Run Server with (Maven):

    $ mvn exec:java

or:

    $ java -jar target/myvidcore-*-jar-with-dependencies.jar

Now you should be able to access the WEB-Interface on [http://localhost:8085/web](http://localhost:8085/web).

## Overview

REST-API:

    http://localhost:8085/application.wadl


WEB-Interface:

    http://localhost:8085/web

For a help and/or overview about default settings run:

    $ java -jar target/myvidcore-*-jar-with-dependencies.jar -h

## Options
* **-h, --help**

  Print help message

* **--host**

  Set host to listen on

* **-p, --port**
  *Default:* `8085`
  
  Set port to listen on
  
* **-ct, --converterThreads**

  Set the number of parallel converters
  
* **--watchDir**
  
  Set directory to watch for incomming videos
  
* **--outputDir**

  Set directory to output converted videos

  