# MyVidCoRe - My Video Converter [![Build Status](https://travis-ci.org/MyCoRe-Org/MyVidCoRe.svg?branch=master)](https://travis-ci.org/MyCoRe-Org/MyVidCoRe)

Converts Videos from hotfolder to output directory with given settings. 

## Getting Started

This Project requires at least Java 8 and Maven 3 to build.

Build Project with this command:

    $ mvn clean install

You also need to install FFMpeg and FFProbe, please visit the [FFMpeg download page](https://ffmpeg.org/download.html).

## Overview

**Running Server**

Run Server with (Maven):

    $ mvn exec:java

or:

    # run with default settings
    $ java -jar target/myvidcore-*-jar-with-dependencies.jar
    # run with custom settings
    $ java -jar target/myvidcore-*-jar-with-dependencies.jar --watchDir convert/input --outputDir convert/output

Now you should be able to access the WEB-Interface on [http://localhost:8085/web](http://localhost:8085/web).
 
**Access Server**

REST-API:

    http://localhost:8085/application.wadl


WEB-Interface:

    http://localhost:8085/web

## Commandline Options

For a help and/or overview about default settings run:

    $ java -jar target/myvidcore-*-jar-with-dependencies.jar -h
    
* **-h, --help**

  Print help message

* **--host**

  Set host to listen on

* **-p, --port**<br />
  *Default:* `8085`
  
  Set port to listen on
  
* **-ct, --converterThreads**

  Set the number of parallel converters
  
* **--watchDir**
  
  Set directory to watch for incomming videos
  
* **--outputDir**

  Set directory to output converted videos

  