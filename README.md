# MusicBot

**MusicBot** is a Discord music bot written in Java that brings the joy of music to your Discord server. With support for YouTube links, playlists, volume control, and looping, it's the perfect addition to enhance your server's audio experience.

## Features

- Play music from YouTube links and playlists.
- Search YouTube for your favorite songs.
- Control the playback with commands like play, pause, seek, skip, and more.
- Adjust the volume to your liking using the volume command.
- Loop your favorite tracks with the repeat command.
- Browse history of played tracks with the history command.
- View the queue and track information with queue and information commands.

## Getting Started

### Prerequisites

Before you can run the MusicBot, you'll need the following:

- Java 8 or higher
- [Maven](https://maven.apache.org/download.cgi) for dependency management
- A Discord bot token (you can [create a bot on the Discord Developer Portal](https://discord.com/developers/applications))

### Installation

1. Clone the repository to your local machine:

   ```
   git clone https://github.com/oekiaby0/MusicBot.git
   ```

2. Navigate to the project directory:

   ```
   cd MusicBot
   ```

3. Open the `Main.java` file and add your Discord bot token:

   ```
   token = "INSERT BOT TOKEN HERE"
   ```

5. Build the project using Maven:

   ```
   mvn clean install
   ```

6. Run the bot:

   ```
   java -jar target/MusicBot-1.0-SNAPSHOT.jar
   ```

### Usage

Once the bot is running and connected to your Discord server, you can use the following commands (prefix each command with `<`):

- `play` (or `p`): Add a video/audio link to the queue. If no argument is provided, it will function like the pause command.
- `pause`: Toggle the audio player's pause state.
- `search` (or `s`): Search YouTube for a song.
- `seek`: Skip to a specific time in the currently playing track.
- `skip` (or `s`): Skip the first item in the queue.
- `volume` (or `vol` or `v`): Set the audio player's volume. If no argument is provided, it will show the current volume level.
- `disconnect` (or `dc`): Disconnect the bot from the voice channel.
- `repeat` (or `loop`): Toggle repeat mode.
- `queue` (or `q`): List the queue of songs.
- `information` (or `info`): Show information about the currently playing track.
- `history` (or `h`): Show the queue history.

Enjoy listening to your favorite music with MusicBot!

## Contributing

Contributions to MusicBot are welcome. Feel free to fork the repository and submit pull requests for improvements or bug fixes.

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

MusicBot relies on the following libraries:

- [Discord JDA](https://github.com/discord-jda/JDA)
- [LavaPlayer](https://github.com/sedmelluq/lavaplayer)

Special thanks to the developers of these libraries for making this project possible.
