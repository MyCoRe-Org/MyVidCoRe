import { Console } from "console";
import os from "os";
import fs from "fs";
import path from "path";
import pLimit from "p-limit";
import imagemin from "imagemin";
import imageminMozjpeg from "imagemin-mozjpeg";
import imageminPngquant from "imagemin-pngquant";
import imageminGiflossy from "imagemin-giflossy";
import imageminSvgo from "imagemin-svgo";

const logger = new Console(process.stdout, process.stderr);

const limit = pLimit(os.cpus().length);

const distDir = process.argv[2];

const humanFileSize = (size) => {
    const i = Math.floor(Math.log(size) / Math.log(1024));
    return (size / Math.pow(1024, i)).toFixed(2) * 1 + ["B", "kB", "MB", "GB", "TB"][i];
};

const walk = (dir, done) => {
    let results = [];
    fs.readdir(dir, (err, list) => {
        if (err) {
            return done(err);
        }

        let pending = list.length;
        if (!pending) {
            return done(null, results);
        }

        list.forEach((file) => {
            file = path.resolve(dir, file);
            fs.stat(file, (err, stat) => {
                if (stat && stat.isDirectory()) {
                    walk(file, (err, res) => {
                        results = results.concat(res);
                        if (!--pending) {
                            done(null, results);
                        }
                    });
                } else {
                    results.push(file);
                    if (!--pending) {
                        done(null, results);
                    }
                }
            });
        });
    });
};

if (!distDir) {
    logger.error("Need \"dist\" path!");
    process.exit(1);
}

walk(distDir, (err, files) => {
    if (err) {
        logger.error(err);
        return;
    }

    const images = files.filter((f) => /.*\.(gif|jpe?g|png|svg)$/.test(f));
    const promises = images.map((f) => {
        const fsize = fs.statSync(f).size;
        return limit(() => imagemin([f], {
            destination: path.resolve(f, ".."),
            plugins: [
                imageminMozjpeg({ quality: 70, dct: "float" }),
                imageminPngquant({ quality: [0.65, 0.8], speed: 1, strip: true }),
                imageminGiflossy({ lossy: 80, optimizationLevel: 3 }),
                imageminSvgo()
            ]
        }).then((done) => {
            if (done[0]) {
                const afsize = fs.statSync(done[0].destinationPath).size;
                logger.log("optimized " + done[0].destinationPath + " (" + humanFileSize(fsize) + " -> " + humanFileSize(afsize) + ").");
            }
        }).catch(logger.error));
    });

    (async () => {
        await Promise.all(promises);
    })();
});
