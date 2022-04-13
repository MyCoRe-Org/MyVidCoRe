# MyVidCoRe - My Video Converter [![Build](https://github.com/MyCoRe-Org/MyVidCoRe/actions/workflows/ci.yml/badge.svg)](https://github.com/MyCoRe-Org/MyVidCoRe/actions/workflows/ci.yml) [![Codacy Badge](https://app.codacy.com/project/badge/Grade/0743cca157aa49df87db3f5b9b71459c)](https://www.codacy.com/gh/MyCoRe-Org/MyVidCoRe/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=MyCoRe-Org/MyVidCoRe&amp;utm_campaign=Badge_Grade)

Converts Videos from hotfolder to output directory with given settings. 

## Getting Started

This Project requires at least Java 11 and Maven 3 to build.

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

Now you should be able to access the WEB-Interface on [http://localhost:8085/](http://localhost:8085/).
 
**Access Server**

REST-API:

    http://localhost:8085/application.wadl


WEB-Interface:

    http://localhost:8085/

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
  
* **--tempDir**

  Set directory for temporary files

* **-cd, --configDir**

  Set configuration dir

## Configure Subtitle Plugin

 To enable Subtitle Plugin you must do some basic settings in your config.properties like below.
 
    # Vosk Model(s) root path
    VoskExtractor.modelPath=/usr/local/vosk/
    # Model for each language
    VoskExtractor.model.de=vosk-model-de
    VoskExtractor.model.en=vosk-model-en-us
 
 Advanced basic options:
  
    # the count of words to guess the audio language
    VoskExtractor.guessMaxWords=200 
    # language guessing concurrent (default is true)
    VoskExtractor.guessConcurrent=false
  
 Advanced guessing options:

    # set guess words for language
    VoskExtractor.model.de.guessMap=ich,du,er,sie,es,wir,ihr,...
 