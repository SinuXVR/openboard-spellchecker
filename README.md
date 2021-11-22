# OpenBoard SpellChecker

Android Spell Checker service extracted from [OpenBoard](https://github.com/dslul/openboard) keyboard (aka AOSP LatinIME)

## Contribute

### How to create a dictionary
You can use [this tool](https://github.com/remi0s/aosp-dictionary-tools) to create a dictionary. You need a wordlist, as described [here](https://github.com/dslul/openboard/blob/master/dictionaries/sample.combined). The output .dict file must be put in [res/raw](https://github.com/dslul/openboard/tree/master/app/src/main/res/raw).

### APK Development

#### Linux

Install java:
```sh
sudo pacman -S jdk8-openjdk jre8-openjdk jre8-openjdk-headless
```

Install Android SDK:
```sh
sudo pacman -S snapd
sudo snap install androidsdk
```

Configure your SDK location in your `~/.bash_profile` or `~/.bashrc`:
```bash
export ANDROID_SDK_ROOT=~/snap/androidsdk/current/AndroidSDK/
```

Install the platform tools for your target android version:
```sh
androidsdk "platform-tools" "platforms;android-29"
```

Compile the project. This will install all dependencies, make sure to accept
licenses when prompted.

```sh
./gradlew assembleDebug
```

Connect your phone and install the debug APK
```sh
adb install ./app/build/outputs/apk/debug/app-debug.apk
```
## Credits

- [OpenBoard](https://github.com/dslul/openboard)
