#!/usr/bin/env node
const yargs = require("yargs");
const cheerio = require("cheerio");
const https = require("https");

const parse2bool = (str) => {
    return str.toLowerCase() === "yes";
};

const getColText = ($, row, i) => {
    var re = new RegExp(String.fromCharCode(160), "g");
    return $(row.children().get(i)).text().trim().replace(re, " ");
};

const findDevice = (devices, name) => {
    return devices.find((dev) => name === dev.name);
};

const parseNVEncRow = ($, row, devices) => {
    if (row.children().length === 16 && !row.children().first().hasClass("warning")) {
        const name = getColText($, row, 0);

        let dev = findDevice(devices, name);
        if (!dev) {
            dev = {
                name: name,
                family: getColText($, row, 1),
                chip: getColText($, row, 2)
            };
            devices.push(dev);
        }

        dev.numChips = dev.numChips || parseInt(getColText($, row, 4), 10);
        dev.numEncoder = parseInt(getColText($, row, 5), 10);
        dev.maxStreams = getColText($, row, 6).indexOf("Unrestricted") === -1 ? -1 : parseInt(getColText($, row, 6), 10);
        dev.encoders = {
            "h264-420": parse2bool(getColText($, row, 7)),
            "h264-444": parse2bool(getColText($, row, 8)),
            "h264": parse2bool(getColText($, row, 9)),
            "h265-4k-420": parse2bool(getColText($, row, 10)),
            "h265-4k-444": parse2bool(getColText($, row, 11)),
            "h265-4k": parse2bool(getColText($, row, 12)),
            "h265-8k": parse2bool(getColText($, row, 13))
        };
    }
};

const parseNVEncTable = ($, devices, complete = false) => {
    devices = devices || [];
    const tables = complete ? $("div > a[name='Encoder']").parent().parent().find("div div.collapse > table") : $("div > a[name='Encoder'] ~ table");

    tables.each((_i, el) => {
        const table = $(el);

        if (table.first().text().indexOf("BOARD") !== -1) {
            table.find("tbody tr").each((_i, el) => {
                const row = $(el);
                parseNVEncRow($, row, devices);
            });
        } else {
            throw "Coludn't parse encode table.";
        }
    });

    return devices;
};

const parseNVDecRow = ($, row, devices) => {
    if (row.children().length === 21 && !row.children().first().hasClass("warning")) {
        const name = getColText($, row, 0);

        let dev = findDevice(devices, name);
        if (!dev) {
            dev = {
                name: name,
                family: getColText($, row, 1),
                chip: getColText($, row, 2)
            };
            devices.push(dev);
        }


        dev.numChips = dev.numChips || parseInt(getColText($, row, 4), 10);
        dev.numDecoder = parseInt(getColText($, row, 5), 10);
        dev.decoders = {
            "mpeg1": parse2bool(getColText($, row, 7)),
            "mpeg2": parse2bool(getColText($, row, 8)),
            "vc1": parse2bool(getColText($, row, 9)),
            "vp8": parse2bool(getColText($, row, 10)),
            "vp9": parse2bool(getColText($, row, 11)),
            "h264": parse2bool(getColText($, row, 14)),
            "h265": parse2bool(getColText($, row, 15)),
        };
    }
};

const parseNVDecTable = ($, devices, complete = false) => {
    devices = devices || [];
    const tables = complete ? $("div > a[name='Decoder'] ~ div div.collapse > table") : $("div > a[name='Decoder'] ~ table");

    tables.each((_i, el) => {
        const table = $(el);

        if (table.first().text().indexOf("BOARD") !== -1) {
            table.find("tbody tr").each((_i, el) => {
                const row = $(el);
                parseNVDecRow($, row, devices);
            });
        } else {
            throw "Coludn't parse decoder table.";
        }
    });

    return devices;
};

const argv = yargs
    .option("complete", {
        alias: "c",
        type: "boolean",
        description: "Parse complete list."
    }).option("formated", {
        alias: "f",
        type: "boolean",
        description: "Output formated json."
    }).argv;

const req = https.request("https://developer.nvidia.com/video-encode-decode-gpu-support-matrix", (res) => {
    let data = "";
    res.setEncoding("utf-8");
    res.on("data", function (chunk) {
        data += chunk;
    });
    res.on("end", function () {
        const $ = cheerio.load(data);
        let devices = [];

        devices = parseNVEncTable($, devices, argv.complete || false);
        devices = parseNVDecTable($, devices, argv.complete || false);

        console.log(JSON.stringify(devices, null, argv.formated && 4));
    });
}).on("error", (e) => console.error(e));
req.end();
