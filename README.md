# Minos
Wrapper for a wrapper. A small library to help myself quickly set-up Discord bots using Discord4J.
Don't expect to be blown-out by what you see here; that's somewhere else!

## Building
To build minos manually (why would you? nevermind...), follow these steps:

1. Prerequisites
    - [Java 25 JDK](https://adoptium.net/temurin/releases) installed.
    - [Maven](https://maven.apache.org/download.cgi) installed:
        - **Windows**: Download the ZIP from the link above, extract, then add `bin/` to your `PATH`.
        - **macOS (Homebrew)**: `brew install maven`.
        - **Linux (APT)**: `apt install maven`.
    - [Git](https://git-scm.com/downloads) installed:
        - **Windows**: Download the installer from [git-scm.com](https://git-scm.com/downloads) and follow the setup.
        - **macOS (Homebrew)**: `brew install git`.
        - **Linux (APT)**: `apt install git`.
2. Clone the repository.
    - `git clone https://github.com/lucasluqui/minos.git`
3. Validate all Maven dependencies.
    - `mvn validate`
4. Build the package using Maven.
    - `mvn clean package`