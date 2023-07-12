# Timelapse

A program that creates timelapses.

**Features:**

- Conversion from picture series to video
- Image preview
- Telegram bot

## Useful Links

- [Downloads](https://github.com/henkelmax/timelapse/releases/)

## Start Parameters

 `-c,--config-location <path>`     The config path
 
 `-C,--convert`                    Start only the video converter
 
 `-d,--debug-log`                  Enables debug logs
 
 `-D,--database-path <path>`       The database path for the telegram bot
 
 `-F,--frame <true|false>`         Shows a frame with preview images
 
 `-f,--frame-rate <fps>`           The frame rate for the converter (30 FPS by default)
 
 `-h,--help`                       Displays possible arguments
 
 `-o,--output-folder <path>`       The image output folder path
 
 `-p,--private`                    Enables private mode
 
 `-s,--save-images <true|false>`   Save images
 
 `-t,--telegram-bot`               Enables the telegram bot

## Examples

```
java -jar timelapse.jar --frame true --telegram-bot
```
This starts the Timelapse Bot with a preview window and without the Telegram Bot enabled

---

```
java -jar timelapse.jar --convert --frame-rate 60
```
This converts the captured images to a video with a frame rate of 60

---

```
java -jar timelapse.jar --telegram-bot --save-images false --frame false
```
This enables the Telegram Bot but disables the image capturing function

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

`max_message_delay` the timeout for Telegram messages to be answered (For example if the bot is not running)

`compression` the compression rate for the output images (0.0-1.0)

## Telegram Bot Commands

`/image` sends a live webcam image

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