# Timelapse Bot

## Start Parameters

`--debug-log` true if you want to see the debug logs

`--config-location` the path to the config.propertes file

`--convert` Starts only the video converter

`--frame-rate` 30 FPS by default. Only applies if convert argument is present

`--telegram-bot` true by default. If you want to enable the telegram bot

`--frame` true by default. If you want the preview window to be present

`--private` false by default

`--save-images` save images true by default


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