# Timelapse Bot

## Startup for Dummies

```
java -jar timelapse-1.x.x.jar [parameter1] [value1] [parameter2] [value2] [...]
```

### Examples

---

```
java -jar timelapse-1.0.5.jar --frame true --telegram-bot false
```
This starts the Timelapse Bot with a preview window and without the Telegram Bot enabled

---

```
java -jar timelapse-1.0.5.jar --convert --frame-rate 60
```
This converts the captured images to a video with a frame rate of 60

---

```
java -jar timelapse-1.0.5.jar --telegram-bot true --save-images false --frame false
```
This enables the Telegram Bot but disables the image capturing function


## Start Parameters

`--debug-log` true if you want to see the debug logs

`--config-location` the path to the config.propertes file

`--telegram-bot` true by default. If you want to enable the telegram bot

`--frame` true by default. If you want the preview window to be present

`--private` false by default. If private mode should be active on startup

`--save-images` save images true by default

`--convert` Starts only the video converter

`--frame-rate` 30 FPS by default. Only applies if convert argument is present


## Config Options

`telegram_date_format` the date format displayed in Telegram messages (For example `dd.MM.yyyy HH\:mm\:ss`) 

`api_token` the Telegram bot token

`frame_date_format` the date format displayed in the image preview window (For example `dd.MM.yyyy HH\:mm\:ss`) 

`webcam` the name of the webcam device (These names will be listed on startup)

`image_height` the height of the webcam resolution

`image_width` the width of the webcam resolution

`file_date_format` the date format for the image files (For example `yyyy-MM-dd-HH-mm-ss`)

`delay` the delay between images in milliseconds

`admin_user_id` the telegram user ID of the admin

`output_folder` the folder name for the images

`max_message_delay` the timeout for Telegram messages to be answered (For example if the bot is not running)

`database_path` the path to the database file containing the data related to the telegram users

`compression` the compression rate for the output images (0.0-1.0)

## Telegram Bot Commands

`/image` or `/bild` sends a live webcam image

`/id` returns the own telegram id

`/private` turns on private mode

`/public` turns off private mode

`/info` sends information about the whitelisted and blacklisted users

`/remove [telegram-id]` removes a user either from the blacklist or the whitelist

## Prerequisites

You need the Oracle JRE to run this program (OpenJDK is not working)

### On Raspberry PI

[Java 8 installation guide](http://wp.brodzinski.net/raspberry-pi-3b/install-latest-java-8-raspbian/)

``` sh 
sudo apt-get install dirmngr

echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | sudo tee /etc/apt/sources.list.d/webupd8team-java.list
echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | sudo tee -a /etc/apt/sources.list.d/webupd8team-java.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886

sudo apt-get update

sudo apt-get install oracle-java8-jdk
```