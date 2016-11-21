# MyVidCoRe - My Video Converter [![Build Status](https://travis-ci.org/MyCoRe-Org/MyVidCoRe.svg?branch=master)](https://travis-ci.org/MyCoRe-Org/MyVidCoRe) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/18bc16e5e20c43b5b99112a4b81525f7)](https://www.codacy.com/app/adlerre/MyVidCoRe?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=MyCoRe-Org/MyVidCoRe&amp;utm_campaign=Badge_Grade)

Converts Videos from hotfolder to output directory with given settings. 

## Getting Started

This Project requires at least Java 8 and Maven 3 to build.

Build Project with this command:

    $ mvn clean install

You also need to install FFMpeg and FFProbe, please visit the [FFMpeg download page](https://ffmpeg.org/download.html).

## Overview

**Running Server**

Run Server with:

    # run with default settings
    $ java -jar myvidcore.jar
    # run with custom settings
    $ java -jar myvidcore.jar --watchDir convert/input --outputDir convert/output

Now you should be able to access the WEB-Interface on [http://localhost:8085/web](http://localhost:8085/web).
 
**Access Server**

REST-API:

    http://localhost:8085/application.wadl


WEB-Interface:

    http://localhost:8085/web

## Commandline Options

For a help and/or overview about default settings run:

    $ java -jar myvidcore.jar -h
    
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

* **-cd, --configDir**

  Set configuration dir

  